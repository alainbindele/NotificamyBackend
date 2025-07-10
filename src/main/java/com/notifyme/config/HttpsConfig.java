package com.notifyme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@Profile("!dev") // Non applicare in sviluppo locale
public class HttpsConfig {

    @Bean
    public SecurityFilterChain httpsSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Forza HTTPS per tutte le richieste
            .requiresChannel(channel -> 
                channel.requestMatchers(r -> r.getHeader("X-Forwarded-Proto") != null)
                       .requiresSecure())
            
            // Headers di sicurezza HTTPS
            .headers(headers -> headers
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000) // 1 anno
                    .includeSubdomains(true)
                    .preload(true)
                )
                .contentTypeOptions(contentTypeOptions -> contentTypeOptions.and())
                .frameOptions(frameOptions -> frameOptions.deny())
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            )
            .build();
    }
}