package org.likelionhsu.hackathon.recommendation.entity;

public enum RecommendationFeature {
    COMPACT("컴팩트함"),
    SPACIOUS("넉넉한 수납"),
    MULTIWAY("다양한 연출");

    private final String label;

    RecommendationFeature(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
