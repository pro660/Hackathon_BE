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
            default ->
                    "AI 작업 처리 중 오류가 발생했습니다.";
        };

        return new AiJobErrorResponse(
                errorCode,
                message
        );
    }
}
