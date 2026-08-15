package org.likelionhsu.hackathon.preference.dto.response;

import java.util.List;

public record PreferenceResponse(
        List<String> preferredColors,
        List<String> preferredCategories,
        List<String> preferredStyleTags
) {

    public PreferenceResponse {
        preferredColors =
                List.copyOf(preferredColors);

        preferredCategories =
                List.copyOf(preferredCategories);

        preferredStyleTags =
                List.copyOf(preferredStyleTags);
    }
}