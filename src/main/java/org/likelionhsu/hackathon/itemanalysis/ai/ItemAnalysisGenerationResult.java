package org.likelionhsu.hackathon.itemanalysis.ai;

import java.util.Objects;

public record ItemAnalysisGenerationResult(
        ItemAnalysisResult result,
        Integer inputTokens,
        Integer outputTokens,
        Long latencyMs
) {

    public ItemAnalysisGenerationResult {
        Objects.requireNonNull(
                result,
                "result는 null일 수 없습니다."
        );
    }
}
