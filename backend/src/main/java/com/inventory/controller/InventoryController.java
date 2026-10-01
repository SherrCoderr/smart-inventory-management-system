package com.inventory.controller;

import com.inventory.dto.request.StockOperationRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.PagedResponse;
import com.inventory.dto.response.ProductResponse;
import com.inventory.dto.response.ReorderRecommendationResponse;
import com.inventory.dto.response.TransactionResponse;
import com.inventory.entity.TransactionType;
import com.inventory.security.UserPrincipal;
import com.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/stock-in")
    public ResponseEntity<ApiResponse<TransactionResponse>> recordStockIn(
            @Valid @RequestBody StockOperationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TransactionResponse response = inventoryService.recordStockIn(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Stock-IN recorded successfully", response));
    }

    @PostMapping("/stock-out")
    public ResponseEntity<ApiResponse<TransactionResponse>> recordStockOut(
            @Valid @RequestBody StockOperationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TransactionResponse response = inventoryService.recordStockOut(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Stock-OUT recorded successfully", response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<TransactionResponse> response = inventoryService.getTransactions(
                productId, userId, transactionType, startDate, endDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Inventory transactions retrieved successfully", response));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getLowStockProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "currentStock") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<ProductResponse> response = inventoryService.getLowStockProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Low-stock products retrieved successfully", response));
    }

    @GetMapping("/reorder-recommendations")
    public ResponseEntity<ApiResponse<List<ReorderRecommendationResponse>>> getReorderRecommendations() {
        List<ReorderRecommendationResponse> recommendations = inventoryService.getReorderRecommendations();
        return ResponseEntity.ok(ApiResponse.success("Reorder recommendations generated successfully", recommendations));
    }
}
