package org.likelionhsu.hackathon.wishlist.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.wishlist.dto.response.WishlistItemResponse;
import org.likelionhsu.hackathon.wishlist.service.WishlistService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(
        name = "Wishlists",
        description = "제품 찜 API"
)
@RestController
public class WishlistController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "createdAt"
            );

    private final WishlistService wishlistService;

    public WishlistController(
            WishlistService wishlistService
    ) {
        this.wishlistService = wishlistService;
    }

    @Operation(
            summary = "제품 찜 등록",
            description = "현재 로그인 사용자의 제품 찜 상태를 등록합니다."
    )
    @PutMapping("/api/products/{productId}/favorite")
    public ResponseEntity<Void> addFavorite(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long productId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        wishlistService.addFavorite(
                Long.valueOf(jwt.getSubject()),
                productId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @Operation(
            summary = "제품 찜 해제",
            description = "현재 로그인 사용자의 제품 찜 상태를 해제합니다."
    )
    @DeleteMapping("/api/products/{productId}/favorite")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long productId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        wishlistService.removeFavorite(
                Long.valueOf(jwt.getSubject()),
                productId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @Operation(
            summary = "찜한 제품 목록 조회",
            description = "현재 로그인 사용자가 찜한 제품을 페이지 단위로 조회합니다."
    )
    @GetMapping("/api/wishlists")
    public ApiResponse<PageResponse<WishlistItemResponse>>
    getWishlists(
            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "0 이상이어야 합니다."
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            @Max(
                    value = 100,
                    message = "100 이하여야 합니다."
            )
            int size,

            @RequestParam(
                    name = "sort",
                    required = false
            )
            List<String> sort,

            @AuthenticationPrincipal Jwt jwt
    ) {
        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        parseSort(sort)
                );

        return ApiResponse.success(
                wishlistService.getWishlists(
                        Long.valueOf(jwt.getSubject()),
                        pageable
                )
        );
    }

    private Sort parseSort(
            List<String> sortValues
    ) {
        if (sortValues == null
                || sortValues.isEmpty()) {

            return Sort.by(
                    Sort.Order.desc(
                            "createdAt"
                    )
            );
        }

        List<String> normalizedValues =
                normalizeSortValues(
                        sortValues
                );

        List<Sort.Order> orders =
                new ArrayList<>();

        for (String sortValue : normalizedValues) {
            String[] parts =
                    sortValue.split(
                            ",",
                            -1
                    );

            if (parts.length != 2) {
                throw invalidSort();
            }

            String field =
                    parts[0].trim();

            String direction =
                    parts[1].trim();

            if (!ALLOWED_SORT_FIELDS.contains(field)) {
                throw invalidSort();
            }

            if (!direction.equals("asc")
                    && !direction.equals("desc")) {

                throw invalidSort();
            }

            orders.add(
                    new Sort.Order(
                            Sort.Direction.fromString(
                                    direction
                            ),
                            field
                    )
            );
        }

        return Sort.by(orders);
    }

    private List<String> normalizeSortValues(
            List<String> sortValues
    ) {
        List<String> normalizedValues =
                new ArrayList<>();

        for (int index = 0;
             index < sortValues.size();) {

            String current =
                    sortValues
                            .get(index)
                            .trim();

            if (current.contains(",")) {
                normalizedValues.add(
                        current
                );
                index++;
                continue;
            }

            if (index + 1 >= sortValues.size()) {
                throw invalidSort();
            }

            String direction =
                    sortValues
                            .get(index + 1)
                            .trim();

            normalizedValues.add(
                    current
                            + ","
                            + direction
            );

            index += 2;
        }

        return normalizedValues;
    }

    private RequestValidationException invalidSort() {
        return new RequestValidationException(
                "sort",
                "지원하지 않는 정렬 조건입니다."
        );
    }
}