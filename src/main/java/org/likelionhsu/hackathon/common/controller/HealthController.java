package org.likelionhsu.hackathon.common.controller;

import java.util.Map;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(
                Map.of(
                        "status", "ok",
                        "message", "Hackathon backend is running"
                )
        );
    }
}
