package org.likelionhsu.hackathon.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@SpringBootTest
class CommonResponseSerializationTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void 성공_응답에는_success와_data만_포함한다()
            throws Exception {

        ApiResponse<Map<String, String>> response =
                ApiResponse.success(
                        Map.of(
                                "productId",
                                "123"
                        )
                );

        String json =
                jsonMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"success\":true")
                .contains("\"data\":")
                .doesNotContain("\"error\"");
    }

    @Test
    void 일반_오류에는_data와_fields를_포함하지_않는다()
            throws Exception {

        ErrorResponse response =
                ErrorResponse.of(
                        "PRODUCT_NOT_FOUND",
                        "제품을 찾을 수 없습니다."
                );

        String json =
                jsonMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"success\":false")
                .contains("\"error\":")
                .contains("\"code\":\"PRODUCT_NOT_FOUND\"")
                .contains("\"message\":\"제품을 찾을 수 없습니다.\"")
                .doesNotContain("\"data\"")
                .doesNotContain("\"fields\"");
    }

    @Test
    void Validation_오류에는_fields를_포함한다()
            throws Exception {

        ErrorResponse response =
                ErrorResponse.validation(
                        "VALIDATION_ERROR",
                        "입력값을 확인해 주세요.",
                        List.of(
                                new FieldErrorResponse(
                                        "email",
                                        "올바른 이메일 형식이 아닙니다."
                                ),
                                new FieldErrorResponse(
                                        "verificationCode",
                                        "인증번호는 6자리여야 합니다."
                                )
                        )
                );

        String json =
                jsonMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"success\":false")
                .contains("\"code\":\"VALIDATION_ERROR\"")
                .contains("\"fields\":")
                .contains("\"field\":\"email\"")
                .contains("\"field\":\"verificationCode\"")
                .doesNotContain("\"data\"");
    }
}

