package org.likelionhsu.hackathon.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.constraints.Min;

@WebMvcTest(
        controllers =
                GlobalExceptionHandlerTest.TestController.class
)
@Import({
        GlobalExceptionHandler.class,
        GlobalExceptionHandlerTest.TestController.class
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 존재하지_않는_API는_ENDPOINT_NOT_FOUND를_반환한다()
            throws Exception {

        mockMvc.perform(
                        get("/test/not-exists")
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("ENDPOINT_NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.error.message")
                                .value(
                                        "요청한 API 경로를 찾을 수 없습니다."
                                )
                )
                .andExpect(
                        jsonPath("$.data")
                                .doesNotExist()
                );
    }

    @Test
    void BusinessException은_정해진_오류_응답을_반환한다()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/exceptions/business"
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("PRODUCT_NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.error.message")
                                .value(
                                        "제품을 찾을 수 없습니다."
                                )
                )
                .andExpect(
                        jsonPath("$.data")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.error.fields")
                                .doesNotExist()
                );
    }

    @Test
    void Validation_실패는_fields를_반환한다()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/test/exceptions/validation"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": ""
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].field"
                        )
                                .value("email")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].reason"
                        )
                                .value("이메일은 필수입니다.")
                )
                .andExpect(
                        jsonPath("$.data")
                                .doesNotExist()
                );
    }

    @Test
    void 잘못된_JSON은_REQUEST_BODY_INVALID를_반환한다()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/test/exceptions/validation"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email":
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value(
                                        "REQUEST_BODY_INVALID"
                                )
                )
                .andExpect(
                        jsonPath("$.error.fields")
                                .doesNotExist()
                );
    }

    @Test
    void 처리되지_않은_예외는_안전한_500_응답을_반환한다()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/exceptions/unexpected"
                        )
                )
                .andExpect(
                        status().isInternalServerError()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value(
                                        "INTERNAL_SERVER_ERROR"
                                )
                )
                .andExpect(
                        jsonPath("$.error.message")
                                .value(
                                        "서버 오류가 발생했습니다."
                                )
                )
                .andExpect(
                        jsonPath("$.data")
                                .doesNotExist()
                );
    }

    @Test
    void queryParameterTypeMismatchReturnsValidationError()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/exceptions/query-type"
                        )
                                .param(
                                        "page",
                                        "abc"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].field"
                        )
                                .value("page")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].reason"
                        )
                                .value("잘못된 입력값입니다.")
                )
                .andExpect(
                        jsonPath("$.data")
                                .doesNotExist()
                );
    }

    @Test
    void pathVariableTypeMismatchReturnsValidationError()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/exceptions/path-type/abc"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].field"
                        )
                                .value("itemId")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].reason"
                        )
                                .value("잘못된 입력값입니다.")
                )
                .andExpect(
                        jsonPath("$.data")
                                .doesNotExist()
                );
    }

    @Test
    void queryParameterValidationFailureReturnsValidationError()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/exceptions/query-validation"
                        )
                                .param(
                                        "page",
                                        "-1"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].field"
                        )
                                .value("page")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].reason"
                        )
                                .value("0 이상이어야 합니다.")
                )
                .andExpect(
                        jsonPath("$.data")
                                .doesNotExist()
                );
    }


    @Test
    void pathVariableValidationFailureReturnsValidationError()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/exceptions/path-validation/0"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].field"
                        )
                                .value("itemId")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].reason"
                        )
                                .value("1 이상이어야 합니다.")
                )
                .andExpect(
                        jsonPath("$.data")
                                .doesNotExist()
                );
    }

    @Test
    void missingRequiredQueryParameterReturnsValidationError()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/test/exceptions/required-query"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].field"
                        )
                                .value("keyword")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].reason"
                        )
                                .value("필수 입력값입니다.")
                )
                .andExpect(
                        jsonPath("$.data")
                                .doesNotExist()
                );
    }

    @RestController
    @RequestMapping("/test/exceptions")
    public static class TestController {

        @GetMapping("/business")
        void business() {
            throw new BusinessException(
                    ErrorCode.PRODUCT_NOT_FOUND
            );
        }

        @PostMapping("/validation")
        void validation(
                @Valid
                @RequestBody
                ValidationRequest request
        ) {
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new RuntimeException(
                    "외부에 노출되면 안 되는 내부 메시지"
            );
        }

        @GetMapping("/query-type")
        void queryType(
                @RequestParam
                int page
        ) {
        }

        @GetMapping("/query-validation")
        void queryValidation(
                @RequestParam
                @Min(
                        value = 0,
                        message = "0 이상이어야 합니다."
                )
                int page
        ) {
        }

        @GetMapping("/path-type/{itemId}")
        void pathType(
                @PathVariable
                Long itemId
        ) {
        }

        @GetMapping("/path-validation/{itemId}")
        void pathValidation(
                @PathVariable
                @Min(
                        value = 1,
                        message = "1 이상이어야 합니다."
                )
                Long itemId
        ) {
        }

        @GetMapping("/required-query")
        void requiredQuery(
                @RequestParam
                String keyword
        ) {
        }
    }

    public record ValidationRequest(
            @NotBlank(
                    message = "이메일은 필수입니다."
            )
            String email
    ) {
    }
}

