package org.likelionhsu.hackathon.preference.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;

class PreferenceProfileTest {

    @Test
    void createManualCreatesManualState() {
        User user = createUser();

        PreferenceProfile profile =
                PreferenceProfile.createManual(
                        user,
                        List.of(
                                ColorGroup.BLACK,
                                ColorGroup.WHITE
                        ),
                        List.of(
                                ItemCategory.BAG
                        ),
                        List.of(
                                PreferenceStyleTag.CASUAL
                        )
                );

        assertThat(profile.getUser())
                .isSameAs(user);

        assertThat(profile.getPreferredColors())
                .containsExactly(
                        ColorGroup.BLACK,
                        ColorGroup.WHITE
                );

        assertThat(profile.getPreferredCategories())
                .containsExactly(
                        ItemCategory.BAG
                );

        assertThat(profile.getPreferredStyleTags())
                .containsExactly(
                        PreferenceStyleTag.CASUAL
                );

        assertThat(profile.getSummary())
                .isNull();

        assertThat(profile.getConfidence())
                .isNull();

        assertThat(profile.getAnalysisVersion())
                .isEqualTo(
                        "preference-manual-v1"
                );

        assertThat(profile.getAiJobId())
                .isNull();

        assertThat(profile.getAnalyzedAt())
                .isNull();
    }

    @Test
    void applyManualPreferencesReturnsTrueWhenPreferencesChange() {
        PreferenceProfile profile =
                createProfile();

        boolean changed =
                profile.applyManualPreferences(
                        List.of(
                                ColorGroup.BEIGE,
                                ColorGroup.BLACK
                        ),
                        List.of(
                                ItemCategory.CLOTHING
                        ),
                        List.of(
                                PreferenceStyleTag.NEAT
                        )
                );

        assertThat(changed)
                .isTrue();

        assertThat(profile.getPreferredColors())
                .containsExactly(
                        ColorGroup.BEIGE,
                        ColorGroup.BLACK
                );

        assertThat(profile.getPreferredCategories())
                .containsExactly(
                        ItemCategory.CLOTHING
                );

        assertThat(profile.getPreferredStyleTags())
                .containsExactly(
                        PreferenceStyleTag.NEAT
                );
    }

    @Test
    void applyManualPreferencesReturnsFalseForIdenticalManualState() {
        PreferenceProfile profile =
                createProfile();

        boolean changed =
                profile.applyManualPreferences(
                        List.of(
                                ColorGroup.BLACK
                        ),
                        List.of(
                                ItemCategory.BAG
                        ),
                        List.of(
                                PreferenceStyleTag.CASUAL
                        )
                );

        assertThat(changed)
                .isFalse();
    }

    @Test
    void preferenceListsAreDefensivelyCopied() {
        List<ColorGroup> colors =
                new ArrayList<>(
                        List.of(
                                ColorGroup.BLACK
                        )
                );

        List<ItemCategory> categories =
                new ArrayList<>(
                        List.of(
                                ItemCategory.BAG
                        )
                );

        List<PreferenceStyleTag> styleTags =
                new ArrayList<>(
                        List.of(
                                PreferenceStyleTag.CASUAL
                        )
                );

        PreferenceProfile profile =
                PreferenceProfile.createManual(
                        createUser(),
                        colors,
                        categories,
                        styleTags
                );

        colors.add(ColorGroup.WHITE);
        categories.add(ItemCategory.SHOES);
        styleTags.add(
                PreferenceStyleTag.FORMAL
        );

        assertThat(profile.getPreferredColors())
                .containsExactly(
                        ColorGroup.BLACK
                );

        assertThat(profile.getPreferredCategories())
                .containsExactly(
                        ItemCategory.BAG
                );

        assertThat(profile.getPreferredStyleTags())
                .containsExactly(
                        PreferenceStyleTag.CASUAL
                );

        assertThatThrownBy(() ->
                profile.getPreferredColors()
                        .add(ColorGroup.WHITE)
        ).isInstanceOf(
                UnsupportedOperationException.class
        );
    }

    @Test
    void manualUpdateResetsExistingAiState() {
        PreferenceProfile profile =
                createProfile();

        setField(
                profile,
                "summary",
                "AI generated summary"
        );

        setField(
                profile,
                "confidence",
                new BigDecimal("0.9000")
        );

        setField(
                profile,
                "analysisVersion",
                "preference-ai-v1"
        );

        setField(
                profile,
                "aiJobId",
                10L
        );

        setField(
                profile,
                "analyzedAt",
                Instant.parse(
                        "2026-08-16T00:00:00Z"
                )
        );

        boolean changed =
                profile.applyManualPreferences(
                        List.of(
                                ColorGroup.BLACK
                        ),
                        List.of(
                                ItemCategory.BAG
                        ),
                        List.of(
                                PreferenceStyleTag.CASUAL
                        )
                );

        assertThat(changed)
                .isTrue();

        assertThat(profile.getSummary())
                .isNull();

        assertThat(profile.getConfidence())
                .isNull();

        assertThat(profile.getAnalysisVersion())
                .isEqualTo(
                        "preference-manual-v1"
                );

        assertThat(profile.getAiJobId())
                .isNull();

        assertThat(profile.getAnalyzedAt())
                .isNull();
    }

    private PreferenceProfile createProfile() {
        return PreferenceProfile.createManual(
                createUser(),
                List.of(
                        ColorGroup.BLACK
                ),
                List.of(
                        ItemCategory.BAG
                ),
                List.of(
                        PreferenceStyleTag.CASUAL
                )
        );
    }

    private User createUser() {
        return User.local(
                "preference@example.com",
                "preference-user",
                Gender.NOT_SPECIFIED
        );
    }

    private void setField(
            PreferenceProfile profile,
            String fieldName,
            Object value
    ) {
        try {
            Field field =
                    PreferenceProfile.class
                            .getDeclaredField(
                                    fieldName
                            );

            field.setAccessible(true);
            field.set(
                    profile,
                    value
            );
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    exception
            );
        }
    }
}