package org.likelionhsu.hackathon.common.controller;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Health",
        description = "백엔드 서버 상태 확인 API"
)

@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(
            summary = "서버 상태 확인",
            description = "백엔드 애플리케이션이 정상적으로 실행 중인지 확인합니다."
    )

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
