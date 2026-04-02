package com.mdframe.forge.starter.message.channel;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.message.config.EmailConfigProvider;
import com.mdframe.forge.starter.message.config.EmailConfigProvider.EmailConfig;
import lombok.extern.slf4j.Slf4j;
import org.dromara.email.jakarta.api.MailClient;
import org.dromara.email.jakarta.comm.config.MailSmtpConfig;
import org.dromara.email.jakarta.comm.entity.MailMessage;
import org.dromara.email.jakarta.core.factory.MailFactory;

import java.util.List;
import java.util.Map;

@Slf4j
public class EmailMessageChannel implements MessageChannel {

    private MailClient mailClient;
    private EmailConfig config;
    private final EmailConfigProvider configProvider;

    public EmailMessageChannel(EmailConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public ChannelType key() {
        return ChannelType.EMAIL;
    }

    @Override
    public void init(Map<String, String> configMap) {
        if (configProvider != null) {
            try {
                EmailConfig cfg = configProvider.getEmailConfig();
                if (cfg != null) {
                    init(cfg);
                    log.info("自动加载邮件配置成功: configId={}", cfg.getConfigId());
                } else {
                    log.info("未找到已启用的邮件配置");
                }
            } catch (Exception e) {
                log.warn("自动加载邮件配置失败: {}", e.getMessage());
            }
        }
    }

    public void init(EmailConfig config) {
        this.config = config;
        try {
            MailSmtpConfig smtpConfig = MailSmtpConfig.builder()
                    .smtpServer(config.getSmtpServer())
                    .port(String.valueOf(config.getPort()))
                    .username(config.getUsername())
                    .password(config.getPassword())
                    .fromAddress(config.getFromAddress())
                    .isSSL(config.getIsSsl() != null && config.getIsSsl() ? "true" : "false")
                    .isAuth(config.getIsAuth() != null && config.getIsAuth() ? "true" : "false")
                    .retryInterval(config.getRetryInterval() != null ? config.getRetryInterval() : 5)
                    .maxRetries(config.getMaxRetries() != null ? config.getMaxRetries() : 1)
                    .build();

            MailFactory.put(config.getConfigId(), smtpConfig);
            this.mailClient = MailFactory.createMailClient(config.getConfigId());
            log.info("邮件通道初始化成功: configId={}", config.getConfigId());
        } catch (Exception e) {
            log.error("邮件通道初始化失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public SendResult send(SendRequest request) {
        if (mailClient == null) {
            return SendResult.fail("邮件通道未初始化");
        }

        try {
            List<String> emails = request.getEmailList();
            if (emails == null || emails.isEmpty()) {
                return SendResult.fail("未找到有效的邮箱地址");
            }

            MailMessage.MailsBuilder builder = MailMessage.Builder()
                    .mailAddress(emails)
                    .title(request.title != null ? request.title : "通知");

            if (StrUtil.isNotBlank(request.content)) {
                builder.body(request.content);
            }

            if (request.params != null) {
                if (request.params.containsKey("html")) {
                    builder.html(String.valueOf(request.params.get("html")));
                }
                if (request.params.containsKey("htmlValues")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> htmlValues = (Map<String, String>) request.params.get("htmlValues");
                    builder.htmlValues(htmlValues);
                }
                if (request.params.containsKey("cc")) {
                    @SuppressWarnings("unchecked")
                    List<String> cc = (List<String>) request.params.get("cc");
                    builder.cc(cc);
                }
                if (request.params.containsKey("bcc")) {
                    @SuppressWarnings("unchecked")
                    List<String> bcc = (List<String>) request.params.get("bcc");
                    builder.bcc(bcc);
                }
                if (request.params.containsKey("files")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> files = (Map<String, String>) request.params.get("files");
                    builder.files(files);
                }
            }

            MailMessage message = builder.build();
            mailClient.send(message);

            return SendResult.ok("EMAIL-" + System.currentTimeMillis());
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage(), e);
            return SendResult.fail("邮件发送异常: " + e.getMessage());
        }
    }
}
