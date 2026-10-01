package com.inventory.dto.response;

import com.inventory.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRecommendationResponse {

    private Long productId;
    private String sku;
    private String name;
    private Integer currentStock;
    private Integer minStockThreshold;
    private Integer baseReorderQuantity;
    private Integer deficit;
    private Integer suggestedReorderQuantity;
    private BigDecimal unitPrice;
    private BigDecimal estimatedCost;
    private SupplierResponse supplier;

    public static ReorderRecommendationResponse fromEntity(Product product) {
        if (product == null) return null;

        int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int threshold = product.getMinStockThreshold() != null ? product.getMinStockThreshold() : 0;
        int baseReorder = product.getReorderQuantity() != null ? product.getReorderQuantity() : 20;

        int deficit = Math.max(0, threshold - current);
        int suggestedQty = Math.max(baseReorder, deficit + baseReorder);

        BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal estimatedCost = unitPrice.multiply(BigDecimal.valueOf(suggestedQty));

        return ReorderRecommendationResponse.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .currentStock(current)
                .minStockThreshold(threshold)
                .baseReorderQuantity(baseReorder)
                .deficit(deficit)
                .suggestedReorderQuantity(suggestedQty)
                .unitPrice(unitPrice)
                .estimatedCost(estimatedCost)
                .supplier(SupplierResponse.fromEntity(product.getSupplier()))
                .build();
    }
}
