package org.likelionhsu.hackathon.auth.controller;

import org.likelionhsu.hackathon.auth.dto.request.EmailVerificationConfirmRequest;
import org.likelionhsu.hackathon.auth.dto.request.EmailVerificationRequest;
import org.likelionhsu.hackathon.auth.dto.request.LoginRequest;
import org.likelionhsu.hackathon.auth.dto.request.PasswordReauthenticationRequest;
import org.likelionhsu.hackathon.auth.dto.request.SignupRequest;
import org.likelionhsu.hackathon.auth.dto.request.SocialSignupRequest;
import org.likelionhsu.hackathon.auth.dto.response.AccessTokenResponse;
import org.likelionhsu.hackathon.auth.dto.response.AuthTokenResponse;
import org.likelionhsu.hackathon.auth.dto.response.EmailVerificationConfirmResponse;
import org.likelionhsu.hackathon.auth.dto.response.EmailVerificationResponse;
import org.likelionhsu.hackathon.auth.dto.response.LoginIdAvailabilityResponse;
import org.likelionhsu.hackathon.auth.oauth.OAuthCookieService;
import org.likelionhsu.hackathon.auth.service.AuthService;
import org.likelionhsu.hackathon.auth.service.AccountReauthenticationService;
import org.likelionhsu.hackathon.auth.service.EmailVerificationService;
import org.likelionhsu.hackathon.auth.service.OAuthService;
import org.likelionhsu.hackathon.auth.service.SocialAuthService;
import org.likelionhsu.hackathon.auth.support.RefreshCookieService;
import org.likelionhsu.hackathon.auth.support.ReauthenticationCookieService;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;
    private final RefreshCookieService refreshCookieService;
    private final OAuthService oauthService;
    private final SocialAuthService socialAuthService;
    private final OAuthCookieService oauthCookieService;
    private final AccountReauthenticationService
            accountReauthenticationService;
    private final ReauthenticationCookieService
            reauthenticationCookieService;

    public AuthController(
            EmailVerificationService emailVerificationService,
            AuthService authService,
            RefreshCookieService refreshCookieService,
            OAuthService oauthService,
            SocialAuthService socialAuthService,
            OAuthCookieService oauthCookieService,
            AccountReauthenticationService
                    accountReauthenticationService,
            ReauthenticationCookieService
                    reauthenticationCookieService
    ) {
        this.emailVerificationService = emailVerificationService;
        this.authService = authService;
        this.refreshCookieService = refreshCookieService;
        this.oauthService = oauthService;
        this.socialAuthService = socialAuthService;
        this.oauthCookieService = oauthCookieService;
        this.accountReauthenticationService =
                accountReauthenticationService;
        this.reauthenticationCookieService =
                reauthenticationCookieService;
    }

    @PostMapping("/email-verifications")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>>
    requestEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        emailVerificationService.request(request)
                ));
    }

    @PostMapping("/email-verifications/confirm")
    public ApiResponse<EmailVerificationConfirmResponse>
    confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return ApiResponse.success(
                emailVerificationService.confirm(request)
        );
    }

    @GetMapping("/login-ids/{loginId}/availability")
    public ApiResponse<LoginIdAvailabilityResponse> checkLoginId(
            @PathVariable
            @Pattern(
                    regexp = "^[a-z0-9_]{4,20}$",
                    message = "로그인 아이디는 영문 소문자, 숫자, 밑줄로 4~20자여야 합니다."
            )
            String loginId
    ) {
        return ApiResponse.success(authService.checkLoginId(loginId));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        AuthService.AuthResult result = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieService
                                .create(result.refreshToken())
                                .toString()
                )
                .body(ApiResponse.success(result.response()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthService.AuthResult result = authService.login(request);
        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieService
                                .create(result.refreshToken())
                                .toString()
                )
                .body(ApiResponse.success(result.response()));
    }

    @GetMapping("/oauth/{provider}")
    public ResponseEntity<Void> startOAuth(
            @PathVariable String provider
    ) {
        OAuthService.StartResult result = oauthService.start(provider);
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(result.authorizationUri())
                .header(
                        HttpHeaders.SET_COOKIE,
                        oauthCookieService
                                .createState(result.state())
                                .toString()
                )
                .build();
    }

    @PostMapping("/reauthentications")
    public ResponseEntity<Void> reauthenticateWithPassword(
            @Valid @RequestBody
            PasswordReauthenticationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String token = accountReauthenticationService
                .reauthenticateWithPassword(
                        Long.valueOf(jwt.getSubject()),
                        request.password()
                );

        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        reauthenticationCookieService
                                .create(token)
                                .toString()
                )
                .build();
    }

    @GetMapping("/oauth/{provider}/reauthentication")
    public ResponseEntity<Void>
    startAccountDeleteReauthentication(
            @PathVariable String provider
    ) {
        OAuthService.StartResult result = oauthService
                .startAccountDeleteReauthentication(provider);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(result.authorizationUri())
                .header(
                        HttpHeaders.SET_COOKIE,
                        oauthCookieService
                                .createState(result.state())
                                .toString()
                )
                .build();
    }

    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> oauthCallback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                oauthCookieService.clearState().toString()
        );

        String cookieState = oauthCookieService.readState(request);

        if (oauthService
                .isAccountDeleteReauthenticationCallback(
                        state,
                        cookieState
                )) {
            String token = oauthService
                    .accountDeleteReauthenticationCallback(
                            provider,
                            code,
                            state,
                            cookieState
                    );

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(oauthService
                            .reauthenticationSuccessRedirectUri())
                    .header(
                            HttpHeaders.SET_COOKIE,
                            reauthenticationCookieService
                                    .create(token)
                                    .toString()
                    )
                    .build();
        }

        SocialAuthService.CallbackResult result =
                oauthService.callback(
                        provider,
                        code,
                        state,
                        cookieState
                );

        if (result.type()
                == SocialAuthService.CallbackType.EXISTING_USER) {
            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(oauthService.successRedirectUri())
                    .header(
                            HttpHeaders.SET_COOKIE,
                            refreshCookieService
                                    .create(result.refreshToken())
                                    .toString()
                    )
                    .build();
        }

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(oauthService.onboardingRedirectUri())
                .header(
                        HttpHeaders.SET_COOKIE,
                        oauthCookieService
                                .createOnboarding(
                                        result.onboardingToken()
                                )
                                .toString()
                )
                .build();
    }

    @PostMapping("/oauth/signup")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> socialSignup(
            @Valid @RequestBody SocialSignupRequest signupRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                oauthCookieService.clearOnboarding().toString()
        );

        SocialAuthService.SignupResult result =
                socialAuthService.signup(
                        oauthCookieService.readOnboarding(request),
                        signupRequest
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieService
                                .create(result.refreshToken())
                                .toString()
                )
                .body(ApiResponse.success(result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(
            HttpServletRequest request
    ) {
        String refreshToken = refreshCookieService.read(request);
        AuthService.RefreshResult result =
                authService.refresh(refreshToken);
        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieService
                                .create(result.refreshToken())
                                .toString()
                )
                .body(ApiResponse.success(result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String refreshToken = refreshCookieService.read(request);
        authService.logout(
                refreshToken,
                Long.valueOf(jwt.getSubject())
        );
        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieService.clear().toString(),
                        reauthenticationCookieService
                                .clear()
                                .toString()
                )
                .build();
    }
}
