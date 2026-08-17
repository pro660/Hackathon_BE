CREATE TABLE user_item_care_reminder_settings (
    user_item_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    enabled_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (user_item_id),

    INDEX idx_care_reminder_settings_enabled
        (enabled),

    CONSTRAINT fk_care_reminder_settings_user_item
        FOREIGN KEY (user_item_id)
            REFERENCES user_items (id)
            ON DELETE CASCADE
);

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(500) NULL,
    user_item_id BIGINT NULL,
    item_name VARCHAR(200) NULL,
    scheduled_date DATE NULL,
    routine_types VARCHAR(100) NULL,
    dedup_key VARCHAR(255) NOT NULL,
    read_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_notifications_dedup_key
        UNIQUE (dedup_key),

    INDEX idx_notifications_user_created_at
        (user_id, created_at),

    INDEX idx_notifications_user_read_at
        (user_id, read_at),

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_notifications_user_item
        FOREIGN KEY (user_item_id)
            REFERENCES user_items (id)
            ON DELETE SET NULL
);
