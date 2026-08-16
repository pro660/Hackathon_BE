package org.likelionhsu.hackathon.useritem.dto.response;

public record UserItemImageResponse(
        String imageId,
        String url,
        int sortOrder
) {
}
