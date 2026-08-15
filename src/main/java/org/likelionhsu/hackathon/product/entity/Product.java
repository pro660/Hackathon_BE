package org.likelionhsu.hackathon.product.entity;

import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "brand",
            nullable = false,
            length = 20
    )
    private ProductBrand brand;

    @Column(
            name = "sku",
            nullable = false,
            length = 100
    )
    private String sku;

    @Column(
            name = "name",
            nullable = false,
            length = 200
    )
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "category",
            nullable = false,
            length = 30
    )
    private ItemCategory category;

    @Column(
            name = "description",
            length = 2000
    )
    private String description;

    @Column(
            name = "price",
            nullable = false
    )
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "primary_color",
            length = 20
    )
    private ColorGroup primaryColor;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "material",
            length = 30
    )
    private MaterialGroup material;

    @Column(
            name = "product_url",
            length = 2048
    )
    private String productUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private ProductStatus status;

    protected Product() {
    }

    private Product(
            ProductBrand brand,
            String sku,
            String name,
            ItemCategory category,
            String description,
            Long price,
            ColorGroup primaryColor,
            MaterialGroup material,
            String productUrl,
            ProductStatus status
    ) {
        this.brand = brand;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.primaryColor = primaryColor;
        this.material = material;
        this.productUrl = productUrl;
        this.status = status;
    }

    public static Product create(
            ProductBrand brand,
            String sku,
            String name,
            ItemCategory category,
            String description,
            Long price,
            ColorGroup primaryColor,
            MaterialGroup material,
            String productUrl,
            ProductStatus status
    ) {
        return new Product(
                brand,
                sku,
                name,
                category,
                description,
                price,
                primaryColor,
                material,
                productUrl,
                status
        );
    }

    public void updateCatalogInfo(
            ProductBrand brand,
            String name,
            ItemCategory category,
            String description,
            Long price,
            ColorGroup primaryColor,
            MaterialGroup material,
            String productUrl,
            ProductStatus status
    ) {
        this.brand = brand;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.primaryColor = primaryColor;
        this.material = material;
        this.productUrl = productUrl;
        this.status = status;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public Long getId() {
        return id;
    }

    public ProductBrand getBrand() {
        return brand;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Long getPrice() {
        return price;
    }

    public ColorGroup getPrimaryColor() {
        return primaryColor;
    }

    public MaterialGroup getMaterial() {
        return material;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public ProductStatus getStatus() {
        return status;
    }
}