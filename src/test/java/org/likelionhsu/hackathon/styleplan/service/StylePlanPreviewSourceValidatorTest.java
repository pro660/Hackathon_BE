package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.styleplan.domain.StyleItemRole;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanCreateRequest;

import tools.jackson.databind.json.JsonMapper;

class StylePlanPreviewSourceValidatorTest {

    private StylePlanPreviewSourceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StylePlanPreviewSourceValidator(
                JsonMapper.builder().build()
        );
    }

    @Test
    void succeededAiPreviewMustMatchComposition() {
        AiJobData job = job(
                AiJobStatus.SUCCEEDED,
                """
                {
                  "previewId":"job:9001",
                  "title":"데이트 룩",
                  "description":"설명",
                  "ownedItems":[
                    {
                      "myItemId":"501",
                      "name":"가방",
                      "imageUrl":null,
                      "role":"BAG",
                      "sortOrder":0
                    }
                  ],
                  "recommendedProducts":[
                    {
                      "productId":"101",
                      "name":"MCM",
                      "imageUrl":null,
                      "rank":1,
                      "reason":"이유"
                    }
                  ],
                  "generationType":"AI"
                }
                """,
                null
        );

        assertThat(
                validator.validate(
                        job,
                        request()
                )
        ).isEqualTo(
                StylePlanGenerationType.AI
        );
    }

    @Test
    void failedJobWithRuleFallbackCanBeSaved() {
        AiJobData job = job(
                AiJobStatus.FAILED,
                null,
                """
                {
                  "previewId":"job:9001",
                  "title":"데이트 룩",
                  "description":"설명",
                  "ownedItems":[
                    {
                      "myItemId":"501",
                      "name":"가방",
                      "imageUrl":null,
                      "role":"BAG",
                      "sortOrder":0
                    }
                  ],
                  "recommendedProducts":[
                    {
                      "productId":"101",
                      "name":"MCM",
                      "imageUrl":null,
                      "rank":1,
                      "reason":"이유"
                    }
                  ],
                  "generationType":"RULE_BASED"
                }
                """
        );

        assertThat(
                validator.validate(
                        job,
                        request()
                )
        ).isEqualTo(
                StylePlanGenerationType.RULE_BASED
        );
    }

    @Test
    void arbitraryProductOutsidePreviewIsRejected() {
        AiJobData job = job(
                AiJobStatus.SUCCEEDED,
                """
                {
                  "ownedItems":[
                    {
                      "myItemId":"501",
                      "role":"BAG",
                      "sortOrder":0
                    }
                  ],
                  "recommendedProducts":[],
                  "generationType":"AI"
                }
                """,
                null
        );

        assertThatThrownBy(() ->
                validator.validate(
                        job,
                        request()
                )
        ).isInstanceOf(
                RequestValidationException.class
        );
    }

    private AiJobData job(
            AiJobStatus status,
            String resultJson,
            String fallbackJson
    ) {
        AiJobData job = mock(AiJobData.class);
        when(job.type())
                .thenReturn(AiJobType.STYLE_PLAN);
        when(job.status()).thenReturn(status);
        when(job.resultJson()).thenReturn(resultJson);
        when(job.fallbackJson())
                .thenReturn(fallbackJson);
        return job;
    }

    private StylePlanCreateRequest request() {
        return new StylePlanCreateRequest(
                9001L,
                "데이트 룩",
                StylePlanOccasion.DATE,
                null,
                null,
                "설명",
                StylePlanStatus.CONFIRMED,
                List.of(
                        new StylePlanCreateRequest
                                .OwnedItem(
                                501L,
                                StyleItemRole.BAG,
                                0
                        )
                ),
                List.of(
                        new StylePlanCreateRequest
                                .RecommendedProduct(
                                101L,
                                1,
                                "이유"
                        )
                )
        );
    }
}
