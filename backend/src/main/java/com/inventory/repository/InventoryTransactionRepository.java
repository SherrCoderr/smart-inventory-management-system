package com.inventory.repository;

import com.inventory.entity.InventoryTransaction;
import com.inventory.entity.Product;
import com.inventory.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {

    @Override
    @EntityGraph(attributePaths = {"product", "user"})
    Page<InventoryTransaction> findAll(Specification<InventoryTransaction> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "user"})
    Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "user"})
    Page<InventoryTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "user"})
    Page<InventoryTransaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType transactionType, Pageable pageable);

    @Query("SELECT SUM(t.quantity) FROM InventoryTransaction t WHERE t.transactionType = :transactionType AND t.createdAt >= :since")
    Long sumQuantityByTypeSince(
        @Param("transactionType") TransactionType transactionType,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT SUM(t.quantity) FROM InventoryTransaction t WHERE t.transactionType = :transactionType")
    Long sumQuantityByType(@Param("transactionType") TransactionType transactionType);

    default long sumQuantityByTypeAndSince(TransactionType transactionType, LocalDateTime since) {
        Long sum = (since != null) ? sumQuantityByTypeSince(transactionType, since) : sumQuantityByType(transactionType);
        return sum != null ? sum : 0L;
    }

    @EntityGraph(attributePaths = {"product", "user"})
    List<InventoryTransaction> findByCreatedAtGreaterThanEqual(LocalDateTime since);

    @EntityGraph(attributePaths = {"product", "user"})
    List<InventoryTransaction> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"product", "user"})
    @Query("SELECT t FROM InventoryTransaction t ORDER BY t.createdAt DESC")
    List<InventoryTransaction> findRecentTransactionsWithDetails(Pageable pageable);

    boolean existsByReferenceReason(String referenceReason);

    boolean existsByProductAndReferenceReason(Product product, String referenceReason);
}
