package com.notifyme.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${auth0.domain}")
    private String auth0Domain;

    @Value("${auth0.audience}")
    private String auth0Audience;

    private JwkProvider jwkProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("Processing JWT authentication for: {} {}", method, requestPath);
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestPath)) {
            logger.debug("Skipping JWT authentication for public endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Handle preflight requests
        if ("OPTIONS".equals(method)) {
            logger.debug("Handling OPTIONS preflight request for: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Check if already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null && 
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            logger.debug("Request already authenticated, skipping: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            
            try {
                DecodedJWT decodedJWT = validateToken(token);
                
                if (decodedJWT != null) {
                    String userId = decodedJWT.getSubject();
                    String email = decodedJWT.getClaim("email").asString();
                    
                    // Fallback per email se non presente nel claim principale
                    if (email == null || email.isEmpty()) {
                        email = decodedJWT.getClaim("https://notificamy.com/email").asString();
                    }
                    if (email == null || email.isEmpty()) {
                        email = decodedJWT.getClaim("name").asString();
                    }
                    
                    // Create authentication token
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_USER")
                    );
                    
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Add user info to request attributes
                    request.setAttribute("userId", userId);
                    request.setAttribute("userEmail", email != null ? email : userId);
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("JWT authentication successful for user: {} ({})", email, userId);
                }
                
            } catch (Exception e) {
                logger.error("JWT authentication failed: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or expired token\",\"success\":false}");
                return;
            }
        } else {
            logger.warn("No valid Authorization header found for request: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authorization token required\",\"success\":false}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.equals("/api/v1/health") || 
               requestPath.equals("/actuator/health") || 
               requestPath.equals("/error") ||
               requestPath.startsWith("/actuator/");
    }

    private DecodedJWT validateToken(String token) throws Exception {
        if (jwkProvider == null) {
            jwkProvider = new UrlJwkProvider(new URL(String.format("https://%s/.well-known/jwks.json", auth0Domain)));
        }

        DecodedJWT jwt = JWT.decode(token);
        
        // Verify the token signature
        Jwk jwk = jwkProvider.get(jwt.getKeyId());
        Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
        
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(String.format("https://%s/", auth0Domain))
                .withAudience(auth0Audience)
                .build();
        
        return verifier.verify(token);
    }
}