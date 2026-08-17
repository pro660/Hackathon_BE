package org.likelionhsu.hackathon.place.dto;

import java.util.List;

public record PlaceSearchResponse(
        List<PlaceResponse> items
) {
    public PlaceSearchResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
