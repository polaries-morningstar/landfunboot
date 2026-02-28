package com.landfun.boot.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

class FileSystemStorageTest {

    private String testPath = "./test_uploads";
    private FileSystemStorage storage;

    @BeforeEach
    void setUp() {
        storage = new FileSystemStorage(testPath);
    }

    @AfterEach
    void tearDown() {
        FileSystemUtils.deleteRecursively(new File(testPath));
    }

    @Test
    void testUploadAndDownload() {
        String content = "Hello Landfun Storage";
        String fileName = "test.txt";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String uploadedFile = storage.upload(inputStream, fileName);
        assertEquals(fileName, uploadedFile);
        assertTrue(storage.exists(fileName));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        storage.download(fileName, outputStream);
        assertEquals(content, outputStream.toString(StandardCharsets.UTF_8));

        storage.delete(fileName);
        assertFalse(storage.exists(fileName));
    }
}
