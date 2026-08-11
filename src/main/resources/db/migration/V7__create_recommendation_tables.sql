CREATE TABLE recommendations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    generation_type VARCHAR(20) NOT NULL,
    summary VARCHAR(1000) NULL,
    context_json JSON NOT NULL,
    ai_job_id BIGINT NULL,
    generated_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_recommendations_ai_job
        (ai_job_id),

    INDEX idx_recommendations_user_generated_at
        (user_id, generated_at DESC),

    CONSTRAINT fk_recommendations_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_recommendations_ai_job
        FOREIGN KEY (ai_job_id)
        REFERENCES ai_jobs (id)
        ON DELETE SET NULL
);

CREATE TABLE recommendation_products (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         recommendation_id BIGINT NOT NULL,
                                         product_id BIGINT NOT NULL,
                                         rank_order INT NOT NULL,
                                         score DECIMAL(5,2) NOT NULL,
                                         reason VARCHAR(1000) NULL,
                                         product_snapshot JSON NOT NULL,

                                         PRIMARY KEY (id),

                                         CONSTRAINT uk_recommendation_products_recommendation_product
                                             UNIQUE (recommendation_id, product_id),

                                         CONSTRAINT uk_recommendation_products_recommendation_rank
                                             UNIQUE (recommendation_id, rank_order),

                                         CONSTRAINT fk_recommendation_products_recommendation
                                             FOREIGN KEY (recommendation_id)
                                                 REFERENCES recommendations (id)
                                                 ON DELETE CASCADE,

                                         CONSTRAINT fk_recommendation_products_product
                                             FOREIGN KEY (product_id)
                                                 REFERENCES products (id)
                                                 ON DELETE RESTRICT,

                                         CONSTRAINT chk_recommendation_products_rank
                                             CHECK (rank_order >= 1),

                                         CONSTRAINT chk_recommendation_products_score
                                             CHECK (
                                                 score >= 0
                                                     AND score <= 100
                                                 )
);

CREATE TABLE purchase_utility_analyses (
                                           id BIGINT NOT NULL AUTO_INCREMENT,
                                           user_id BIGINT NOT NULL,
                                           product_id BIGINT NOT NULL,
                                           utility_score DECIMAL(5,2) NOT NULL,
                                           compatible_item_count INT NOT NULL,
                                           duplicate_similarity_score DECIMAL(5,2) NULL,
                                           factor_json JSON NOT NULL,
                                           summary VARCHAR(1500) NULL,
                                           ai_job_id BIGINT NULL,
                                           analyzed_at TIMESTAMP(6) NOT NULL,
                                           created_at TIMESTAMP(6) NOT NULL,
                                           updated_at TIMESTAMP(6) NOT NULL,

                                           PRIMARY KEY (id),

                                           UNIQUE KEY uk_purchase_utility_analyses_ai_job
                                               (ai_job_id),

                                           INDEX idx_purchase_utility_analyses_user_analyzed_at
                                               (user_id, analyzed_at DESC),

                                           CONSTRAINT fk_purchase_utility_analyses_user
                                               FOREIGN KEY (user_id)
                                                   REFERENCES users (id)
                                                   ON DELETE CASCADE,

                                           CONSTRAINT fk_purchase_utility_analyses_product
                                               FOREIGN KEY (product_id)
                                                   REFERENCES products (id)
                                                   ON DELETE RESTRICT,

                                           CONSTRAINT fk_purchase_utility_analyses_ai_job
                                               FOREIGN KEY (ai_job_id)
                                                   REFERENCES ai_jobs (id)
                                                   ON DELETE SET NULL,

                                           CONSTRAINT chk_purchase_utility_analyses_utility_score
                                               CHECK (
                                                   utility_score >= 0
                                                       AND utility_score <= 100
                                                   ),

                                           CONSTRAINT chk_purchase_utility_analyses_duplicate_similarity_score
                                               CHECK (
                                                   duplicate_similarity_score IS NULL
                                                       OR (
                                                       duplicate_similarity_score >= 0
                                                           AND duplicate_similarity_score <= 100
                                                       )
                                                   )
);