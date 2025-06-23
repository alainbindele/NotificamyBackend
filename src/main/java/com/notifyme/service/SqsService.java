package com.notifyme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyme.dto.SqsNotificationMessage;
import com.notifyme.entity.Query;
import com.notifyme.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Service
public class SqsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SqsService.class);
    
    @Autowired
    private SqsClient sqsClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${aws.sqs.queue-url}")
    private String queueUrl;
    
    public boolean sendNotificationMessage(Query query, User user) {
        try {
            // Crea il messaggio SQS
            SqsNotificationMessage message = buildNotificationMessage(query, user);
            
            // Verifica che almeno un canale sia presente
            if (!hasAtLeastOneChannel(message)) {
                logger.warn("No notification channels configured for user: {} (query ID: {})", 
                           user.getEmail(), query.getId());
                return false;
            }
            
            // Serializza il messaggio in JSON
            String messageBody = objectMapper.writeValueAsString(message);
            
            // Invia il messaggio a SQS
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
            
            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
            
            logger.info("Successfully sent notification message to SQS for query ID: {} (Message ID: {})", 
                       query.getId(), response.messageId());
            
            return true;
            
        } catch (SqsException e) {
            logger.error("Failed to send message to SQS for query ID: {} - AWS Error: {}", 
                        query.getId(), e.awsErrorDetails().errorMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Failed to send notification message to SQS for query ID: {}", 
                        query.getId(), e);
            return false;
        }
    }
    
    private SqsNotificationMessage buildNotificationMessage(Query query, User user) {
        SqsNotificationMessage message = new SqsNotificationMessage(
            query.getId(), 
            user.getEmail(), 
            query.getPrompt()
        );
        
        // Aggiungi solo i canali configurati (non vuoti)
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            message.setUserPhone(user.getPhone().trim());
        }
        
        if (user.getSlackWebhook() != null && !user.getSlackWebhook().trim().isEmpty()) {
            message.setUserSlackWebhook(user.getSlackWebhook().trim());
        }
        
        if (user.getDiscordWebhook() != null && !user.getDiscordWebhook().trim().isEmpty()) {
            message.setUserDiscordWebhook(user.getDiscordWebhook().trim());
        }
        
        return message;
    }
    
    private boolean hasAtLeastOneChannel(SqsNotificationMessage message) {
        // L'email è sempre presente (campo obbligatorio)
        // Verifica che almeno un canale sia configurato
        return message.getUserEmail() != null || 
               message.getUserPhone() != null || 
               message.getUserSlackWebhook() != null || 
               message.getUserDiscordWebhook() != null;
    }
    
    public void sendTestMessage(String testMessage) {
        try {
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(testMessage)
                    .build();
            
            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
            logger.info("Test message sent to SQS (Message ID: {})", response.messageId());
            
        } catch (Exception e) {
            logger.error("Failed to send test message to SQS", e);
        }
    }
}