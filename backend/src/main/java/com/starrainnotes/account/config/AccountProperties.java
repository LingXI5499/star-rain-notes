package com.starrainnotes.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.account")
public class AccountProperties {

    private String superAdminEmail;
    private Mail mail = new Mail();

    public String getSuperAdminEmail() { return superAdminEmail; }
    public void setSuperAdminEmail(String v) { this.superAdminEmail = v; }
    public Mail getMail() { return mail; }
    public void setMail(Mail m) { this.mail = m; }

    public static class Mail {
        private String host;
        private int port = 465;
        private String username;
        private String authCode;
        private String from;
        private boolean sslEnabled = true;
        private String baseUrl = "http://localhost:24680";

        public String getHost() { return host; }
        public void setHost(String v) { this.host = v; }
        public int getPort() { return port; }
        public void setPort(int v) { this.port = v; }
        public String getUsername() { return username; }
        public void setUsername(String v) { this.username = v; }
        public String getAuthCode() { return authCode; }
        public void setAuthCode(String v) { this.authCode = v; }
        public String getFrom() { return from; }
        public void setFrom(String v) { this.from = v; }
        public boolean isSslEnabled() { return sslEnabled; }
        public void setSslEnabled(boolean v) { this.sslEnabled = v; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
    }
}