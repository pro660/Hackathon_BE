CREATE TABLE ai_jobs (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         type VARCHAR(40) NOT NULL,
                         status VARCHAR(20) NOT NULL,
                         idempotency_key VARCHAR(255) NOT NULL,
                         model VARCHAR(100) NOT NULL,
                         prompt_version VARCHAR(50) NOT NULL,
                         input_hash VARCHAR(64) NOT NULL,
                         result_json JSON NULL,
                         fallback_json JSON NULL,
                         input_tokens INT NULL,
                         output_tokens INT NULL,
                         latency_ms BIGINT NULL,
                         retry_count INT NOT NULL DEFAULT 0,
                         error_code VARCHAR(100) NULL,
                         started_at TIMESTAMP(6) NULL,
                         completed_at TIMESTAMP(6) NULL,
                         created_at TIMESTAMP(6) NOT NULL,
                         updated_at TIMESTAMP(6) NOT NULL,

                         PRIMARY KEY (id),

                         CONSTRAINT uk_ai_jobs_user_idempotency_key
                             UNIQUE (user_id, idempotency_key),

                         INDEX idx_ai_jobs_user_created_at (user_id, created_at DESC),
                         INDEX idx_ai_jobs_status_created_at (status, created_at),
                         INDEX idx_ai_jobs_cache_lookup
                             (user_id, type, input_hash, prompt_version, model, status, completed_at),

                         CONSTRAINT fk_ai_jobs_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users (id),

                         CONSTRAINT chk_ai_jobs_retry_count
                             CHECK (retry_count BETWEEN 0 AND 1),

                         CONSTRAINT chk_ai_jobs_input_tokens
                             CHECK (input_tokens IS NULL OR input_tokens >= 0),

                         CONSTRAINT chk_ai_jobs_output_tokens
                             CHECK (output_tokens IS NULL OR output_tokens >= 0),

                         CONSTRAINT chk_ai_jobs_latency_ms
                             CHECK (latency_ms IS NULL OR latency_ms >= 0)
);