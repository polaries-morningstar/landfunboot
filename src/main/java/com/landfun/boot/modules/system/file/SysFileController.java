package com.landfun.boot.modules.system.file;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.landfun.boot.infrastructure.storage.Storage;
import com.landfun.boot.infrastructure.web.R;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "File", description = "File Management APIs")
@Slf4j
@RestController
@RequestMapping("/sys/file")
@RequiredArgsConstructor
public class SysFileController {

    private final Storage storage;

    @Operation(summary = "Upload File")
    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail(400, "File is empty");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            // In a real application, you might want to generate a unique filename
            String fileName = System.currentTimeMillis() + "_" + originalFilename;

            storage.upload(file.getInputStream(), fileName);
            String url = storage.getUrl(fileName);

            Map<String, String> result = new HashMap<>();
            result.put("name", originalFilename);
            result.put("fileName", fileName);
            result.put("url", url);

            return R.ok(result);
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            return R.fail(500, "Upload failed: " + e.getMessage());
        }
    }
}
