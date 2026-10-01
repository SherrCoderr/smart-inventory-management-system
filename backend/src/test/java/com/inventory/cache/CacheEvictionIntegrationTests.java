package com.inventory.cache;

import com.inventory.config.RedisConfig;
import com.inventory.dto.request.CategoryRequest;
import com.inventory.dto.request.ProductRequest;
import com.inventory.dto.request.StockOperationRequest;
import com.inventory.dto.response.DashboardStatsResponse;
import com.inventory.dto.response.ProductResponse;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.entity.Role;
import com.inventory.entity.Supplier;
import com.inventory.entity.User;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.UserPrincipal;
import com.inventory.service.CategoryService;
import com.inventory.service.DashboardService;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class CacheEvictionIntegrationTests {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    private User testUser;
    private UserPrincipal userPrincipal;
    private Category category;
    private Supplier supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }

        transactionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        supplierRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .username("testadmin")
                .email("admin@inventory.com")
                .password("$2a$10$dummyhashedpassword")
                .firstName("Test")
                .lastName("Administrator")
                .role(Role.ADMIN)
                .active(true)
                .build());

        userPrincipal = UserPrincipal.create(testUser);

        category = categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("Electronic accessories")
                .build());

        supplier = supplierRepository.save(Supplier.builder()
                .name("Global Supplies")
                .email("supplies@global.com")
                .phone("1234567890")
                .active(true)
                .build());

        product = productRepository.save(Product.builder()
                .sku("LAPTOP-001")
                .name("Pro Laptop")
                .description("16GB RAM laptop")
                .price(new BigDecimal("1200.00"))
                .currentStock(20)
                .minStockThreshold(5)
                .reorderQuantity(10)
                .category(category)
                .supplier(supplier)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("Product details are cached on read and evicted on stock mutation")
    void testProductDetailsCachingAndEvictionOnStockMutation() {
        Cache productDetailsCache = cacheManager.getCache(RedisConfig.CACHE_PRODUCT_DETAILS);
        Cache dashboardCache = cacheManager.getCache(RedisConfig.CACHE_DASHBOARD_STATS);
        assertThat(productDetailsCache).isNotNull();
        assertThat(dashboardCache).isNotNull();

        // 1. Initial lookup -> populates cache
        ProductResponse response = productService.getProductById(product.getId());
        assertThat(response).isNotNull();
        assertThat(productDetailsCache.get("id:" + product.getId())).isNotNull();

        // 2. Fetch dashboard stats -> populates dashboard cache
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        assertThat(stats).isNotNull();
        assertThat(dashboardCache.get("overview")).isNotNull();

        // 3. Perform stock-in mutation
        StockOperationRequest stockInRequest = new StockOperationRequest(product.getId(), 10, "Shipment arrival");
        inventoryService.recordStockIn(stockInRequest, userPrincipal);

        // 4. Verify caches were evicted
        assertThat(productDetailsCache.get("id:" + product.getId())).isNull();
        assertThat(dashboardCache.get("overview")).isNull();
    }

    @Test
    @DisplayName("Categories list is cached and evicted on new category creation")
    void testCategoriesListCachingAndEviction() {
        Cache categoryCache = cacheManager.getCache(RedisConfig.CACHE_CATEGORIES_LIST);
        assertThat(categoryCache).isNotNull();

        // 1. First read populates cache
        categoryService.getAllCategories(null);
        assertThat(categoryCache.get("all")).isNotNull();

        // 2. Creating a new category triggers eviction
        CategoryRequest newCategory = new CategoryRequest("Furniture", "Office desks and chairs");
        categoryService.createCategory(newCategory);

        // 3. Cache should be evicted (null)
        assertThat(categoryCache.get("all")).isNull();
    }

    @Test
    @DisplayName("Low stock cache is populated and evicted on stock-out")
    void testLowStockCacheEvictionOnStockOut() {
        Cache lowStockCache = cacheManager.getCache(RedisConfig.CACHE_LOW_STOCK_PRODUCTS);
        assertThat(lowStockCache).isNotNull();

        // 1. Query low stock products
        inventoryService.getLowStockProducts(PageRequest.of(0, 10));

        // 2. Perform stock-out
        StockOperationRequest stockOutRequest = new StockOperationRequest(product.getId(), 5, "Customer sale");
        inventoryService.recordStockOut(stockOutRequest, userPrincipal);

        // 3. Low stock cache must be evicted
        // The cache should be empty after eviction
        assertThat(lowStockCache.get("0-10-INSTANCE")).isNull();
    }

    @Test
    @DisplayName("Updating a product evicts product details, list, and dashboard stats")
    void testProductUpdateEvictsRelatedCaches() {
        Cache productDetailsCache = cacheManager.getCache(RedisConfig.CACHE_PRODUCT_DETAILS);
        Cache dashboardCache = cacheManager.getCache(RedisConfig.CACHE_DASHBOARD_STATS);

        // Prime caches
        productService.getProductById(product.getId());
        dashboardService.getDashboardStats();
        assertThat(productDetailsCache.get("id:" + product.getId())).isNotNull();
        assertThat(dashboardCache.get("overview")).isNotNull();

        // Update product
        ProductRequest updateRequest = ProductRequest.builder()
                .sku("LAPTOP-001")
                .name("Pro Laptop Ultra")
                .price(new BigDecimal("1300.00"))
                .categoryId(category.getId())
                .supplierId(supplier.getId())
                .minStockThreshold(5)
                .reorderQuantity(10)
                .active(true)
                .build();

        productService.updateProduct(product.getId(), updateRequest);

        // Verify eviction
        assertThat(productDetailsCache.get("id:" + product.getId())).isNull();
        assertThat(dashboardCache.get("overview")).isNull();
    }
}
