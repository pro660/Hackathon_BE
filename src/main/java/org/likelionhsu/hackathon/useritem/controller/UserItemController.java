package org.likelionhsu.hackathon.useritem.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemCreateRequest;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemUpdateRequest;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemCreateResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemDetailResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemListItemResponse;
import org.likelionhsu.hackathon.useritem.service.UserItemService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Tag(
        name = "My Items",
        description = "현재 로그인 사용자의 마이 아이템 관리 API"
)
@RestController
@RequestMapping("/api/my-items")
public class UserItemController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "createdAt",
                    "name",
                    "purchaseDate",
                    "nextCareDate"
            );

    private final UserItemService userItemService;

    public UserItemController(
            UserItemService userItemService
    ) {
        this.userItemService = userItemService;
    }

    @Operation(
            summary = "마이 아이템 목록 조회",
            description = "이름·브랜드 검색과 카테고리·색상 필터를 적용해 페이지 단위로 조회합니다."
    )
    @GetMapping
    public ApiResponse<PageResponse<UserItemListItemResponse>>
    getMyItems(
            @RequestParam(required = false)
            @Size(
                    max = 200,
                    message = "200자 이하여야 합니다."
            )
            String keyword,

            @RequestParam(required = false)
            ItemCategory category,

            @RequestParam(
                    name = "color",
                    required = false
            )
            ColorGroup color,

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
        Pageable pageable = PageRequest.of(
                page,
                size,
                parseSort(sort)
        );

        return ApiResponse.success(
                userItemService.getMyItems(
                        Long.valueOf(jwt.getSubject()),
                        keyword,
                        category,
                        color,
                        pageable
                )
        );
    }

    @Operation(
            summary = "마이 아이템 상세 조회",
            description = "현재 로그인 사용자가 소유한 마이 아이템 상세 정보를 조회합니다."
    )
    @GetMapping("/{myItemId}")
    public ApiResponse<UserItemDetailResponse> getMyItem(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long myItemId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                userItemService.getMyItem(
                        Long.valueOf(jwt.getSubject()),
                        myItemId
                )
        );
    }

    @Operation(
            summary = "마이 아이템 등록",
            description = "마이 아이템 정보를 먼저 등록하며 이미지는 이후 별도 업로드할 수 있습니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<UserItemCreateResponse>>
    createMyItem(
            @Valid @RequestBody UserItemCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserItemCreateResponse response =
                userItemService.createMyItem(
                        Long.valueOf(jwt.getSubject()),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(
            summary = "마이 아이템 수정",
            description = "version을 확인해 마이 아이템 정보를 부분 수정합니다."
    )
    @PatchMapping("/{myItemId}")
    public ApiResponse<UserItemDetailResponse> updateMyItem(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long myItemId,
            @Valid @RequestBody UserItemUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                userItemService.updateMyItem(
                        Long.valueOf(jwt.getSubject()),
                        myItemId,
                        request
                )
        );
    }

    @Operation(
            summary = "마이 아이템 삭제",
            description = "마이 아이템을 Soft Delete하고 연결 이미지를 삭제 대기 상태로 전환합니다."
    )
    @DeleteMapping("/{myItemId}")
    public ResponseEntity<Void> deleteMyItem(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long myItemId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        userItemService.deleteMyItem(
                Long.valueOf(jwt.getSubject()),
                myItemId
        );

        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(List<String> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) {
            return Sort.by(
                    Sort.Order.desc("createdAt")
            );
        }

        List<String> normalizedValues =
                normalizeSortValues(sortValues);

        List<Sort.Order> orders = new ArrayList<>();

        for (String sortValue : normalizedValues) {
            String[] parts = sortValue.split(",", -1);

            if (parts.length != 2) {
                throw invalidSort();
            }

            String field = parts[0].trim();
            String direction = parts[1].trim();

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
        List<String> normalizedValues = new ArrayList<>();

        for (int index = 0;
             index < sortValues.size();) {
            String current = sortValues.get(index).trim();

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
