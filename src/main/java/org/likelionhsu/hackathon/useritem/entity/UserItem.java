package org.likelionhsu.hackathon.useritem.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;
import org.likelionhsu.hackathon.product.entity.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "user_items")
public class UserItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "brand_name", length = 100)
    private String brandName;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_color", length = 20)
    private ColorGroup primaryColor;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MaterialGroup material;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_source", length = 30)
    private MaterialSource materialSource;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price")
    private Long purchasePrice;

    @Column(name = "purchase_order_number", length = 100)
    private String purchaseOrderNumber;

    @Column(name = "purchase_place", length = 200)
    private String purchasePlace;

    @Column(length = 1000)
    private String memo;

    @Column(name = "ai_job_id")
    private Long aiJobId;

    @Column(name = "next_care_date")
    private LocalDate nextCareDate;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected UserItem() {
    }

    private UserItem(
            User user,
            Product product,
            String brandName,
            String name,
            ItemCategory category,
            ColorGroup primaryColor,
            MaterialGroup material,
            MaterialSource materialSource,
            LocalDate purchaseDate,
            Long purchasePrice,
            String purchaseOrderNumber,
            String purchasePlace,
            String memo,
            Long aiJobId,
            LocalDate nextCareDate
    ) {
        this.user = user;
        this.product = product;
        this.brandName = brandName;
        this.name = name;
        this.category = category;
        this.primaryColor = primaryColor;
        this.material = material;
        this.materialSource = materialSource;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
        this.purchaseOrderNumber = purchaseOrderNumber;
        this.purchasePlace = purchasePlace;
        this.memo = memo;
        this.aiJobId = aiJobId;
        this.nextCareDate = nextCareDate;
    }

    public static UserItem create(
            User user,
            Product product,
            String brandName,
            String name,
            ItemCategory category,
            ColorGroup primaryColor,
            MaterialGroup material,
            MaterialSource materialSource,
            LocalDate purchaseDate,
            Long purchasePrice,
            String memo,
            Long aiJobId,
            LocalDate nextCareDate
    ) {
        return create(
                user,
                product,
                brandName,
                name,
                category,
                primaryColor,
                material,
                materialSource,
                purchaseDate,
                purchasePrice,
                null,
                null,
                memo,
                aiJobId,
                nextCareDate
        );
    }

    public static UserItem create(
            User user,
            Product product,
            String brandName,
            String name,
            ItemCategory category,
            ColorGroup primaryColor,
            MaterialGroup material,
            MaterialSource materialSource,
            LocalDate purchaseDate,
            Long purchasePrice,
            String purchaseOrderNumber,
            String purchasePlace,
            String memo,
            Long aiJobId,
            LocalDate nextCareDate
    ) {
        return new UserItem(
                user,
                product,
                brandName,
                name,
                category,
                primaryColor,
                material,
                materialSource,
                purchaseDate,
                purchasePrice,
                purchaseOrderNumber,
                purchasePlace,
                memo,
                aiJobId,
                nextCareDate
        );
    }

    public void update(
            Product product,
            String brandName,
            String name,
            ItemCategory category,
            ColorGroup primaryColor,
            MaterialGroup material,
            MaterialSource materialSource,
            LocalDate purchaseDate,
            Long purchasePrice,
            String memo,
            LocalDate nextCareDate
    ) {
        update(
                product,
                brandName,
                name,
                category,
                primaryColor,
                material,
                materialSource,
                purchaseDate,
                purchasePrice,
                this.purchaseOrderNumber,
                this.purchasePlace,
                memo,
                nextCareDate
        );
    }

    public void update(
            Product product,
            String brandName,
            String name,
            ItemCategory category,
            ColorGroup primaryColor,
            MaterialGroup material,
            MaterialSource materialSource,
            LocalDate purchaseDate,
            Long purchasePrice,
            String purchaseOrderNumber,
            String purchasePlace,
            String memo,
            LocalDate nextCareDate
    ) {
        this.product = product;
        this.brandName = brandName;
        this.name = name;
        this.category = category;
        this.primaryColor = primaryColor;
        this.material = material;
        this.materialSource = materialSource;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
        this.purchaseOrderNumber = purchaseOrderNumber;
        this.purchasePlace = purchasePlace;
        this.memo = memo;
        this.nextCareDate = nextCareDate;
    }

    public void softDelete(Instant deletedAt) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt;
        }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Product getProduct() { return product; }
    public String getBrandName() { return brandName; }
    public String getName() { return name; }
    public ItemCategory getCategory() { return category; }
    public ColorGroup getPrimaryColor() { return primaryColor; }
    public MaterialGroup getMaterial() { return material; }
    public MaterialSource getMaterialSource() { return materialSource; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public Long getPurchasePrice() { return purchasePrice; }
    public String getPurchaseOrderNumber() { return purchaseOrderNumber; }
    public String getPurchasePlace() { return purchasePlace; }
    public String getMemo() { return memo; }
    public Long getAiJobId() { return aiJobId; }
    public LocalDate getNextCareDate() { return nextCareDate; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
