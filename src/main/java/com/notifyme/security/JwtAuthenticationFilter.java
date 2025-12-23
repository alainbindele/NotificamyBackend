package com.notifyme.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.annotation.PostConstruct;
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
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${logto.endpoint}")
    private String logtoEndpoint;

    @Value("${logto.app-id}")
    private String logtoAppId;

    @Value("${logto.issuer:#{null}}")
    private String logtoIssuer;

    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    @PostConstruct
    public void init() throws Exception {
        if (logtoEndpoint == null || logtoEndpoint.equals("https://not-configured.logto.app") ||
            logtoAppId == null || logtoAppId.equals("not-configured")) {
            logger.warn("Logto authentication is not configured. JWT validation will be disabled.");
            logger.warn("Please set LOGTO_ENDPOINT and LOGTO_APP_ID environment variables.");
            return;
        }

        String issuer = (logtoIssuer != null && !logtoIssuer.isEmpty()) ? logtoIssuer : logtoEndpoint + "/oidc";
        String jwksUrl = logtoEndpoint + "/oidc/jwks";

        logger.info("Initializing Logto JWT processor with issuer: {} and JWKS URL: {}", issuer, jwksUrl);

        jwtProcessor = new DefaultJWTProcessor<>();
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(jwksUrl));
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        logger.debug("Processing JWT authentication for: {} {}", method, requestPath);

        if (isPublicEndpoint(requestPath)) {
            logger.debug("Skipping JWT authentication for public endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equals(method)) {
            logger.debug("Handling OPTIONS preflight request for: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            logger.debug("Request already authenticated, skipping: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());

            if (jwtProcessor == null) {
                logger.error("JWT processor not initialized. Logto authentication is not configured.");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Authentication not configured\",\"success\":false}");
                return;
            }

            try {
                LogtoUserInfo userInfo = validateLogtoToken(token);

                if (userInfo != null) {
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_USER")
                    );

                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userInfo.userId, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    request.setAttribute("userId", userInfo.userId);
                    request.setAttribute("userEmail", userInfo.email);
                    request.setAttribute("authSubject", userInfo.userId);

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Logto JWT authentication successful for user: {} ({})", userInfo.email, userInfo.userId);
                }

            } catch (Exception e) {
                logger.error("Logto JWT authentication failed for path {}: {}", requestPath, e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or expired token\",\"success\":false}");
                return;
            }
        } else {
            logger.warn("No valid Authorization header found for protected request: {}", requestPath);
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

    private LogtoUserInfo validateLogtoToken(String token) throws Exception {
        try {
            JWTClaimsSet claimsSet = jwtProcessor.process(token, null);

            String issuer = (logtoIssuer != null && !logtoIssuer.isEmpty()) ? logtoIssuer : logtoEndpoint + "/oidc";
            if (!issuer.equals(claimsSet.getIssuer())) {
                throw new RuntimeException("Invalid issuer: " + claimsSet.getIssuer());
            }

            if (!logtoAppId.equals(claimsSet.getAudience().get(0))) {
                throw new RuntimeException("Invalid audience: " + claimsSet.getAudience());
            }

            String userId = claimsSet.getSubject();
            String email = claimsSet.getStringClaim("email");

            if (email == null || email.isEmpty()) {
                logger.warn("No email found in token for user: {}", userId);
                email = "no-email@logto.user";
            }

            logger.debug("Successfully validated Logto token - userId: {}, email: {}", userId, email);

            return new LogtoUserInfo(userId, email);

        } catch (Exception e) {
            logger.error("Failed to validate Logto token: {}", e.getMessage(), e);
            throw e;
        }
    }

    private static class LogtoUserInfo {
        final String userId;
        final String email;

        LogtoUserInfo(String userId, String email) {
            this.userId = userId;
            this.email = email;
        }
    }
}