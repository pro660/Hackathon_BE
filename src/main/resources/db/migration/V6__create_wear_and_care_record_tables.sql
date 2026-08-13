CREATE TABLE wear_records (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              user_id BIGINT NOT NULL,
                              worn_at TIMESTAMP(6) NOT NULL,
                              occasion VARCHAR(20) NOT NULL,
                              place_name VARCHAR(200) NULL,
                              weather_summary VARCHAR(300) NULL,
                              memo VARCHAR(1000) NULL,
                              version BIGINT NOT NULL DEFAULT 0,
                              created_at TIMESTAMP(6) NOT NULL,
                              updated_at TIMESTAMP(6) NOT NULL,

                              PRIMARY KEY (id),

                              INDEX idx_wear_records_user_worn_at
                                  (user_id, worn_at DESC),

                              CONSTRAINT fk_wear_records_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users (id)
                                      ON DELETE CASCADE
);

CREATE TABLE wear_record_items (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   wear_record_id BIGINT NOT NULL,
                                   user_item_id BIGINT NOT NULL,
                                   sort_order INT NOT NULL DEFAULT 0,

                                   PRIMARY KEY (id),

                                   CONSTRAINT uk_wear_record_items_record_item
                                       UNIQUE (wear_record_id, user_item_id),

                                   CONSTRAINT fk_wear_record_items_wear_record
                                       FOREIGN KEY (wear_record_id)
                                           REFERENCES wear_records (id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_wear_record_items_user_item
                                       FOREIGN KEY (user_item_id)
                                           REFERENCES user_items (id),

                                   CONSTRAINT chk_wear_record_items_sort_order
                                       CHECK (sort_order >= 0)
);

CREATE TABLE care_records (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              user_item_id BIGINT NOT NULL,
                              care_type VARCHAR(20) NOT NULL,
                              cared_at TIMESTAMP(6) NOT NULL,
                              provider_name VARCHAR(200) NULL,
                              cost BIGINT NULL,
                              memo VARCHAR(1000) NULL,
                              next_care_at TIMESTAMP(6) NULL,
                              created_at TIMESTAMP(6) NOT NULL,
                              updated_at TIMESTAMP(6) NOT NULL,

                              PRIMARY KEY (id),

                              INDEX idx_care_records_user_item_cared_at
                                  (user_item_id, cared_at DESC),

                              CONSTRAINT fk_care_records_user_item
                                  FOREIGN KEY (user_item_id)
                                      REFERENCES user_items (id)
                                      ON DELETE RESTRICT,

                              CONSTRAINT chk_care_records_cost
                                  CHECK (
                                      cost IS NULL
                                          OR cost >= 0
                                      )
);