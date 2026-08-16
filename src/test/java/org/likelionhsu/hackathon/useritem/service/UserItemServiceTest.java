package org.likelionhsu.hackathon.useritem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemCreateRequest;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemUpdateRequest;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemCreateResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemDetailResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemListItemResponse;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemAiJobValidator;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageData;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserItemServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant NOW =
            Instant.parse("2026-08-16T00:00:00Z");

    @Mock
    UserItemRepository userItemRepository;

    @Mock
    UserItemImageRepository userItemImageRepository;

    @Mock
    UserItemAiJobValidator userItemAiJobValidator;

    @Mock
    UserRepository userRepository;

    @Mock
    ProductRepository productRepository;

    UserItemService userItemService;

    @BeforeEach
    void setUp() {
        userItemService = new UserItemService(
                userItemRepository,
                userItemImageRepository,
                userItemAiJobValidator,
                userRepository,
                productRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void itemCanBeCreatedWithDefaultBrand() {
        User user = user();

        when(userRepository.getReferenceById(USER_ID))
                .thenReturn(user);

        when(userItemRepository.save(any(UserItem.class)))
                .thenAnswer(invocation -> {
                    UserItem item = invocation.getArgument(0);
                    ReflectionTestUtils.setField(item, "id", 10L);
                    return item;
                });

        UserItemCreateResponse response =
                userItemService.createMyItem(
                        USER_ID,
                        new UserItemCreateRequest(
                                null,
                                null,
                                " 브라운 토트백 ",
                                ItemCategory.BAG,
                                ColorGroup.BROWN,
                                MaterialGroup.LEATHER,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                );

        assertThat(response.myItemId()).isEqualTo("10");

        verify(userItemRepository).save(any(UserItem.class));
        verify(userItemAiJobValidator, never())
                .validateOwnedSucceededItemAnalysis(any(), any());
    }

    @Test
    void itemListUsesPrimaryItemImages() {
        UserItem item = item(10L, 0L);
        PageRequest pageable = PageRequest.of(0, 20);

        when(userItemRepository.findAll(
                any(Specification.class),
                any(PageRequest.class)
        ))
                .thenReturn(
                        new PageImpl<>(
                                List.of(item),
                                pageable,
                                1
                        )
                );

        when(userItemImageRepository.findPrimaryImageUrls(
                USER_ID,
                List.of(10L)
        )).thenReturn(
                Map.of(
                        10L,
                        "https://example.com/item.webp"
                )
        );

        PageResponse<UserItemListItemResponse> response =
                userItemService.getMyItems(
                        USER_ID,
                        "토트",
                        ItemCategory.BAG,
                        ColorGroup.BROWN,
                        pageable
                );

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.items().getFirst().myItemId())
                .isEqualTo("10");
        assertThat(response.items().getFirst().primaryImageUrl())
                .isEqualTo("https://example.com/item.webp");
    }

    @Test
    void itemDetailReturnsOrderedImages() {
        UserItem item = item(10L, 2L);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        when(userItemImageRepository.findActiveImages(
                USER_ID,
                10L
        )).thenReturn(
                List.of(
                        new UserItemImageData(
                                100L,
                                10L,
                                "https://example.com/item.webp",
                                0
                        )
                )
        );

        UserItemDetailResponse response =
                userItemService.getMyItem(USER_ID, 10L);

        assertThat(response.myItemId()).isEqualTo("10");
        assertThat(response.images()).hasSize(1);
        assertThat(response.images().getFirst().imageId())
                .isEqualTo("100");
        assertThat(response.version()).isEqualTo(2L);
    }

    @Test
    void anotherUsersItemIsHiddenAsNotFound() {
        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        999L,
                        USER_ID
                )).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> userItemService.getMyItem(USER_ID, 999L)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(ErrorCode.MY_ITEM_NOT_FOUND)
                );
    }

    @Test
    void staleVersionIsRejected() {
        UserItem item = item(10L, 3L);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setName("수정 이름");
        request.setVersion(2L);

        assertThatThrownBy(
                () -> userItemService.updateMyItem(
                        USER_ID,
                        10L,
                        request
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.RESOURCE_VERSION_CONFLICT
                        )
                );

        verify(userItemRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void nullablePatchFieldCanBeCleared() {
        UserItem item = item(10L, 2L);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        when(userItemImageRepository.findActiveImages(
                USER_ID,
                10L
        )).thenReturn(List.of());

        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setMemo(null);
        request.setVersion(2L);

        UserItemDetailResponse response =
                userItemService.updateMyItem(
                        USER_ID,
                        10L,
                        request
                );

        assertThat(response.memo()).isNull();
        verify(userItemRepository).saveAndFlush(item);
    }

    @Test
    void itemDeleteSoftDeletesAndMarksImagesPending() {
        UserItem item = item(10L, 0L);

        when(userItemRepository.findByIdAndUser_Id(
                10L,
                USER_ID
        )).thenReturn(Optional.of(item));

        userItemService.deleteMyItem(USER_ID, 10L);

        assertThat(item.getDeletedAt()).isEqualTo(NOW);
        verify(userItemImageRepository)
                .markDeletePending(USER_ID, 10L);
        verify(userItemRepository).saveAndFlush(item);
    }

    @Test
    void repeatedDeleteIsIdempotent() {
        UserItem item = item(10L, 1L);
        ReflectionTestUtils.setField(item, "deletedAt", NOW);

        when(userItemRepository.findByIdAndUser_Id(
                10L,
                USER_ID
        )).thenReturn(Optional.of(item));

        userItemService.deleteMyItem(USER_ID, 10L);

        verify(userItemImageRepository, never())
                .markDeletePending(any(), any());
        verify(userItemRepository, never())
                .saveAndFlush(any());
    }

    private User user() {
        User user = User.local(
                "item@example.com",
                "아이템사용자",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private UserItem item(Long id, Long version) {
        UserItem item = UserItem.create(
                user(),
                null,
                "MCM",
                "브라운 토트백",
                ItemCategory.BAG,
                ColorGroup.BROWN,
                MaterialGroup.LEATHER,
                MaterialSource.USER_CONFIRMED,
                null,
                null,
                "메모",
                null,
                null
        );

        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "version", version);
        ReflectionTestUtils.setField(item, "createdAt", NOW);
        ReflectionTestUtils.setField(item, "updatedAt", NOW);
        return item;
    }
}
