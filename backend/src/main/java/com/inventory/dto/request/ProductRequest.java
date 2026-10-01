package com.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(min = 2, max = 50, message = "SKU must be between 2 and 50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 150, message = "Product name must be between 2 and 150 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Price format must have up to 8 integer digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Current stock is required")
    @PositiveOrZero(message = "Current stock cannot be negative")
    @Builder.Default
    private Integer currentStock = 0;

    @NotNull(message = "Minimum stock threshold is required")
    @PositiveOrZero(message = "Minimum stock threshold cannot be negative")
    @Builder.Default
    private Integer minStockThreshold = 5;

    @NotNull(message = "Reorder quantity is required")
    @PositiveOrZero(message = "Reorder quantity cannot be negative")
    @Builder.Default
    private Integer reorderQuantity = 20;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @Builder.Default
    private Boolean active = true;
}
