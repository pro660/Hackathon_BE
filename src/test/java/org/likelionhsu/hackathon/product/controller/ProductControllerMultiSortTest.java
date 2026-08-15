package org.likelionhsu.hackathon.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductListItemResponse;
import org.likelionhsu.hackathon.product.service.ProductService;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
class ProductControllerMultiSortTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void productListAcceptsMultipleSorts() throws Exception {
        PageResponse<ProductListItemResponse> response =
                new PageResponse<>(
                        List.of(),
                        0,
                        20,
                        0,
                        0,
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
                                .param(
                                        "sort",
                                        "price,asc"
                                )
                                .param(
                                        "sort",
                                        "createdAt,desc"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(productService)
                .getProducts(
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        pageableCaptor.capture()
                );

        List<Sort.Order> orders =
                pageableCaptor.getValue()
                        .getSort()
                        .stream()
                        .toList();

        assertThat(orders)
                .extracting(
                        order ->
                                order.getProperty()
                                        + ","
                                        + order.getDirection()
                                                .name()
                                                .toLowerCase()
                )
                .containsExactly(
                        "price,asc",
                        "createdAt,desc"
                );
    }
}
