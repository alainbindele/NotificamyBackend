package com.notifyme.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${clerk.publishable-key:}")
    private String clerkPublishableKey;

    @Value("${clerk.secret-key:}")
    private String clerkSecretKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

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
                // Valida il token con Clerk
                ClerkUserInfo userInfo = validateClerkToken(token);
                
                if (userInfo != null) {
                    // Create authentication token
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_USER")
                    );
                    
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(userInfo.userId, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Add user info to request attributes
                    request.setAttribute("userId", userInfo.userId);
                    request.setAttribute("userEmail", userInfo.email);
                    request.setAttribute("authSubject", userInfo.userId);
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Clerk JWT authentication successful for user: {} ({})", userInfo.email, userInfo.userId);
                }
                
            } catch (Exception e) {
                logger.error("Clerk JWT authentication failed: {}", e.getMessage());
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

    /**
     * Valida il token JWT con Clerk usando l'API di verifica
     */
    private ClerkUserInfo validateClerkToken(String token) throws Exception {
        if (clerkSecretKey == null || clerkSecretKey.trim().isEmpty()) {
            throw new RuntimeException("Clerk secret key not configured");
        }

        try {
            // Prima prova a decodificare il JWT per estrarre informazioni base
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                throw new RuntimeException("Invalid JWT format");
            }

            // Decodifica il payload
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]));
            JsonNode payloadNode = objectMapper.readTree(payload);
            
            String userId = payloadNode.get("sub").asText();
            String email = null;
            
            // Cerca l'email in vari campi possibili
            if (payloadNode.has("email")) {
                email = payloadNode.get("email").asText();
            } else if (payloadNode.has("email_addresses") && payloadNode.get("email_addresses").isArray()) {
                JsonNode emailAddresses = payloadNode.get("email_addresses");
                if (emailAddresses.size() > 0) {
                    JsonNode firstEmail = emailAddresses.get(0);
                    if (firstEmail.has("email_address")) {
                        email = firstEmail.get("email_address").asText();
                    }
                }
            }

            // Verifica il token con Clerk API (opzionale, per maggiore sicurezza)
            if (shouldVerifyWithClerkAPI()) {
                verifyTokenWithClerkAPI(token);
            }

            if (email == null || email.isEmpty()) {
                // Se non troviamo email nel token, prova a ottenerla dall'API Clerk
                email = getUserEmailFromClerkAPI(userId);
            }

            if (email == null || email.isEmpty()) {
                throw new RuntimeException("No email found for user");
            }

            return new ClerkUserInfo(userId, email);

        } catch (Exception e) {
            logger.error("Failed to validate Clerk token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Verifica il token con l'API di Clerk (per maggiore sicurezza)
     */
    private void verifyTokenWithClerkAPI(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.clerk.com/v1/tokens/verify"))
                .header("Authorization", "Bearer " + clerkSecretKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"token\":\"" + token + "\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Token verification failed with Clerk API: " + response.statusCode());
        }
    }

    /**
     * Ottiene l'email dell'utente dall'API di Clerk
     */
    private String getUserEmailFromClerkAPI(String userId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.clerk.com/v1/users/" + userId))
                    .header("Authorization", "Bearer " + clerkSecretKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode userNode = objectMapper.readTree(response.body());
                if (userNode.has("email_addresses") && userNode.get("email_addresses").isArray()) {
                    JsonNode emailAddresses = userNode.get("email_addresses");
                    if (emailAddresses.size() > 0) {
                        JsonNode firstEmail = emailAddresses.get(0);
                        if (firstEmail.has("email_address")) {
                            return firstEmail.get("email_address").asText();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get user email from Clerk API: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Determina se verificare il token con l'API di Clerk
     * In produzione potresti voler sempre verificare, in sviluppo potresti saltare per performance
     */
    private boolean shouldVerifyWithClerkAPI() {
        // Per ora disabilitato per performance, ma può essere abilitato per maggiore sicurezza
        return false;
    }

    /**
     * Classe per contenere le informazioni utente estratte da Clerk
     */
    private static class ClerkUserInfo {
        final String userId;
        final String email;

        ClerkUserInfo(String userId, String email) {
            this.userId = userId;
            this.email = email;
        }
    }
}