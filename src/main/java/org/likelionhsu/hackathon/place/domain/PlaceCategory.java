package org.likelionhsu.hackathon.place.domain;

import java.util.Locale;

public enum PlaceCategory {
    CAFE("카페", "CE7"),
    RESTAURANT("음식점", "FD6"),
    CULTURE("문화시설", "CT1"),
    ATTRACTION("관광명소", "AT4"),
    SHOPPING("쇼핑", null),
    OTHER("장소", null);

    private final String searchKeyword;
    private final String kakaoCategoryGroupCode;

    PlaceCategory(String searchKeyword, String kakaoCategoryGroupCode) {
        this.searchKeyword = searchKeyword;
        this.kakaoCategoryGroupCode = kakaoCategoryGroupCode;
    }

    public String searchKeyword() {
        return searchKeyword;
    }

    public String kakaoCategoryGroupCode() {
        return kakaoCategoryGroupCode;
    }

    public static PlaceCategory fromKakao(
            String categoryGroupCode,
            String categoryName
    ) {
        String code = categoryGroupCode == null
                ? ""
                : categoryGroupCode.trim().toUpperCase(Locale.ROOT);

        return switch (code) {
            case "CE7" -> CAFE;
            case "FD6" -> RESTAURANT;
            case "CT1" -> CULTURE;
            case "AT4" -> ATTRACTION;
            case "MT1", "CS2" -> SHOPPING;
            default -> fromCategoryName(categoryName);
        };
    }

    public static PlaceCategory fromCategoryName(String categoryName) {
        String value = categoryName == null ? "" : categoryName;

        if (containsAny(value, "카페", "커피", "디저트")) {
            return CAFE;
        }
        if (containsAny(value, "음식점", "식당", "레스토랑", "요리", "주점")) {
            return RESTAURANT;
        }
        if (containsAny(
                value,
                "문화", "예술", "전시", "미술관", "박물관", "공연", "극장", "갤러리"
        )) {
            return CULTURE;
        }
        if (containsAny(
                value,
                "관광", "명소", "공원", "테마파크", "유원지", "자연"
        )) {
            return ATTRACTION;
        }
        if (containsAny(
                value,
                "쇼핑", "백화점", "아울렛", "대형마트", "편의점", "시장",
                "패션", "의류", "잡화"
        )) {
            return SHOPPING;
        }
        return OTHER;
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
