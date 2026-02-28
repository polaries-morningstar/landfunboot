package com.landfun.boot.infrastructure.storage;

import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.landfun.boot.infrastructure.exception.BizException;

import lombok.extern.slf4j.Slf4j;

/**
 * S3 compatible storage implementation.
 */
@Slf4j
public class S3Storage implements Storage {

    private final AmazonS3 s3Client;
    private final String bucketName;

    public S3Storage(String endpoint, String bucketName, String accessKey, String secretKey) {
        this.bucketName = bucketName;
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withPathStyleAccessEnabled(true)
                .build();

        if (!s3Client.doesBucketExistV2(bucketName)) {
            s3Client.createBucket(bucketName);
        }
    }

    @Override
    public String upload(InputStream inputStream, String fileName) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            // In a real scenario, you'd probably want to set content type etc.
            s3Client.putObject(bucketName, fileName, inputStream, metadata);
            return fileName;
        } catch (Exception e) {
            log.error("Failed to upload file to S3", e);
            throw new BizException(500, "S3 upload failed: " + e.getMessage());
        }
    }

    @Override
    public void download(String fileName, OutputStream outputStream) {
        S3Object s3Object = null;
        try {
            s3Object = s3Client.getObject(bucketName, fileName);
            try (S3ObjectInputStream inputStream = s3Object.getObjectContent()) {
                IOUtils.copy(inputStream, outputStream);
            }
        } catch (Exception e) {
            log.error("Failed to download file from S3", e);
            throw new BizException(500, "S3 download failed: " + e.getMessage());
        }
    }

    @Override
    public void delete(String fileName) {
        try {
            s3Client.deleteObject(bucketName, fileName);
        } catch (Exception e) {
            log.error("Failed to delete file from S3", e);
        }
    }

    @Override
    public boolean exists(String fileName) {
        return s3Client.doesObjectExist(bucketName, fileName);
    }

    @Override
    public String getUrl(String fileName) {
        return s3Client.getUrl(bucketName, fileName).toString();
    }
}
