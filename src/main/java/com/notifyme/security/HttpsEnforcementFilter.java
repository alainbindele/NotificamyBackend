package com.notifyme.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1) // Esegui prima di altri filtri
@Profile("!dev") // Non applicare in sviluppo locale
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(HttpsEnforcementFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Controlla se la richiesta è HTTPS
        boolean isSecure = request.isSecure() || 
                          "https".equals(request.getHeader("X-Forwarded-Proto")) ||
                          "443".equals(request.getHeader("X-Forwarded-Port"));
        
        String requestUrl = request.getRequestURL().toString();
        
        // Se è una richiesta API e non è HTTPS, rifiuta
        if (requestUrl.contains("/api/v1/") && !isSecure) {
            logger.warn("HTTPS required for API access. Rejecting HTTP request: {}", requestUrl);
            
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"success\":false,\"error\":\"HTTPS required for API access\",\"code\":\"HTTPS_REQUIRED\"}"
            );
            return;
        }
        
        // Se non è HTTPS ma non è API, redirect a HTTPS
        if (!isSecure && !requestUrl.contains("/actuator/health")) {
            String httpsUrl = requestUrl.replace("http://", "https://");
            logger.info("Redirecting HTTP to HTTPS: {} -> {}", requestUrl, httpsUrl);
            
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", httpsUrl);
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}