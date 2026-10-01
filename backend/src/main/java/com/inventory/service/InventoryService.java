package com.inventory.service;

import com.inventory.dto.request.StockOperationRequest;
import com.inventory.dto.response.PagedResponse;
import com.inventory.dto.response.ProductResponse;
import com.inventory.dto.response.ReorderRecommendationResponse;
import com.inventory.dto.response.TransactionResponse;
import com.inventory.entity.TransactionType;
import com.inventory.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryService {

    TransactionResponse recordStockIn(StockOperationRequest request, UserPrincipal currentUser);

    TransactionResponse recordStockOut(StockOperationRequest request, UserPrincipal currentUser);

    PagedResponse<TransactionResponse> getTransactions(
            Long productId,
            Long userId,
            TransactionType transactionType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    PagedResponse<ProductResponse> getLowStockProducts(Pageable pageable);

    List<ReorderRecommendationResponse> getReorderRecommendations();
}
