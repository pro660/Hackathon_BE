package org.likelionhsu.hackathon.useritem.repository;

import java.util.Locale;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.springframework.data.jpa.domain.Specification;

public final class UserItemSpecification {

    private UserItemSpecification() {
    }

    public static Specification<UserItem> ownedBy(
            Long userId
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("user").get("id"),
                        userId
                );
    }

    public static Specification<UserItem> notDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(
                        root.get("deletedAt")
                );
    }

    public static Specification<UserItem> containsKeyword(
            String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return Specification.unrestricted();
        }

        String pattern = "%"
                + keyword.trim()
                        .toLowerCase(Locale.ROOT)
                + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("name")
                                ),
                                pattern
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("brandName")
                                ),
                                pattern
                        )
                );
    }

    public static Specification<UserItem> hasCategory(
            ItemCategory category
    ) {
        if (category == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("category"),
                        category
                );
    }

    public static Specification<UserItem> hasPrimaryColor(
            ColorGroup color
    ) {
        if (color == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("primaryColor"),
                        color
                );
    }
}
