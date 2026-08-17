package org.likelionhsu.hackathon.styleplan.dto.request;

import java.time.Instant;

import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;

import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class StylePlanUpdateRequest {

    @Size(
            max = 200,
            message = "200자 이하여야 합니다."
    )
    private String title;
    private boolean titlePresent;

    private Instant plannedAt;
    private boolean plannedAtPresent;

    private StylePlanStatus status;
    private boolean statusPresent;

    @NotNull(message = "필수 입력값입니다.")
    @PositiveOrZero(message = "0 이상이어야 합니다.")
    private Long version;

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    @JsonSetter("plannedAt")
    public void setPlannedAt(Instant plannedAt) {
        this.plannedAt = plannedAt;
        this.plannedAtPresent = true;
    }

    @JsonSetter("status")
    public void setStatus(StylePlanStatus status) {
        this.status = status;
        this.statusPresent = true;
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public boolean isTitlePresent() {
        return titlePresent;
    }

    public Instant getPlannedAt() {
        return plannedAt;
    }

    public boolean isPlannedAtPresent() {
        return plannedAtPresent;
    }

    public StylePlanStatus getStatus() {
        return status;
    }

    public boolean isStatusPresent() {
        return statusPresent;
    }

    public Long getVersion() {
        return version;
    }

    public boolean hasChanges() {
        return titlePresent
                || plannedAtPresent
                || statusPresent;
    }
}
