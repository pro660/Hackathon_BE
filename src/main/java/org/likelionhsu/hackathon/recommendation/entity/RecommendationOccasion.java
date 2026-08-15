package org.likelionhsu.hackathon.recommendation.entity;

public enum RecommendationOccasion {
    DAILY("일상"),
    DATE("데이트"),
    TRAVEL("여행"),
    GATHERING("모임"),
    CEREMONY("격식 있는 자리"),
    OUTDOOR("야외 활동"),
    OTHER("선택한 상황");

    private final String label;

    RecommendationOccasion(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
