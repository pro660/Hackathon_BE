package org.likelionhsu.hackathon.itemanalysis.ai;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;

public record ItemAnalysisResult(
        String brandName,
        String name,
        ItemCategory category,
        ColorGroup primaryColor,
        MaterialGroup material
) {
}
