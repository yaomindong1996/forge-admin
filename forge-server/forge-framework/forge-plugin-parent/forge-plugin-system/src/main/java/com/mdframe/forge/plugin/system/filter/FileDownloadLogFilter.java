package com.mdframe.forge.plugin.system.filter;

import com.mdframe.forge.plugin.system.service.impl.SysFileDownloadLogServiceImpl;
import com.mdframe.forge.plugin.system.support.FileDownloadLogSupport;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * 在响应写出附件头之后记一笔下载。文件预览不带下载意图头，不会进入记录。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class FileDownloadLogFilter extends OncePerRequestFilter {

    private final SysFileDownloadLogServiceImpl downloadLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 请求结束后租户拦截器会清上下文，身份要在进入业务前先留下。
        LoginUser loginUser = SessionHelper.getLoginUser();
        DispositionResponse wrapped = new DispositionResponse(response);
        chain.doFilter(request, wrapped);
        String disposition = wrapped.disposition();
        if (disposition == null) {
            disposition = wrapped.getHeader("Content-Disposition");
        }
        if (!FileDownloadLogSupport.shouldRecord(request.getRequestURI(), disposition,
                request.getHeader(FileDownloadLogSupport.INTENT_HEADER), wrapped.getStatus())) {
            return;
        }
        downloadLogService.record(request, disposition, wrapped.getHeader("Content-Length"), loginUser);
    }

    private static final class DispositionResponse extends HttpServletResponseWrapper {

        private String disposition;

        private DispositionResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setHeader(String name, String value) {
            capture(name, value);
            super.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            capture(name, value);
            super.addHeader(name, value);
        }

        private void capture(String name, String value) {
            if (name != null && "content-disposition".equals(name.toLowerCase(Locale.ROOT))) {
                disposition = value;
            }
        }

        private String disposition() {
            return disposition;
        }
    }
}
