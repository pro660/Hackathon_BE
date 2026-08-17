package org.likelionhsu.hackathon.place.client.kakao;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.likelionhsu.hackathon.place.client.ExternalPlace;
import org.likelionhsu.hackathon.place.client.PlaceSearchCommand;
import org.likelionhsu.hackathon.place.client.PlaceSearchException;
import org.likelionhsu.hackathon.place.client.PlaceSearchException.FailureKind;
import org.likelionhsu.hackathon.place.client.PlaceSearchPort;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.springframework.util.StringUtils;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class KakaoPlaceClient implements PlaceSearchPort {

    private static final String KEYWORD_SEARCH_URL =
            "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_SIZE = 15;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String restApiKey;

    public KakaoPlaceClient(
            ObjectMapper objectMapper,
            String restApiKey
    ) {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(REQUEST_TIMEOUT)
                        .build(),
                objectMapper,
                restApiKey
        );
    }

    KakaoPlaceClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            String restApiKey
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.restApiKey = restApiKey == null ? "" : restApiKey.trim();
    }

    @Override
    public List<ExternalPlace> search(PlaceSearchCommand command) {
        Objects.requireNonNull(command, "command");

        if (!StringUtils.hasText(restApiKey)) {
            throw new PlaceSearchException(
                    FailureKind.UNAVAILABLE,
                    "Kakao Local REST API Key가 설정되지 않았습니다."
            );
        }

        HttpRequest request = HttpRequest.newBuilder(buildUri(command))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "KakaoAK " + restApiKey)
                .GET()
                .build();

        HttpResponse<String> response;

        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        } catch (HttpTimeoutException exception) {
            throw new PlaceSearchException(
                    FailureKind.TIMEOUT,
                    "Kakao Local 응답 시간이 초과되었습니다.",
                    exception
            );
        } catch (IOException exception) {
            throw new PlaceSearchException(
                    FailureKind.UNAVAILABLE,
                    "Kakao Local 통신 중 오류가 발생했습니다.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PlaceSearchException(
                    FailureKind.UNAVAILABLE,
                    "Kakao Local 요청이 중단되었습니다.",
                    exception
            );
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new PlaceSearchException(
                    FailureKind.UNAVAILABLE,
                    "Kakao Local이 정상 응답을 반환하지 않았습니다."
            );
        }

        return parsePlaces(response.body(), command.category());
    }

    private URI buildUri(PlaceSearchCommand command) {
        String query = StringUtils.hasText(command.query())
                ? command.query().trim()
                : command.category().searchKeyword();

        List<String> parameters = new ArrayList<>();
        parameters.add("query=" + encode(query));
        parameters.add("size=" + MAX_SIZE);

        if (command.category() != null
                && command.category().kakaoCategoryGroupCode() != null) {
            parameters.add(
                    "category_group_code="
                            + encode(command.category().kakaoCategoryGroupCode())
            );
        }

        if (command.latitude() != null && command.longitude() != null) {
            parameters.add("x=" + encode(command.longitude().toPlainString()));
            parameters.add("y=" + encode(command.latitude().toPlainString()));
            if (command.radius() != null) {
                parameters.add("radius=" + command.radius());
            }
            parameters.add("sort=distance");
        }

        return URI.create(KEYWORD_SEARCH_URL + "?" + String.join("&", parameters));
    }

    private List<ExternalPlace> parsePlaces(
            String body,
            PlaceCategory requestedCategory
    ) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JacksonException exception) {
            throw new PlaceSearchException(
                    FailureKind.INVALID_RESPONSE,
                    "Kakao Local 응답 JSON을 해석할 수 없습니다.",
                    exception
            );
        }

        JsonNode documents = root.path("documents");
        if (!documents.isArray()) {
            throw new PlaceSearchException(
                    FailureKind.INVALID_RESPONSE,
                    "Kakao Local 응답에 documents 배열이 없습니다."
            );
        }

        List<ExternalPlace> places = new ArrayList<>();
        for (JsonNode document : documents) {
            ExternalPlace place = mapPlace(document);
            if (requestedCategory == null
                    || requestedCategory == place.category()) {
                places.add(place);
            }
        }
        return places;
    }

    private ExternalPlace mapPlace(JsonNode document) {
        String categoryName = document.path("category_name").asString();
        PlaceCategory category = PlaceCategory.fromKakao(
                document.path("category_group_code").asString(),
                categoryName
        );

        try {
            return new ExternalPlace(
                    required(document, "id"),
                    required(document, "place_name"),
                    category,
                    categoryName,
                    emptyToNull(document.path("address_name").asString()),
                    emptyToNull(document.path("road_address_name").asString()),
                    new BigDecimal(required(document, "y")),
                    new BigDecimal(required(document, "x")),
                    emptyToNull(document.path("place_url").asString())
            );
        } catch (NumberFormatException exception) {
            throw new PlaceSearchException(
                    FailureKind.INVALID_RESPONSE,
                    "Kakao Local 좌표를 해석할 수 없습니다.",
                    exception
            );
        }
    }

    private String required(JsonNode node, String field) {
        String value = node.path(field).asString();
        if (!StringUtils.hasText(value)) {
            throw new PlaceSearchException(
                    FailureKind.INVALID_RESPONSE,
                    "Kakao Local 응답의 필수 필드가 비어 있습니다: " + field
            );
        }
        return value;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
