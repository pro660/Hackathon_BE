package org.likelionhsu.hackathon.styleplan.service;

import java.util.List;

import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StylePlanFallbackService {

    private static final int MAX_OWNED_ITEMS = 10;

    private final UserItemRepository userItemRepository;
    private final StylePlanInputHasher inputHasher;

    public StylePlanFallbackService(
            UserItemRepository userItemRepository,
            StylePlanInputHasher inputHasher
    ) {
        this.userItemRepository = userItemRepository;
        this.inputHasher = inputHasher;
    }

    @Transactional(readOnly = true)
    public BuildResult build(
            Long userId,
            Long jobId,
            StylePlanJobRequest request
    ) {
        List<UserItem> activeItems =
                userItemRepository
                        .findAllByUser_IdAndDeletedAtIsNullOrderByIdAsc(
                                userId
                        );

        List<StylePlanPreview.OwnedItem> ownedItems =
                activeItems.stream()
                        .limit(MAX_OWNED_ITEMS)
                        .map(item ->
                                new StylePlanPreview.OwnedItem(
                                        String.valueOf(item.getId()),
                                        item.getName(),
                                        null,
                                        item.getCategory().name(),
                                        0
                                )
                        )
                        .toList();

        ownedItems = withSortOrder(ownedItems);

        StylePlanPreview preview =
                new StylePlanPreview(
                        "job:" + jobId,
                        titleFor(request.occasion()),
                        descriptionFor(
                                request,
                                ownedItems.size()
                        ),
                        ownedItems,
                        List.of(),
                        "RULE_BASED"
                );

        return new BuildResult(
                preview,
                inputHasher.hash(
                        request,
                        activeItems
                )
        );
    }

    private List<StylePlanPreview.OwnedItem>
            withSortOrder(
            List<StylePlanPreview.OwnedItem> items
    ) {
        java.util.ArrayList<StylePlanPreview.OwnedItem>
                result = new java.util.ArrayList<>();

        for (int index = 0;
             index < items.size();
             index++) {
            StylePlanPreview.OwnedItem item =
                    items.get(index);

            result.add(
                    new StylePlanPreview.OwnedItem(
                            item.myItemId(),
                            item.name(),
                            item.imageUrl(),
                            item.role(),
                            index
                    )
            );
        }

        return List.copyOf(result);
    }

    private String titleFor(String occasion) {
        return switch (occasion) {
            case "DATE" -> "데이트 룩";
            case "TRAVEL" -> "여행 룩";
            case "GATHERING" -> "모임 룩";
            case "CEREMONY" -> "격식 있는 룩";
            case "OUTDOOR" -> "아웃도어 룩";
            case "DAILY" -> "데일리 룩";
            default -> "오늘의 추천 룩";
        };
    }

    private String descriptionFor(
            StylePlanJobRequest request,
            int itemCount
    ) {
        String styles = String.join(
                ", ",
                request.styleTags()
        );

        if (itemCount == 0) {
            return styles
                    + " 분위기를 기준으로 추천했지만 "
                    + "현재 등록된 보유 아이템이 없습니다.";
        }

        return styles
                + " 분위기와 보유 아이템 "
                + itemCount
                + "개를 중심으로 구성한 기본 추천입니다.";
    }

    public record BuildResult(
            StylePlanPreview preview,
            String inputHash
    ) {
    }
}
