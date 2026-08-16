package org.likelionhsu.hackathon.useritem.repository;

import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserItemAiJobValidator {

    private final JdbcTemplate jdbcTemplate;

    public UserItemAiJobValidator(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void validateOwnedSucceededItemAnalysis(
            Long userId,
            Long aiJobId
    ) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + " FROM ai_jobs"
                        + " WHERE id = ?"
                        + " AND user_id = ?"
                        + " AND type = 'ITEM_ANALYSIS'"
                        + " AND status = 'SUCCEEDED'",
                Integer.class,
                aiJobId,
                userId
        );

        if (count == null || count == 0) {
            throw new RequestValidationException(
                    "aiJobId",
                    "현재 사용자의 완료된 아이템 분석 작업이어야 합니다."
            );
        }
    }
}
