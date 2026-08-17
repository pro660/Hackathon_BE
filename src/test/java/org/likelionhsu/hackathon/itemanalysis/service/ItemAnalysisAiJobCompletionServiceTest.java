package org.likelionhsu.hackathon.itemanalysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisAiJobGateway;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisResult;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemAnalysisAiJobCompletionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long JOB_ID = 9101L;

    @Mock
    private ItemAnalysisAiJobGateway gateway;

    private ObjectMapper objectMapper;
    private ItemAnalysisAiJobCompletionService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ItemAnalysisAiJobCompletionService(
                gateway,
                objectMapper
        );
    }

    @Test
    void succeededResultIsSerializedIntoCommonJobResultJson()
            throws Exception {
        ItemAnalysisResult result =
                new ItemAnalysisResult(
                        "MCM",
                        "백팩",
                        ItemCategory.BAG,
                        ColorGroup.BLACK,
                        MaterialGroup.LEATHER
                );

        when(gateway.markSucceeded(
                eq(USER_ID),
                eq(JOB_ID),
                org.mockito.ArgumentMatchers.anyString(),
                eq(100),
                eq(30),
                eq(700L),
                eq(0)
        )).thenReturn(true);

        service.completeSucceeded(
                USER_ID,
                JOB_ID,
                result,
                100,
                30,
                700L,
                0
        );

        ArgumentCaptor<String> jsonCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(gateway).markSucceeded(
                eq(USER_ID),
                eq(JOB_ID),
                jsonCaptor.capture(),
                eq(100),
                eq(30),
                eq(700L),
                eq(0)
        );

        JsonNode json =
                objectMapper.readTree(
                        jsonCaptor.getValue()
                );

        assertThat(json.get("brandName").asText())
                .isEqualTo("MCM");
        assertThat(json.get("name").asText())
                .isEqualTo("백팩");
        assertThat(json.get("category").asText())
                .isEqualTo("BAG");
        assertThat(json.get("primaryColor").asText())
                .isEqualTo("BLACK");
        assertThat(json.get("material").asText())
                .isEqualTo("LEATHER");
    }

    @Test
    void failedAnalysisStoresTypeSpecificErrorCode() {
        when(gateway.markFailed(
                USER_ID,
                JOB_ID,
                "AI_ITEM_ANALYSIS_FAILED",
                900L,
                1
        )).thenReturn(true);

        service.completeFailed(
                USER_ID,
                JOB_ID,
                900L,
                1
        );

        verify(gateway).markFailed(
                USER_ID,
                JOB_ID,
                "AI_ITEM_ANALYSIS_FAILED",
                900L,
                1
        );
    }

    @Test
    void invalidStateTransitionFailsLoudly() {
        when(gateway.markFailed(
                USER_ID,
                JOB_ID,
                "AI_ITEM_ANALYSIS_FAILED",
                null,
                0
        )).thenReturn(false);

        assertThatThrownBy(() ->
                service.completeFailed(
                        USER_ID,
                        JOB_ID,
                        null,
                        0
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "ITEM_ANALYSIS AI Job 상태 전이"
                );
    }
}
