package com.inventory.controller;

import com.inventory.dto.request.SupplierRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.PagedResponse;
import com.inventory.dto.response.SupplierResponse;
import com.inventory.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SupplierResponse>>> getAllSuppliers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<SupplierResponse> response = supplierService.getAllSuppliers(search, active, pageable);
        return ResponseEntity.ok(ApiResponse.success("Suppliers retrieved successfully", response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getActiveSuppliers() {
        List<SupplierResponse> suppliers = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(ApiResponse.success("Active suppliers retrieved successfully", suppliers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplierById(@PathVariable Long id) {
        SupplierResponse supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier retrieved successfully", supplier));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(@Valid @RequestBody SupplierRequest request) {
        SupplierResponse supplier = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supplier created successfully", supplier));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request
    ) {
        SupplierResponse supplier = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.success("Supplier updated successfully", supplier));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponse>> toggleSupplierStatus(@PathVariable Long id) {
        SupplierResponse supplier = supplierService.toggleSupplierStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier status toggled successfully", supplier));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier deleted successfully"));
    }
}
