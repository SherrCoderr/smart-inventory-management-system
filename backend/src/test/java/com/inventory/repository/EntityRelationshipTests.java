package com.inventory.repository;

import com.inventory.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EntityRelationshipTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Test
    @DisplayName("Verify User, Category, Supplier, Product, and Transaction entities and relationships")
    void testEntitiesAndRelationships() {
        // 1. Create User
        User user = User.builder()
                .username("admin_user")
                .email("admin@inventory.com")
                .password("$2a$10$encodedpasswordhashhere")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .active(true)
                .build();
        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();

        // 2. Create Category
        Category category = Category.builder()
                .name("Electronics")
                .description("Electronic gadgets and accessories")
                .build();
        Category savedCategory = categoryRepository.save(category);
        assertThat(savedCategory.getId()).isNotNull();

        // 3. Create Supplier
        Supplier supplier = Supplier.builder()
                .name("Global Tech Supplies")
                .email("contact@globaltech.com")
                .phone("+1-555-0199")
                .address("100 Industrial Parkway, Tech City")
                .active(true)
                .build();
        Supplier savedSupplier = supplierRepository.save(supplier);
        assertThat(savedSupplier.getId()).isNotNull();

        // 4. Create Product with Category & Supplier references
        Product product = Product.builder()
                .sku("TECH-WIDGET-001")
                .name("Wireless Noise Cancelling Headphones")
                .description("Premium over-ear Bluetooth headphones")
                .price(new BigDecimal("149.99"))
                .currentStock(50)
                .minStockThreshold(10)
                .reorderQuantity(25)
                .category(savedCategory)
                .supplier(savedSupplier)
                .active(true)
                .build();
        Product savedProduct = productRepository.save(product);
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getVersion()).isNotNull();

        // 5. Create Inventory Transaction linked to Product and User
        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(savedProduct)
                .transactionType(TransactionType.STOCK_IN)
                .quantity(50)
                .previousStock(0)
                .newStock(50)
                .referenceReason("Initial Stock Intake - PO #1001")
                .user(savedUser)
                .build();
        InventoryTransaction savedTxn = transactionRepository.save(transaction);
        assertThat(savedTxn.getId()).isNotNull();
        assertThat(savedTxn.getProduct().getId()).isEqualTo(savedProduct.getId());
        assertThat(savedTxn.getUser().getId()).isEqualTo(savedUser.getId());

        // 6. Test Query Methods
        Optional<Product> foundProduct = productRepository.findBySku("TECH-WIDGET-001");
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("Wireless Noise Cancelling Headphones");

        BigDecimal totalValue = productRepository.calculateTotalInventoryValue();
        // 50 * 149.99 = 7499.50
        assertThat(totalValue).isEqualByComparingTo(new BigDecimal("7499.50"));

        List<InventoryTransaction> txns = transactionRepository.findTop10ByOrderByCreatedAtDesc();
        assertThat(txns).hasSize(1);
        assertThat(txns.get(0).getTransactionType()).isEqualTo(TransactionType.STOCK_IN);
    }
}
