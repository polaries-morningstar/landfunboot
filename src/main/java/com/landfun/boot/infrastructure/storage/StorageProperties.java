package com.landfun.boot.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Storage configuration properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "landfun.storage")
public class StorageProperties {

    /**
     * Storage type: local, s3
     */
    private String type = "local";

    /**
     * Local storage configuration
     */
    private Local local = new Local();

    /**
     * S3 storage configuration
     */
    private S3 s3 = new S3();

    @Data
    public static class Local {
        private String basePath = "./uploads";
    }

    @Data
    public static class S3 {
        private String endpoint;
        private String bucket;
        private String accessKey;
        private String secretKey;
    }
}
