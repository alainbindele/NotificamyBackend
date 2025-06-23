package com.notifyme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyme.dto.SqsNotificationMessage;
import com.notifyme.entity.Query;
import com.notifyme.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsServiceTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SqsService sqsService;

    private User testUser;
    private Query testQuery;

    @BeforeEach
    void setUp() {
        // Usa reflection per impostare il queueUrl
        try {
            var field = SqsService.class.getDeclaredField("queueUrl");
            field.setAccessible(true);
            field.set(sqsService, "https://sqs.eu-west-1.amazonaws.com/123456789/test-queue");
        } catch (Exception e) {
            fail("Failed to set queue URL: " + e.getMessage());
        }

        testUser = new User("test@example.com");
        testUser.setId(1L);
        testUser.setPhone("+393123456789");
        testUser.setSlackWebhook("https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXX");
        testUser.setDiscordWebhook("https://discord.com/api/webhooks/123456789/abcdefgh");

        testQuery = new Query(testUser, "Test prompt");
        testQuery.setId(17L);
        testQuery.setIsValid(true);
    }

    @Test
    void testSendNotificationMessageSuccess() throws Exception {
        // Arrange
        String expectedJson = "{\"query_id\":17,\"user_email\":\"test@example.com\",\"user_phone\":\"+393123456789\",\"user_slack_webhook\":\"https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXX\",\"user_discord_webhook\":\"https://discord.com/api/webhooks/123456789/abcdefgh\",\"prompt\":\"Test prompt\"}";
        
        when(objectMapper.writeValueAsString(any(SqsNotificationMessage.class)))
            .thenReturn(expectedJson);
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId("test-message-id").build());

        // Act
        boolean result = sqsService.sendNotificationMessage(testQuery, testUser);

        // Assert
        assertTrue(result);
        verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
        verify(objectMapper, times(1)).writeValueAsString(any(SqsNotificationMessage.class));
    }

    @Test
    void testSendNotificationMessageWithOnlyEmail() throws Exception {
        // Arrange - User con solo email
        User emailOnlyUser = new User("email-only@example.com");
        emailOnlyUser.setId(2L);
        // Non impostiamo phone, slack, discord (rimangono vuoti)

        String expectedJson = "{\"query_id\":17,\"user_email\":\"email-only@example.com\",\"prompt\":\"Test prompt\"}";
        
        when(objectMapper.writeValueAsString(any(SqsNotificationMessage.class)))
            .thenReturn(expectedJson);
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId("test-message-id").build());

        // Act
        boolean result = sqsService.sendNotificationMessage(testQuery, emailOnlyUser);

        // Assert
        assertTrue(result);
        verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void testSendNotificationMessageWithPartialChannels() throws Exception {
        // Arrange - User con solo email e phone
        User partialUser = new User("partial@example.com");
        partialUser.setId(3L);
        partialUser.setPhone("+393123456789");
        // Slack e Discord rimangono vuoti

        String expectedJson = "{\"query_id\":17,\"user_email\":\"partial@example.com\",\"user_phone\":\"+393123456789\",\"prompt\":\"Test prompt\"}";
        
        when(objectMapper.writeValueAsString(any(SqsNotificationMessage.class)))
            .thenReturn(expectedJson);
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId("test-message-id").build());

        // Act
        boolean result = sqsService.sendNotificationMessage(testQuery, partialUser);

        // Assert
        assertTrue(result);
        verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void testSendNotificationMessageFailure() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any(SqsNotificationMessage.class)))
            .thenThrow(new RuntimeException("JSON serialization failed"));

        // Act
        boolean result = sqsService.sendNotificationMessage(testQuery, testUser);

        // Assert
        assertFalse(result);
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }
}