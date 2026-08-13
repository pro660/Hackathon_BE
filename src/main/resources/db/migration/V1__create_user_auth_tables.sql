CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       email VARCHAR(320) NULL,
                       nickname VARCHAR(20) NOT NULL,
                       gender VARCHAR(20) NOT NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       status VARCHAR(20) NOT NULL,
                       notification_email VARCHAR(320) NULL,
                       notification_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       deleted_at TIMESTAMP NULL,
                       version BIGINT NOT NULL DEFAULT 0,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,

                       PRIMARY KEY (id),
                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT chk_users_deleted_at_status CHECK (
                           (status = 'DELETED' AND deleted_at IS NOT NULL)
                               OR
                           (status <> 'DELETED' AND deleted_at IS NULL)
                           )
);

CREATE INDEX idx_users_status_deleted_at
    ON users (status, deleted_at);

CREATE TABLE local_credentials (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   user_id BIGINT NOT NULL,
                                   login_id VARCHAR(20) NOT NULL,
                                   password_hash VARCHAR(255) NOT NULL,
                                   created_at TIMESTAMP NOT NULL,
                                   updated_at TIMESTAMP NOT NULL,

                                   PRIMARY KEY (id),
                                   CONSTRAINT uk_local_credentials_user_id UNIQUE (user_id),
                                   CONSTRAINT uk_local_credentials_login_id UNIQUE (login_id),
                                   CONSTRAINT fk_local_credentials_user
                                       FOREIGN KEY (user_id)
                                           REFERENCES users (id)
                                           ON DELETE CASCADE
);

CREATE TABLE social_accounts (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 user_id BIGINT NOT NULL,
                                 provider VARCHAR(20) NOT NULL,
                                 provider_user_id VARCHAR(255) NOT NULL,
                                 provider_email VARCHAR(320) NULL,
                                 created_at TIMESTAMP NOT NULL,
                                 updated_at TIMESTAMP NOT NULL,

                                 PRIMARY KEY (id),
                                 CONSTRAINT uk_social_accounts_provider_user
                                     UNIQUE (provider, provider_user_id),
                                 INDEX idx_social_accounts_user_id (user_id),
                                 CONSTRAINT fk_social_accounts_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users (id)
                                         ON DELETE CASCADE
);

CREATE TABLE pending_social_signups (
                                        id BIGINT NOT NULL AUTO_INCREMENT,
                                        provider VARCHAR(20) NOT NULL,
                                        provider_user_id VARCHAR(255) NOT NULL,
                                        provider_email VARCHAR(320) NULL,
                                        onboarding_token_hash VARCHAR(64) NOT NULL,
                                        onboarding_token_expires_at TIMESTAMP NOT NULL,
                                        onboarding_token_consumed_at TIMESTAMP NULL,
                                        created_at TIMESTAMP NOT NULL,
                                        updated_at TIMESTAMP NOT NULL,

                                        PRIMARY KEY (id),
                                        CONSTRAINT uk_pending_social_signups_provider_user
                                            UNIQUE (provider, provider_user_id),
                                        CONSTRAINT uk_pending_social_signups_token_hash
                                            UNIQUE (onboarding_token_hash),
                                                INDEX idx_pending_social_signups_expiry_consumed (
                                                onboarding_token_expires_at,
                                                onboarding_token_consumed_at
                                                )
);

CREATE TABLE terms_agreements (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  user_id BIGINT NOT NULL,
                                  terms_type VARCHAR(40) NOT NULL,
                                  terms_version VARCHAR(30) NOT NULL,
                                  agreed BOOLEAN NOT NULL,
                                  agreed_at TIMESTAMP NULL,
                                  withdrawn_at TIMESTAMP NULL,

                                  PRIMARY KEY (id),
                                  CONSTRAINT uk_terms_agreements_user_type_version
                                      UNIQUE (user_id, terms_type, terms_version),
                                  CONSTRAINT fk_terms_agreements_user
                                      FOREIGN KEY (user_id)
                                          REFERENCES users (id)
                                          ON DELETE CASCADE
);

CREATE TABLE email_verifications (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     user_id BIGINT NULL,
                                     email VARCHAR(320) NOT NULL,
                                     purpose VARCHAR(30) NOT NULL,
                                     code_hash VARCHAR(255) NOT NULL,
                                     attempt_count INT NOT NULL DEFAULT 0,
                                     code_expires_at TIMESTAMP NOT NULL,
                                     verified_at TIMESTAMP NULL,
                                     signup_token_hash VARCHAR(64) NULL,
                                     signup_token_expires_at TIMESTAMP NULL,
                                     signup_token_consumed_at TIMESTAMP NULL,
                                     created_at TIMESTAMP NOT NULL,
                                     updated_at TIMESTAMP NOT NULL,

                                     PRIMARY KEY (id),
                                     CONSTRAINT uk_email_verifications_signup_token_hash
                                         UNIQUE (signup_token_hash),
                                     INDEX idx_email_verifications_email_purpose_created_at (email, purpose, created_at),
                                     INDEX idx_email_verifications_user_purpose_created_at (user_id, purpose, created_at),
                                     CONSTRAINT fk_email_verifications_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES users (id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT chk_email_verifications_purpose_user
                                         CHECK (
                                             (purpose = 'SIGNUP' AND user_id IS NULL)
                                                 OR
                                             (purpose = 'NOTIFICATION_EMAIL' AND user_id IS NOT NULL)
                                             )
);

CREATE TABLE refresh_tokens (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                token_hash VARCHAR(64) NOT NULL,
                                jti VARCHAR(36) NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                revoked_at TIMESTAMP NULL,
                                created_at TIMESTAMP NOT NULL,

                                PRIMARY KEY (id),
                                CONSTRAINT uk_refresh_tokens_token_hash
                                    UNIQUE (token_hash),
                                CONSTRAINT uk_refresh_tokens_jti
                                    UNIQUE (jti),
                                INDEX idx_refresh_tokens_user_expires_at (user_id, expires_at),
                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users (id)
                                        ON DELETE CASCADE
);