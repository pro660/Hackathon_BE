ALTER TABLE ai_jobs
    ADD COLUMN request_hash VARCHAR(64) NULL
        AFTER idempotency_key;

ALTER TABLE ai_jobs
    MODIFY COLUMN input_hash VARCHAR(64) NULL;
