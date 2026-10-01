package com.inventory.service.impl;

import com.inventory.config.RedisConfig;
import com.inventory.dto.response.DailyStockMovementResponse;
import com.inventory.dto.response.DashboardStatsResponse;
import com.inventory.dto.response.TransactionResponse;
import com.inventory.entity.InventoryTransaction;
import com.inventory.entity.TransactionType;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_DASHBOARD_STATS, key = "'overview'")
    public DashboardStatsResponse getDashboardStats() {
        log.info("Aggregating dashboard statistics and inventory metrics");

        LocalDate today = LocalDate.now();
        LocalDateTime sevenDaysAgo = today.minusDays(6).atStartOfDay();

        // 1. High-level metric counts via database aggregation
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByActiveTrue();
        long totalSuppliers = supplierRepository.countByActiveTrue();
        long totalCategories = categoryRepository.count();
        BigDecimal totalInventoryValue = productRepository.calculateTotalInventoryValue();
        long lowStockProductCount = productRepository.countLowStockProducts();

        // 2. 7-day stock movement volume aggregations via SQL SUM
        long totalStockIn = transactionRepository.sumQuantityByTypeAndSince(TransactionType.STOCK_IN, sevenDaysAgo);
        long totalStockOut = transactionRepository.sumQuantityByTypeAndSince(TransactionType.STOCK_OUT, sevenDaysAgo);

        // 3. Top 10 recent transactions with JOIN FETCH
        List<TransactionResponse> recentTransactions = transactionRepository
                .findRecentTransactionsWithDetails(PageRequest.of(0, 10))
                .stream()
                .map(TransactionResponse::fromEntity)
                .toList();

        // 4. Deterministic 7-day daily breakdown for frontend charts
        List<DailyStockMovementResponse> dailyMovements = buildDailyMovements(sevenDaysAgo, today);

        return DashboardStatsResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalSuppliers(totalSuppliers)
                .totalCategories(totalCategories)
                .totalInventoryValue(totalInventoryValue != null ? totalInventoryValue : BigDecimal.ZERO)
                .lowStockProductCount(lowStockProductCount)
                .totalStockInQuantity(totalStockIn)
                .totalStockOutQuantity(totalStockOut)
                .recentTransactions(recentTransactions)
                .dailyMovements(dailyMovements)
                .build();
    }

    private List<DailyStockMovementResponse> buildDailyMovements(LocalDateTime since, LocalDate today) {
        // Map 7 calendar days with 0 defaults
        Map<LocalDate, DailyStockMovementResponse> dateMap = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dateMap.put(date, DailyStockMovementResponse.builder()
                    .date(date)
                    .stockIn(0L)
                    .stockOut(0L)
                    .build());
        }

        // Fetch last 7 days transactions and accumulate
        List<InventoryTransaction> recentTxns = transactionRepository.findByCreatedAtGreaterThanEqual(since);
        for (InventoryTransaction tx : recentTxns) {
            LocalDate txDate = tx.getCreatedAt().toLocalDate();
            DailyStockMovementResponse daily = dateMap.get(txDate);
            if (daily != null) {
                if (tx.getTransactionType() == TransactionType.STOCK_IN) {
                    daily.setStockIn(daily.getStockIn() + tx.getQuantity());
                } else if (tx.getTransactionType() == TransactionType.STOCK_OUT) {
                    daily.setStockOut(daily.getStockOut() + tx.getQuantity());
                }
            }
        }

        return new ArrayList<>(dateMap.values());
    }
}
