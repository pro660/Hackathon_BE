CREATE TABLE user_items (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            user_id BIGINT NOT NULL,
                            product_id BIGINT NULL,
                            brand_name VARCHAR(100) NOT NULL DEFAULT 'MCM',
                            name VARCHAR(200) NOT NULL,
                            category VARCHAR(30) NOT NULL,
                            primary_color VARCHAR(20) NULL,
                            material VARCHAR(30) NULL,
                            material_source VARCHAR(30) NULL,
                            purchase_date DATE NULL,
                            purchase_price BIGINT NULL,
                            memo VARCHAR(1000) NULL,
                            status VARCHAR(20) NOT NULL,
                            ai_job_id BIGINT NULL,
                            deleted_at TIMESTAMP(6) NULL,
                            version BIGINT NOT NULL DEFAULT 0,
                            created_at TIMESTAMP(6) NOT NULL,
                            updated_at TIMESTAMP(6) NOT NULL,

                            PRIMARY KEY (id),

                            INDEX idx_user_items_user_deleted_at
                                (user_id, deleted_at),

                            INDEX idx_user_items_user_status
                                (user_id, status),

                            INDEX idx_user_items_user_category
                                (user_id, category),

                            CONSTRAINT fk_user_items_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users (id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_items_product
                                FOREIGN KEY (product_id)
                                    REFERENCES products (id),

                            CONSTRAINT fk_user_items_ai_job
                                FOREIGN KEY (ai_job_id)
                                    REFERENCES ai_jobs (id)
                                    ON DELETE SET NULL,

                            CONSTRAINT chk_user_items_purchase_price
                                CHECK (
                                    purchase_price IS NULL
                                        OR purchase_price >= 0
                                    )
);

CREATE TABLE image_assets (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              owner_user_id BIGINT NOT NULL,
                              purpose VARCHAR(20) NOT NULL,
                              user_item_id BIGINT NULL,
                              ai_job_id BIGINT NULL,
                              public_id VARCHAR(255) NOT NULL,
                              secure_url VARCHAR(2048) NOT NULL,
                              format VARCHAR(20) NOT NULL,
                              bytes BIGINT NOT NULL,
                              width INT NOT NULL,
                              height INT NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              sort_order INT NOT NULL DEFAULT 0,
                              created_at TIMESTAMP(6) NOT NULL,
                              activated_at TIMESTAMP(6) NULL,
                              deleted_at TIMESTAMP(6) NULL,

                              PRIMARY KEY (id),

                              CONSTRAINT uk_image_assets_public_id
                                  UNIQUE (public_id),

                              INDEX idx_image_assets_owner_status
                                  (owner_user_id, status),

                              INDEX idx_image_assets_status_created_at
                                  (status, created_at),

                              CONSTRAINT fk_image_assets_owner_user
                                  FOREIGN KEY (owner_user_id)
                                      REFERENCES users (id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_image_assets_user_item
                                  FOREIGN KEY (user_item_id)
                                      REFERENCES user_items (id),

                              CONSTRAINT fk_image_assets_ai_job
                                  FOREIGN KEY (ai_job_id)
                                      REFERENCES ai_jobs (id)
                                      ON DELETE SET NULL,

                              CONSTRAINT chk_image_assets_sort_order
                                  CHECK (sort_order >= 0),

                              CONSTRAINT chk_image_assets_bytes
                                  CHECK (bytes > 0),

                              CONSTRAINT chk_image_assets_width
                                  CHECK (width > 0),

                              CONSTRAINT chk_image_assets_height
                                  CHECK (height > 0)
);