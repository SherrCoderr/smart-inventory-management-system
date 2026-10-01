package com.inventory.service.impl;

import com.inventory.config.RedisConfig;
import com.inventory.dto.request.StockOperationRequest;
import com.inventory.dto.response.PagedResponse;
import com.inventory.dto.response.ProductResponse;
import com.inventory.dto.response.ReorderRecommendationResponse;
import com.inventory.dto.response.TransactionResponse;
import com.inventory.entity.InventoryTransaction;
import com.inventory.entity.Product;
import com.inventory.entity.TransactionType;
import com.inventory.entity.User;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.UserPrincipal;
import com.inventory.service.InventoryService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_DETAILS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_LOW_STOCK_PRODUCTS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    })
    public TransactionResponse recordStockIn(StockOperationRequest request, UserPrincipal currentUser) {
        log.info("Processing STOCK_IN for product ID: {}, quantity: {} by user: {}",
                request.getProductId(), request.getQuantity(), currentUser.getUsername());

        // 1. Lock product row to prevent concurrency races
        Product product = productRepository.findByIdWithLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Cannot perform stock-in on an inactive product (SKU: " + product.getSku() + ")");
        }

        // 2. Fetch authenticated user entity
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        int previousStock = product.getCurrentStock();
        int newStock = previousStock + request.getQuantity();

        // 3. Atomically update product stock
        product.setCurrentStock(newStock);
        productRepository.save(product);

        // 4. Create immutable audit transaction record
        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(product)
                .user(user)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(request.getQuantity())
                .previousStock(previousStock)
                .newStock(newStock)
                .referenceReason(request.getReferenceReason() != null ? request.getReferenceReason().trim() : "Stock replenishment")
                .build();

        InventoryTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("STOCK_IN completed successfully for SKU: {}. Stock: {} -> {}",
                product.getSku(), previousStock, newStock);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_DETAILS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_LOW_STOCK_PRODUCTS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    })
    public TransactionResponse recordStockOut(StockOperationRequest request, UserPrincipal currentUser) {
        log.info("Processing STOCK_OUT for product ID: {}, quantity: {} by user: {}",
                request.getProductId(), request.getQuantity(), currentUser.getUsername());

        // 1. Lock product row to prevent concurrency races
        Product product = productRepository.findByIdWithLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Cannot perform stock-out on an inactive product (SKU: " + product.getSku() + ")");
        }

        int previousStock = product.getCurrentStock();

        // 2. Stock Floor Invariant Validation
        if (previousStock < request.getQuantity()) {
            log.warn("STOCK_OUT failed: Insufficient stock for SKU: {}. Requested: {}, Available: {}",
                    product.getSku(), request.getQuantity(), previousStock);
            throw new InsufficientStockException(product.getSku(), request.getQuantity(), previousStock);
        }

        // 3. Fetch authenticated user entity
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        int newStock = previousStock - request.getQuantity();

        // 4. Atomically update product stock
        product.setCurrentStock(newStock);
        productRepository.save(product);

        // 5. Create immutable audit transaction record
        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(product)
                .user(user)
                .transactionType(TransactionType.STOCK_OUT)
                .quantity(request.getQuantity())
                .previousStock(previousStock)
                .newStock(newStock)
                .referenceReason(request.getReferenceReason() != null ? request.getReferenceReason().trim() : "Stock dispatch")
                .build();

        InventoryTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("STOCK_OUT completed successfully for SKU: {}. Stock: {} -> {}",
                product.getSku(), previousStock, newStock);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactions(
            Long productId,
            Long userId,
            TransactionType transactionType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Specification<InventoryTransaction> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<InventoryTransaction> page = transactionRepository.findAll(spec, pageable);

        List<TransactionResponse> responses = page.getContent().stream()
                .map(TransactionResponse::fromEntity)
                .toList();

        return PagedResponse.fromPage(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = RedisConfig.CACHE_LOW_STOCK_PRODUCTS,
        key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()"
    )
    public PagedResponse<ProductResponse> getLowStockProducts(Pageable pageable) {
        Page<Product> page = productRepository.findLowStockProducts(pageable);
        List<ProductResponse> responses = page.getContent().stream()
                .map(ProductResponse::fromEntity)
                .toList();

        return PagedResponse.fromPage(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReorderRecommendationResponse> getReorderRecommendations() {
        List<Product> lowStockProducts = productRepository.findLowStockProducts();
        return lowStockProducts.stream()
                .map(ReorderRecommendationResponse::fromEntity)
                .toList();
    }
}
