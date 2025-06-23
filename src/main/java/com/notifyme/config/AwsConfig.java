package com.notifyme.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

    @Value("${aws.region:eu-west-1}")
    private String awsRegion;

    @Value("${aws.access-key-id:}")
    private String awsAccessKeyId;

    @Value("${aws.secret-access-key:}")
    private String awsSecretAccessKey;

    @Bean
    public SqsClient sqsClient() {
        AwsCredentialsProvider credentialsProvider;
        
        // Se sono fornite credenziali esplicite, usale
        if (!awsAccessKeyId.isEmpty() && !awsSecretAccessKey.isEmpty()) {
            credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey)
            );
        } else {
            // Altrimenti usa il provider di default (IAM role, environment variables, etc.)
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}