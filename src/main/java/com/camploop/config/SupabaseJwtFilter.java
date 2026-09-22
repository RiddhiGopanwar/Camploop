package com.camploop.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.security.AlgorithmParameters;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTR = "camploop.currentUser";

    private final SupabaseProperties supabaseProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SupabaseJwtFilter(SupabaseProperties supabaseProperties) {
        this.supabaseProperties = supabaseProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            try {
                PublicKey publicKey = getSupabasePublicKey(token);

                Claims claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String email = claims.get("email", String.class);

                if (userId != null) {
                    request.setAttribute(
                            REQUEST_ATTR,
                            new CurrentUser(userId, email)
                    );
                }

            } catch (Exception ex) {
                // Invalid token — leave request unauthenticated.
            }
        }

        filterChain.doFilter(request, response);
    }

    private PublicKey getSupabasePublicKey(String token) throws Exception {

        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT");
        }

        String headerJson = new String(
                Base64.getUrlDecoder().decode(parts[0])
        );

        JsonNode header = objectMapper.readTree(headerJson);

        String kid = header.get("kid").asText();
        String alg = header.get("alg").asText();

        if (!"ES256".equals(alg)) {
            throw new IllegalArgumentException(
                    "Unsupported JWT algorithm: " + alg
            );
        }

        String jwksUrl =
                supabaseProperties.getUrl()
                        + "/auth/v1/.well-known/jwks.json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwksUrl))
                .GET()
                .build();

        HttpResponse<String> response =
                HttpClient.newHttpClient().send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Failed to fetch Supabase JWKS"
            );
        }

        JsonNode keys = objectMapper.readTree(response.body()).get("keys");

        for (JsonNode key : keys) {

            if (!kid.equals(key.get("kid").asText())) {
                continue;
            }

            String x = key.get("x").asText();
            String y = key.get("y").asText();

            return createEcPublicKey(x, y);
        }

        throw new IllegalArgumentException(
                "No matching Supabase public key found"
        );
    }

    private PublicKey createEcPublicKey(String x, String y)
            throws Exception {

        byte[] xBytes = Base64.getUrlDecoder().decode(x);
        byte[] yBytes = Base64.getUrlDecoder().decode(y);

        BigInteger xCoord = new BigInteger(1, xBytes);
        BigInteger yCoord = new BigInteger(1, yBytes);

        ECPoint point = new ECPoint(xCoord, yCoord);

        AlgorithmParameters parameters =
                AlgorithmParameters.getInstance("EC");

       parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecSpec =
                parameters.getParameterSpec(ECParameterSpec.class);

        ECPublicKeySpec publicKeySpec =
                new ECPublicKeySpec(point, ecSpec);

        KeyFactory keyFactory =
                KeyFactory.getInstance("EC");

        return keyFactory.generatePublic(publicKeySpec);
    }
}
