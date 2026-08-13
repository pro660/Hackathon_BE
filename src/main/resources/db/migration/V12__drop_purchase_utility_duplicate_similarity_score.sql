ALTER TABLE purchase_utility_analyses
    DROP CHECK chk_purchase_utility_analyses_duplicate_similarity_score,
    DROP COLUMN duplicate_similarity_score;
