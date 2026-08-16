package org.likelionhsu.hackathon.purchaseutility.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationException;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationRequest;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationResult;
import org.likelionhsu.hackathon.purchaseutility.domain.CareDifficulty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class OpenAiPurchaseUtilityExplanationAdapterTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private OpenAiPurchaseUtilityExplanationAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
        ObjectMapper objectMapper = JsonMapper.builder().build();

        adapter = new OpenAiPurchaseUtilityExplanationAdapter(
                httpClient,
                objectMapper,
                "test-api-key",
                "gpt-5.6-luna"
        );
    }

    @Test
    void sendsResponsesApiStructuredOutputRequestAndParsesSummary()
            throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
                {
                  "status":"completed",
                  "output":[
                    {
                      "type":"message",
                      "content":[
                        {
                          "type":"output_text",
                          "text":"{\\"summary\\":\\"기존 아이템과 조합하기 쉬워 활용도가 높아요. 계절 활용성과 관리 난이도도 서버 분석 결과 안에서 균형 있게 확인할 수 있어요.\\"}"
                        }
                      ]
                    }
                  ],
                  "usage":{
                    "input_tokens":120,
                    "output_tokens":36
                  }
                }
                """);
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        PurchaseUtilityExplanationResult result =
                adapter.generate(request());

        assertThat(result.summary()).contains("기존 아이템");
        assertThat(result.inputTokens()).isEqualTo(120);
        assertThat(result.outputTokens()).isEqualTo(36);
        assertThat(result.latencyMs()).isNotNull();

        ArgumentCaptor<HttpRequest> captor =
                ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any());

        HttpRequest sent = captor.getValue();

        assertThat(sent.uri().toString())
                .isEqualTo("https://api.openai.com/v1/responses");
        assertThat(
                sent.headers().firstValue("Authorization")
        ).contains("Bearer test-api-key");
        assertThat(sent.timeout()).contains(java.time.Duration.ofSeconds(15));
    }

    @Test
    void rateLimitFailureIsRetryable()
            throws Exception {
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpResponse.body()).thenReturn("{}");
        when(httpClient.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse);

        assertThatThrownBy(() -> adapter.generate(request()))
                .isInstanceOf(PurchaseUtilityExplanationException.class)
                .satisfies(exception ->
                        assertThat(
                                ((PurchaseUtilityExplanationException)
                                        exception).isRetryable()
                        ).isTrue()
                );
    }

    @Test
    void clientFailureIsNotRetryable()
            throws Exception {
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{}");
        when(httpClient.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse);

        assertThatThrownBy(() -> adapter.generate(request()))
                .isInstanceOf(PurchaseUtilityExplanationException.class)
                .satisfies(exception ->
                        assertThat(
                                ((PurchaseUtilityExplanationException)
                                        exception).isRetryable()
                        ).isFalse()
                );
    }

    @Test
    void malformedStructuredOutputIsRetryable()
            throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
                {
                  "status":"completed",
                  "output":[
                    {
                      "type":"message",
                      "content":[
                        {
                          "type":"output_text",
                          "text":"not-json"
                        }
                      ]
                    }
                  ]
                }
                """);
        when(httpClient.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse);

        assertThatThrownBy(() -> adapter.generate(request()))
                .isInstanceOf(PurchaseUtilityExplanationException.class)
                .satisfies(exception ->
                        assertThat(
                                ((PurchaseUtilityExplanationException)
                                        exception).isRetryable()
                        ).isTrue()
                );
    }

    @Test
    void refusalIsNotRetryable()
            throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
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
        when(httpClient.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse);

        assertThatThrownBy(() -> adapter.generate(request()))
                .isInstanceOf(PurchaseUtilityExplanationException.class)
                .satisfies(exception ->
                        assertThat(
                                ((PurchaseUtilityExplanationException)
                                        exception).isRetryable()
                        ).isFalse()
                );
    }

    private PurchaseUtilityExplanationRequest request() {
        return new PurchaseUtilityExplanationRequest(
                "801",
                "purchase-utility-score-v1",
                new PurchaseUtilityExplanationRequest.ProductContext(
                        "123",
                        "MCM Sample Bag",
                        ItemCategory.BAG,
                        ColorGroup.BLACK
                ),
                new BigDecimal("77.00"),
                new PurchaseUtilityExplanationRequest.FactorScores(
                        new BigDecimal("20.00"),
                        new BigDecimal("30.00"),
                        new BigDecimal("15.00"),
                        new BigDecimal("12.00")
                ),
                1,
                List.of(
                        new PurchaseUtilityExplanationRequest
                                .CompatibleItemContext(
                                "41",
                                "Black Jacket",
                                ItemCategory.CLOTHING,
                                ColorGroup.BLACK,
                                "색상 조합"
                        )
                ),
                CareDifficulty.MODERATE,
                "ko"
        );
    }
}
