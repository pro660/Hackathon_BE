package org.likelionhsu.hackathon.useritem.repository;

import java.util.List;
import java.util.regex.Pattern;

import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Repository
public class UserItemAiJobValidator {

    private static final Pattern SHA_256_HEX =
            Pattern.compile("^[0-9a-f]{64}$");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public UserItemAiJobValidator(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public ItemAnalysisProvenance
    validateOwnedSucceededItemAnalysis(
            Long userId,
            Long aiJobId
    ) {
        List<ItemAnalysisProvenance> matches =
                jdbcTemplate.query(
                        """
                        SELECT input_hash, result_json
                        FROM ai_jobs
                        WHERE id = ?
                          AND user_id = ?
                          AND type = 'ITEM_ANALYSIS'
                          AND status = 'SUCCEEDED'
                        """,
                        (resultSet, rowNumber) ->
                                parseProvenance(
                                        resultSet.getString(
                                                "input_hash"
                                        ),
                                        resultSet.getString(
                                                "result_json"
                                        )
                                ),
                        aiJobId,
                        userId
                );

        if (matches.size() != 1) {
            throw invalidAiJob();
        }

        return matches.getFirst();
    }

    private ItemAnalysisProvenance parseProvenance(
            String inputHash,
            String resultJson
    ) {
        if (inputHash == null
                || !SHA_256_HEX.matcher(inputHash).matches()
                || resultJson == null
                || resultJson.isBlank()) {
            throw invalidAiJob();
        }

        try {
            JsonNode root = objectMapper.readTree(resultJson);

            if (root == null
                    || !root.isObject()
                    || !hasRequiredResultFields(root)) {
                throw invalidAiJob();
            }

            ItemAnalysisResult result =
                    objectMapper.treeToValue(
                            root,
                            ItemAnalysisResult.class
                    );

            return new ItemAnalysisProvenance(
                    inputHash,
                    result
            );
        } catch (JacksonException exception) {
            throw invalidAiJob();
        }
    }

    private boolean hasRequiredResultFields(JsonNode root) {
        return root.has("brandName")
                && root.has("name")
                && root.has("category")
                && root.has("primaryColor")
                && root.has("material");
    }

    private RequestValidationException invalidAiJob() {
        return new RequestValidationException(
                "aiJobId",
                "현재 사용자의 완료된 아이템 분석 작업이어야 합니다."
        );
    }

    public record ItemAnalysisProvenance(
            String inputHash,
            ItemAnalysisResult result
    ) {
    }
}
