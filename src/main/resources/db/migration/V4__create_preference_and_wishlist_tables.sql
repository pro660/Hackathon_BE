CREATE TABLE preference_profiles (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     user_id BIGINT NOT NULL,
                                     preferred_colors JSON NOT NULL DEFAULT (JSON_ARRAY()),
                                     preferred_categories JSON NOT NULL DEFAULT (JSON_ARRAY()),
                                     preferred_style_tags JSON NOT NULL DEFAULT (JSON_ARRAY()),
                                     summary VARCHAR(500) NULL,
                                     confidence DECIMAL(5, 4) NULL,
                                     analysis_version VARCHAR(50) NOT NULL,
                                     ai_job_id BIGINT NULL,
                                     analyzed_at TIMESTAMP(6) NULL,
                                     version BIGINT NOT NULL DEFAULT 0,
                                     created_at TIMESTAMP(6) NOT NULL,
                                     updated_at TIMESTAMP(6) NOT NULL,

                                     PRIMARY KEY (id),

                                     CONSTRAINT uk_preference_profiles_user_id
                                         UNIQUE (user_id),

                                     CONSTRAINT fk_preference_profiles_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES users (id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT fk_preference_profiles_ai_job
                                         FOREIGN KEY (ai_job_id)
                                             REFERENCES ai_jobs (id)
                                             ON DELETE SET NULL,

                                     CONSTRAINT chk_preference_profiles_confidence
                                         CHECK (
                                             confidence IS NULL
                                                 OR confidence BETWEEN 0 AND 1
                                             )
);

CREATE TABLE wishlists (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           user_id BIGINT NOT NULL,
                           product_id BIGINT NOT NULL,
                           created_at TIMESTAMP(6) NOT NULL,
                           updated_at TIMESTAMP(6) NOT NULL,

                           PRIMARY KEY (id),

                           CONSTRAINT uk_wishlists_user_product
                               UNIQUE (user_id, product_id),

                           CONSTRAINT fk_wishlists_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users (id)
                                   ON DELETE CASCADE,

                           CONSTRAINT fk_wishlists_product
                               FOREIGN KEY (product_id)
                                   REFERENCES products (id)
);