ALTER TABLE purchase_utility_analyses
    ADD COLUMN care_difficulty VARCHAR(20) NULL AFTER factor_json;

UPDATE purchase_utility_analyses pua
JOIN products p
    ON p.id = pua.product_id
SET pua.care_difficulty = CASE
    WHEN p.material = 'NYLON' THEN 'EASY'
    WHEN p.material IN (
        'SYNTHETIC_LEATHER',
        'CANVAS',
        'FABRIC',
        'METAL'
    ) THEN 'MODERATE'
    WHEN p.material = 'LEATHER' THEN 'HARD'
    ELSE 'UNKNOWN'
END;

UPDATE purchase_utility_analyses
SET care_difficulty = 'UNKNOWN'
WHERE care_difficulty IS NULL;

ALTER TABLE purchase_utility_analyses
    MODIFY COLUMN care_difficulty VARCHAR(20) NOT NULL;

ALTER TABLE purchase_utility_analyses
    ADD CONSTRAINT chk_purchase_utility_analyses_care_difficulty
        CHECK (
            care_difficulty IN ('EASY', 'MODERATE', 'HARD', 'UNKNOWN')
        );
