package org.likelionhsu.hackathon.itemanalysis.ai.openai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisException;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisException.FailureKind;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisGenerationResult;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisPort;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisRequest;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisResult;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class OpenAiItemAnalysisAdapter
        implements ItemAnalysisPort {

    private static final URI RESPONSES_URI =
            URI.create("https://api.openai.com/v1/responses");
    private static final Duration ATTEMPT_TIMEOUT =
            Duration.ofSeconds(15);

    private static final int MAX_BRAND_NAME_LENGTH = 100;
    private static final int MAX_ITEM_NAME_LENGTH = 200;

    private static final String SYSTEM_PROMPT = """
            당신은 사용자가 업로드한 패션 아이템 사진을 분석해
            마이 아이템 등록 화면의 초깃값을 제안하는 분석기입니다.

            사진에서 시각적으로 확인할 수 있는 정보만 사용하세요.
            보이지 않는 정보나 확신할 수 없는 정보를 추측하지 마세요.

            brandName:
            로고나 문자 등 사진에서 브랜드를 명확히 확인할 수 있을 때만
            브랜드명을 반환하고, 확신할 수 없으면 null을 반환하세요.

            name:
            사진에서 확인 가능한 짧고 일반적인 아이템 이름을 한국어로
            반환하세요. 사진에 명확히 표시되지 않은 모델명이나 상품명을
            만들어내지 마세요. 판단할 수 없으면 null을 반환하세요.

            category:
            BAG, LEATHER_GOODS, FASHION_ACCESSORY, CLOTHING, SHOES 중
            하나로 분류하세요. 판단할 수 없으면 null을 반환하세요.

            primaryColor:
            아이템의 대표 색상을 허용된 enum 값 중 하나로 반환하세요.
            판단할 수 없으면 null을 반환하세요.

            material:
            사진만으로 소재를 충분히 판단할 수 있으면 허용된 enum 값을
            반환하세요. 아이템은 보이지만 소재를 특정하기 어려우면
            UNKNOWN을 사용하고, 이미지 자체를 분석하기 어려우면
            null을 반환하세요.

            구매일, 구매가격, 메모 등 사진으로 알 수 없는 정보를
            생성하거나 추측하지 마세요.
            """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiItemAnalysisAdapter(
            ObjectMapper objectMapper,
            String apiKey,
            String model
    ) {
        this(
                HttpClient
                        .newBuilder()
                        .connectTimeout(ATTEMPT_TIMEOUT)
                        .build(),
                objectMapper,
                apiKey,
                model
        );
    }

    OpenAiItemAnalysisAdapter(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            String apiKey,
            String model
    ) {
        this.httpClient = Objects.requireNonNull(
                httpClient,
                "httpClient는 null일 수 없습니다."
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper는 null일 수 없습니다."
        );
        this.apiKey = requireText(
                apiKey,
                "apiKey"
        );
        this.model = requireText(
                model,
                "model"
        );
    }

    @Override
    public ItemAnalysisGenerationResult analyze(
            ItemAnalysisRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request는 null일 수 없습니다."
        );

        long startedAt = System.nanoTime();

        HttpRequest httpRequest = HttpRequest
                .newBuilder(RESPONSES_URI)
                .timeout(ATTEMPT_TIMEOUT)
                .header(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                createRequestBody(request),
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> response;

        try {
            response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );
        } catch (HttpTimeoutException exception) {
            throw new ItemAnalysisException(
                    FailureKind.TRANSIENT_PROVIDER,
                    "OpenAI 응답 시간이 초과되었습니다.",
                    exception
            );
        } catch (IOException exception) {
            throw new ItemAnalysisException(
                    FailureKind.TRANSIENT_PROVIDER,
                    "OpenAI 통신 중 오류가 발생했습니다.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ItemAnalysisException(
                    FailureKind.NON_RETRYABLE_PROVIDER,
                    "OpenAI 요청이 중단되었습니다.",
                    exception
            );
        }

        long latencyMs = Duration
                .ofNanos(
                        System.nanoTime() - startedAt
                )
                .toMillis();

        validateHttpStatus(
                response.statusCode()
        );

        return parseCompletedResponse(
                response.body(),
                latencyMs
        );
    }

    String createRequestBody(
            ItemAnalysisRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request는 null일 수 없습니다."
        );

        String imageUrl = requireHttpsUrl(
                request.imageUrl()
        );

        Map<String, Object> metadata =
                new LinkedHashMap<>();
        metadata.put(
                "imageAssetId",
                request.imageAssetId()
        );
        metadata.put(
                "format",
                request.format()
        );
        metadata.put(
                "width",
                request.width()
        );
        metadata.put(
                "height",
                request.height()
        );

        Map<String, Object> schema =
                itemAnalysisSchema();

        Map<String, Object> body =
                new LinkedHashMap<>();
        body.put(
                "model",
                model
        );
        body.put(
                "input",
                List.of(
                        Map.of(
                                "role",
                                "system",
                                "content",
                                SYSTEM_PROMPT
                        ),
                        Map.of(
                                "role",
                                "user",
                                "content",
                                List.of(
                                        Map.of(
                                                "type",
                                                "input_text",
                                                "text",
                                                "이미지 메타데이터: "
                                                        + writeJson(
                                                        metadata
                                                )
                                        ),
                                        Map.of(
                                                "type",
                                                "input_image",
                                                "image_url",
                                                imageUrl
                                        )
                                )
                        )
                )
        );
        body.put(
                "text",
                Map.of(
                        "format",
                        Map.of(
                                "type",
                                "json_schema",
                                "name",
                                "item_analysis",
                                "strict",
                                true,
                                "schema",
                                schema
                        )
                )
        );
        body.put(
                "max_output_tokens",
                300
        );
        body.put(
                "store",
                false
        );

        return writeJson(body);
    }

    private Map<String, Object> itemAnalysisSchema() {
        Map<String, Object> properties =
                new LinkedHashMap<>();

        properties.put(
                "brandName",
                nullableStringSchema(
                        MAX_BRAND_NAME_LENGTH
                )
        );
        properties.put(
                "name",
                nullableStringSchema(
                        MAX_ITEM_NAME_LENGTH
                )
        );
        properties.put(
                "category",
                nullableEnumSchema(
                        enumNames(
                                ItemCategory.values()
                        )
                )
        );
        properties.put(
                "primaryColor",
                nullableEnumSchema(
                        enumNames(
                                ColorGroup.values()
                        )
                )
        );
        properties.put(
                "material",
                nullableEnumSchema(
                        enumNames(
                                MaterialGroup.values()
                        )
                )
        );

        Map<String, Object> schema =
                new LinkedHashMap<>();
        schema.put(
                "type",
                "object"
        );
        schema.put(
                "properties",
                properties
        );
        schema.put(
                "required",
                List.of(
                        "brandName",
                        "name",
                        "category",
                        "primaryColor",
                        "material"
                )
        );
        schema.put(
                "additionalProperties",
                false
        );

        return schema;
    }

    private Map<String, Object> nullableStringSchema(
            int maxLength
    ) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException(
                    "maxLength는 1 이상이어야 합니다."
            );
        }

        return Map.of(
                "anyOf",
                List.of(
                        Map.of(
                                "type",
                                "string"
                        ),
                        Map.of(
                                "type",
                                "null"
                        )
                )
        );
    }

    private Map<String, Object> nullableEnumSchema(
            List<String> values
    ) {
        return Map.of(
                "anyOf",
                List.of(
                        Map.of(
                                "type",
                                "string",
                                "enum",
                                values
                        ),
                        Map.of(
                                "type",
                                "null"
                        )
                )
        );
    }

    private List<String> enumNames(
            Enum<?>[] values
    ) {
        return java.util.Arrays
                .stream(values)
                .map(Enum::name)
                .toList();
    }

    private ItemAnalysisGenerationResult
            parseCompletedResponse(
            String responseBody,
            long latencyMs
    ) {
        JsonNode root = readJson(
                responseBody,
                FailureKind.INVALID_RESPONSE,
                "OpenAI 응답 JSON을 해석할 수 없습니다."
        );

        if (!"completed".equals(
                root.path("status").asString()
        )) {
            throw new ItemAnalysisException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI 응답이 완료 상태가 아닙니다."
            );
        }

        String outputText =
                findOutputText(root);

        JsonNode structured = readJson(
                outputText,
                FailureKind.INVALID_RESPONSE,
                "OpenAI Structured Output을 해석할 수 없습니다."
        );

        ItemAnalysisResult result =
                new ItemAnalysisResult(
                        nullableText(
                                structured,
                                "brandName",
                                MAX_BRAND_NAME_LENGTH
                        ),
                        nullableText(
                                structured,
                                "name",
                                MAX_ITEM_NAME_LENGTH
                        ),
                        nullableEnum(
                                structured,
                                "category",
                                ItemCategory.class
                        ),
                        nullableEnum(
                                structured,
                                "primaryColor",
                                ColorGroup.class
                        ),
                        nullableEnum(
                                structured,
                                "material",
                                MaterialGroup.class
                        )
                );

        JsonNode usage = root.path("usage");

        return new ItemAnalysisGenerationResult(
                result,
                nullableInt(
                        usage.get("input_tokens")
                ),
                nullableInt(
                        usage.get("output_tokens")
                ),
                latencyMs
        );
    }

    private String findOutputText(
            JsonNode root
    ) {
        JsonNode output = root.get("output");

        if (output == null || !output.isArray()) {
            throw new ItemAnalysisException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI 응답에 output 배열이 없습니다."
            );
        }

        for (JsonNode item : output) {
            JsonNode content = item.get("content");

            if (content == null
                    || !content.isArray()) {
                continue;
            }

            for (JsonNode part : content) {
                String type =
                        part.path("type").asString();

                if ("refusal".equals(type)) {
                    throw new ItemAnalysisException(
                            FailureKind
                                    .NON_RETRYABLE_PROVIDER,
                            "OpenAI가 요청 처리를 거부했습니다."
                    );
                }

                if ("output_text".equals(type)
                        && part.hasNonNull("text")) {
                    String text = part
                            .get("text")
                            .asString()
                            .trim();

                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
        }

        throw new ItemAnalysisException(
                FailureKind.INVALID_RESPONSE,
                "OpenAI 응답에 output_text가 없습니다."
        );
    }

    private String nullableText(
            JsonNode structured,
            String field,
            int maxLength
    ) {
        JsonNode node = structured.get(field);

        if (node == null) {
            throw invalidField(field);
        }

        if (node.isNull()) {
            return null;
        }

        if (!node.isString()) {
            throw invalidField(field);
        }

        String value =
                node.asString().trim();

        if (value.isEmpty()
                || value.length() > maxLength) {
            throw invalidField(field);
        }

        return value;
    }

    private <E extends Enum<E>> E nullableEnum(
            JsonNode structured,
            String field,
            Class<E> enumType
    ) {
        JsonNode node = structured.get(field);

        if (node == null) {
            throw invalidField(field);
        }

        if (node.isNull()) {
            return null;
        }

        if (!node.isString()) {
            throw invalidField(field);
        }

        String value =
                node.asString().trim();

        try {
            return Enum.valueOf(
                    enumType,
                    value
            );
        } catch (IllegalArgumentException exception) {
            throw new ItemAnalysisException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI Structured Output의 "
                            + field
                            + " 값이 허용 범위를 벗어났습니다.",
                    exception
            );
        }
    }

    private ItemAnalysisException invalidField(
            String field
    ) {
        return new ItemAnalysisException(
                FailureKind.INVALID_RESPONSE,
                "OpenAI Structured Output의 "
                        + field
                        + " 값이 올바르지 않습니다."
        );
    }

    private void validateHttpStatus(
            int statusCode
    ) {
        if (statusCode >= 200
                && statusCode < 300) {
            return;
        }

        boolean retryable =
                statusCode == 408
                        || statusCode == 409
                        || statusCode == 429
                        || statusCode >= 500;

        throw new ItemAnalysisException(
                retryable
                        ? FailureKind.TRANSIENT_PROVIDER
                        : FailureKind
                                .NON_RETRYABLE_PROVIDER,
                "OpenAI API 요청이 실패했습니다. status="
                        + statusCode
        );
    }

    private JsonNode readJson(
            String value,
            FailureKind failureKind,
            String message
    ) {
        if (value == null
                || value.isBlank()) {
            throw new ItemAnalysisException(
                    failureKind,
                    message
            );
        }

        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new ItemAnalysisException(
                    failureKind,
                    message,
                    exception
            );
        }
    }

    private String writeJson(
            Object value
    ) {
        try {
            return objectMapper
                    .writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new ItemAnalysisException(
                    FailureKind.CONFIGURATION,
                    "OpenAI 요청 JSON을 생성할 수 없습니다.",
                    exception
            );
        }
    }

    private Integer nullableInt(
            JsonNode node
    ) {
        if (node == null
                || !node.isIntegralNumber()) {
            return null;
        }

        return node.intValue();
    }

    private String requireHttpsUrl(
            String value
    ) {
        String normalized =
                requireText(
                        value,
                        "imageUrl"
                );

        URI uri;

        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ItemAnalysisException(
                    FailureKind.CONFIGURATION,
                    "imageUrl이 올바른 URI가 아닙니다.",
                    exception
            );
        }

        if (!"https".equalsIgnoreCase(
                uri.getScheme()
        ) || uri.getHost() == null) {
            throw new ItemAnalysisException(
                    FailureKind.CONFIGURATION,
                    "imageUrl은 유효한 HTTPS URL이어야 합니다."
            );
        }

        return normalized;
    }

    private String requireText(
            String value,
            String field
    ) {
        if (value == null
                || value.isBlank()) {
            throw new ItemAnalysisException(
                    FailureKind.CONFIGURATION,
                    field
                            + "는 비어 있을 수 없습니다."
            );
        }

        return value.trim();
    }
}
