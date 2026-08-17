package org.likelionhsu.hackathon.place.dto;

public record PlaceSavedStateResponse(
        String placeId,
        boolean saved
) {
}
