package com.camploop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    private String url;
    private String anonKey;
    private String jwtSecret;
    private String serviceRoleKey;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getAnonKey() { return anonKey; }
    public void setAnonKey(String anonKey) { this.anonKey = anonKey; }
    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public String getServiceRoleKey() { return serviceRoleKey; }
    public void setServiceRoleKey(String serviceRoleKey) { this.serviceRoleKey = serviceRoleKey; }
}
