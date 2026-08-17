package org.likelionhsu.hackathon.itemanalysis.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisException;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisGenerationResult;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisRequest;
import org.mockito.ArgumentCaptor;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class OpenAiItemAnalysisAdapterTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private ObjectMapper objectMapper;
    private OpenAiItemAnalysisAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
        objectMapper = JsonMapper
                .builder()
                .build();

        adapter = new OpenAiItemAnalysisAdapter(
                httpClient,
                objectMapper,
                "test-api-key",
                "gpt-5.6-luna"
        );
    }

    @Test
    void requestUsesResponsesApiImageInputAndStrictSchema()
            throws Exception {
        JsonNode body = objectMapper.readTree(
                adapter.createRequestBody(
                        request()
                )
        );

        assertThat(
                body.path("model").asString()
        ).isEqualTo("gpt-5.6-luna");

        JsonNode input = body.path("input");

        assertThat(input.isArray()).isTrue();
        assertThat(input.size()).isEqualTo(2);

        JsonNode userContent = input
                .get(1)
                .path("content");

        assertThat(
                userContent.get(0)
                        .path("type")
                        .asString()
        ).isEqualTo("input_text");

        assertThat(
                userContent.get(1)
                        .path("type")
                        .asString()
        ).isEqualTo("input_image");

        assertThat(
                userContent.get(1)
                        .path("image_url")
                        .asString()
        ).isEqualTo(
                "https://res.cloudinary.com/"
                        + "demo/image/upload/item.jpg"
        );

        JsonNode format = body
                .path("text")
                .path("format");

        assertThat(
                format.path("type").asString()
        ).isEqualTo("json_schema");
        assertThat(
                format.path("strict").booleanValue()
        ).isTrue();

        JsonNode schema =
                format.path("schema");

        assertThat(
                schema.path("additionalProperties")
                        .booleanValue()
        ).isFalse();

        assertThat(
                schema.path("required").size()
        ).isEqualTo(5);

        assertThat(
                body.path("store").booleanValue()
        ).isFalse();
    }

    @Test
    void parsesStructuredItemAnalysisResult()
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
                                  "text":"{\\"brandName\\":\\"MCM\\",\\"name\\":\\"백팩\\",\\"category\\":\\"BAG\\",\\"primaryColor\\":\\"BLACK\\",\\"material\\":\\"LEATHER\\"}"
                                }
                              ]
                            }
                          ],
                          "usage":{
                            "input_tokens":210,
                            "output_tokens":44
                          }
                        }
                        """);

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        ItemAnalysisGenerationResult result =
                adapter.analyze(request());

        assertThat(result.result().brandName())
                .isEqualTo("MCM");
        assertThat(result.result().name())
                .isEqualTo("백팩");
        assertThat(result.result().category())
                .isEqualTo(ItemCategory.BAG);
        assertThat(result.result().primaryColor())
                .isEqualTo(ColorGroup.BLACK);
        assertThat(result.result().material())
                .isEqualTo(MaterialGroup.LEATHER);
        assertThat(result.inputTokens())
                .isEqualTo(210);
        assertThat(result.outputTokens())
                .isEqualTo(44);
        assertThat(result.latencyMs())
                .isNotNull();

        ArgumentCaptor<HttpRequest> captor =
                ArgumentCaptor.forClass(
                        HttpRequest.class
                );

        org.mockito.Mockito.verify(
                httpClient
        ).send(
                captor.capture(),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()
        );

        HttpRequest sent = captor.getValue();

        assertThat(sent.uri().toString())
                .isEqualTo(
                        "https://api.openai.com/v1/responses"
                );
        assertThat(
                sent.headers()
                        .firstValue("Authorization")
        ).contains("Bearer test-api-key");
        assertThat(sent.timeout())
                .contains(
                        Duration.ofSeconds(15)
                );
    }

    @Test
    void nullFieldsAreAcceptedForUncertainVisualFacts()
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
                                  "text":"{\\"brandName\\":null,\\"name\\":\\"지갑\\",\\"category\\":\\"LEATHER_GOODS\\",\\"primaryColor\\":null,\\"material\\":\\"UNKNOWN\\"}"
                                }
                              ]
                            }
                          ],
                          "usage":{}
                        }
                        """);

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        ItemAnalysisGenerationResult result =
                adapter.analyze(request());

        assertThat(result.result().brandName())
                .isNull();
        assertThat(result.result().name())
                .isEqualTo("지갑");
        assertThat(result.result().category())
                .isEqualTo(
                        ItemCategory.LEATHER_GOODS
                );
        assertThat(result.result().primaryColor())
                .isNull();
        assertThat(result.result().material())
                .isEqualTo(MaterialGroup.UNKNOWN);
        assertThat(result.inputTokens()).isNull();
        assertThat(result.outputTokens()).isNull();
    }

    @Test
    void rateLimitFailureIsRetryable()
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
                adapter.analyze(request())
        )
                .isInstanceOf(
                        ItemAnalysisException.class
                )
                .satisfies(exception ->
                        assertThat(
                                ((ItemAnalysisException)
                                        exception)
                                        .isRetryable()
                        ).isTrue()
                );
    }

    @Test
    void invalidEnumValueIsRetryableInvalidResponse()
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
                                  "text":"{\\"brandName\\":null,\\"name\\":\\"가방\\",\\"category\\":\\"HAT\\",\\"primaryColor\\":\\"BLACK\\",\\"material\\":\\"LEATHER\\"}"
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
                adapter.analyze(request())
        )
                .isInstanceOf(
                        ItemAnalysisException.class
                )
                .satisfies(exception ->
                        assertThat(
                                ((ItemAnalysisException)
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
                              "type":"message",
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
                adapter.analyze(request())
        )
                .isInstanceOf(
                        ItemAnalysisException.class
                )
                .satisfies(exception ->
                        assertThat(
                                ((ItemAnalysisException)
                                        exception)
                                        .isRetryable()
                        ).isFalse()
                );
    }

    @Test
    void nonHttpsImageUrlIsRejectedBeforeHttpCall() {
        ItemAnalysisRequest request =
                new ItemAnalysisRequest(
                        "51",
                        "http://example.com/item.jpg",
                        "jpg",
                        1200,
                        900
                );

        assertThatThrownBy(() ->
                adapter.analyze(request)
        )
                .isInstanceOf(
                        ItemAnalysisException.class
                )
                .satisfies(exception ->
                        assertThat(
                                ((ItemAnalysisException)
                                        exception)
                                        .getFailureKind()
                        ).isEqualTo(
                                ItemAnalysisException
                                        .FailureKind
                                        .CONFIGURATION
                        )
                );
    }

    private ItemAnalysisRequest request() {
        return new ItemAnalysisRequest(
                "51",
                "https://res.cloudinary.com/"
                        + "demo/image/upload/item.jpg",
                "jpg",
                1200,
                900
        );
    }
}
