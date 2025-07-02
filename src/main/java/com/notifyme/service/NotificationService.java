package com.notifyme.service;

import com.notifyme.entity.TUser;
import com.notifyme.entity.TQuery;
import com.notifyme.entity.TNotification;
import com.notifyme.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Invia una notifica all'utente sui canali configurati
     */
    public boolean sendNotification(TUser user, TQuery query, String content) {
        logger.info("Sending notification to user {} for query {}", user.getEmail(), query.getId());
        
        boolean anyChannelSucceeded = false;
        List<String> enabledChannels = getEnabledChannels(query);
        
        if (enabledChannels.isEmpty()) {
            // Fallback: usa email come canale predefinito
            logger.debug("No specific channels configured, using email as fallback");
            anyChannelSucceeded = sendEmailNotification(user, query, content);
        } else {
            // Invia su tutti i canali abilitati
            for (String channel : enabledChannels) {
                try {
                    boolean channelSuccess = sendToChannel(user, query, content, channel);
                    if (channelSuccess) {
                        anyChannelSucceeded = true;
                        logger.debug("Successfully sent notification via {}", channel);
                    } else {
                        logger.warn("Failed to send notification via {}", channel);
                    }
                } catch (Exception e) {
                    logger.error("Error sending notification via {}: {}", channel, e.getMessage(), e);
                }
            }
        }
        
        // Salva record della notifica se almeno un canale ha funzionato
        if (anyChannelSucceeded) {
            saveNotificationRecord(user, query, content);
        }
        
        return anyChannelSucceeded;
    }
    
    /**
     * Ottiene i canali abilitati per la query
     */
    private List<String> getEnabledChannels(TQuery query) {
        try {
            if (query.getEnabledChannels() != null && !query.getEnabledChannels().trim().isEmpty()) {
                String[] channels = objectMapper.readValue(query.getEnabledChannels(), String[].class);
                return List.of(channels);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse enabled channels for query {}: {}", query.getId(), e.getMessage());
        }
        
        return List.of();
    }
    
    /**
     * Invia notifica su un canale specifico
     */
    private boolean sendToChannel(TUser user, TQuery query, String content, String channel) {
        switch (channel.toLowerCase()) {
            case "email":
                return sendEmailNotification(user, query, content);
            case "discord":
                return sendDiscordNotification(user, query, content);
            case "slack":
                return sendSlackNotification(user, query, content);
            case "whatsapp":
                return sendWhatsAppNotification(user, query, content);
            default:
                logger.warn("Unknown notification channel: {}", channel);
                return false;
        }
    }
    
    /**
     * Invia notifica via email (simulata)
     */
    private boolean sendEmailNotification(TUser user, TQuery query, String content) {
        try {
            logger.info("Sending email notification to: {}", user.getEmail());
            
            // TODO: Implementare invio email reale (es. con SendGrid, AWS SES, etc.)
            // Per ora simuliamo l'invio
            
            String subject = generateEmailSubject(query);
            String body = generateEmailBody(content, query);
            
            logger.info("Email notification simulated - Subject: {}, Body: {}", subject, body);
            
            return true; // Simulazione sempre riuscita
            
        } catch (Exception e) {
            logger.error("Failed to send email notification: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Invia notifica via Discord webhook
     */
    private boolean sendDiscordNotification(TUser user, TQuery query, String content) {
        try {
            if (user.getDiscordWebhook() == null || user.getDiscordWebhook().trim().isEmpty()) {
                logger.warn("Discord webhook not configured for user: {}", user.getEmail());
                return false;
            }
            
            logger.info("Sending Discord notification to user: {}", user.getEmail());
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("content", content);
            payload.put("username", "NotifyMe Bot");
            
            // Aggiungi embed per un messaggio più ricco
            Map<String, Object> embed = new HashMap<>();
            embed.put("title", "🔔 Notifica Programmata");
            embed.put("description", content);
            embed.put("color", 5814783); // Blu
            embed.put("timestamp", java.time.Instant.now().toString());
            
            Map<String, Object> footer = new HashMap<>();
            footer.put("text", "NotifyMe - Query #" + query.getId());
            embed.put("footer", footer);
            
            payload.put("embeds", List.of(embed));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(user.getDiscordWebhook(), request, String.class);
            
            logger.info("Discord notification sent successfully to user: {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send Discord notification: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Invia notifica via Slack webhook
     */
    private boolean sendSlackNotification(TUser user, TQuery query, String content) {
        try {
            if (user.getSlackWebhook() == null || user.getSlackWebhook().trim().isEmpty()) {
                logger.warn("Slack webhook not configured for user: {}", user.getEmail());
                return false;
            }
            
            logger.info("Sending Slack notification to user: {}", user.getEmail());
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", "🔔 *Notifica Programmata*");
            payload.put("username", "NotifyMe Bot");
            payload.put("icon_emoji", ":bell:");
            
            // Aggiungi attachment per un messaggio più ricco
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", "good");
            attachment.put("text", content);
            attachment.put("footer", "NotifyMe - Query #" + query.getId());
            attachment.put("ts", System.currentTimeMillis() / 1000);
            
            payload.put("attachments", List.of(attachment));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(user.getSlackWebhook(), request, String.class);
            
            logger.info("Slack notification sent successfully to user: {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send Slack notification: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Invia notifica via WhatsApp (simulata)
     */
    private boolean sendWhatsAppNotification(TUser user, TQuery query, String content) {
        try {
            if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
                logger.warn("WhatsApp phone not configured for user: {}", user.getEmail());
                return false;
            }
            
            logger.info("Sending WhatsApp notification to: {}", user.getPhone());
            
            // TODO: Implementare invio WhatsApp reale (es. con Twilio, WhatsApp Business API, etc.)
            // Per ora simuliamo l'invio
            
            String message = "🔔 NotifyMe: " + content;
            
            logger.info("WhatsApp notification simulated - Phone: {}, Message: {}", user.getPhone(), message);
            
            return true; // Simulazione sempre riuscita
            
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp notification: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Genera l'oggetto dell'email
     */
    private String generateEmailSubject(TQuery query) {
        if (query.getSummaryText() != null && !query.getSummaryText().trim().isEmpty()) {
            return "🔔 NotifyMe: " + query.getSummaryText().substring(0, Math.min(50, query.getSummaryText().length()));
        }
        return "🔔 NotifyMe: Promemoria Programmato";
    }
    
    /**
     * Genera il corpo dell'email
     */
    private String generateEmailBody(String content, TQuery query) {
        StringBuilder body = new StringBuilder();
        body.append("Ciao,\n\n");
        body.append("Ecco il tuo promemoria programmato:\n\n");
        body.append(content);
        body.append("\n\n");
        body.append("---\n");
        body.append("Query ID: ").append(query.getId()).append("\n");
        body.append("Prompt originale: ").append(query.getPrompt()).append("\n");
        body.append("\nGrazie per aver usato NotifyMe!");
        
        return body.toString();
    }
    
    /**
     * Salva il record della notifica nel database
     */
    private void saveNotificationRecord(TUser user, TQuery query, String content) {
        try {
            String subject = generateEmailSubject(query);
            TNotification notification = new TNotification(user, query, subject, content);
            notificationRepository.save(notification);
            
            logger.debug("Saved notification record for user {} and query {}", user.getEmail(), query.getId());
            
        } catch (Exception e) {
            logger.error("Failed to save notification record: {}", e.getMessage(), e);
        }
    }
}