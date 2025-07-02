package com.example.crypto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "proxy")
public class ProxyConfig {
    private boolean enabled;
    private String type;
    private String host;
    private int port;
    private String wsType;
    private int wsPort;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getWsType() { return wsType; }
    public void setWsType(String wsType) { this.wsType = wsType; }
    public int getWsPort() { return wsPort; }
    public void setWsPort(int wsPort) { this.wsPort = wsPort; }
} 