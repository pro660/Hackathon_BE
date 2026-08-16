package org.likelionhsu.hackathon.purchaseutility.ai.openai;

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

import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationException;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationException.FailureKind;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationPort;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationRequest;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationResult;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class OpenAiPurchaseUtilityExplanationAdapter
        implements PurchaseUtilityExplanationPort {

    private static final URI RESPONSES_URI =
            URI.create("https://api.openai.com/v1/responses");
    private static final Duration ATTEMPT_TIMEOUT =
            Duration.ofSeconds(15);
    private static final int MAX_SUMMARY_LENGTH = 400;

    private static final String SYSTEM_PROMPT = """
            당신은 명품 구매 전 활용 가능성 분석의 설명 생성기입니다.
            반드시 입력 JSON에 포함된 서버 계산 결과와 사실만 사용하세요.
            입력에 없는 브랜드 특성, 소재 특성, 수치, 사용자 취향, 코디 사실을 새로 만들거나 추측하지 마세요.
            summary는 한국어 2~3문장으로 작성하고 약 400자 이내로 유지하세요.
            핵심은 사용자의 기존 아이템과의 조합 가능성, 취향 적합도, 계절 활용성, 관리 난이도를 간결하게 설명하는 것입니다.
            """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiPurchaseUtilityExplanationAdapter(
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

    OpenAiPurchaseUtilityExplanationAdapter(
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
    public PurchaseUtilityExplanationResult generate(
            PurchaseUtilityExplanationRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request는 null일 수 없습니다."
        );

        long startedAt = System.nanoTime();

        HttpRequest httpRequest = HttpRequest
                .newBuilder(RESPONSES_URI)
                .timeout(ATTEMPT_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
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
            throw new PurchaseUtilityExplanationException(
                    FailureKind.TRANSIENT_PROVIDER,
                    "OpenAI 응답 시간이 초과되었습니다.",
                    exception
            );
        } catch (IOException exception) {
            throw new PurchaseUtilityExplanationException(
                    FailureKind.TRANSIENT_PROVIDER,
                    "OpenAI 통신 중 오류가 발생했습니다.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PurchaseUtilityExplanationException(
                    FailureKind.NON_RETRYABLE_PROVIDER,
                    "OpenAI 요청이 중단되었습니다.",
                    exception
            );
        }

        long latencyMs = Duration
                .ofNanos(System.nanoTime() - startedAt)
                .toMillis();

        validateHttpStatus(response.statusCode());

        return parseCompletedResponse(
                response.body(),
                latencyMs
        );
    }

    private String createRequestBody(
            PurchaseUtilityExplanationRequest request
    ) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("scorePolicyVersion", request.scorePolicyVersion());
        facts.put("product", request.product());
        facts.put("utilityScore", request.utilityScore());
        facts.put("factors", request.factors());
        facts.put("compatibleItemCount", request.compatibleItemCount());
        facts.put("compatibleItems", request.compatibleItems());
        facts.put("careDifficulty", request.careDifficulty());
        facts.put("language", request.language());

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "summary", Map.of(
                                "type", "string"
                        )
                ),
                "required", List.of("summary"),
                "additionalProperties", false
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put(
                "input",
                List.of(
                        Map.of(
                                "role", "system",
                                "content", SYSTEM_PROMPT
                        ),
                        Map.of(
                                "role", "user",
                                "content", writeJson(facts)
                        )
                )
        );
        body.put(
                "text",
                Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", "purchase_utility_summary",
                                "strict", true,
                                "schema", schema
                        )
                )
        );
        body.put("max_output_tokens", 300);
        body.put("store", false);

        return writeJson(body);
    }

    private PurchaseUtilityExplanationResult parseCompletedResponse(
            String responseBody,
            long latencyMs
    ) {
        JsonNode root = readJson(
                responseBody,
                FailureKind.INVALID_RESPONSE,
                "OpenAI 응답 JSON을 해석할 수 없습니다."
        );

        if (!"completed".equals(root.path("status").asString())) {
            throw new PurchaseUtilityExplanationException(
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

        JsonNode summaryNode = structured.get("summary");

        if (summaryNode == null || !summaryNode.isString()) {
            throw new PurchaseUtilityExplanationException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI Structured Output에 summary가 없습니다."
            );
        }

        String summary = summaryNode.asString().trim();

        if (summary.isEmpty() || summary.length() > MAX_SUMMARY_LENGTH) {
            throw new PurchaseUtilityExplanationException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI summary가 길이 규칙을 만족하지 않습니다."
            );
        }

        JsonNode usage = root.path("usage");

        return new PurchaseUtilityExplanationResult(
                summary,
                nullableInt(usage.get("input_tokens")),
                nullableInt(usage.get("output_tokens")),
                latencyMs
        );
    }

    private String findOutputText(
            JsonNode root
    ) {
        JsonNode output = root.get("output");

        if (output == null || !output.isArray()) {
            throw new PurchaseUtilityExplanationException(
                    FailureKind.INVALID_RESPONSE,
                    "OpenAI 응답에 output 배열이 없습니다."
            );
        }

        for (JsonNode item : output) {
            JsonNode content = item.get("content");

            if (content == null || !content.isArray()) {
                continue;
            }

            for (JsonNode part : content) {
                String type = part.path("type").asString();

                if ("refusal".equals(type)) {
                    throw new PurchaseUtilityExplanationException(
                            FailureKind.NON_RETRYABLE_PROVIDER,
                            "OpenAI가 요청 처리를 거부했습니다."
                    );
                }

                if ("output_text".equals(type)
                        && part.hasNonNull("text")) {
                    String text = part.get("text").asString().trim();

                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
        }

        throw new PurchaseUtilityExplanationException(
                FailureKind.INVALID_RESPONSE,
                "OpenAI 응답에 output_text가 없습니다."
        );
    }

    private void validateHttpStatus(
            int statusCode
    ) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }

        boolean retryable = statusCode == 408
                || statusCode == 409
                || statusCode == 429
                || statusCode >= 500;

        throw new PurchaseUtilityExplanationException(
                retryable
                        ? FailureKind.TRANSIENT_PROVIDER
                        : FailureKind.NON_RETRYABLE_PROVIDER,
                "OpenAI API 요청이 실패했습니다. status=" + statusCode
        );
    }

    private JsonNode readJson(
            String value,
            FailureKind failureKind,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new PurchaseUtilityExplanationException(
                    failureKind,
                    message
            );
        }

        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new PurchaseUtilityExplanationException(
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
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new PurchaseUtilityExplanationException(
                    FailureKind.CONFIGURATION,
                    "OpenAI 요청 JSON을 생성할 수 없습니다.",
                    exception
            );
        }
    }

    private Integer nullableInt(
            JsonNode node
    ) {
        if (node == null || !node.isIntegralNumber()) {
            return null;
        }

        return node.intValue();
    }

    private String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new PurchaseUtilityExplanationException(
                    FailureKind.CONFIGURATION,
                    field + "는 비어 있을 수 없습니다."
            );
        }

        return value.trim();
    }
}
