package org.likelionhsu.hackathon.product.entity;

import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;

    @Column(
            name = "url",
            nullable = false,
            length = 2048
    )
    private String url;

    @Column(
            name = "public_id",
            length = 255
    )
    private String publicId;

    @Column(
            name = "alt_text",
            length = 300
    )
    private String altText;

    @Column(
            name = "sort_order",
            nullable = false
    )
    private int sortOrder;

    @Column(
            name = "is_primary",
            nullable = false
    )
    private boolean primary;

    protected ProductImage() {
    }

    private ProductImage(
            Product product,
            String url,
            String publicId,
            String altText,
            int sortOrder,
            boolean primary
    ) {
        this.product = product;
        this.url = url;
        this.publicId = publicId;
        this.altText = altText;
        this.sortOrder = sortOrder;
        this.primary = primary;
    }

    public static ProductImage create(
            Product product,
            String url,
            String publicId,
            String altText,
            int sortOrder,
            boolean primary
    ) {
        return new ProductImage(
                product,
                url,
                publicId,
                altText,
                sortOrder,
                primary
        );
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getUrl() {
        return url;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getAltText() {
        return altText;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isPrimary() {
        return primary;
    }
}