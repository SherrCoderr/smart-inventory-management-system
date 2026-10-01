package com.inventory.service;

import com.inventory.dto.request.ProductRequest;
import com.inventory.dto.response.PagedResponse;
import com.inventory.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    PagedResponse<ProductResponse> getAllProducts(
            String query,
            Long categoryId,
            Long supplierId,
            Boolean active,
            Pageable pageable
    );

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySku(String sku);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    ProductResponse toggleProductStatus(Long id);

    void deleteProduct(Long id);
}
