package org.likelionhsu.hackathon.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@SpringBootTest
class DateTimePolicyTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private Clock clock;

    @Test
    void 공통_Clock은_UTC를_사용한다() {
        assertThat(clock.getZone())
                .isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void 날짜와_시간은_ISO_형식으로_직렬화된다()
            throws Exception {

        DateTimeSample sample =
                new DateTimeSample(
                        LocalDate.of(
                                2026,
                                8,
                                8
                        ),
                        LocalTime.of(
                                18,
                                30,
                                15
                        ),
                        Instant.parse(
                                "2026-08-08T09:30:00Z"
                        )
                );

        String json =
                jsonMapper.writeValueAsString(sample);

        assertThat(json)
                .contains(
                        "\"planDate\":\"2026-08-08\""
                )
                .contains(
                        "\"planTime\":\"18:30:15\""
                )
                .contains(
                        "\"createdAt\":\"2026-08-08T09:30:00Z\""
                );
    }

    @Test
    void ISO_날짜와_시간을_Java_타입으로_역직렬화한다()
            throws Exception {

        String json =
                """
                {
                  "planDate": "2026-08-08",
                  "planTime": "18:30:15",
                  "createdAt": "2026-08-08T09:30:00Z"
                }
                """;

        DateTimeSample result =
                jsonMapper.readValue(
                        json,
                        DateTimeSample.class
                );

        assertThat(result.planDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                8,
                                8
                        )
                );

        assertThat(result.planTime())
                .isEqualTo(
                        LocalTime.of(
                                18,
                                30,
                                15
                        )
                );

        assertThat(result.createdAt())
                .isEqualTo(
                        Instant.parse(
                                "2026-08-08T09:30:00Z"
                        )
                );
    }

    record DateTimeSample(
            LocalDate planDate,
            LocalTime planTime,
            Instant createdAt
    ) {
    }
}
