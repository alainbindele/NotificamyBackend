package com.notifyme.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@Profile("!dev") // Non applicare in sviluppo locale
public class HttpsConfig {

    public void configureHttps(HttpSecurity http) throws Exception {
        // Forza HTTPS per tutte le richieste API
        http.requiresChannel(channel -> 
            channel.requestMatchers("/api/v1/**")
                   .requiresSecure()
        );
        
        // Headers di sicurezza essenziali
        http.headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.deny())
            .contentTypeOptions(contentType -> {})
        );
    }
}