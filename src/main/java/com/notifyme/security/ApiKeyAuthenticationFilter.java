package com.notifyme.security;

import com.notifyme.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "api-key";

    @Autowired
    private ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("Processing request: {} {}", method, requestPath);
        
        // Skip authentication for health check endpoint and error endpoint
        if (requestPath.equals("/api/v1/health") || 
            requestPath.equals("/actuator/health") || 
            requestPath.equals("/error") ||
            requestPath.startsWith("/actuator/")) {
            logger.debug("Skipping authentication for: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Only process API endpoints that require authentication
        if (!requestPath.startsWith("/api/v1/")) {
            logger.debug("Skipping authentication for non-API endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Check if already authenticated to prevent double processing
        if (SecurityContextHolder.getContext().getAuthentication() != null && 
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
            !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser")) {
            logger.debug("Request already authenticated, skipping: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey != null) {
            if (apiKeyService.isValidApiKey(apiKey)) {
                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken("api-user", null, new ArrayList<>());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("API key authentication successful for request: {}", requestPath);
                
                filterChain.doFilter(request, response);
                return;
            } else {
                logger.warn("Invalid API key provided for request: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid API key\",\"success\":false}");
                response.setContentType("application/json");
                return;
            }
        } else {
            logger.warn("No API key provided for request: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"API key required\",\"success\":false}");
            response.setContentType("application/json");
            return;
        }
    }
}