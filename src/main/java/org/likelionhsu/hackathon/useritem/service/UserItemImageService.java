package org.likelionhsu.hackathon.useritem.service;

import java.util.List;
import java.util.Objects;

import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.imageasset.storage.ImageStorageException;
import org.likelionhsu.hackathon.imageasset.storage.ImageStoragePort;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemImageLinkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserItemImageService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    UserItemImageService.class
            );

    private final UserItemImageMutationService mutationService;
    private final ImageStoragePort imageStoragePort;
    private final ImageAssetJdbcRepository imageAssetRepository;

    public UserItemImageService(
            UserItemImageMutationService mutationService,
            ImageStoragePort imageStoragePort,
            ImageAssetJdbcRepository imageAssetRepository
    ) {
        this.mutationService =
                Objects.requireNonNull(mutationService);
        this.imageStoragePort =
                Objects.requireNonNull(imageStoragePort);
        this.imageAssetRepository =
                Objects.requireNonNull(imageAssetRepository);
    }

    public UserItemImageLinkResponse attach(
            Long userId,
            Long userItemId,
            Long imageAssetId
    ) {
        UserItemImageMutationService.AttachMutation
                mutation = mutationService.attach(
                userId,
                userItemId,
                imageAssetId
        );

        cleanup(mutation.cleanupTargets());

        return mutation.response();
    }

    public void delete(
            Long userId,
            Long userItemId,
            Long imageAssetId
    ) {
        UserItemImageMutationService.DeleteMutation
                mutation =
                mutationService.deleteLinkedImage(
                        userId,
                        userItemId,
                        imageAssetId
                );

        if (mutation.cleanupTarget() != null) {
            cleanup(
                    List.of(mutation.cleanupTarget())
            );
        }
    }

    private void cleanup(
            List<ImageAssetData> targets
    ) {
        for (ImageAssetData target : targets) {
            try {
                imageStoragePort.delete(
                        target.publicId()
                );
            } catch (ImageStorageException exception) {
                log.warn(
                        "연결 이미지 저장소 삭제에 실패했습니다. "
                                + "DELETE_PENDING으로 재시도합니다. "
                                + "imageAssetId={}",
                        target.id(),
                        exception
                );
                continue;
            }

            imageAssetRepository.markDeleted(
                    target.ownerUserId(),
                    target.id()
            );
        }
    }
}
