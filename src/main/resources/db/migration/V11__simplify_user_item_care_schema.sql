DROP TABLE care_records;

ALTER TABLE user_items
    DROP INDEX idx_user_items_user_status,
    DROP COLUMN status,
    ADD COLUMN next_care_date DATE NULL;
