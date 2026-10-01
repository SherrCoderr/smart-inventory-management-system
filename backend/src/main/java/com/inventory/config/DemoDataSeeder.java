package com.inventory.config;

import com.inventory.entity.*;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Idempotent Demo Data Seeder for Portfolio and Production Demonstrations.
 * Activated conditionally when app.demo-data.enabled=true (or DEMO_DATA_ENABLED=true).
 * Safely inserts realistic enterprise inventory data without modifying existing records or resetting IDs.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Optional<CacheManager> cacheManager;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking demo data initialization status...");

        // Idempotency check: if demo admin or products already exist, skip execution
        if (userRepository.existsByUsername("admin_demo") || productRepository.existsBySkuIgnoreCase("DELL-LAT-5440")) {
            log.info("Demo dataset is already present in database. Skipping seeding to preserve idempotency.");
            return;
        }

        log.info("Starting demo dataset seeding (Users, Categories, Suppliers, Products, and Transactions)...");

        // 1. Seed Users
        Map<String, User> users = seedUsers();

        // 2. Seed Categories
        Map<String, Category> categories = seedCategories();

        // 3. Seed Suppliers
        Map<String, Supplier> suppliers = seedSuppliers();

        // 4. Seed Products
        Map<String, Product> products = seedProducts(categories, suppliers);

        // 5. Seed Historical Inventory Transactions
        seedTransactions(products, users);

        // 6. Clear Redis/Spring caches so fresh seed data is immediately visible
        cacheManager.ifPresent(cm -> {
            for (String cacheName : cm.getCacheNames()) {
                Cache cache = cm.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            }
            log.info("Cleared all application cache regions following demo data seeding.");
        });

        log.info("Successfully seeded realistic demo inventory dataset with 3 users, 6 categories, 6 suppliers, 20 products, and 30 audit transactions.");
    }

    private Map<String, User> seedUsers() {
        Map<String, User> userMap = new HashMap<>();

        // Admin User
        User admin = User.builder()
                .username("admin_demo")
                .email("admin@inventory.demo")
                .password(passwordEncoder.encode("DemoAdmin@123"))
                .firstName("Alexander")
                .lastName("Wright")
                .role(Role.ADMIN)
                .active(true)
                .build();
        userMap.put("admin", userRepository.save(admin));

        // Employee 1
        User sarah = User.builder()
                .username("sarah_inventory")
                .email("sarah@inventory.demo")
                .password(passwordEncoder.encode("DemoStaff@123"))
                .firstName("Sarah")
                .lastName("Jenkins")
                .role(Role.EMPLOYEE)
                .active(true)
                .build();
        userMap.put("sarah", userRepository.save(sarah));

        // Employee 2
        User marcus = User.builder()
                .username("marcus_warehouse")
                .email("marcus@inventory.demo")
                .password(passwordEncoder.encode("DemoStaff@123"))
                .firstName("Marcus")
                .lastName("Vance")
                .role(Role.EMPLOYEE)
                .active(true)
                .build();
        userMap.put("marcus", userRepository.save(marcus));

        log.info("Seeded 3 demo users (1 ADMIN, 2 EMPLOYEES).");
        return userMap;
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> catMap = new HashMap<>();

        List<Category> categories = List.of(
                Category.builder().name("Electronics & Hardware").description("Laptops, workstations, and high-performance computing hardware.").build(),
                Category.builder().name("Computer Accessories").description("Keyboards, mice, docks, monitors, and ergonomic workstation peripherals.").build(),
                Category.builder().name("Networking & Infrastructure").description("Routers, managed switches, access points, and server connectivity hardware.").build(),
                Category.builder().name("Storage & Memory").description("Enterprise NVMe SSDs, NAS hard drives, and high-speed DDR5 memory kits.").build(),
                Category.builder().name("Office Furniture").description("Ergonomic executive chairs, motorized standing desks, and conference tables.").build(),
                Category.builder().name("Office Printing & Supplies").description("Laser multifunction printers, commercial shredders, and workplace equipment.").build()
        );

        for (Category cat : categories) {
            Category saved = categoryRepository.save(cat);
            catMap.put(saved.getName(), saved);
        }

        log.info("Seeded 6 product categories.");
        return catMap;
    }

    private Map<String, Supplier> seedSuppliers() {
        Map<String, Supplier> supMap = new HashMap<>();

        List<Supplier> suppliers = List.of(
                Supplier.builder().name("Apex Logistics & Tech Supply").email("contact@apextech.demo").phone("+1 (555) 234-5678").address("742 Evergreen Terrace, Suite 400, Austin, TX").active(true).build(),
                Supplier.builder().name("NexaByte Distribution Global").email("sales@nexabyte.demo").phone("+1 (555) 345-6789").address("100 Technology Parkway, San Jose, CA").active(true).build(),
                Supplier.builder().name("Global Office Solutions").email("support@globaloffice.demo").phone("+1 (555) 456-7890").address("250 Enterprise Blvd, Chicago, IL").active(true).build(),
                Supplier.builder().name("Silicon Harbor Imports Ltd").email("orders@siliconharbor.demo").phone("+1 (555) 567-8901").address("1800 Pacific Rim Way, Seattle, WA").active(true).build(),
                Supplier.builder().name("Quantum Peripherals International").email("supply@quantumperipherals.demo").phone("+1 (555) 678-9012").address("50 Innovation Drive, Boston, MA").active(true).build(),
                Supplier.builder().name("Vantage Network Systems Corp").email("procurement@vantagenetworks.demo").phone("+1 (555) 789-0123").address("880 Telecom Way, Atlanta, GA").active(true).build()
        );

        for (Supplier sup : suppliers) {
            Supplier saved = supplierRepository.save(sup);
            supMap.put(saved.getName(), saved);
        }

        log.info("Seeded 6 enterprise suppliers.");
        return supMap;
    }

    private Map<String, Product> seedProducts(Map<String, Category> cats, Map<String, Supplier> sups) {
        Map<String, Product> prodMap = new HashMap<>();

        Category hardware = cats.get("Electronics & Hardware");
        Category accessories = cats.get("Computer Accessories");
        Category networking = cats.get("Networking & Infrastructure");
        Category storage = cats.get("Storage & Memory");
        Category furniture = cats.get("Office Furniture");
        Category printing = cats.get("Office Printing & Supplies");

        Supplier apex = sups.get("Apex Logistics & Tech Supply");
        Supplier nexabyte = sups.get("NexaByte Distribution Global");
        Supplier globalOffice = sups.get("Global Office Solutions");
        Supplier siliconHarbor = sups.get("Silicon Harbor Imports Ltd");
        Supplier quantum = sups.get("Quantum Peripherals International");
        Supplier vantage = sups.get("Vantage Network Systems Corp");

        List<Product> products = List.of(
                // 1. Healthy stock items
                Product.builder().sku("DELL-LAT-5440").name("Dell Latitude 5440 14\" Business Laptop").description("Intel Core i7-1365U, 16GB RAM, 512GB NVMe SSD, FHD IPS display.")
                        .price(new BigDecimal("1199.00")).currentStock(45).minStockThreshold(10).reorderQuantity(25).category(hardware).supplier(apex).active(true).build(),

                Product.builder().sku("LOGI-MXM-3S").name("Logitech MX Master 3S Wireless Mouse").description("8K DPI any-surface tracking, quiet clicks, MagSpeed electromagnetic scrolling.")
                        .price(new BigDecimal("99.99")).currentStock(85).minStockThreshold(15).reorderQuantity(50).category(accessories).supplier(quantum).active(true).build(),

                Product.builder().sku("TPL-ARCH-AX55").name("TP-Link Archer AX55 WiFi 6 Router").description("Dual-Band Gigabit Wi-Fi 6 Router with OFDMA, MU-MIMO, and HomeShield security.")
                        .price(new BigDecimal("129.99")).currentStock(35).minStockThreshold(8).reorderQuantity(20).category(networking).supplier(vantage).active(true).build(),

                Product.builder().sku("COR-VENG-DDR5").name("Corsair Vengeance 32GB DDR5 6000MHz").description("High-performance 32GB (2x16GB) DDR5 desktop memory kit with on-board voltage regulation.")
                        .price(new BigDecimal("114.99")).currentStock(60).minStockThreshold(12).reorderQuantity(30).category(storage).supplier(nexabyte).active(true).build(),

                Product.builder().sku("DELL-U2723QE").name("Dell UltraSharp 27\" 4K USB-C Monitor").description("IPS Black technology, 4K UHD 3840x2160, 98% DCI-P3, integrated 90W USB-C hub.")
                        .price(new BigDecimal("579.99")).currentStock(28).minStockThreshold(5).reorderQuantity(15).category(accessories).supplier(apex).active(true).build(),

                Product.builder().sku("ANKER-ELT-13").name("Anker PowerExpand Elite 13-in-1 Dock").description("Thunderbolt 3 dock with 85W host charging, dual 4K/single 5K display support, Gigabit Ethernet.")
                        .price(new BigDecimal("249.99")).currentStock(40).minStockThreshold(8).reorderQuantity(20).category(accessories).supplier(quantum).active(true).build(),

                Product.builder().sku("SONY-WH1000XM5").name("Sony WH-1000XM5 ANC Wireless Headphones").description("Industry-leading noise cancellation with Auto NC Optimizer, 30hr battery life.")
                        .price(new BigDecimal("398.00")).currentStock(32).minStockThreshold(6).reorderQuantity(15).category(accessories).supplier(quantum).active(true).build(),

                Product.builder().sku("UBI-U6-PRO").name("Ubiquiti UniFi 6 Pro WiFi Access Point").description("Dual-band WiFi 6 AP with 5.3 Gbps aggregate throughput rate, PoE+ powered.")
                        .price(new BigDecimal("159.00")).currentStock(24).minStockThreshold(5).reorderQuantity(15).category(networking).supplier(vantage).active(true).build(),

                // 2. Medium stock items
                Product.builder().sku("SAMSUNG-990-2TB").name("Samsung 990 PRO 2TB PCIe 4.0 NVMe SSD").description("Sequential read/write speeds up to 7450/6900 MB/s, thermal control technology.")
                        .price(new BigDecimal("179.99")).currentStock(18).minStockThreshold(10).reorderQuantity(25).category(storage).supplier(siliconHarbor).active(true).build(),

                Product.builder().sku("KEYCHRON-Q1-PRO").name("Keychron Q1 Pro Custom Mechanical Keyboard").description("Full metal 75% QMK/VIA wireless mechanical keyboard with double-gasket design.")
                        .price(new BigDecimal("199.00")).currentStock(14).minStockThreshold(8).reorderQuantity(15).category(accessories).supplier(quantum).active(true).build(),

                Product.builder().sku("WD-RED-4TB").name("Western Digital Red Plus 4TB NAS HDD").description("3.5\" CMR SATA 6Gb/s 5400 RPM 128MB cache drive designed for RAID environments.")
                        .price(new BigDecimal("89.99")).currentStock(16).minStockThreshold(8).reorderQuantity(20).category(storage).supplier(siliconHarbor).active(true).build(),

                Product.builder().sku("STEELCASE-GEST").name("Steelcase Gesture Ergonomic Desk Chair").description("Synchronized interface between 3D liveback and user spine with 360-degree rotating arms.")
                        .price(new BigDecimal("1399.00")).currentStock(8).minStockThreshold(4).reorderQuantity(10).category(furniture).supplier(globalOffice).active(true).build(),

                Product.builder().sku("FEL-POW-99CI").name("Fellowes Powershred 99Ci Cross-Cut Shredder").description("100% jam proof commercial cross-cut shredder, 18-sheet capacity, silent operation.")
                        .price(new BigDecimal("279.99")).currentStock(11).minStockThreshold(5).reorderQuantity(10).category(printing).supplier(globalOffice).active(true).build(),

                Product.builder().sku("APPLE-MBP-14").name("Apple MacBook Pro 14\" M3 Pro Space Black").description("11-Core CPU, 14-Core GPU, 18GB Unified Memory, 512GB SSD Storage, Liquid Retina XDR.")
                        .price(new BigDecimal("1999.00")).currentStock(12).minStockThreshold(5).reorderQuantity(10).category(hardware).supplier(siliconHarbor).active(true).build(),

                // 3. Low stock & Reorder recommendation items (currentStock <= minStockThreshold)
                Product.builder().sku("HP-LJP-4101").name("HP LaserJet Pro MFP 4101fdw Printer").description("High-speed wireless monochrome multifunction printer with dual-sided scanning.")
                        .price(new BigDecimal("449.00")).currentStock(3).minStockThreshold(8).reorderQuantity(15).category(printing).supplier(globalOffice).active(true).build(),

                Product.builder().sku("CISCO-CAT-1000").name("Cisco Catalyst 1000 24-Port Gigabit Switch").description("24x 10/100/1000 Ethernet ports, 4x 1G SFP uplinks, enterprise-grade L2 managed switch.")
                        .price(new BigDecimal("689.00")).currentStock(2).minStockThreshold(6).reorderQuantity(10).category(networking).supplier(vantage).active(true).build(),

                Product.builder().sku("HM-MOTIA-DESK").name("Herman Miller Motia Standing Desk").description("Motorized dual-leg adjustable height desk with touch programmable control paddle.")
                        .price(new BigDecimal("1495.00")).currentStock(1).minStockThreshold(4).reorderQuantity(8).category(furniture).supplier(globalOffice).active(true).build(),

                Product.builder().sku("APC-SMART-1500").name("APC Smart-UPS 1500VA LCD 120V UPS").description("Pure sine wave battery backup with SmartConnect cloud remote monitoring.")
                        .price(new BigDecimal("569.99")).currentStock(2).minStockThreshold(5).reorderQuantity(10).category(hardware).supplier(apex).active(true).build(),

                Product.builder().sku("SEAGATE-IW-8TB").name("Seagate IronWolf Pro 8TB Enterprise HDD").description("7200 RPM 256MB cache SATA 6Gb/s CMR hard drive built for 24/7 multi-user NAS workloads.")
                        .price(new BigDecimal("199.99")).currentStock(4).minStockThreshold(10).reorderQuantity(25).category(storage).supplier(siliconHarbor).active(true).build(),

                Product.builder().sku("ERGOHUMAN-CHAIR").name("ErgoHuman High-Back Mesh Ergonomic Chair").description("Full mesh executive chair with dynamic lumbar support and pneumatic tilt-lock mechanism.")
                        .price(new BigDecimal("749.00")).currentStock(0).minStockThreshold(6).reorderQuantity(12).category(furniture).supplier(globalOffice).active(true).build()
        );

        for (Product p : products) {
            Product saved = productRepository.save(p);
            prodMap.put(saved.getSku(), saved);
        }

        log.info("Seeded 20 realistic products (14 healthy/medium stock, 6 low-stock/reorder candidates).");
        return prodMap;
    }

    private void seedTransactions(Map<String, Product> products, Map<String, User> users) {
        User admin = users.get("admin");
        User sarah = users.get("sarah");
        User marcus = users.get("marcus");

        LocalDateTime now = LocalDateTime.now();

        List<InventoryTransaction> transactions = new ArrayList<>();

        // Helper to add transaction
        // 13 Days ago
        transactions.add(createTx(products.get("DELL-LAT-5440"), admin, TransactionType.STOCK_IN, 50, 0, 50, "Initial Q3 supplier restock PO #9401", now.minusDays(13).minusHours(2)));
        transactions.add(createTx(products.get("LOGI-MXM-3S"), marcus, TransactionType.STOCK_IN, 100, 0, 100, "Bulk accessories warehouse intake PO #9402", now.minusDays(13).minusHours(5)));

        // 12 Days ago
        transactions.add(createTx(products.get("TPL-ARCH-AX55"), sarah, TransactionType.STOCK_IN, 40, 0, 40, "Network hardware delivery PO #9405", now.minusDays(12).minusHours(1)));
        transactions.add(createTx(products.get("COR-VENG-DDR5"), marcus, TransactionType.STOCK_IN, 70, 0, 70, "Memory kit bulk pallet intake PO #9408", now.minusDays(12).minusHours(4)));

        // 11 Days ago
        transactions.add(createTx(products.get("DELL-LAT-5440"), sarah, TransactionType.STOCK_OUT, 3, 50, 47, "Sales department laptop provisioning", now.minusDays(11).minusHours(2)));
        transactions.add(createTx(products.get("LOGI-MXM-3S"), marcus, TransactionType.STOCK_OUT, 5, 100, 95, "Engineering team workstation setup", now.minusDays(11).minusHours(6)));

        // 10 Days ago
        transactions.add(createTx(products.get("DELL-U2723QE"), admin, TransactionType.STOCK_IN, 30, 0, 30, "Monitor consignment intake PO #9412", now.minusDays(10).minusHours(3)));
        transactions.add(createTx(products.get("ANKER-ELT-13"), sarah, TransactionType.STOCK_IN, 45, 0, 45, "Thunderbolt dock delivery PO #9415", now.minusDays(10).minusHours(7)));

        // 9 Days ago
        transactions.add(createTx(products.get("SAMSUNG-990-2TB"), marcus, TransactionType.STOCK_IN, 25, 0, 25, "Fast NVMe storage order PO #9420", now.minusDays(9).minusHours(2)));
        transactions.add(createTx(products.get("COR-VENG-DDR5"), sarah, TransactionType.STOCK_OUT, 10, 70, 60, "Server memory upgrade dispatch", now.minusDays(9).minusHours(5)));

        // 8 Days ago
        transactions.add(createTx(products.get("SONY-WH1000XM5"), admin, TransactionType.STOCK_IN, 35, 0, 35, "Executive audio peripherals PO #9425", now.minusDays(8).minusHours(1)));
        transactions.add(createTx(products.get("DELL-LAT-5440"), marcus, TransactionType.STOCK_OUT, 2, 47, 45, "New employee onboarding equipment", now.minusDays(8).minusHours(4)));

        // 7 Days ago
        transactions.add(createTx(products.get("UBI-U6-PRO"), sarah, TransactionType.STOCK_IN, 30, 0, 30, "Office WiFi 6 upgrade hardware PO #9430", now.minusDays(7).minusHours(2)));
        transactions.add(createTx(products.get("UBI-U6-PRO"), marcus, TransactionType.STOCK_OUT, 6, 30, 24, "Floor 3 WiFi installation deployment", now.minusDays(7).minusHours(6)));

        // 6 Days ago
        transactions.add(createTx(products.get("KEYCHRON-Q1-PRO"), admin, TransactionType.STOCK_IN, 20, 0, 20, "Mechanical keyboard intake PO #9435", now.minusDays(6).minusHours(3)));
        transactions.add(createTx(products.get("KEYCHRON-Q1-PRO"), sarah, TransactionType.STOCK_OUT, 6, 20, 14, "Design team hardware distribution", now.minusDays(6).minusHours(7)));

        // 5 Days ago
        transactions.add(createTx(products.get("WD-RED-4TB"), marcus, TransactionType.STOCK_IN, 20, 0, 20, "NAS storage supply batch PO #9440", now.minusDays(5).minusHours(2)));
        transactions.add(createTx(products.get("WD-RED-4TB"), sarah, TransactionType.STOCK_OUT, 4, 20, 16, "Data center backup array expansion", now.minusDays(5).minusHours(5)));

        // 4 Days ago
        transactions.add(createTx(products.get("STEELCASE-GEST"), admin, TransactionType.STOCK_IN, 10, 0, 10, "Executive furniture shipment PO #9445", now.minusDays(4).minusHours(1)));
        transactions.add(createTx(products.get("STEELCASE-GEST"), marcus, TransactionType.STOCK_OUT, 2, 10, 8, "C-Suite office relocation dispatch", now.minusDays(4).minusHours(4)));

        // 3 Days ago
        transactions.add(createTx(products.get("FEL-POW-99CI"), sarah, TransactionType.STOCK_IN, 15, 0, 15, "Facility shredders order PO #9450", now.minusDays(3).minusHours(2)));
        transactions.add(createTx(products.get("FEL-POW-99CI"), marcus, TransactionType.STOCK_OUT, 4, 15, 11, "Accounting & Legal compliance deployment", now.minusDays(3).minusHours(6)));

        // 2 Days ago
        transactions.add(createTx(products.get("APPLE-MBP-14"), admin, TransactionType.STOCK_IN, 15, 0, 15, "MacBook Pro hardware order PO #9455", now.minusDays(2).minusHours(1)));
        transactions.add(createTx(products.get("APPLE-MBP-14"), sarah, TransactionType.STOCK_OUT, 3, 15, 12, "Senior Tech Lead equipment assignment", now.minusDays(2).minusHours(5)));
        transactions.add(createTx(products.get("LOGI-MXM-3S"), marcus, TransactionType.STOCK_OUT, 10, 95, 85, "Remote workforce ergonomic distribution", now.minusDays(2).minusHours(8)));

        // 1 Day ago
        transactions.add(createTx(products.get("HP-LJP-4101"), sarah, TransactionType.STOCK_OUT, 5, 8, 3, "Branch office replacement printer dispatch", now.minusDays(1).minusHours(3)));
        transactions.add(createTx(products.get("CISCO-CAT-1000"), marcus, TransactionType.STOCK_OUT, 4, 6, 2, "Server rack switch replacement emergency dispatch", now.minusDays(1).minusHours(6)));
        transactions.add(createTx(products.get("APC-SMART-1500"), admin, TransactionType.STOCK_OUT, 3, 5, 2, "Server room UPS installation", now.minusDays(1).minusHours(9)));

        // Today
        transactions.add(createTx(products.get("HM-MOTIA-DESK"), sarah, TransactionType.STOCK_OUT, 3, 4, 1, "Executive suite desk fulfillment", now.minusHours(4)));
        transactions.add(createTx(products.get("SEAGATE-IW-8TB"), marcus, TransactionType.STOCK_OUT, 6, 10, 4, "Enterprise storage array replacement", now.minusHours(2)));

        transactionRepository.saveAll(transactions);
        log.info("Seeded {} audit inventory transactions spanning the previous 14 days.", transactions.size());
    }

    private InventoryTransaction createTx(Product product, User user, TransactionType type,
                                          int quantity, int previousStock, int newStock,
                                          String reason, LocalDateTime timestamp) {
        return InventoryTransaction.builder()
                .product(product)
                .user(user)
                .transactionType(type)
                .quantity(quantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .referenceReason(reason)
                .createdAt(timestamp)
                .build();
    }
}
