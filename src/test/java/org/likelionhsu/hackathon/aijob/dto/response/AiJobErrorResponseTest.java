package org.likelionhsu.hackathon.aijob.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiJobErrorResponseTest {

    @Test
    void itemAnalysisFailureUsesItemAnalysisMessage() {
        AiJobErrorResponse response =
                AiJobErrorResponse.fromCode(
                        "AI_ITEM_ANALYSIS_FAILED"
                );

        assertThat(response).isNotNull();
        assertThat(response.code())
                .isEqualTo("AI_ITEM_ANALYSIS_FAILED");
        assertThat(response.message())
                .isEqualTo(
                        "아이템 이미지 분석에 실패했습니다."
                );
    }
}
