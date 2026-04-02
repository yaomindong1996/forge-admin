package com.mdframe.forge.starter.message.config;

public interface EmailConfigProvider {

    EmailConfig getEmailConfig();

    class EmailConfig {
        private String configId;
        private String smtpServer;
        private Integer port;
        private String username;
        private String password;
        private String fromAddress;
        private String fromName;
        private Boolean isSsl;
        private Boolean isAuth;
        private Integer retryInterval;
        private Integer maxRetries;
        private Integer status;

        public String getConfigId() { return configId; }
        public void setConfigId(String configId) { this.configId = configId; }
        public String getSmtpServer() { return smtpServer; }
        public void setSmtpServer(String smtpServer) { this.smtpServer = smtpServer; }
        public Integer getPort() { return port; }
        public void setPort(Integer port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }
        public Boolean getIsSsl() { return isSsl; }
        public void setIsSsl(Boolean isSsl) { this.isSsl = isSsl; }
        public Boolean getIsAuth() { return isAuth; }
        public void setIsAuth(Boolean isAuth) { this.isAuth = isAuth; }
        public Integer getRetryInterval() { return retryInterval; }
        public void setRetryInterval(Integer retryInterval) { this.retryInterval = retryInterval; }
        public Integer getMaxRetries() { return maxRetries; }
        public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}