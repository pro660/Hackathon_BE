package org.likelionhsu.hackathon.place.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.dto.PlaceSavedStateResponse;
import org.likelionhsu.hackathon.place.dto.PlaceSearchResponse;
import org.likelionhsu.hackathon.place.dto.SavedPlaceResponse;
import org.likelionhsu.hackathon.place.service.PlaceService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(
        name = "Places",
        description = "Kakao Local 기반 장소 검색·추천·저장 API"
)
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private static final Set<String> ALLOWED_SAVED_SORT_FIELDS =
            Set.of("createdAt");

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @Operation(
            summary = "장소 검색",
            description = """
                    Kakao Local에서 실제 장소를 검색하고 places 캐시에 Upsert합니다.
                    query 또는 latitude/longitude + category 검색 조건이 필요합니다.
                    사용자 좌표 자체는 DB에 저장하지 않습니다.
                    최대 15개를 반환합니다.
                    """
    )
    @GetMapping
    public ApiResponse<PlaceSearchResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PlaceCategory category,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) Integer radius,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                placeService.search(
                        Long.valueOf(jwt.getSubject()),
                        query,
                        category,
                        latitude,
                        longitude,
                        radius
                )
        );
    }

    @Operation(
            summary = "저장 장소 목록",
            description = "현재 로그인 사용자가 저장한 장소를 페이지 단위로 조회합니다."
    )
    @GetMapping("/saved")
    public ApiResponse<PageResponse<SavedPlaceResponse>> getSavedPlaces(
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
                parseSavedSort(sort)
        );

        return ApiResponse.success(
                placeService.getSavedPlaces(
                        Long.valueOf(jwt.getSubject()),
                        pageable
                )
        );
    }

    @Operation(
            summary = "장소 저장",
            description = "장소를 저장 상태로 만듭니다. 같은 요청을 반복해도 멱등입니다."
    )
    @PutMapping("/{placeId}/saved")
    public ApiResponse<PlaceSavedStateResponse> savePlace(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long placeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                placeService.savePlace(
                        Long.valueOf(jwt.getSubject()),
                        placeId
                )
        );
    }

    @Operation(
            summary = "장소 저장 해제",
            description = "장소 저장 상태를 해제합니다. 이미 해제되어 있어도 204를 반환합니다."
    )
    @DeleteMapping("/{placeId}/saved")
    public ResponseEntity<Void> unsavePlace(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long placeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        placeService.unsavePlace(
                Long.valueOf(jwt.getSubject()),
                placeId
        );

        return ResponseEntity.noContent().build();
    }

    private Sort parseSavedSort(List<String> sortValues) {
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

            if (!ALLOWED_SAVED_SORT_FIELDS.contains(field)) {
                throw invalidSort();
            }
            if (!direction.equals("asc")
                    && !direction.equals("desc")) {
                throw invalidSort();
            }

            orders.add(new Sort.Order(
                    Sort.Direction.fromString(direction),
                    field
            ));
        }

        return Sort.by(orders);
    }

    private List<String> normalizeSortValues(
            List<String> sortValues
    ) {
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
                    current + ","
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
