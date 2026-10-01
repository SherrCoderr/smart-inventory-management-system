package com.inventory.service.impl;

import com.inventory.config.RedisConfig;
import com.inventory.dto.request.SupplierRequest;
import com.inventory.dto.response.PagedResponse;
import com.inventory.dto.response.SupplierResponse;
import com.inventory.entity.Supplier;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.SupplierRepository;
import com.inventory.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierServiceImpl.class);

    private final SupplierRepository supplierRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SupplierResponse> getAllSuppliers(String search, Boolean active, Pageable pageable) {
        String searchQuery = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Supplier> page = supplierRepository.searchSuppliers(searchQuery, active, pageable);

        List<SupplierResponse> responses = page.getContent().stream()
                .map(SupplierResponse::fromEntity)
                .toList();

        return PagedResponse.fromPage(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        return SupplierResponse.fromEntity(supplier);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true)
    })
    public SupplierResponse createSupplier(SupplierRequest request) {
        String trimmedEmail = request.getEmail().trim().toLowerCase();
        if (supplierRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            throw new DuplicateResourceException("Supplier", "email", trimmedEmail);
        }

        Supplier supplier = Supplier.builder()
                .name(request.getName().trim())
                .email(trimmedEmail)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Supplier savedSupplier = supplierRepository.save(supplier);
        log.info("Supplier created with ID: {}", savedSupplier.getId());
        return SupplierResponse.fromEntity(savedSupplier);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_DETAILS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    })
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        String trimmedEmail = request.getEmail().trim().toLowerCase();
        if (supplierRepository.existsByEmailIgnoreCaseAndIdNot(trimmedEmail, id)) {
            throw new DuplicateResourceException("Supplier", "email", trimmedEmail);
        }

        supplier.setName(request.getName().trim());
        supplier.setEmail(trimmedEmail);
        supplier.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        supplier.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        if (request.getActive() != null) {
            supplier.setActive(request.getActive());
        }

        Supplier updatedSupplier = supplierRepository.save(supplier);
        log.info("Supplier updated with ID: {}", updatedSupplier.getId());
        return SupplierResponse.fromEntity(updatedSupplier);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_DETAILS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    })
    public SupplierResponse toggleSupplierStatus(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        supplier.setActive(!supplier.isActive());
        Supplier updatedSupplier = supplierRepository.save(supplier);
        log.info("Supplier {} active status toggled to: {}", id, updatedSupplier.isActive());
        return SupplierResponse.fromEntity(updatedSupplier);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_DETAILS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    })
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        // Soft delete for audit & integrity
        supplier.setActive(false);
        supplierRepository.save(supplier);
        log.info("Supplier soft-deleted (deactivated) with ID: {}", id);
    }
}
