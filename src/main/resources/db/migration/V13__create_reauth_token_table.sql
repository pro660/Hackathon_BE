CREATE TABLE reauth_tokens (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               user_id BIGINT NOT NULL,
                               purpose VARCHAR(40) NOT NULL,
                               token_hash VARCHAR(64) NOT NULL,
                               expires_at TIMESTAMP(6) NOT NULL,
                               consumed_at TIMESTAMP(6) NULL,
                               created_at TIMESTAMP(6) NOT NULL,
                               updated_at TIMESTAMP(6) NOT NULL,

                               PRIMARY KEY (id),

                               CONSTRAINT uk_reauth_tokens_token_hash
                                   UNIQUE (token_hash),

                               INDEX idx_reauth_tokens_user_purpose_expires_consumed
                                   (user_id, purpose, expires_at, consumed_at),

                               CONSTRAINT fk_reauth_tokens_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users (id)
                                       ON DELETE CASCADE
);
