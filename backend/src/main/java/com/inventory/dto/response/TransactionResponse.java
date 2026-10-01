package com.inventory.dto.response;

import com.inventory.entity.InventoryTransaction;
import com.inventory.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private TransactionType transactionType;
    private Integer quantity;
    private Integer previousStock;
    private Integer newStock;
    private String referenceReason;
    private Long userId;
    private String username;
    private String userFullName;
    private LocalDateTime createdAt;

    public static TransactionResponse fromEntity(InventoryTransaction txn) {
        if (txn == null) return null;

        String fullName = "";
        if (txn.getUser() != null) {
            String fn = txn.getUser().getFirstName() != null ? txn.getUser().getFirstName() : "";
            String ln = txn.getUser().getLastName() != null ? txn.getUser().getLastName() : "";
            fullName = (fn + " " + ln).trim();
            if (fullName.isEmpty()) {
                fullName = txn.getUser().getUsername();
            }
        }

        return TransactionResponse.builder()
                .id(txn.getId())
                .productId(txn.getProduct() != null ? txn.getProduct().getId() : null)
                .productSku(txn.getProduct() != null ? txn.getProduct().getSku() : null)
                .productName(txn.getProduct() != null ? txn.getProduct().getName() : null)
                .transactionType(txn.getTransactionType())
                .quantity(txn.getQuantity())
                .previousStock(txn.getPreviousStock())
                .newStock(txn.getNewStock())
                .referenceReason(txn.getReferenceReason())
                .userId(txn.getUser() != null ? txn.getUser().getId() : null)
                .username(txn.getUser() != null ? txn.getUser().getUsername() : null)
                .userFullName(fullName)
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
