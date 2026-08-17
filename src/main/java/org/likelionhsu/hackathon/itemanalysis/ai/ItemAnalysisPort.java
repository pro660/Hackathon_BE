package org.likelionhsu.hackathon.itemanalysis.ai;

public interface ItemAnalysisPort {

    ItemAnalysisGenerationResult analyze(
            ItemAnalysisRequest request
    );
}
