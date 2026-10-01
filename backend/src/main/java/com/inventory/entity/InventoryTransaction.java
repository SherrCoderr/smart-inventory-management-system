package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "inventory_transactions",
    indexes = {
        @Index(name = "idx_inv_txn_product", columnList = "product_id"),
        @Index(name = "idx_inv_txn_user", columnList = "user_id"),
        @Index(name = "idx_inv_txn_created_at", columnList = "created_at"),
        @Index(name = "idx_inv_txn_type", columnList = "transaction_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "previous_stock", nullable = false)
    private Integer previousStock;

    @Column(name = "new_stock", nullable = false)
    private Integer newStock;

    @Column(name = "reference_reason", length = 255)
    private String referenceReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
