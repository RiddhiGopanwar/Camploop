package com.camploop.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    private final SupabaseJwtFilter supabaseJwtFilter;

    @Autowired
    public FilterConfig(SupabaseJwtFilter supabaseJwtFilter) {
        this.supabaseJwtFilter = supabaseJwtFilter;
    }

    @Bean
    public FilterRegistrationBean<SupabaseJwtFilter> jwtFilterRegistration() {
        FilterRegistrationBean<SupabaseJwtFilter> registration = new FilterRegistrationBean<>(supabaseJwtFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
