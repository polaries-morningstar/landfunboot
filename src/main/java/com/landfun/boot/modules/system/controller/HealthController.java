package com.landfun.boot.modules.system.controller;

import com.landfun.boot.infrastructure.consts.GlobalConstants;
import com.landfun.boot.infrastructure.web.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public R<String> health() {
        return R.ok(GlobalConstants.SYSTEM_NAME + " is running...");
    }
}
