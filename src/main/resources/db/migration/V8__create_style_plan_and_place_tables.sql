CREATE TABLE places (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        provider VARCHAR(20) NOT NULL,
                        provider_place_id VARCHAR(100) NOT NULL,
                        name VARCHAR(200) NOT NULL,
                        category_name VARCHAR(200) NULL,
                        address VARCHAR(500) NULL,
                        road_address VARCHAR(500) NULL,
                        latitude DECIMAL(10,7) NOT NULL,
                        longitude DECIMAL(10,7) NOT NULL,
                        place_url VARCHAR(2048) NULL,
                        created_at TIMESTAMP(6) NOT NULL,
                        updated_at TIMESTAMP(6) NOT NULL,

                        PRIMARY KEY (id),

                        CONSTRAINT uk_places_provider_place
                            UNIQUE (provider, provider_place_id)
);

CREATE TABLE saved_places (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              user_id BIGINT NOT NULL,
                              place_id BIGINT NOT NULL,
                              created_at TIMESTAMP(6) NOT NULL,

                              PRIMARY KEY (id),

                              CONSTRAINT uk_saved_places_user_place
                                  UNIQUE (user_id, place_id),

                              INDEX idx_saved_places_user_created_at
                                  (user_id, created_at DESC),

                              CONSTRAINT fk_saved_places_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users (id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_saved_places_place
                                  FOREIGN KEY (place_id)
                                      REFERENCES places (id)
                                      ON DELETE RESTRICT
);

CREATE TABLE style_plans (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             user_id BIGINT NOT NULL,
                             title VARCHAR(200) NOT NULL,
                             occasion VARCHAR(20) NOT NULL,
                             planned_at TIMESTAMP(6) NULL,
                             weather_summary VARCHAR(300) NULL,
                             weather_condition VARCHAR(20) NULL,
                             description VARCHAR(1500) NULL,
                             generation_type VARCHAR(20) NOT NULL,
                             status VARCHAR(20) NOT NULL,
                             ai_job_id BIGINT NULL,
                             version BIGINT NOT NULL DEFAULT 0,
                             created_at TIMESTAMP(6) NOT NULL,
                             updated_at TIMESTAMP(6) NOT NULL,

                             PRIMARY KEY (id),

                             UNIQUE KEY uk_style_plans_ai_job
                                 (ai_job_id),

                             INDEX idx_style_plans_user_created_at
                                 (user_id, created_at DESC),

                             CONSTRAINT fk_style_plans_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES users (id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_style_plans_ai_job
                                 FOREIGN KEY (ai_job_id)
                                     REFERENCES ai_jobs (id)
                                     ON DELETE SET NULL
);

CREATE TABLE style_plan_items (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  style_plan_id BIGINT NOT NULL,
                                  user_item_id BIGINT NOT NULL,
                                  role VARCHAR(20) NOT NULL,
                                  sort_order INT NOT NULL DEFAULT 0,

                                  PRIMARY KEY (id),

                                  CONSTRAINT uk_style_plan_items_plan_item
                                      UNIQUE (style_plan_id, user_item_id),

                                  CONSTRAINT fk_style_plan_items_style_plan
                                      FOREIGN KEY (style_plan_id)
                                          REFERENCES style_plans (id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_style_plan_items_user_item
                                      FOREIGN KEY (user_item_id)
                                          REFERENCES user_items (id)
                                          ON DELETE RESTRICT,

                                  CONSTRAINT chk_style_plan_items_sort_order
                                      CHECK (sort_order >= 0)
);

CREATE TABLE style_plan_products (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     style_plan_id BIGINT NOT NULL,
                                     product_id BIGINT NOT NULL,
                                     rank_order INT NOT NULL,
                                     reason VARCHAR(1000) NULL,

                                     PRIMARY KEY (id),

                                     CONSTRAINT uk_style_plan_products_plan_product
                                         UNIQUE (style_plan_id, product_id),

                                     CONSTRAINT uk_style_plan_products_plan_rank
                                         UNIQUE (style_plan_id, rank_order),

                                     CONSTRAINT fk_style_plan_products_style_plan
                                         FOREIGN KEY (style_plan_id)
                                             REFERENCES style_plans (id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT fk_style_plan_products_product
                                         FOREIGN KEY (product_id)
                                             REFERENCES products (id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT chk_style_plan_products_rank
                                         CHECK (rank_order >= 1)
);

CREATE TABLE style_plan_places (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   style_plan_id BIGINT NOT NULL,
                                   place_id BIGINT NOT NULL,
                                   rank_order INT NOT NULL,
                                   reason VARCHAR(1000) NULL,

                                   PRIMARY KEY (id),

                                   CONSTRAINT uk_style_plan_places_plan_place
                                       UNIQUE (style_plan_id, place_id),

                                   CONSTRAINT uk_style_plan_places_plan_rank
                                       UNIQUE (style_plan_id, rank_order),

                                   CONSTRAINT fk_style_plan_places_style_plan
                                       FOREIGN KEY (style_plan_id)
                                           REFERENCES style_plans (id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_style_plan_places_place
                                       FOREIGN KEY (place_id)
                                           REFERENCES places (id)
                                           ON DELETE RESTRICT,

                                   CONSTRAINT chk_style_plan_places_rank
                                       CHECK (rank_order >= 1)
);
