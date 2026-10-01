package com.inventory.repository;

import com.inventory.entity.InventoryTransaction;
import com.inventory.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @Query(
        value = "SELECT t FROM InventoryTransaction t " +
                "JOIN FETCH t.product p " +
                "JOIN FETCH t.user u " +
                "WHERE (:productId IS NULL OR p.id = :productId) " +
                "AND (:userId IS NULL OR u.id = :userId) " +
                "AND (:transactionType IS NULL OR t.transactionType = :transactionType) " +
                "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
                "AND (:endDate IS NULL OR t.createdAt <= :endDate)",
        countQuery = "SELECT COUNT(t) FROM InventoryTransaction t " +
                     "WHERE (:productId IS NULL OR t.product.id = :productId) " +
                     "AND (:userId IS NULL OR t.user.id = :userId) " +
                     "AND (:transactionType IS NULL OR t.transactionType = :transactionType) " +
                     "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
                     "AND (:endDate IS NULL OR t.createdAt <= :endDate)"
    )
    Page<InventoryTransaction> searchTransactions(
        @Param("productId") Long productId,
        @Param("userId") Long userId,
        @Param("transactionType") TransactionType transactionType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<InventoryTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<InventoryTransaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType transactionType, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM InventoryTransaction t " +
           "WHERE t.transactionType = :transactionType " +
           "AND (:since IS NULL OR t.createdAt >= :since)")
    long sumQuantityByTypeAndSince(
        @Param("transactionType") TransactionType transactionType,
        @Param("since") LocalDateTime since
    );

    List<InventoryTransaction> findByCreatedAtGreaterThanEqual(LocalDateTime since);

    List<InventoryTransaction> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT t FROM InventoryTransaction t JOIN FETCH t.product JOIN FETCH t.user ORDER BY t.createdAt DESC")
    List<InventoryTransaction> findRecentTransactionsWithDetails(Pageable pageable);
}
