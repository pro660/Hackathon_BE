package org.likelionhsu.hackathon.product.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductDetailResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductListItemResponse;
import org.likelionhsu.hackathon.product.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Products",
        description = "MCM 제품 카탈로그 API"
)
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "createdAt",
                    "name",
                    "price"
            );

    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    @Operation(
            summary = "제품 목록 조회",
            description = "MCM 제품 목록을 필터링하고 페이지 단위로 조회합니다."
    )
    @GetMapping
    public ApiResponse<PageResponse<ProductListItemResponse>>
    getProducts(
            @RequestParam(required = false)
            ItemCategory category,

            @RequestParam(
                    name = "color",
                    required = false
            )
            ColorGroup color,

            @RequestParam(required = false)
            @PositiveOrZero(
                    message = "0 이상이어야 합니다."
            )
            Long minPrice,

            @RequestParam(required = false)
            @PositiveOrZero(
                    message = "0 이상이어야 합니다."
            )
            Long maxPrice,

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
            List<String> sort
    ) {
        validatePriceRange(
                minPrice,
                maxPrice
        );

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        parseSort(sort)
                );

        return ApiResponse.success(
                productService.getProducts(
                        category,
                        color,
                        minPrice,
                        maxPrice,
                        pageable
                )
        );
    }

    @Operation(
            summary = "제품 상세 조회",
            description = "제품 ID를 사용해 MCM 제품의 상세 정보를 조회합니다."
    )
    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> getProduct(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long productId
    ) {
        return ApiResponse.success(
                productService.getProduct(
                        productId
                )
        );
    }

    private void validatePriceRange(
            Long minPrice,
            Long maxPrice
    ) {
        if (minPrice != null
                && maxPrice != null
                && minPrice > maxPrice) {

            throw new RequestValidationException(
                    "minPrice",
                    "minPrice는 maxPrice보다 클 수 없습니다."
            );
        }
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

            if (!ALLOWED_SORT_FIELDS.contains(field)) {
                throw invalidSort();
            }

            if (!direction.equals("asc")
                    && !direction.equals("desc")) {
                throw invalidSort();
            }

            Sort.Direction sortDirection =
                    Sort.Direction.fromString(
                            direction
                    );

            orders.add(
                    new Sort.Order(
                            sortDirection,
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

            String direction =
                    sortValues.get(index + 1)
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
