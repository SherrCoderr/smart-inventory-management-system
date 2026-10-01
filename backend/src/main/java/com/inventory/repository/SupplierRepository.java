package com.inventory.repository;

import com.inventory.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Supplier> findByActiveTrueOrderByNameAsc();

    Page<Supplier> findByActiveTrue(Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE " +
           "(:query IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:active IS NULL OR s.active = :active)")
    Page<Supplier> searchSuppliers(
        @Param("query") String query,
        @Param("active") Boolean active,
        Pageable pageable
    );

    long countByActiveTrue();
}
