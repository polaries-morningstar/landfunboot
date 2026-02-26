package com.landfun.boot.modules.system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.landfun.boot.infrastructure.consts.GlobalConstants;
import com.landfun.boot.infrastructure.web.R;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "System", description = "System APIs")
@RestController
public class HealthController {

    @Operation(summary = "System Health Check")
    @GetMapping("/health")
    public R<String> health() {
        return R.ok(GlobalConstants.SYSTEM_NAME + " is running...");
    }
}
