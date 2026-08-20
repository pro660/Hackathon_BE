package org.likelionhsu.hackathon.styleplan.ai.openai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.likelionhsu.hackathon.styleplan.ai.StylePlanAiSelection;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationException;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationException.FailureKind;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationPort;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationResult;
import org.likelionhsu.hackathon.styleplan.service.StylePlanRecommendationContext;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class OpenAiStylePlanAdapter
        implements StylePlanGenerationPort {

    private static final URI RESPONSES_URI =
            URI.create(
                    "https://api.openai.com/v1/responses"
            );
    private static final Duration ATTEMPT_TIMEOUT =
            Duration.ofSeconds(15);
    private static final int MAX_OUTPUT_TOKENS = 1500;

    private static final String SYSTEM_PROMPT = """
            당신은 스마트 착용 추천 생성기입니다.
            반드시 입력 JSON에 포함된 서버 후보만 사용하세요.
            ownedItems의 myItemId와 recommendedProducts의 productId는
            입력에 실제 존재하는 ID만 그대로 선택해야 합니다.
            새로운 상품, 보유 아이템, 가격, 재고, 소재 사실을 만들거나 추측하지 마세요.
            title, description, reason만 자연스러운 한국어로 작성하세요.
            role은 MAIN, TOP, BOTTOM, SHOES, BAG, ACCESSORY 중 하나만 사용하세요.
            casualFormalLevel은 1=캐주얼, 10=포멀인 10단계 값입니다.
            neatGlamorousLevel은 1=깔끔, 10=화려인 10단계 값입니다.
            두 스타일 강도 값을 추천 분위기에 직접 반영하세요.
            보유 아이템은 최대 10개, 추천 MCM 상품은 최대 3개만 선택하세요.
            """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiStylePlanAdapter(
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

    OpenAiStylePlanAdapter(
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
        this.apiKey = requireText(apiKey, "apiKey");
        this.model = requireText(model, "model");
    }

    @Override
    public StylePlanGenerationResult generate(
            StylePlanRecommendationContext context
    ) {
        Objects.requireNonNull(
                context,
                "context는 null일 수 없습니다."
        );

        long startedAt = System.nanoTime();

        HttpRequest request = HttpRequest
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
                                createRequestBody(context),
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> response;

        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );
        } catch (HttpTimeoutException exception) {
            throw new StylePlanGenerationException(
                    FailureKind.TRANSIENT_PROVIDER,
                    "OpenAI 응답 시간이 초과되었습니다.",
                    exception
            );
        } catch (IOException exception) {
            throw new StylePlanGenerationException(
                    FailureKind.TRANSIENT_PROVIDER,
                    "OpenAI 통신 중 오류가 발생했습니다.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new StylePlanGenerationException(
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

        validateHttpStatus(response.statusCode());

        return parseCompletedResponse(
                response.body(),
                latencyMs
        );
    }

    String createRequestBody(
            StylePlanRecommendationContext context
    ) {
        Map<String, Object> body =
                new LinkedHashMap<>();
        body.put("model", model);
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
                                writeJson(
                                        facts(context)
                                )
                        )
                )
        );
        body.put(
                "text",
                Map.of(
                        "format",
                        format()
                )
        );
        body.put(
                "max_output_tokens",
                MAX_OUTPUT_TOKENS
        );
        body.put("store", false);

        return writeJson(body);
    }

    private Map<String, Object> facts(
            StylePlanRecommendationContext context
    ) {
        Map<String, Object> facts =
                new LinkedHashMap<>();
        facts.put(
                "request",
                context.request()
        );
        facts.put(
                "preferredStyleTags",
                context.preferredStyleTags()
        );
        facts.put(
                "preferredColors",
                context.preferredColors()
        );
        facts.put(
                "preferredCategories",
                context.preferredCategories()
        );
        facts.put(
                "ownedItemCandidates",
                context.ownedItems()
        );
        facts.put(
                "productCandidates",
                context.productCandidates()
        );
        return facts;
    }

    private Map<String, Object> format() {
        Map<String, Object> format =
                new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "style_plan_preview");
        format.put("strict", true);
        format.put("schema", schema());
        return format;
    }

    private Map<String, Object> schema() {
        Map<String, Object> ownedItem =
                objectSchema(
                        Map.of(
                                "myItemId",
                                Map.of("type", "string"),
                                "role",
                                Map.of(
                                        "type", "string",
                                        "enum",
                                        List.of(
                                                "MAIN",
                                                "TOP",
                                                "BOTTOM",
                                                "SHOES",
                                                "BAG",
                                                "ACCESSORY"
                                        )
                                )
                        ),
                        List.of(
                                "myItemId",
                                "role"
                        )
                );

        Map<String, Object> product =
                objectSchema(
                        Map.of(
                                "productId",
                                Map.of("type", "string"),
                                "reason",
                                Map.of("type", "string")
                        ),
                        List.of(
                                "productId",
                                "reason"
                        )
                );

        Map<String, Object> properties =
                new LinkedHashMap<>();
        properties.put(
                "title",
                Map.of("type", "string")
        );
        properties.put(
                "description",
                Map.of("type", "string")
        );
        properties.put(
                "ownedItems",
                Map.of(
                        "type", "array",
                        "items", ownedItem
                )
        );
        properties.put(
                "recommendedProducts",
                Map.of(
                        "type", "array",
                        "items", product
                )
        );

        return objectSchema(
                properties,
                List.of(
                        "title",
                        "description",
                        "ownedItems",
                        "recommendedProducts"
                )
        );
    }

    private Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required
    ) {
        Map<String, Object> schema =
                new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put(
                "additionalProperties",
                false
        );
        return schema;
    }

    private StylePlanGenerationResult
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
            throw new StylePlanGenerationException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI 응답이 완료 상태가 아닙니다."
            );
        }

        String outputText = findOutputText(root);
        JsonNode structured = readJson(
                outputText,
                FailureKind.INVALID_RESPONSE,
                "OpenAI Structured Output을 해석할 수 없습니다."
        );

        StylePlanAiSelection selection =
                parseSelection(structured);

        JsonNode usage = root.path("usage");

        return new StylePlanGenerationResult(
                selection,
                nullableInt(
                        usage.get("input_tokens")
                ),
                nullableInt(
                        usage.get("output_tokens")
                ),
                latencyMs
        );
    }

    private StylePlanAiSelection parseSelection(
            JsonNode structured
    ) {
        String title = requiredText(
                structured,
                "title"
        );
        String description = requiredText(
                structured,
                "description"
        );

        JsonNode owned = requiredArray(
                structured,
                "ownedItems"
        );
        List<StylePlanAiSelection
                .OwnedItemSelection> ownedItems =
                new ArrayList<>();

        for (JsonNode item : owned) {
            ownedItems.add(
                    new StylePlanAiSelection
                            .OwnedItemSelection(
                            requiredText(
                                    item,
                                    "myItemId"
                            ),
                            requiredText(
                                    item,
                                    "role"
                            )
                    )
            );
        }

        JsonNode products = requiredArray(
                structured,
                "recommendedProducts"
        );
        List<StylePlanAiSelection.ProductSelection>
                recommendedProducts =
                new ArrayList<>();

        for (JsonNode product : products) {
            recommendedProducts.add(
                    new StylePlanAiSelection
                            .ProductSelection(
                            requiredText(
                                    product,
                                    "productId"
                            ),
                            requiredText(
                                    product,
                                    "reason"
                            )
                    )
            );
        }

        return new StylePlanAiSelection(
                title,
                description,
                ownedItems,
                recommendedProducts
        );
    }

    private JsonNode requiredArray(
            JsonNode parent,
            String field
    ) {
        JsonNode node = parent.get(field);

        if (node == null || !node.isArray()) {
            throw new StylePlanGenerationException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI Structured Output에 "
                            + field
                            + " 배열이 없습니다."
            );
        }

        return node;
    }

    private String requiredText(
            JsonNode parent,
            String field
    ) {
        JsonNode node = parent.get(field);

        if (node == null || !node.isString()) {
            throw new StylePlanGenerationException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI Structured Output에 "
                            + field
                            + " 문자열이 없습니다."
            );
        }

        String value = node.asString().trim();

        if (value.isEmpty()) {
            throw new StylePlanGenerationException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI Structured Output의 "
                            + field
                            + " 값이 비어 있습니다."
            );
        }

        return value;
    }

    private String findOutputText(
            JsonNode root
    ) {
        JsonNode output = root.get("output");

        if (output == null || !output.isArray()) {
            throw new StylePlanGenerationException(
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
                    throw new StylePlanGenerationException(
                            FailureKind.NON_RETRYABLE_PROVIDER,
                            "OpenAI가 요청 처리를 거부했습니다."
                    );
                }

                if ("output_text".equals(type)
                        && part.hasNonNull("text")) {
                    String text =
                            part.get("text")
                                    .asString()
                                    .trim();

                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
        }

        throw new StylePlanGenerationException(
                FailureKind.INVALID_RESPONSE,
                "OpenAI 응답에 output_text가 없습니다."
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

        throw new StylePlanGenerationException(
                retryable
                        ? FailureKind.TRANSIENT_PROVIDER
                        : FailureKind.NON_RETRYABLE_PROVIDER,
                "OpenAI API 요청이 실패했습니다. status="
                        + statusCode
        );
    }

    private JsonNode readJson(
            String value,
            FailureKind failureKind,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new StylePlanGenerationException(
                    failureKind,
                    message
            );
        }

        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new StylePlanGenerationException(
                    failureKind,
                    message,
                    exception
            );
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(
                    value
            );
        } catch (JacksonException exception) {
            throw new StylePlanGenerationException(
                    FailureKind.CONFIGURATION,
                    "OpenAI 요청 JSON을 생성할 수 없습니다.",
                    exception
            );
        }
    }

    private Integer nullableInt(JsonNode node) {
        if (node == null
                || !node.isIntegralNumber()) {
            return null;
        }

        return node.intValue();
    }

    private String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new StylePlanGenerationException(
                    FailureKind.CONFIGURATION,
                    field + "는 비어 있을 수 없습니다."
            );
        }

        return value.trim();
    }
}
