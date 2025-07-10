package com.notifyme.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@Profile("!dev") // Non applicare in sviluppo locale
public class HttpsConfig {

    public void configureHttps(HttpSecurity http) throws Exception {
        // Forza HTTPS per tutte le richieste API
        http.requiresChannel(channel -> 
            channel.requestMatchers("/api/v1/**")
                   .requiresSecure()
        );
        
        // Headers di sicurezza base
        http.headers(headers -> headers
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubdomains(true)
            )
            .frameOptions().deny()
            .contentTypeOptions().and()
        );
    }
}