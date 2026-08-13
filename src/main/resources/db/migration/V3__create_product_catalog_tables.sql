CREATE TABLE products (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          brand VARCHAR(20) NOT NULL,
                          sku VARCHAR(100) NOT NULL,
                          name VARCHAR(200) NOT NULL,
                          category VARCHAR(30) NOT NULL,
                          description VARCHAR(2000) NULL,
                          price BIGINT NOT NULL,
                          primary_color VARCHAR(20) NULL,
                          material VARCHAR(30) NULL,
                          product_url VARCHAR(2048) NULL,
                          status VARCHAR(20) NOT NULL,
                          created_at TIMESTAMP(6) NOT NULL,
                          updated_at TIMESTAMP(6) NOT NULL,

                          PRIMARY KEY (id),

                          CONSTRAINT uk_products_sku
                              UNIQUE (sku),

                          INDEX idx_products_brand_status (brand, status),
                          INDEX idx_products_category_status (category, status),
                          INDEX idx_products_primary_color_status (primary_color, status),
                          INDEX idx_products_price (price),

                          CONSTRAINT chk_products_price
                              CHECK (price >= 0)
);

CREATE TABLE product_images (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                product_id BIGINT NOT NULL,
                                url VARCHAR(2048) NOT NULL,
                                public_id VARCHAR(255) NULL,
                                alt_text VARCHAR(300) NULL,
                                sort_order INT NOT NULL,
                                is_primary BOOLEAN NOT NULL,
                                created_at TIMESTAMP(6) NOT NULL,
                                updated_at TIMESTAMP(6) NOT NULL,

                                PRIMARY KEY (id),

                                CONSTRAINT uk_product_images_product_sort_order
                                    UNIQUE (product_id, sort_order),

                                CONSTRAINT fk_product_images_product
                                    FOREIGN KEY (product_id)
                                        REFERENCES products (id)
                                        ON DELETE CASCADE,

                                CONSTRAINT chk_product_images_sort_order
                                    CHECK (sort_order >= 0)
);

CREATE TABLE product_tags (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              type VARCHAR(20) NOT NULL,
                              code VARCHAR(100) NOT NULL,
                              display_name VARCHAR(100) NOT NULL,

                              PRIMARY KEY (id),

                              CONSTRAINT uk_product_tags_type_code
                                  UNIQUE (type, code)
);

CREATE TABLE product_tag_mappings (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      product_id BIGINT NOT NULL,
                                      product_tag_id BIGINT NOT NULL,

                                      PRIMARY KEY (id),

                                      CONSTRAINT uk_product_tag_mappings_product_tag
                                          UNIQUE (product_id, product_tag_id),

                                      INDEX idx_product_tag_mappings_tag_product
                                          (product_tag_id, product_id),

                                      CONSTRAINT fk_product_tag_mappings_product
                                          FOREIGN KEY (product_id)
                                              REFERENCES products (id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_product_tag_mappings_product_tag
                                          FOREIGN KEY (product_tag_id)
                                              REFERENCES product_tags (id)
);