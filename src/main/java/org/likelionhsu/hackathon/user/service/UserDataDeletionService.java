package org.likelionhsu.hackathon.user.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserDataDeletionService {

    private final JdbcTemplate jdbcTemplate;

    public UserDataDeletionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteOwnedData(Long userId) {
        jdbcTemplate.update(
                "DELETE FROM style_plans WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM saved_places WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM purchase_utility_analyses WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM recommendations WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM wear_records WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM image_assets WHERE owner_user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM user_items WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM preference_profiles WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM wishlists WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM ai_jobs WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM email_verifications "
                        + "WHERE user_id = ? "
                        + "OR email = ("
                        + "SELECT email FROM users WHERE id = ?"
                        + ")",
                userId,
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM terms_agreements WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM refresh_tokens WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM local_credentials WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM pending_social_signups "
                        + "WHERE EXISTS ("
                        + "SELECT 1 FROM social_accounts "
                        + "WHERE social_accounts.user_id = ? "
                        + "AND social_accounts.provider = "
                        + "pending_social_signups.provider "
                        + "AND social_accounts.provider_user_id = "
                        + "pending_social_signups.provider_user_id"
                        + ")",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM social_accounts WHERE user_id = ?",
                userId
        );
        jdbcTemplate.update(
                "DELETE FROM reauth_tokens WHERE user_id = ?",
                userId
        );
    }
}
