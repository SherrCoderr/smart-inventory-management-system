package com.inventory.service.impl;

import com.inventory.config.RedisConfig;
import com.inventory.dto.request.CategoryRequest;
import com.inventory.dto.response.CategoryResponse;
import com.inventory.entity.Category;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CategoryRepository;
import com.inventory.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_CATEGORIES_LIST, key = "#search != null ? #search : 'all'")
    public List<CategoryResponse> getAllCategories(String search) {
        log.info("Fetching categories from PostgreSQL DB (cache miss). Search query: {}", search);
        List<Category> categories = (search != null && !search.trim().isEmpty())
                ? categoryRepository.searchCategories(search.trim())
                : categoryRepository.findAll();

        return categories.stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_CATEGORIES_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true)
    })
    public CategoryResponse createCategory(CategoryRequest request) {
        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Category", "name", trimmedName);
        }

        Category category = Category.builder()
                .name(trimmedName)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created with ID: {}", savedCategory.getId());
        return CategoryResponse.fromEntity(savedCategory);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_CATEGORIES_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_DETAILS, allEntries = true)
    })
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Category", "name", trimmedName);
        }

        category.setName(trimmedName);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated with ID: {}", updatedCategory.getId());
        return CategoryResponse.fromEntity(updatedCategory);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_CATEGORIES_LIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_LIST, allEntries = true)
    })
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", "id", id);
        }
        categoryRepository.deleteById(id);
        log.info("Category deleted with ID: {}", id);
    }
}
