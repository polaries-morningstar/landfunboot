package com.landfun.boot.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

/**
 * Configuration for Storage beans.
 */
@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final StorageProperties properties;

    @Bean
    @ConditionalOnProperty(name = "landfun.storage.type", havingValue = "local", matchIfMissing = true)
    public Storage fileSystemStorage() {
        return new FileSystemStorage(properties.getLocal().getBasePath());
    }

    @Bean
    @ConditionalOnProperty(name = "landfun.storage.type", havingValue = "s3")
    public Storage s3Storage() {
        StorageProperties.S3 s3 = properties.getS3();
        return new S3Storage(
                s3.getEndpoint(),
                s3.getBucket(),
                s3.getAccessKey(),
                s3.getSecretKey());
    }
}
