package com.landfun.boot.infrastructure.storage;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.landfun.boot.infrastructure.exception.BizException;

import lombok.extern.slf4j.Slf4j;

/**
 * Local file system implementation of Storage.
 */
@Slf4j
public class FileSystemStorage implements Storage {

    private final String basePath;

    public FileSystemStorage(String basePath) {
        this.basePath = basePath;
        File root = new File(basePath);
        if (!root.exists()) {
            root.mkdirs();
        }
    }

    @Override
    public String upload(InputStream inputStream, String fileName) {
        try {
            Path targetPath = Paths.get(basePath, fileName);
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (Exception e) {
            log.error("Failed to upload file to file system", e);
            throw new BizException(500, "File upload failed: " + e.getMessage());
        }
    }

    @Override
    public void download(String fileName, OutputStream outputStream) {
        try {
            Path targetPath = Paths.get(basePath, fileName);
            Files.copy(targetPath, outputStream);
        } catch (Exception e) {
            log.error("Failed to download file from file system", e);
            throw new BizException(500, "File download failed: " + e.getMessage());
        }
    }

    @Override
    public void delete(String fileName) {
        try {
            Path targetPath = Paths.get(basePath, fileName);
            Files.deleteIfExists(targetPath);
        } catch (Exception e) {
            log.error("Failed to delete file from file system", e);
        }
    }

    @Override
    public boolean exists(String fileName) {
        return Files.exists(Paths.get(basePath, fileName));
    }

    @Override
    public String getUrl(String fileName) {
        // In a real application, this might return a relative URL served by a static
        // handler
        return "/api/files/" + fileName;
    }
}
