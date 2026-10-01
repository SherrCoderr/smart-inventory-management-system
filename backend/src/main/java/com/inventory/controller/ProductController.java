package com.inventory.controller;

import com.inventory.dto.request.ProductRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.PagedResponse;
import com.inventory.dto.response.ProductResponse;
import com.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<ProductResponse> response = productService.getAllProducts(query, categoryId, supplierId, active, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        ProductResponse product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleProductStatus(@PathVariable Long id) {
        ProductResponse product = productService.toggleProductStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Product status toggled successfully", product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }
}
