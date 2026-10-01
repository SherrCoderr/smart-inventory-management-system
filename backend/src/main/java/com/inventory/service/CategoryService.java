package com.inventory.service;

import com.inventory.dto.request.CategoryRequest;
import com.inventory.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories(String search);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
