package org.likelionhsu.hackathon.imageasset.storage;

public record StoredImage(
        String publicId,
        String secureUrl,
        String format,
        long bytes,
        int width,
        int height
) {
}
