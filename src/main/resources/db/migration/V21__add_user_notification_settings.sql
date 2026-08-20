ALTER TABLE users
    ADD COLUMN care_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN recommendation_update_enabled BOOLEAN NOT NULL DEFAULT TRUE;
