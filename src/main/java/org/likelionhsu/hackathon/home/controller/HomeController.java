package org.likelionhsu.hackathon.home.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.home.dto.HomeResponse;
import org.likelionhsu.hackathon.home.service.HomeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Home", description = "홈 집계 조회 API")
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @Operation(
            summary = "홈 조회",
            description = """
                    현재 로그인 사용자의 홈 데이터를 집계 조회합니다.
                    기존 저장 데이터만 사용하며 이 호출 자체로 Recommendation,
                    AI Job, OpenAI, Kakao Local, 장소 추천을 새로 실행하지 않습니다.
                    """
    )
    @GetMapping
    public ApiResponse<HomeResponse> getHome(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                homeService.getHome(Long.valueOf(jwt.getSubject()))
        );
    }
}
