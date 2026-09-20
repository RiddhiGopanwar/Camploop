package com.camploop.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;

/**
 * Verifies the JWT that Supabase Auth issues on signup/login.
 *
 * The frontend calls Supabase directly for auth, then sends the access token on every
 * request to this backend as "Authorization: Bearer <token>". This filter checks the
 * token's signature against the project's JWT secret and, if valid, stores a CurrentUser
 * (id + email from the token's claims) as a request attribute for controllers to read.
 *
 * Requests with no/invalid token are simply left unauthenticated — SessionAuth-equivalent
 * checks in each controller (see AuthContext) are what actually reject them with 401,
 * exactly like the old session-based version did.
 */
@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTR = "camploop.currentUser";

    private final SupabaseProperties supabaseProperties;

    @Autowired
    public SupabaseJwtFilter(SupabaseProperties supabaseProperties) {
        this.supabaseProperties = supabaseProperties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                SecretKey key = new SecretKeySpec(
                        supabaseProperties.getJwtSecret().getBytes(), "HmacSHA256");

                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String email = claims.get("email", String.class);

                if (userId != null) {
                    request.setAttribute(REQUEST_ATTR, new CurrentUser(userId, email));
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid/expired token — leave request unauthenticated, controllers will 401.
            }
        }

        filterChain.doFilter(request, response);
    }
}
