package org.likelionhsu.hackathon.imageasset.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.cloudinary.Cloudinary;

public class CloudinaryImageStorageAdapter
        implements ImageStoragePort {

    private static final String ITEM_FOLDER =
            "wear-it/user-items";

    private final Cloudinary cloudinary;

    public CloudinaryImageStorageAdapter(
            Cloudinary cloudinary
    ) {
        this.cloudinary = Objects.requireNonNull(
                cloudinary,
                "cloudinary는 null일 수 없습니다."
        );
    }

    @Override
    public StoredImage upload(
            ImageStorageUploadRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request는 null일 수 없습니다."
        );

        try {
            Map<?, ?> result = cloudinary
                    .uploader()
                    .upload(
                            request.bytes(),
                            uploadOptions(request.publicId())
                    );

            return toStoredImage(result);
        } catch (ImageStorageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ImageStorageException(
                    "이미지 저장소 업로드에 실패했습니다.",
                    exception
            );
        }
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException(
                    "publicId는 비어 있을 수 없습니다."
            );
        }

        try {
            Map<?, ?> result = cloudinary
                    .uploader()
                    .destroy(
                            publicId.trim(),
                            deleteOptions()
                    );

            String deletionResult = requireText(
                    result,
                    "result"
            );

            if (!"ok".equalsIgnoreCase(deletionResult)
                    && !"not found".equalsIgnoreCase(
                    deletionResult
            )) {
                throw new ImageStorageException(
                        "이미지 저장소 삭제 결과가 올바르지 않습니다."
                );
            }
        } catch (ImageStorageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ImageStorageException(
                    "이미지 저장소 삭제에 실패했습니다.",
                    exception
            );
        }
    }

    StoredImage toStoredImage(Map<?, ?> result) {
        Objects.requireNonNull(
                result,
                "Cloudinary 응답은 null일 수 없습니다."
        );

        String publicId = requireText(
                result,
                "public_id"
        );
        String secureUrl = requireText(
                result,
                "secure_url"
        );
        String format = requireText(
                result,
                "format"
        );

        long bytes = requirePositiveLong(
                result,
                "bytes"
        );
        int width = requirePositiveInt(
                result,
                "width"
        );
        int height = requirePositiveInt(
                result,
                "height"
        );

        return new StoredImage(
                publicId,
                secureUrl,
                format,
                bytes,
                width,
                height
        );
    }

    private Map<String, Object> uploadOptions(
            String publicId
    ) {
        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("folder", ITEM_FOLDER);
        options.put("public_id", publicId);
        options.put("overwrite", false);
        options.put("unique_filename", false);
        options.put("use_filename", false);
        options.put("discard_original_filename", true);

        return options;
    }

    private Map<String, Object> deleteOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("invalidate", true);

        return options;
    }

    private String requireText(
            Map<?, ?> result,
            String key
    ) {
        Object value = result.get(key);

        if (!(value instanceof String text)
                || text.isBlank()) {
            throw new ImageStorageException(
                    "이미지 저장소 응답의 "
                            + key
                            + " 값이 올바르지 않습니다."
            );
        }

        return text;
    }

    private long requirePositiveLong(
            Map<?, ?> result,
            String key
    ) {
        Object value = result.get(key);

        if (!(value instanceof Number number)
                || number.longValue() <= 0) {
            throw new ImageStorageException(
                    "이미지 저장소 응답의 "
                            + key
                            + " 값이 올바르지 않습니다."
            );
        }

        return number.longValue();
    }

    private int requirePositiveInt(
            Map<?, ?> result,
            String key
    ) {
        Object value = result.get(key);

        if (!(value instanceof Number number)) {
            throw new ImageStorageException(
                    "이미지 저장소 응답의 "
                            + key
                            + " 값이 올바르지 않습니다."
            );
        }

        long numericValue = number.longValue();

        if (numericValue <= 0
                || numericValue > Integer.MAX_VALUE) {
            throw new ImageStorageException(
                    "이미지 저장소 응답의 "
                            + key
                            + " 값이 올바르지 않습니다."
            );
        }

        return (int) numericValue;
    }
}
