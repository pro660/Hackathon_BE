package org.likelionhsu.hackathon.useritem.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.useritem.repository.UserItemAiJobValidator.ItemAnalysisProvenance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import tools.jackson.databind.ObjectMapper;

class UserItemAiJobValidatorTest {

    private static final Long USER_ID = 1L;
    private static final Long AI_JOB_ID = 77L;
    private static final String VALID_INPUT_HASH =
            "a".repeat(64);

    private JdbcTemplate jdbcTemplate;
    private UserItemAiJobValidator validator;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        validator = new UserItemAiJobValidator(
                jdbcTemplate,
                new ObjectMapper()
        );
    }

    @Test
    void validProvenanceIsReturned() throws Exception {
        stubSingleRow(
                VALID_INPUT_HASH,
                """
                {
                  "brandName":"MCM",
                  "name":"백팩",
                  "category":"BAG",
                  "primaryColor":"BLACK",
                  "material":"LEATHER"
                }
                """
        );

        ItemAnalysisProvenance provenance =
                validator.validateOwnedSucceededItemAnalysis(
                        USER_ID,
                        AI_JOB_ID
                );

        assertThat(provenance.inputHash())
                .isEqualTo(VALID_INPUT_HASH);
        assertThat(provenance.result().material())
                .isEqualTo(MaterialGroup.LEATHER);
    }

    @Test
    void invalidInputHashIsRejected() throws Exception {
        stubSingleRow(
                "invalid-hash",
                validResultJson()
        );

        assertInvalidProvenance();
    }

    @Test
    void nullResultJsonIsRejected() throws Exception {
        stubSingleRow(
                VALID_INPUT_HASH,
                null
        );

        assertInvalidProvenance();
    }

    @Test
    void missingRequiredResultFieldIsRejected()
            throws Exception {
        stubSingleRow(
                VALID_INPUT_HASH,
                """
                {
                  "brandName":"MCM",
                  "name":"백팩",
                  "category":"BAG",
                  "primaryColor":"BLACK"
                }
                """
        );

        assertInvalidProvenance();
    }

    @Test
    void invalidEnumValueIsRejected() throws Exception {
        stubSingleRow(
                VALID_INPUT_HASH,
                """
                {
                  "brandName":"MCM",
                  "name":"백팩",
                  "category":"NOT_A_CATEGORY",
                  "primaryColor":"BLACK",
                  "material":"LEATHER"
                }
                """
        );

        assertInvalidProvenance();
    }

    @Test
    void missingOwnedSucceededItemAnalysisIsRejected() {
        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                any(),
                any()
        )).thenReturn(List.of());

        assertInvalidProvenance();
    }

    private void stubSingleRow(
            String inputHash,
            String resultJson
    ) throws Exception {
        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                any(),
                any()
        )).thenAnswer(invocation -> {
            RowMapper<ItemAnalysisProvenance> rowMapper =
                    invocation.getArgument(1);

            ResultSet resultSet = mock(ResultSet.class);

            when(resultSet.getString("input_hash"))
                    .thenReturn(inputHash);
            when(resultSet.getString("result_json"))
                    .thenReturn(resultJson);

            return List.of(
                    rowMapper.mapRow(resultSet, 0)
            );
        });
    }

    private void assertInvalidProvenance() {
        assertThatThrownBy(
                () -> validator
                        .validateOwnedSucceededItemAnalysis(
                                USER_ID,
                                AI_JOB_ID
                        )
        )
                .isInstanceOf(RequestValidationException.class)
                .satisfies(exception -> {
                    RequestValidationException validation =
                            (RequestValidationException) exception;

                    assertThat(validation.getField())
                            .isEqualTo("aiJobId");
                    assertThat(validation.getReason())
                            .isEqualTo(
                                    "현재 사용자의 완료된 아이템 분석 작업이어야 합니다."
                            );
                });
    }

    private String validResultJson() {
        return """
                {
                  "brandName":"MCM",
                  "name":"백팩",
                  "category":"BAG",
                  "primaryColor":"BLACK",
                  "material":"LEATHER"
                }
                """;
    }
}
