package com.inventory.dto.response;

import com.inventory.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer currentStock;
    private Integer minStockThreshold;
    private Integer reorderQuantity;
    private boolean lowStock;
    private CategoryResponse category;
    private SupplierResponse supplier;
    private boolean active;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product) {
        if (product == null) return null;

        boolean isLow = product.getCurrentStock() != null &&
                        product.getMinStockThreshold() != null &&
                        product.getCurrentStock() <= product.getMinStockThreshold();

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .currentStock(product.getCurrentStock())
                .minStockThreshold(product.getMinStockThreshold())
                .reorderQuantity(product.getReorderQuantity())
                .lowStock(isLow)
                .category(CategoryResponse.fromEntity(product.getCategory()))
                .supplier(SupplierResponse.fromEntity(product.getSupplier()))
                .active(product.isActive())
                .version(product.getVersion())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
