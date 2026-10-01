package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_product_sku", columnList = "sku", unique = true),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_supplier", columnList = "supplier_id"),
        @Index(name = "idx_product_current_stock", columnList = "current_stock"),
        @Index(name = "idx_product_active", columnList = "active")
    }
)
@Check(constraints = "current_stock >= 0 AND min_stock_threshold >= 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "current_stock", nullable = false)
    @Builder.Default
    private Integer currentStock = 0;

    @Column(name = "min_stock_threshold", nullable = false)
    @Builder.Default
    private Integer minStockThreshold = 5;

    @Column(name = "reorder_quantity", nullable = false)
    @Builder.Default
    private Integer reorderQuantity = 20;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
