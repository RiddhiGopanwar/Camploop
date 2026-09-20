package com.camploop.config;

import com.camploop.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Small helper controllers use to require a logged-in Supabase user for an endpoint. */
@Component
public class AuthContext {

    public CurrentUser require(HttpServletRequest request) {
        Object attr = request.getAttribute(SupabaseJwtFilter.REQUEST_ATTR);
        if (attr == null) {
            throw new ApiException("Please log in to continue", HttpStatus.UNAUTHORIZED);
        }
        return (CurrentUser) attr;
    }

    public CurrentUser optional(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute(SupabaseJwtFilter.REQUEST_ATTR);
    }
}
