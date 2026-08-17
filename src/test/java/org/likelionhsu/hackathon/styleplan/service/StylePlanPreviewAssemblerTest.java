package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanAiSelection;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationException;
import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;

class StylePlanPreviewAssemblerTest {

    private final StylePlanPreviewAssembler assembler =
            new StylePlanPreviewAssembler();

    @Test
    void serverCandidateFactsAreUsedForFinalPreview() {
        StylePlanRecommendationContext context =
                context();

        StylePlanAiSelection selection =
                new StylePlanAiSelection(
                        "데이트 룩",
                        "깔끔하게 구성했어요.",
                        List.of(
                                new StylePlanAiSelection
                                        .OwnedItemSelection(
                                        "501",
                                        "BAG"
                                )
                        ),
                        List.of(
                                new StylePlanAiSelection
                                        .ProductSelection(
                                        "101",
                                        "전체 분위기와 잘 어울려요."
                                )
                        )
                );

        StylePlanPreview preview =
                assembler.assemble(
                        9201L,
                        context,
                        selection
                );

        assertThat(preview.generationType())
                .isEqualTo("AI");
        assertThat(preview.ownedItems())
                .hasSize(1);
        assertThat(
                preview.ownedItems()
                        .getFirst()
                        .name()
        ).isEqualTo("서버 보유 가방");
        assertThat(
                preview.ownedItems()
                        .getFirst()
                        .imageUrl()
        ).isEqualTo(
                "https://example.com/item.webp"
        );
        assertThat(
                preview.recommendedProducts()
                        .getFirst()
                        .name()
        ).isEqualTo("서버 MCM 상품");
        assertThat(
                preview.recommendedProducts()
                        .getFirst()
                        .rank()
        ).isEqualTo(1);
    }

    @Test
    void unknownProductIdIsRejectedAsRetryable() {
        StylePlanAiSelection selection =
                new StylePlanAiSelection(
                        "데이트 룩",
                        "설명",
                        List.of(),
                        List.of(
                                new StylePlanAiSelection
                                        .ProductSelection(
                                        "999999",
                                        "이유"
                                )
                        )
                );

        assertThatThrownBy(() ->
                assembler.assemble(
                        9201L,
                        context(),
                        selection
                )
        )
                .isInstanceOf(
                        StylePlanGenerationException.class
                )
                .satisfies(exception ->
                        assertThat(
                                ((StylePlanGenerationException)
                                        exception)
                                        .isRetryable()
                        ).isTrue()
                );
    }

    @Test
    void invalidRoleIsRejected() {
        StylePlanAiSelection selection =
                new StylePlanAiSelection(
                        "데이트 룩",
                        "설명",
                        List.of(
                                new StylePlanAiSelection
                                        .OwnedItemSelection(
                                        "501",
                                        "CLOTHING"
                                )
                        ),
                        List.of()
                );

        assertThatThrownBy(() ->
                assembler.assemble(
                        9201L,
                        context(),
                        selection
                )
        ).isInstanceOf(
                StylePlanGenerationException.class
        );
    }

    private StylePlanRecommendationContext context() {
        return new StylePlanRecommendationContext(
                new StylePlanJobRequest(
                        "DATE",
                        List.of("NEAT"),
                        null,
                        true,
                        "ko"
                ),
                List.of("NEAT"),
                List.of("BLACK"),
                List.of("BAG"),
                List.of(
                        new StylePlanRecommendationContext
                                .OwnedItemCandidate(
                                "501",
                                "서버 보유 가방",
                                "https://example.com/item.webp",
                                "BAG",
                                "BLACK",
                                "LEATHER",
                                3L,
                                5
                        )
                ),
                List.of(
                        new StylePlanRecommendationContext
                                .ProductCandidate(
                                "101",
                                "서버 MCM 상품",
                                "https://example.com/product.webp",
                                "BAG",
                                "BLACK",
                                "LEATHER",
                                List.of(
                                        "DATE",
                                        "NEAT"
                                ),
                                9
                        )
                )
        );
    }
}
