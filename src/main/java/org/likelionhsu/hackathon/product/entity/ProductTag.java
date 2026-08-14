package org.likelionhsu.hackathon.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_tags")
public class ProductTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            nullable = false,
            length = 20
    )
    private ProductTagType type;

    @Column(
            name = "code",
            nullable = false,
            length = 100
    )
    private String code;

    protected ProductTag() {
    }

    public Long getId() {
        return id;
    }

    public ProductTagType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }
}