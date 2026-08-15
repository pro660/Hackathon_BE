package org.likelionhsu.hackathon.recommendation.entity;

public enum RecommendationSeason {
    SPRING("봄"),
    SUMMER("여름"),
    AUTUMN("가을"),
    WINTER("겨울");

    private final String label;

    RecommendationSeason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
