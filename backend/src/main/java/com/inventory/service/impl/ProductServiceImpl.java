package com.inventory.service.impl;

import com.inventory.config.RedisConfig;
import com.inventory.dto.request.ProductRequest;
import com.inventory.dto.response.PagedResponse;
import com.inventory.dto.response.ProductResponse;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.entity.Supplier;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = RedisConfig.CACHE_PRODUCTS_LIST,
        key = "T(java.util.Objects).hash(#query, #categoryId, #supplierId, #active, #pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString())"
    )
    public PagedResponse<ProductResponse> getAllProducts(
            String query,
            Long categoryId,
            Long supplierId,
            Boolean active,
            Pageable pageable
    ) {
        log.info("Querying products from PostgreSQL DB (cache miss). Filter: query={}, categoryId={}, supplierId={}, active={}",
                query, categoryId, supplierId, active);

        Specification<Product> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> page = productRepository.findAll(spec, pageable);

        List<ProductResponse> responses = page.getContent().stream()
                .map(ProductResponse::fromEntity)
                .toList();

        return PagedResponse.fromPage(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_PRODUCT_DETAILS, key = "'id:' + #id")
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product details by ID from PostgreSQL DB (cache miss): {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_PRODUCT_DETAILS, key = "'sku:' + #sku.trim().toLowerCase()")
    public ProductResponse getProductBySku(String sku) {
        log.info("Fetching product details by SKU from PostgreSQL DB (cache miss): {}", sku);
        Product product = productRepository.findBySkuIgnoreCase(sku.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public ProductResponse createProduct(ProductRequest request) {
        String trimmedSku = request.getSku().trim().toUpperCase();

        if (productRepository.existsBySkuIgnoreCase(trimmedSku)) {
            throw new DuplicateResourceException("Product", "SKU", trimmedSku);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        Product product = Product.builder()
                .sku(trimmedSku)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .price(request.getPrice())
                .currentStock(request.getCurrentStock() != null ? request.getCurrentStock() : 0)
                .minStockThreshold(request.getMinStockThreshold() != null ? request.getMinStockThreshold() : 5)
                .reorderQuantity(request.getReorderQuantity() != null ? request.getReorderQuantity() : 20)
                .category(category)
                .supplier(supplier)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {} and SKU: {}", savedProduct.getId(), savedProduct.getSku());
        return ProductResponse.fromEntity(savedProduct);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_DETAILS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        String trimmedSku = request.getSku().trim().toUpperCase();
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(trimmedSku, id)) {
            throw new DuplicateResourceException("Product", "SKU", trimmedSku);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        product.setSku(trimmedSku);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setPrice(request.getPrice());
        if (request.getCurrentStock() != null) {
            product.setCurrentStock(request.getCurrentStock());
        }
        if (request.getMinStockThreshold() != null) {
            product.setMinStockThreshold(request.getMinStockThreshold());
        }
        if (request.getReorderQuantity() != null) {
            product.setReorderQuantity(request.getReorderQuantity());
        }
        product.setCategory(category);
        product.setSupplier(supplier);
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated with ID: {} and SKU: {}", updatedProduct.getId(), updatedProduct.getSku());
        return ProductResponse.fromEntity(updatedProduct);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_DETAILS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public ProductResponse toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setActive(!product.isActive());
        Product updatedProduct = productRepository.save(product);
        log.info("Product {} active status toggled to: {}", id, updatedProduct.isActive());
        return ProductResponse.fromEntity(updatedProduct);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_DETAILS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Soft delete: deactivating product ensures transaction audit history remains intact
        product.setActive(false);
        productRepository.save(product);
        log.info("Product soft-deleted (deactivated) with ID: {}", id);
    }
}
