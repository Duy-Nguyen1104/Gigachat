package com.project.gigachat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 configuration.
 *
 * Credentials selection:
 * - If AWS_PROFILE (or AWS_DEFAULT_PROFILE) is set, it will use that profile.
 * - Otherwise, it falls back to DefaultCredentialsProvider, which checks
 *   environment variables, system properties, default profile, etc.
 */
@Configuration
public class S3Config {
    
    @Value("${aws.s3.region}")
    private String region;

    @Value("${AWS_PROFILE:gigachat-user}")
    private String awsProfile;
    
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }
    
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    private AwsCredentialsProvider credentialsProvider() {
        // Prefer an explicit profile if the env var is set to something other than "default".
        if (awsProfile != null && !awsProfile.isBlank() && !"default".equals(awsProfile)) {
            return ProfileCredentialsProvider.builder()
                    .profileName(awsProfile)
                    .build();
        }
        return DefaultCredentialsProvider.create();
    }
}

