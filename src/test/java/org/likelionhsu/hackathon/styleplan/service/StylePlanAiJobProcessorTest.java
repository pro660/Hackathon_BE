package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanAiSelection;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationException;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationException.FailureKind;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationPort;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationResult;
import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanAiJobGateway;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

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
    @Mock
    private ObjectProvider<StylePlanGenerationPort>
            portProvider;
    @Mock
    private StylePlanPreviewAssembler assembler;
    @Mock
    private StylePlanAiJobCompletionService
            completionService;
    @Mock
    private StylePlanGenerationPort port;

    private StylePlanAiJobProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new StylePlanAiJobProcessor(
                gateway,
                contextService,
                inputHasher,
                fallbackService,
                portProvider,
                assembler,
                completionService
        );
    }

    @Test
    void successfulGenerationCompletesAiResult() {
        Fixture fixture = fixture();

        when(gateway.claimProcessing(1L, 9201L))
                .thenReturn(true);
        when(contextService.prepare(
                1L,
                fixture.request()
        )).thenReturn(fixture.context());
        when(inputHasher.hash(fixture.context()))
                .thenReturn(fixture.inputHash());
        when(gateway.updateInputHashIfProcessing(
                1L,
                9201L,
                fixture.inputHash()
        )).thenReturn(true);
        when(fallbackService.build(
                9201L,
                fixture.context()
        )).thenReturn(fixture.fallback());
        when(portProvider.getIfAvailable())
                .thenReturn(port);
        when(port.generate(fixture.context()))
                .thenReturn(fixture.generation());
        when(assembler.assemble(
                9201L,
                fixture.context(),
                fixture.generation().selection()
        )).thenReturn(fixture.aiPreview());

        var result = processor.process(
                1L,
                9201L,
                fixture.request()
        );

        assertThat(result.outcome()).isEqualTo(
                StylePlanAiJobProcessor
                        .ProcessingOutcome.AI
        );

        verify(completionService).completeSuccess(
                1L,
                9201L,
                fixture.aiPreview(),
                fixture.generation(),
                0
        );
        verify(completionService, never())
                .completeFallback(
                        org.mockito.ArgumentMatchers
                                .anyLong(),
                        org.mockito.ArgumentMatchers
                                .anyLong(),
                        org.mockito.ArgumentMatchers
                                .any(),
                        org.mockito.ArgumentMatchers
                                .anyString(),
                        org.mockito.ArgumentMatchers
                                .anyInt()
                );
    }

    @Test
    void retryableInvalidSelectionRetriesOnce() {
        Fixture fixture = fixture();

        when(gateway.claimProcessing(1L, 9201L))
                .thenReturn(true);
        when(contextService.prepare(
                1L,
                fixture.request()
        )).thenReturn(fixture.context());
        when(inputHasher.hash(fixture.context()))
                .thenReturn(fixture.inputHash());
        when(gateway.updateInputHashIfProcessing(
                1L,
                9201L,
                fixture.inputHash()
        )).thenReturn(true);
        when(fallbackService.build(
                9201L,
                fixture.context()
        )).thenReturn(fixture.fallback());
        when(portProvider.getIfAvailable())
                .thenReturn(port);
        when(port.generate(fixture.context()))
                .thenReturn(
                        fixture.generation(),
                        fixture.generation()
                );
        when(assembler.assemble(
                9201L,
                fixture.context(),
                fixture.generation().selection()
        ))
                .thenThrow(
                        new StylePlanGenerationException(
                                FailureKind.INVALID_RESPONSE,
                                "unknown id"
                        )
                )
                .thenReturn(fixture.aiPreview());

        var result = processor.process(
                1L,
                9201L,
                fixture.request()
        );

        assertThat(result.outcome()).isEqualTo(
                StylePlanAiJobProcessor
                        .ProcessingOutcome.AI
        );
        verify(port, times(2))
                .generate(fixture.context());
        verify(completionService).completeSuccess(
                1L,
                9201L,
                fixture.aiPreview(),
                fixture.generation(),
                1
        );
    }

    @Test
    void finalProviderFailureKeepsRuleFallback() {
        Fixture fixture = fixture();

        when(gateway.claimProcessing(1L, 9201L))
                .thenReturn(true);
        when(contextService.prepare(
                1L,
                fixture.request()
        )).thenReturn(fixture.context());
        when(inputHasher.hash(fixture.context()))
                .thenReturn(fixture.inputHash());
        when(gateway.updateInputHashIfProcessing(
                1L,
                9201L,
                fixture.inputHash()
        )).thenReturn(true);
        when(fallbackService.build(
                9201L,
                fixture.context()
        )).thenReturn(fixture.fallback());
        when(portProvider.getIfAvailable())
                .thenReturn(port);
        when(port.generate(fixture.context()))
                .thenThrow(
                        new StylePlanGenerationException(
                                FailureKind.TRANSIENT_PROVIDER,
                                "temporary"
                        )
                );

        var result = processor.process(
                1L,
                9201L,
                fixture.request()
        );

        assertThat(result.outcome()).isEqualTo(
                StylePlanAiJobProcessor
                        .ProcessingOutcome.FALLBACK
        );
        verify(port, times(2))
                .generate(fixture.context());
        verify(completionService).completeFallback(
                1L,
                9201L,
                fixture.fallback(),
                "AI_STYLE_PLAN_FAILED",
                1
        );
    }

    @Test
    void missingProviderUsesFallbackWithoutRetry() {
        Fixture fixture = fixture();

        when(gateway.claimProcessing(1L, 9201L))
                .thenReturn(true);
        when(contextService.prepare(
                1L,
                fixture.request()
        )).thenReturn(fixture.context());
        when(inputHasher.hash(fixture.context()))
                .thenReturn(fixture.inputHash());
        when(gateway.updateInputHashIfProcessing(
                1L,
                9201L,
                fixture.inputHash()
        )).thenReturn(true);
        when(fallbackService.build(
                9201L,
                fixture.context()
        )).thenReturn(fixture.fallback());
        when(portProvider.getIfAvailable())
                .thenReturn(null);

        var result = processor.process(
                1L,
                9201L,
                fixture.request()
        );

        assertThat(result.outcome()).isEqualTo(
                StylePlanAiJobProcessor
                        .ProcessingOutcome.FALLBACK
        );
        verify(completionService).completeFallback(
                1L,
                9201L,
                fixture.fallback(),
                "AI_STYLE_PLAN_FAILED",
                0
        );
        verifyNoInteractions(port);
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

        var result = processor.process(
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
        verifyNoInteractions(portProvider);
        verifyNoInteractions(completionService);
    }

    private Fixture fixture() {
        StylePlanJobRequest request =
                new StylePlanJobRequest(
                        "DATE",
                        List.of("NEAT"),
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

        StylePlanAiSelection selection =
                new StylePlanAiSelection(
                        "데이트 룩",
                        "설명",
                        List.of(),
                        List.of()
                );

        StylePlanGenerationResult generation =
                new StylePlanGenerationResult(
                        selection,
                        100,
                        50,
                        120L
                );

        StylePlanPreview fallback =
                new StylePlanPreview(
                        "job:9201",
                        "데이트 룩",
                        "fallback",
                        List.of(),
                        List.of(),
                        "RULE_BASED"
                );

        StylePlanPreview aiPreview =
                new StylePlanPreview(
                        "job:9201",
                        "데이트 룩",
                        "AI",
                        List.of(),
                        List.of(),
                        "AI"
                );

        return new Fixture(
                request,
                context,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                generation,
                fallback,
                aiPreview
        );
    }

    private record Fixture(
            StylePlanJobRequest request,
            StylePlanRecommendationContext context,
            String inputHash,
            StylePlanGenerationResult generation,
            StylePlanPreview fallback,
            StylePlanPreview aiPreview
    ) {
    }
}
