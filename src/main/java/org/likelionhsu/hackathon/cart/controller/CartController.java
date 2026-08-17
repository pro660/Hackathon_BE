package org.likelionhsu.hackathon.cart.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.cart.dto.response.CartItemResponse;
import org.likelionhsu.hackathon.cart.service.CartService;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.common.response.PageResponse;
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
        name = "Cart Items",
        description = "MCM 제품 확인 목록 API"
)
@RestController
public class CartController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt");

    private final CartService cartService;

    public CartController(
            CartService cartService
    ) {
        this.cartService = cartService;
    }

    @Operation(
            summary = "제품 담기",
            description = """
                    현재 로그인 사용자의 MCM 제품 확인 목록에 ACTIVE 제품을 담습니다.
                    동일 제품을 다시 담아도 중복 Row를 생성하지 않는 멱등 동작입니다.
                    """
    )
    @PutMapping("/api/products/{productId}/cart")
    public ResponseEntity<Void> addCartItem(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long productId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        cartService.addCartItem(
                Long.valueOf(jwt.getSubject()),
                productId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @Operation(
            summary = "담은 제품 제거",
            description = """
                    현재 로그인 사용자의 MCM 제품 확인 목록에서 제품을 제거합니다.
                    이미 담겨 있지 않아도 성공하는 멱등 동작입니다.
                    """
    )
    @DeleteMapping("/api/products/{productId}/cart")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long productId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        cartService.removeCartItem(
                Long.valueOf(jwt.getSubject()),
                productId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @Operation(
            summary = "담은 제품 목록 조회",
            description = """
                    현재 로그인 사용자가 담은 ACTIVE MCM 제품을 페이지 단위로 조회합니다.
                    제품명, 가격, 이미지, 공식 제품 URL은 현재 Product 정보를 사용합니다.
                    """
    )
    @GetMapping("/api/cart-items")
    public ApiResponse<PageResponse<CartItemResponse>> getCartItems(
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
                cartService.getCartItems(
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
                    Sort.Order.desc("createdAt")
            );
        }

        List<String> normalizedValues =
                normalizeSortValues(sortValues);

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

            if (!ALLOWED_SORT_FIELDS.contains(field)
                    || (!direction.equals("asc")
                    && !direction.equals("desc"))) {
                throw invalidSort();
            }

            orders.add(
                    new Sort.Order(
                            Sort.Direction.fromString(direction),
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
                    sortValues.get(index).trim();

            if (current.contains(",")) {
                normalizedValues.add(current);
                index++;
                continue;
            }

            if (index + 1 >= sortValues.size()) {
                throw invalidSort();
            }

            normalizedValues.add(
                    current
                            + ","
                            + sortValues.get(index + 1).trim()
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