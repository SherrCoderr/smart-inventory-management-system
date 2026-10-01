package com.inventory.repository;

import com.inventory.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    long countByActiveTrue();

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    Page<Product> findBySupplierIdAndActiveTrue(Long supplierId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.currentStock <= p.minStockThreshold ORDER BY p.currentStock ASC")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.currentStock <= p.minStockThreshold ORDER BY p.currentStock ASC")
    Page<Product> findLowStockProducts(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.currentStock <= p.minStockThreshold")
    long countLowStockProducts();

    @Query("SELECT SUM(p.price * p.currentStock) FROM Product p WHERE p.active = true")
    BigDecimal calculateTotalInventoryValue();

    @Query("SELECT p FROM Product p WHERE " +
           "(:active IS NULL OR p.active = :active) AND " +
           "(:pattern IS NULL OR LOWER(p.name) LIKE :pattern OR LOWER(p.sku) LIKE :pattern) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:supplierId IS NULL OR p.supplier.id = :supplierId)")
    Page<Product> searchProducts(
        @Param("pattern") String pattern,
        @Param("categoryId") Long categoryId,
        @Param("supplierId") Long supplierId,
        @Param("active") Boolean active,
        Pageable pageable
    );
}
