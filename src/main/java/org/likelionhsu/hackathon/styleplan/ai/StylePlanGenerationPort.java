package org.likelionhsu.hackathon.styleplan.ai;

import org.likelionhsu.hackathon.styleplan.service.StylePlanRecommendationContext;

public interface StylePlanGenerationPort {

    StylePlanGenerationResult generate(
            StylePlanRecommendationContext context
    );
}
