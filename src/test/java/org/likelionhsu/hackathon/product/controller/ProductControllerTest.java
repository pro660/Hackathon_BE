package org.likelionhsu.hackathon.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductDetailResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductImageResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductListItemResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductTagsResponse;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void productListReturnsDefaultPage() throws Exception {
        PageResponse<ProductListItemResponse> response =
                new PageResponse<>(
                        List.of(
                                new ProductListItemResponse(
                                        "1",
                                        ProductBrand.MCM,
                                        "MCM Black Bag",
                                        ItemCategory.BAG,
                                        1_500_000L,
                                        ColorGroup.BLACK,
                                        "https://example.com/product.webp"
                                )
                        ),
                        0,
                        20,
                        1,
                        1,
                        false,
                        false
                );

        when(
                productService.getProducts(
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        any(Pageable.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/products")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data.items[0].productId")
                                .value("1")
                )
                .andExpect(
                        jsonPath("$.data.items[0].brand")
                                .value("MCM")
                )
                .andExpect(
                        jsonPath("$.data.items[0].category")
                                .value("BAG")
                )
                .andExpect(
                        jsonPath("$.data.items[0].price")
                                .value(1_500_000)
                )
                .andExpect(
                        jsonPath("$.data.items[0].primaryColor")
                                .value("BLACK")
                )
                .andExpect(
                        jsonPath("$.data.page")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.data.size")
                                .value(20)
                );

        verify(productService)
                .getProducts(
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        any(Pageable.class)
                );
    }

    @Test
    void productListAcceptsFilters() throws Exception {
        PageResponse<ProductListItemResponse> response =
                new PageResponse<>(
                        List.of(),
                        0,
                        10,
                        0,
                        0,
                        false,
                        false
                );

        when(
                productService.getProducts(
                        eq(ItemCategory.BAG),
                        eq(ColorGroup.BLACK),
                        eq(500_000L),
                        eq(2_000_000L),
                        any(Pageable.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/products")
                                .param(
                                        "category",
                                        "BAG"
                                )
                                .param(
                                        "color",
                                        "BLACK"
                                )
                                .param(
                                        "minPrice",
                                        "500000"
                                )
                                .param(
                                        "maxPrice",
                                        "2000000"
                                )
                                .param(
                                        "page",
                                        "0"
                                )
                                .param(
                                        "size",
                                        "10"
                                )
                                .param(
                                        "sort",
                                        "price,asc"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data.items")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.data.page")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.data.size")
                                .value(10)
                );
    }

    @Test
    void productDetailReturnsProduct() throws Exception {
        ProductDetailResponse response =
                new ProductDetailResponse(
                        "1",
                        ProductBrand.MCM,
                        "MCM-SKU-001",
                        "MCM Black Bag",
                        ItemCategory.BAG,
                        "제품 설명",
                        1_500_000L,
                        ColorGroup.BLACK,
                        MaterialGroup.LEATHER,
                        "https://example.com/product",
                        List.of(
                                new ProductImageResponse(
                                        "https://example.com/product.webp",
                                        "MCM Black Bag",
                                        0,
                                        true
                                )
                        ),
                        new ProductTagsResponse(
                                List.of("CASUAL"),
                                List.of("ALL_SEASON"),
                                List.of("DAILY"),
                                List.of("SPACIOUS")
                        )
                );

        when(
                productService.getProduct(1L)
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/products/1")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data.productId")
                                .value("1")
                )
                .andExpect(
                        jsonPath("$.data.sku")
                                .value("MCM-SKU-001")
                )
                .andExpect(
                        jsonPath("$.data.images[0].isPrimary")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data.tags.styles[0]")
                                .value("CASUAL")
                );
    }

    @Test
    void missingProductReturns404() throws Exception {
        when(
                productService.getProduct(999L)
        ).thenThrow(
                new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND
                )
        );

        mockMvc.perform(
                        get("/api/products/999")
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("PRODUCT_NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.error.message")
                                .value("제품을 찾을 수 없습니다.")
                );
    }

    @Test
    void negativePageReturnsValidationError() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .param(
                                        "page",
                                        "-1"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("page")
                );
    }

    @Test
    void oversizedPageSizeReturnsValidationError() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .param(
                                        "size",
                                        "101"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("size")
                );
    }

    @Test
    void negativeMinPriceReturnsValidationError() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .param(
                                        "minPrice",
                                        "-1"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("minPrice")
                );
    }

    @Test
    void minPriceGreaterThanMaxPriceReturnsValidationError()
            throws Exception {

        mockMvc.perform(
                        get("/api/products")
                                .param(
                                        "minPrice",
                                        "2000000"
                                )
                                .param(
                                        "maxPrice",
                                        "1000000"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("minPrice")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].reason")
                                .value(
                                        "minPrice는 maxPrice보다 클 수 없습니다."
                                )
                );
    }

    @Test
    void invalidCategoryReturnsValidationError() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .param(
                                        "category",
                                        "INVALID"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("category")
                );
    }

    @Test
    void invalidSortFieldReturnsValidationError() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .param(
                                        "sort",
                                        "status,desc"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("sort")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].reason")
                                .value(
                                        "지원하지 않는 정렬 조건입니다."
                                )
                );
    }

    @Test
    void invalidSortDirectionReturnsValidationError()
            throws Exception {

        mockMvc.perform(
                        get("/api/products")
                                .param(
                                        "sort",
                                        "price,up"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("sort")
                );
    }

    @Test
    void zeroProductIdReturnsValidationError() throws Exception {
        mockMvc.perform(
                        get("/api/products/0")
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("productId")
                );
    }
}