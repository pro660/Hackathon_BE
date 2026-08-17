package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanAiJobGateway;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StylePlanAiJobProcessorTest {

    @Mock
    private StylePlanAiJobGateway gateway;

    @Mock
    private StylePlanRecommendationContextService
            contextService;

    @Mock
    private StylePlanInputHasher inputHasher;

    @Mock
    private StylePlanFallbackService fallbackService;

    private StylePlanAiJobProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new StylePlanAiJobProcessor(
                gateway,
                contextService,
                inputHasher,
                fallbackService,
                new ObjectMapper()
        );
    }

    @Test
    void processingStoresInputHashAndRuleBasedFallback() {
        StylePlanJobRequest request =
                new StylePlanJobRequest(
                        "DATE",
                        List.of("GLAMOROUS", "NEAT"),
                        null,
                        true,
                        "ko"
                );

        StylePlanRecommendationContext context =
                new StylePlanRecommendationContext(
                        request,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                );

        StylePlanPreview preview =
                new StylePlanPreview(
                        "job:9201",
                        "데이트 룩",
                        "기본 추천",
                        List.of(),
                        List.of(),
                        "RULE_BASED"
                );

        String inputHash =
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        when(gateway.claimProcessing(1L, 9201L))
                .thenReturn(true);
        when(contextService.prepare(1L, request))
                .thenReturn(context);
        when(inputHasher.hash(context))
                .thenReturn(inputHash);
        when(gateway.updateInputHashIfProcessing(
                1L,
                9201L,
                inputHash
        )).thenReturn(true);
        when(fallbackService.build(
                9201L,
                context
        )).thenReturn(preview);
        when(gateway.markFailedWithFallback(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(9201L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(
                        "AI_STYLE_PLAN_FAILED"
                )
        )).thenReturn(true);

        StylePlanAiJobProcessor.ProcessingResult result =
                processor.process(
                        1L,
                        9201L,
                        request
                );

        assertThat(result.outcome()).isEqualTo(
                StylePlanAiJobProcessor
                        .ProcessingOutcome.FALLBACK
        );

        verify(gateway).markFailedWithFallback(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(9201L),
                org.mockito.ArgumentMatchers.contains(
                        "\"generationType\":\"RULE_BASED\""
                ),
                org.mockito.ArgumentMatchers.eq(
                        "AI_STYLE_PLAN_FAILED"
                )
        );
    }

    @Test
    void alreadyClaimedJobIsNotProcessedAgain() {
        StylePlanJobRequest request =
                new StylePlanJobRequest(
                        "DATE",
                        List.of("NEAT"),
                        null,
                        true,
                        "ko"
                );

        when(gateway.claimProcessing(1L, 9201L))
                .thenReturn(false);

        StylePlanAiJobProcessor.ProcessingResult result =
                processor.process(
                        1L,
                        9201L,
                        request
                );

        assertThat(result.outcome()).isEqualTo(
                StylePlanAiJobProcessor
                        .ProcessingOutcome.NOT_CLAIMED
        );

        verifyNoInteractions(contextService);
        verifyNoInteractions(inputHasher);
        verifyNoInteractions(fallbackService);
    }
}
