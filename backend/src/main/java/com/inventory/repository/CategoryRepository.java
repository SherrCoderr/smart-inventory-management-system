package com.inventory.repository;

import com.inventory.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT c FROM Category c WHERE " +
           "(:query IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY c.name ASC")
    List<Category> searchCategories(@Param("query") String query);
}
