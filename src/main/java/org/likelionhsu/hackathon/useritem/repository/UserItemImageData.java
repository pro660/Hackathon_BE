package org.likelionhsu.hackathon.useritem.repository;

public record UserItemImageData(
        Long imageId,
        Long userItemId,
        String url,
        int sortOrder
) {
}
