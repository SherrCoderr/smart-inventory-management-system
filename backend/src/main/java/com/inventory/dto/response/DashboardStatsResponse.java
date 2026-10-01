package com.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalProducts;
    private long activeProducts;
    private long totalSuppliers;
    private long totalCategories;
    private BigDecimal totalInventoryValue;
    private long lowStockProductCount;
    private long totalStockInQuantity;
    private long totalStockOutQuantity;
    private List<TransactionResponse> recentTransactions;
    private List<DailyStockMovementResponse> dailyMovements;
}
