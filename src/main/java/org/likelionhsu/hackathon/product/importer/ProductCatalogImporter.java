package org.likelionhsu.hackathon.product.importer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.likelionhsu.hackathon.product.entity.ProductTag;
import org.likelionhsu.hackathon.product.entity.ProductTagMapping;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.importer.dto.ProductCatalogImportData;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportItem;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportTag;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductCatalogImporter {

    private final ProductCatalogImportValidator catalogValidator;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductTagRepository productTagRepository;
    private final ProductTagMappingRepository productTagMappingRepository;

    public ProductCatalogImporter(
            ProductCatalogImportValidator catalogValidator,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ProductTagRepository productTagRepository,
            ProductTagMappingRepository productTagMappingRepository
    ) {
        this.catalogValidator = catalogValidator;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productTagRepository = productTagRepository;
        this.productTagMappingRepository =
                productTagMappingRepository;
    }

    @Transactional
    public void importCatalog(
            ProductCatalogImportData data
    ) {
        catalogValidator.validate(data);

        Map<TagKey, ProductTag> availableTags =
                loadAvailableTags();

        Set<String> importedSkus =
                data.products()
                        .stream()
                        .map(ProductImportItem::sku)
                        .collect(Collectors.toSet());

        deactivateMissingProducts(
                importedSkus
        );

        for (ProductImportItem item : data.products()) {
            importProduct(
                    item,
                    availableTags
            );
        }
    }

    private void deactivateMissingProducts(
            Set<String> importedSkus
    ) {
        productRepository
                .findAllByBrand(
                        ProductBrand.MCM
                )
                .stream()
                .filter(product ->
                        !importedSkus.contains(
                                product.getSku()
                        )
                )
                .forEach(Product::deactivate);
    }

    private void importProduct(
            ProductImportItem item,
            Map<TagKey, ProductTag> availableTags
    ) {
        Product product =
                productRepository
                        .findBySku(
                                item.sku()
                        )
                        .map(existing -> {
                            existing.updateCatalogInfo(
                                    item.brand(),
                                    item.name(),
                                    item.category(),
                                    item.description(),
                                    item.price(),
                                    item.primaryColor(),
                                    item.material(),
                                    item.productUrl(),
                                    item.status()
                            );

                            return existing;
                        })
                        .orElseGet(
                                () ->
                                        productRepository.save(
                                                Product.create(
                                                        item.brand(),
                                                        item.sku(),
                                                        item.name(),
                                                        item.category(),
                                                        item.description(),
                                                        item.price(),
                                                        item.primaryColor(),
                                                        item.material(),
                                                        item.productUrl(),
                                                        item.status()
                                                )
                                        )
                        );

        replaceImages(
                product,
                item
        );

        replaceTags(
                product,
                item.tags(),
                availableTags
        );
    }

    private void replaceImages(
            Product product,
            ProductImportItem item
    ) {
        productImageRepository.deleteAllByProductId(
                product.getId()
        );

        List<ProductImage> images =
                item.images()
                        .stream()
                        .map(image ->
                                ProductImage.create(
                                        product,
                                        image.url(),
                                        image.publicId(),
                                        image.altText(),
                                        image.sortOrder(),
                                        image.isPrimary()
                                )
                        )
                        .toList();

        productImageRepository.saveAll(
                images
        );
    }

    private void replaceTags(
            Product product,
            List<ProductImportTag> importedTags,
            Map<TagKey, ProductTag> availableTags
    ) {
        productTagMappingRepository.deleteAllByProductId(
                product.getId()
        );

        Set<TagKey> usedTags =
                new HashSet<>();

        List<ProductTagMapping> mappings =
                importedTags.stream()
                        .map(importedTag -> {
                            TagKey key =
                                    new TagKey(
                                            importedTag.type(),
                                            importedTag.code()
                                    );

                            if (!usedTags.add(key)) {
                                throw new ProductCatalogImportValidationException(
                                        "중복 태그가 존재합니다. sku="
                                                + product.getSku()
                                                + ", type="
                                                + importedTag.type()
                                                + ", code="
                                                + importedTag.code()
                                );
                            }

                            ProductTag productTag =
                                    availableTags.get(
                                            key
                                    );

                            if (productTag == null) {
                                throw new ProductCatalogImportValidationException(
                                        "DB에 존재하지 않는 ProductTag입니다. "
                                                + "sku="
                                                + product.getSku()
                                                + ", type="
                                                + importedTag.type()
                                                + ", code="
                                                + importedTag.code()
                                );
                            }

                            return ProductTagMapping.create(
                                    product,
                                    productTag
                            );
                        })
                        .toList();

        productTagMappingRepository.saveAll(
                mappings
        );
    }

    private Map<TagKey, ProductTag> loadAvailableTags() {
        Map<TagKey, ProductTag> tags =
                new HashMap<>();

        for (ProductTag tag
                : productTagRepository.findAll()) {

            TagKey key =
                    new TagKey(
                            tag.getType(),
                            tag.getCode()
                    );

            tags.put(
                    key,
                    tag
            );
        }

        return tags;
    }

    private record TagKey(
            ProductTagType type,
            String code
    ) {
    }
}