package org.likelionhsu.hackathon.useritem.dto.request;

import java.time.LocalDate;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;

import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class UserItemUpdateRequest {

    private Long productId;
    private boolean productIdPresent;

    @Size(max = 100, message = "100자 이하여야 합니다.")
    private String brandName;
    private boolean brandNamePresent;

    @Size(max = 200, message = "200자 이하여야 합니다.")
    private String name;
    private boolean namePresent;

    private ItemCategory category;
    private boolean categoryPresent;

    private ColorGroup primaryColor;
    private boolean primaryColorPresent;

    private MaterialGroup material;
    private boolean materialPresent;

    private MaterialSource materialSource;
    private boolean materialSourcePresent;

    @PastOrPresent(message = "미래 날짜일 수 없습니다.")
    private LocalDate purchaseDate;
    private boolean purchaseDatePresent;

    @PositiveOrZero(message = "0 이상이어야 합니다.")
    private Long purchasePrice;
    private boolean purchasePricePresent;

    @Size(max = 100, message = "100자 이하여야 합니다.")
    private String purchaseOrderNumber;
    private boolean purchaseOrderNumberPresent;

    @Size(max = 200, message = "200자 이하여야 합니다.")
    private String purchasePlace;
    private boolean purchasePlacePresent;

    @Size(max = 1000, message = "1000자 이하여야 합니다.")
    private String memo;
    private boolean memoPresent;

    private Long aiJobId;
    private boolean aiJobIdPresent;

    private LocalDate nextCareDate;
    private boolean nextCareDatePresent;

    @NotNull(message = "필수 입력값입니다.")
    private Long version;

    @JsonSetter("productId")
    public void setProductId(Long productId) {
        this.productId = productId;
        this.productIdPresent = true;
    }

    @JsonSetter("brandName")
    public void setBrandName(String brandName) {
        this.brandName = brandName;
        this.brandNamePresent = true;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
        this.namePresent = true;
    }

    @JsonSetter("category")
    public void setCategory(ItemCategory category) {
        this.category = category;
        this.categoryPresent = true;
    }

    @JsonSetter("primaryColor")
    public void setPrimaryColor(ColorGroup primaryColor) {
        this.primaryColor = primaryColor;
        this.primaryColorPresent = true;
    }

    @JsonSetter("material")
    public void setMaterial(MaterialGroup material) {
        this.material = material;
        this.materialPresent = true;
    }

    @JsonSetter("materialSource")
    public void setMaterialSource(MaterialSource materialSource) {
        this.materialSource = materialSource;
        this.materialSourcePresent = true;
    }

    @JsonSetter("purchaseDate")
    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
        this.purchaseDatePresent = true;
    }

    @JsonSetter("purchasePrice")
    public void setPurchasePrice(Long purchasePrice) {
        this.purchasePrice = purchasePrice;
        this.purchasePricePresent = true;
    }

    @JsonSetter("purchaseOrderNumber")
    public void setPurchaseOrderNumber(String purchaseOrderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
        this.purchaseOrderNumberPresent = true;
    }

    @JsonSetter("purchasePlace")
    public void setPurchasePlace(String purchasePlace) {
        this.purchasePlace = purchasePlace;
        this.purchasePlacePresent = true;
    }

    @JsonSetter("memo")
    public void setMemo(String memo) {
        this.memo = memo;
        this.memoPresent = true;
    }

    @JsonSetter("aiJobId")
    public void setAiJobId(Long aiJobId) {
        this.aiJobId = aiJobId;
        this.aiJobIdPresent = true;
    }

    @JsonSetter("nextCareDate")
    public void setNextCareDate(LocalDate nextCareDate) {
        this.nextCareDate = nextCareDate;
        this.nextCareDatePresent = true;
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getProductId() { return productId; }
    public boolean isProductIdPresent() { return productIdPresent; }
    public String getBrandName() { return brandName; }
    public boolean isBrandNamePresent() { return brandNamePresent; }
    public String getName() { return name; }
    public boolean isNamePresent() { return namePresent; }
    public ItemCategory getCategory() { return category; }
    public boolean isCategoryPresent() { return categoryPresent; }
    public ColorGroup getPrimaryColor() { return primaryColor; }
    public boolean isPrimaryColorPresent() { return primaryColorPresent; }
    public MaterialGroup getMaterial() { return material; }
    public boolean isMaterialPresent() { return materialPresent; }
    public MaterialSource getMaterialSource() { return materialSource; }
    public boolean isMaterialSourcePresent() { return materialSourcePresent; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public boolean isPurchaseDatePresent() { return purchaseDatePresent; }
    public Long getPurchasePrice() { return purchasePrice; }
    public boolean isPurchasePricePresent() { return purchasePricePresent; }
    public String getPurchaseOrderNumber() { return purchaseOrderNumber; }
    public boolean isPurchaseOrderNumberPresent() { return purchaseOrderNumberPresent; }
    public String getPurchasePlace() { return purchasePlace; }
    public boolean isPurchasePlacePresent() { return purchasePlacePresent; }
    public String getMemo() { return memo; }
    public boolean isMemoPresent() { return memoPresent; }
    public Long getAiJobId() { return aiJobId; }
    public boolean isAiJobIdPresent() { return aiJobIdPresent; }
    public LocalDate getNextCareDate() { return nextCareDate; }
    public boolean isNextCareDatePresent() { return nextCareDatePresent; }
    public Long getVersion() { return version; }

    public boolean hasChanges() {
        return productIdPresent
                || brandNamePresent
                || namePresent
                || categoryPresent
                || primaryColorPresent
                || materialPresent
                || materialSourcePresent
                || purchaseDatePresent
                || purchasePricePresent
                || purchaseOrderNumberPresent
                || purchasePlacePresent
                || memoPresent
                || aiJobIdPresent
                || nextCareDatePresent;
    }
}
