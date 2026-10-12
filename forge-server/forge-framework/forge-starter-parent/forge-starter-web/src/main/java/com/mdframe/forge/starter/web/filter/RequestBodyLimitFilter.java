package com.mdframe.forge.starter.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Tomcat 的 maxPostSize 只限制表单；JSON/分块请求必须在读取时另外计数，不能提前无限缓存。 */
public final class RequestBodyLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    public RequestBodyLimitFilter(long maxBytes) {
        if (maxBytes < -1) {
            throw new IllegalArgumentException("请求体限制必须为非负数或 -1");
        }
        this.maxBytes = maxBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (maxBytes < 0) {
            chain.doFilter(request, response);
            return;
        }
        if (request.getContentLengthLong() > maxBytes) {
            reject(response);
            return;
        }
        try {
            chain.doFilter(new LimitedRequest(request, maxBytes), response);
        } catch (RequestBodyTooLargeException ex) {
            reject(response);
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":413,\"message\":\"请求体超过大小限制\"}");
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {

        private final long limit;
        private ServletInputStream input;

        private LimitedRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (input == null) {
                input = new LimitedInputStream(super.getInputStream(), limit);
            }
            return input;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            return new BufferedReader(encoding == null
                    ? new InputStreamReader(getInputStream(), StandardCharsets.ISO_8859_1)
                    : new InputStreamReader(getInputStream(), encoding));
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long limit;
        private long count;

        private LimitedInputStream(ServletInputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                addBytes(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            // 最多读取到超限的第一个字节；不因调用方 readAllBytes 而一次分配巨量内存。
            int bounded = (int) Math.min(length, Math.min(Integer.MAX_VALUE, Math.max(1, limit - count)));
            int read = delegate.read(buffer, offset, bounded);
            if (read > 0) {
                addBytes(read);
            }
            return read;
        }

        private void addBytes(int amount) throws RequestBodyTooLargeException {
            count += amount;
            if (count > limit) {
                throw new RequestBodyTooLargeException();
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
