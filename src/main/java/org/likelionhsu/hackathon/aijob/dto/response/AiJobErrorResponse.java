package org.likelionhsu.hackathon.aijob.dto.response;

public record AiJobErrorResponse(
        String code,
        String message
) {

    public static AiJobErrorResponse fromCode(
            String errorCode
    ) {
        if (errorCode == null || errorCode.isBlank()) {
            return null;
        }

        String message = switch (errorCode) {
            case "AI_JOB_TIMEOUT" ->
                    "AI 작업 처리 시간이 초과되었습니다.";
            case "AI_GENERATION_FAILED" ->
                    "AI 설명 생성에 실패했습니다.";
            case "AI_ITEM_ANALYSIS_FAILED" ->
                    "아이템 이미지 분석에 실패했습니다.";
            case "AI_STYLE_PLAN_FAILED" ->
                    "스마트 착용 추천 생성에 실패했습니다.";
            default ->
                    "AI 작업 처리 중 오류가 발생했습니다.";
        };

        return new AiJobErrorResponse(
                errorCode,
                message
        );
    }
}
