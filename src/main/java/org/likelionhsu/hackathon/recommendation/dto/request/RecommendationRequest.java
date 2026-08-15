package org.likelionhsu.hackathon.recommendation.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RecommendationRequest {

    @NotNull(message = "필수 입력값입니다.")
    private String occasion;

    @NotNull(message = "필수 입력값입니다.")
    private String season;

    @NotNull(message = "필수 입력값입니다.")
    @Size(
            min = 1,
            max = 3,
            message = "1개 이상 3개 이하로 선택해 주세요."
    )
    private List<String> preferredFeatures;

    private String category;

    @Min(
            value = 1,
            message = "1 이상 3 이하로 입력해 주세요."
    )
    @Max(
            value = 3,
            message = "1 이상 3 이하로 입력해 주세요."
    )
    private Integer limit;

    private boolean categoryProvided;
    private boolean limitProvided;

    public RecommendationRequest() {
    }

    public RecommendationRequest(
            String occasion,
            String season,
            List<String> preferredFeatures,
            String category,
            Integer limit
    ) {
        this.occasion = occasion;
        this.season = season;
        this.preferredFeatures = preferredFeatures;
        this.category = category;
        this.limit = limit;
        this.categoryProvided = category != null;
        this.limitProvided = limit != null;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("occasion")
    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("season")
    public void setSeason(String season) {
        this.season = season;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("preferredFeatures")
    public void setPreferredFeatures(List<String> preferredFeatures) {
        this.preferredFeatures = preferredFeatures;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("category")
    public void setCategory(String category) {
        this.categoryProvided = true;
        this.category = category;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("limit")
    public void setLimit(Integer limit) {
        this.limitProvided = true;
        this.limit = limit;
    }

    public String occasion() {
        return occasion;
    }

    public String season() {
        return season;
    }

    public List<String> preferredFeatures() {
        return preferredFeatures;
    }

    public String category() {
        return category;
    }

    public Integer limit() {
        return limit;
    }

    public boolean categoryProvided() {
        return categoryProvided;
    }

    public boolean limitProvided() {
        return limitProvided;
    }
}
