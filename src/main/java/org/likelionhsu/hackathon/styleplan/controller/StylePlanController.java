package org.likelionhsu.hackathon.styleplan.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanCreateRequest;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanUpdateRequest;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanCreateResponse;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanDetailResponse;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanListItemResponse;
import org.likelionhsu.hackathon.styleplan.service.StylePlanService;
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

@Tag(
        name = "Style Plans",
        description = "스마트 착용 추천 결과 저장 및 관리 API"
)
@RestController
@RequestMapping("/api/style-plans")
public class StylePlanController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "plannedAt",
            "title"
    );

    private final StylePlanService stylePlanService;

    public StylePlanController(StylePlanService stylePlanService) {
        this.stylePlanService = stylePlanService;
    }

    @Operation(
            summary = "스마트 착용 추천 저장",
            description = """
                    STYLE_PLAN 미리보기를 사용자가 확정한 뒤 저장합니다.
                    AI Job 성공만으로 자동 저장하지 않습니다.
                    aiJobId가 있으면 현재 사용자의 STYLE_PLAN 결과와 아이템/상품 조합을 재검증합니다.
                    AI 성공 결과는 AI, 규칙 기반 fallback은 RULE_BASED, aiJobId가 없는 직접 저장은 MANUAL로 기록합니다.
                    보유 아이템은 최대 10개, MCM 추천 상품은 최대 3개입니다.
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<StylePlanCreateResponse>> create(
            @Valid @RequestBody StylePlanCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        StylePlanCreateResponse response = stylePlanService.create(
                Long.valueOf(jwt.getSubject()),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(
            summary = "스마트 착용 추천 목록 조회",
            description = """
                    현재 로그인 사용자가 저장한 스타일 플랜을 페이지 단위로 조회합니다.
                    status는 선택 필터이며 기본 정렬은 createdAt,desc입니다.
                    빈 페이지는 404가 아니라 items=[]인 200 응답을 반환합니다.
                    """
    )
    @GetMapping
    public ApiResponse<PageResponse<StylePlanListItemResponse>> getStylePlans(
            @RequestParam(required = false) StylePlanStatus status,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "0 이상이어야 합니다.")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "1 이상이어야 합니다.")
            @Max(value = 100, message = "100 이하여야 합니다.")
            int size,
            @RequestParam(name = "sort", required = false)
            List<String> sort,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                parseSort(sort)
        );

        return ApiResponse.success(
                stylePlanService.getStylePlans(
                        Long.valueOf(jwt.getSubject()),
                        status,
                        pageable
                )
        );
    }

    @Operation(
            summary = "스마트 착용 추천 상세 조회",
            description = """
                    현재 로그인 사용자가 소유한 스타일 플랜 상세를 조회합니다.
                    저장된 보유 아이템과 추천 MCM 상품 조합, generationType, version을 반환합니다.
                    장소 추천은 A7에서 연결하며 현재는 places=[]입니다.
                    """
    )
    @GetMapping("/{stylePlanId}")
    public ApiResponse<StylePlanDetailResponse> getStylePlan(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long stylePlanId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                stylePlanService.getStylePlan(
                        Long.valueOf(jwt.getSubject()),
                        stylePlanId
                )
        );
    }

    @Operation(
            summary = "스마트 착용 추천 수정",
            description = """
                    현재 로그인 사용자의 스타일 플랜 metadata를 수정합니다.
                    수정 가능 필드는 title, plannedAt, status이며 조합 자체는 변경하지 않습니다.
                    version 기반 optimistic locking을 사용합니다.
                    plannedAt을 null로 명시하면 저장된 일정을 제거합니다.
                    """
    )
    @PatchMapping("/{stylePlanId}")
    public ApiResponse<StylePlanDetailResponse> updateStylePlan(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long stylePlanId,
            @Valid @RequestBody
            StylePlanUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                stylePlanService.updateStylePlan(
                        Long.valueOf(jwt.getSubject()),
                        stylePlanId,
                        request
                )
        );
    }

    @Operation(
            summary = "스마트 착용 추천 삭제",
            description = """
                    현재 로그인 사용자의 스타일 플랜을 Hard Delete합니다.
                    style_plan_items, style_plan_products, style_plan_places 연결 Row는 DB cascade로 함께 삭제됩니다.
                    UserItem, Product, Place 원본은 삭제하지 않습니다.
                    """
    )
    @DeleteMapping("/{stylePlanId}")
    public ResponseEntity<Void> deleteStylePlan(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long stylePlanId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        stylePlanService.deleteStylePlan(
                Long.valueOf(jwt.getSubject()),
                stylePlanId
        );

        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(List<String> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) {
            return Sort.by(Sort.Order.desc("createdAt"));
        }

        List<String> normalizedValues = normalizeSortValues(sortValues);
        List<Sort.Order> orders = new ArrayList<>();

        for (String sortValue : normalizedValues) {
            String[] parts = sortValue.split(",", -1);
            if (parts.length != 2) {
                throw invalidSort();
            }

            String field = parts[0].trim();
            String direction = parts[1].trim();

            if (!ALLOWED_SORT_FIELDS.contains(field)) {
                throw invalidSort();
            }
            if (!direction.equals("asc") && !direction.equals("desc")) {
                throw invalidSort();
            }

            orders.add(new Sort.Order(
                    Sort.Direction.fromString(direction),
                    field
            ));
        }

        return Sort.by(orders);
    }

    private List<String> normalizeSortValues(List<String> sortValues) {
        List<String> normalizedValues = new ArrayList<>();

        for (int index = 0; index < sortValues.size();) {
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
                    current + "," + sortValues.get(index + 1).trim()
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
