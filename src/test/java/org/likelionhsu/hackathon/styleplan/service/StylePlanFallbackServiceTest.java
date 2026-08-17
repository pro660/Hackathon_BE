package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StylePlanFallbackServiceTest {

    @Mock
    private UserItemRepository userItemRepository;

    @Test
    void fallbackUsesAtMostTenOwnedItemsWithStableSortOrder() {
        List<UserItem> items = new ArrayList<>();

        for (long id = 1L; id <= 12L; id++) {
            UserItem item = mock(UserItem.class);
            when(item.getId()).thenReturn(id);
            when(item.getVersion()).thenReturn(0L);
            if (id <= 10L) {
                when(item.getName()).thenReturn("아이템 " + id);
            }
            when(item.getCategory()).thenReturn(
                    ItemCategory.BAG
            );
            items.add(item);
        }

        when(userItemRepository
                .findAllByUser_IdAndDeletedAtIsNullOrderByIdAsc(
                        1L
                )
        ).thenReturn(items);

        StylePlanFallbackService service =
                new StylePlanFallbackService(
                        userItemRepository,
                        new StylePlanInputHasher()
                );

        StylePlanFallbackService.BuildResult result =
                service.build(
                        1L,
                        9201L,
                        new StylePlanJobRequest(
                                "DATE",
                                List.of("NEAT"),
                                null,
                                true,
                                "ko"
                        )
                );

        StylePlanPreview preview = result.preview();

        assertThat(preview.previewId())
                .isEqualTo("job:9201");
        assertThat(preview.title())
                .isEqualTo("데이트 룩");
        assertThat(preview.generationType())
                .isEqualTo("RULE_BASED");
        assertThat(preview.ownedItems())
                .hasSize(10);
        assertThat(preview.ownedItems())
                .extracting(
                        StylePlanPreview.OwnedItem::sortOrder
                )
                .containsExactly(
                        0, 1, 2, 3, 4,
                        5, 6, 7, 8, 9
                );
        assertThat(preview.recommendedProducts())
                .isEmpty();
        assertThat(result.inputHash())
                .matches("[0-9a-f]{64}");
    }
}
