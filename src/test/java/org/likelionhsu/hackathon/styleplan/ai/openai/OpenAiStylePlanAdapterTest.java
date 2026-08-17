package org.likelionhsu.hackathon.styleplan.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationException;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationResult;
import org.likelionhsu.hackathon.styleplan.service.StylePlanJobRequest;
import org.likelionhsu.hackathon.styleplan.service.StylePlanRecommendationContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class OpenAiStylePlanAdapterTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private ObjectMapper objectMapper;
    private OpenAiStylePlanAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
        objectMapper = JsonMapper
                .builder()
                .build();

        adapter = new OpenAiStylePlanAdapter(
                httpClient,
                objectMapper,
                "test-api-key",
                "gpt-5.6-luna"
        );
    }

    @Test
    void requestUsesStrictSchemaAndStoreFalse()
            throws Exception {
        JsonNode body = objectMapper.readTree(
                adapter.createRequestBody(
                        context()
                )
        );

        assertThat(
                body.path("model").asString()
        ).isEqualTo("gpt-5.6-luna");

        JsonNode format = body
                .path("text")
                .path("format");

        assertThat(
                format.path("type").asString()
        ).isEqualTo("json_schema");
        assertThat(
                format.path("strict").booleanValue()
        ).isTrue();
        assertThat(
                format.path("schema")
                        .path("additionalProperties")
                        .booleanValue()
        ).isFalse();
        assertThat(
                body.path("store").booleanValue()
        ).isFalse();
        assertThat(
                body.path("max_output_tokens")
                        .intValue()
        ).isEqualTo(1500);
    }

    @Test
    void parsesStructuredSelection()
            throws Exception {
        when(httpResponse.statusCode())
                .thenReturn(200);
        when(httpResponse.body())
                .thenReturn("""
                        {
                          "status":"completed",
                          "output":[
                            {
                              "type":"message",
                              "content":[
                                {
                                  "type":"output_text",
                                  "text":"{\\"title\\":\\"데이트 룩\\",\\"description\\":\\"깔끔하게 구성했어요.\\",\\"ownedItems\\":[{\\"myItemId\\":\\"501\\",\\"role\\":\\"BAG\\"}],\\"recommendedProducts\\":[{\\"productId\\":\\"101\\",\\"reason\\":\\"잘 어울려요.\\"}]}"
                                }
                              ]
                            }
                          ],
                          "usage":{
                            "input_tokens":230,
                            "output_tokens":70
                          }
                        }
                        """);

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        StylePlanGenerationResult result =
                adapter.generate(context());

        assertThat(result.selection().title())
                .isEqualTo("데이트 룩");
        assertThat(
                result.selection()
                        .ownedItems()
                        .getFirst()
                        .myItemId()
        ).isEqualTo("501");
        assertThat(
                result.selection()
                        .recommendedProducts()
                        .getFirst()
                        .productId()
        ).isEqualTo("101");
        assertThat(result.inputTokens())
                .isEqualTo(230);
        assertThat(result.outputTokens())
                .isEqualTo(70);
        assertThat(result.latencyMs())
                .isNotNull();
    }

    @Test
    void rateLimitIsRetryable()
            throws Exception {
        when(httpResponse.statusCode())
                .thenReturn(429);
        when(httpResponse.body())
                .thenReturn("{}");

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        assertThatThrownBy(() ->
                adapter.generate(context())
        )
                .isInstanceOf(
                        StylePlanGenerationException.class
                )
                .satisfies(exception ->
                        assertThat(
                                ((StylePlanGenerationException)
                                        exception)
                                        .isRetryable()
                        ).isTrue()
                );
    }

    @Test
    void refusalIsNotRetryable()
            throws Exception {
        when(httpResponse.statusCode())
                .thenReturn(200);
        when(httpResponse.body())
                .thenReturn("""
                        {
                          "status":"completed",
                          "output":[
                            {
                              "content":[
                                {
                                  "type":"refusal",
                                  "refusal":"cannot comply"
                                }
                              ]
                            }
                          ]
                        }
                        """);

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        assertThatThrownBy(() ->
                adapter.generate(context())
        )
                .isInstanceOf(
                        StylePlanGenerationException.class
                )
                .satisfies(exception ->
                        assertThat(
                                ((StylePlanGenerationException)
                                        exception)
                                        .isRetryable()
                        ).isFalse()
                );
    }

    private StylePlanRecommendationContext context() {
        return new StylePlanRecommendationContext(
                new StylePlanJobRequest(
                        "DATE",
                        List.of("NEAT"),
                        null,
                        true,
                        "ko"
                ),
                List.of("NEAT"),
                List.of("BLACK"),
                List.of("BAG"),
                List.of(
                        new StylePlanRecommendationContext
                                .OwnedItemCandidate(
                                "501",
                                "보유 가방",
                                null,
                                "BAG",
                                "BLACK",
                                "LEATHER",
                                1L,
                                5
                        )
                ),
                List.of(
                        new StylePlanRecommendationContext
                                .ProductCandidate(
                                "101",
                                "MCM 상품",
                                null,
                                "BAG",
                                "BLACK",
                                "LEATHER",
                                List.of(
                                        "DATE",
                                        "NEAT"
                                ),
                                9
                        )
                )
        );
    }
}
