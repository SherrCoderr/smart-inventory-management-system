package com.inventory.service;

import com.inventory.dto.request.SupplierRequest;
import com.inventory.dto.response.PagedResponse;
import com.inventory.dto.response.SupplierResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierService {

    PagedResponse<SupplierResponse> getAllSuppliers(String search, Boolean active, Pageable pageable);

    List<SupplierResponse> getActiveSuppliers();

    SupplierResponse getSupplierById(Long id);

    SupplierResponse createSupplier(SupplierRequest request);

    SupplierResponse updateSupplier(Long id, SupplierRequest request);

    SupplierResponse toggleSupplierStatus(Long id);

    void deleteSupplier(Long id);
}
