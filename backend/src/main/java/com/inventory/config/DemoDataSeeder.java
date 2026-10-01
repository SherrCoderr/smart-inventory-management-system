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
 * Idempotent, Non-Destructive Demo Data Seeder for Portfolio and Production Demonstrations.
 * Activated conditionally when app.demo-data.enabled=true (or DEMO_DATA_ENABLED=true).
 * 
 * Safely inspects existing records before insertion to guarantee that existing user accounts,
 * custom categories, suppliers, stock counts, and transactions are never overwritten or duplicated.
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
        log.info("Starting safe, idempotent demo dataset synchronization...");

        try {
            // 1. Seed / Resolve Users
            Map<String, User> users = seedUsers();

            // 2. Seed / Resolve Categories
            Map<String, Category> categories = seedCategories();

            // 3. Seed / Resolve Suppliers
            Map<String, Supplier> suppliers = seedSuppliers();

            // 4. Seed / Resolve Products
            Map<String, Product> products = seedProducts(categories, suppliers);

            // 5. Seed Historical Inventory Transactions idempotently
            seedTransactions(products, users);

            // 6. Safely Clear Redis/Spring caches
            clearApplicationCachesSafely();

            log.info("Demo data synchronization completed successfully without data corruption or duplicates.");
        } catch (Exception e) {
            log.error("An unexpected error occurred during demo data seeding: {}. Application will continue startup safely.", e.getMessage(), e);
        }
    }

    private Map<String, User> seedUsers() {
        Map<String, User> userMap = new HashMap<>();

        userMap.put("admin", getOrCreateUser(
                "admin_demo",
                "admin@inventory.demo",
                "DemoAdmin@123",
                "Alexander",
                "Wright",
                Role.ADMIN
        ));

        userMap.put("sarah", getOrCreateUser(
                "sarah_inventory",
                "sarah@inventory.demo",
                "DemoStaff@123",
                "Sarah",
                "Jenkins",
                Role.EMPLOYEE
        ));

        userMap.put("marcus", getOrCreateUser(
                "marcus_warehouse",
                "marcus@inventory.demo",
                "DemoStaff@123",
                "Marcus",
                "Vance",
                Role.EMPLOYEE
        ));

        log.info("Users resolved/seeded: {} active user accounts mapped for demo context.", userMap.size());
        return userMap;
    }

    private User getOrCreateUser(String username, String email, String rawPassword, String firstName, String lastName, Role role) {
        // Check if user already exists by username
        Optional<User> byUsername = userRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            log.info("User with username '{}' already exists. Preserving existing record without modifying credentials.", username);
            return byUsername.get();
        }

        // Check if user already exists by email
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            log.info("User with email '{}' already exists. Preserving existing record without modifying credentials.", email);
            return byEmail.get();
        }

        // Neither exists: safely create new demo user
        User newUser = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .active(true)
                .build();

        User saved = userRepository.save(newUser);
        log.info("Created new demo user: username='{}', role='{}'", username, role);
        return saved;
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> catMap = new HashMap<>();

        record CategoryDef(String name, String description) {}

        List<CategoryDef> defs = List.of(
                new CategoryDef("Electronics & Hardware", "Laptops, workstations, and high-performance computing hardware."),
                new CategoryDef("Computer Accessories", "Keyboards, mice, docks, monitors, and ergonomic workstation peripherals."),
                new CategoryDef("Networking & Infrastructure", "Routers, managed switches, access points, and server connectivity hardware."),
                new CategoryDef("Storage & Memory", "Enterprise NVMe SSDs, NAS hard drives, and high-speed DDR5 memory kits."),
                new CategoryDef("Office Furniture", "Ergonomic executive chairs, motorized standing desks, and conference tables."),
                new CategoryDef("Office Printing & Supplies", "Laser multifunction printers, commercial shredders, and workplace equipment.")
        );

        for (CategoryDef def : defs) {
            Category cat = categoryRepository.findByNameIgnoreCase(def.name())
                    .orElseGet(() -> {
                        Category newCat = Category.builder()
                                .name(def.name())
                                .description(def.description())
                                .build();
                        Category saved = categoryRepository.save(newCat);
                        log.info("Created new category: '{}'", saved.getName());
                        return saved;
                    });
            catMap.put(def.name(), cat);
        }

        log.info("Categories resolved/seeded: {} categories available.", catMap.size());
        return catMap;
    }

    private Map<String, Supplier> seedSuppliers() {
        Map<String, Supplier> supMap = new HashMap<>();

        record SupplierDef(String name, String email, String phone, String address) {}

        List<SupplierDef> defs = List.of(
                new SupplierDef("Apex Logistics & Tech Supply", "contact@apextech.demo", "+1 (555) 234-5678", "742 Evergreen Terrace, Suite 400, Austin, TX"),
                new SupplierDef("NexaByte Distribution Global", "sales@nexabyte.demo", "+1 (555) 345-6789", "100 Technology Parkway, San Jose, CA"),
                new SupplierDef("Global Office Solutions", "support@globaloffice.demo", "+1 (555) 456-7890", "250 Enterprise Blvd, Chicago, IL"),
                new SupplierDef("Silicon Harbor Imports Ltd", "orders@siliconharbor.demo", "+1 (555) 567-8901", "1800 Pacific Rim Way, Seattle, WA"),
                new SupplierDef("Quantum Peripherals International", "supply@quantumperipherals.demo", "+1 (555) 678-9012", "50 Innovation Drive, Boston, MA"),
                new SupplierDef("Vantage Network Systems Corp", "procurement@vantagenetworks.demo", "+1 (555) 789-0123", "880 Telecom Way, Atlanta, GA")
        );

        for (SupplierDef def : defs) {
            Supplier sup = supplierRepository.findByEmailIgnoreCase(def.email())
                    .orElseGet(() -> {
                        Supplier newSup = Supplier.builder()
                                .name(def.name())
                                .email(def.email())
                                .phone(def.phone())
                                .address(def.address())
                                .active(true)
                                .build();
                        Supplier saved = supplierRepository.save(newSup);
                        log.info("Created new supplier: '{}' ({})", saved.getName(), saved.getEmail());
                        return saved;
                    });
            supMap.put(def.name(), sup);
        }

        log.info("Suppliers resolved/seeded: {} suppliers available.", supMap.size());
        return supMap;
    }

    private Map<String, Product> seedProducts(Map<String, Category> cats, Map<String, Supplier> sups) {
        Map<String, Product> prodMap = new HashMap<>();

        record ProductDef(String sku, String name, String description, BigDecimal price,
                          int currentStock, int minStockThreshold, int reorderQuantity,
                          String categoryName, String supplierName) {}

        List<ProductDef> defs = List.of(
                // 1. Healthy stock items
                new ProductDef("DELL-LAT-5440", "Dell Latitude 5440 14\" Business Laptop", "Intel Core i7-1365U, 16GB RAM, 512GB NVMe SSD, FHD IPS display.",
                        new BigDecimal("1199.00"), 45, 10, 25, "Electronics & Hardware", "Apex Logistics & Tech Supply"),

                new ProductDef("LOGI-MXM-3S", "Logitech MX Master 3S Wireless Mouse", "8K DPI any-surface tracking, quiet clicks, MagSpeed electromagnetic scrolling.",
                        new BigDecimal("99.99"), 85, 15, 50, "Computer Accessories", "Quantum Peripherals International"),

                new ProductDef("TPL-ARCH-AX55", "TP-Link Archer AX55 WiFi 6 Router", "Dual-Band Gigabit Wi-Fi 6 Router with OFDMA, MU-MIMO, and HomeShield security.",
                        new BigDecimal("129.99"), 35, 8, 20, "Networking & Infrastructure", "Vantage Network Systems Corp"),

                new ProductDef("COR-VENG-DDR5", "Corsair Vengeance 32GB DDR5 6000MHz", "High-performance 32GB (2x16GB) DDR5 desktop memory kit with on-board voltage regulation.",
                        new BigDecimal("114.99"), 60, 12, 30, "Storage & Memory", "NexaByte Distribution Global"),

                new ProductDef("DELL-U2723QE", "Dell UltraSharp 27\" 4K USB-C Monitor", "IPS Black technology, 4K UHD 3840x2160, 98% DCI-P3, integrated 90W USB-C hub.",
                        new BigDecimal("579.99"), 28, 5, 15, "Computer Accessories", "Apex Logistics & Tech Supply"),

                new ProductDef("ANKER-ELT-13", "Anker PowerExpand Elite 13-in-1 Dock", "Thunderbolt 3 dock with 85W host charging, dual 4K/single 5K display support, Gigabit Ethernet.",
                        new BigDecimal("249.99"), 40, 8, 20, "Computer Accessories", "Quantum Peripherals International"),

                new ProductDef("SONY-WH1000XM5", "Sony WH-1000XM5 ANC Wireless Headphones", "Industry-leading noise cancellation with Auto NC Optimizer, 30hr battery life.",
                        new BigDecimal("398.00"), 32, 6, 15, "Computer Accessories", "Quantum Peripherals International"),

                new ProductDef("UBI-U6-PRO", "Ubiquiti UniFi 6 Pro WiFi Access Point", "Dual-band WiFi 6 AP with 5.3 Gbps aggregate throughput rate, PoE+ powered.",
                        new BigDecimal("159.00"), 24, 5, 15, "Networking & Infrastructure", "Vantage Network Systems Corp"),

                // 2. Medium stock items
                new ProductDef("SAMSUNG-990-2TB", "Samsung 990 PRO 2TB PCIe 4.0 NVMe SSD", "Sequential read/write speeds up to 7450/6900 MB/s, thermal control technology.",
                        new BigDecimal("179.99"), 18, 10, 25, "Storage & Memory", "Silicon Harbor Imports Ltd"),

                new ProductDef("KEYCHRON-Q1-PRO", "Keychron Q1 Pro Custom Mechanical Keyboard", "Full metal 75% QMK/VIA wireless mechanical keyboard with double-gasket design.",
                        new BigDecimal("199.00"), 14, 8, 15, "Computer Accessories", "Quantum Peripherals International"),

                new ProductDef("WD-RED-4TB", "Western Digital Red Plus 4TB NAS HDD", "3.5\" CMR SATA 6Gb/s 5400 RPM 128MB cache drive designed for RAID environments.",
                        new BigDecimal("89.99"), 16, 8, 20, "Storage & Memory", "Silicon Harbor Imports Ltd"),

                new ProductDef("STEELCASE-GEST", "Steelcase Gesture Ergonomic Desk Chair", "Synchronized interface between 3D liveback and user spine with 360-degree rotating arms.",
                        new BigDecimal("1399.00"), 8, 4, 10, "Office Furniture", "Global Office Solutions"),

                new ProductDef("FEL-POW-99CI", "Fellowes Powershred 99Ci Cross-Cut Shredder", "100% jam proof commercial cross-cut shredder, 18-sheet capacity, silent operation.",
                        new BigDecimal("279.99"), 11, 5, 10, "Office Printing & Supplies", "Global Office Solutions"),

                new ProductDef("APPLE-MBP-14", "Apple MacBook Pro 14\" M3 Pro Space Black", "11-Core CPU, 14-Core GPU, 18GB Unified Memory, 512GB SSD Storage, Liquid Retina XDR.",
                        new BigDecimal("1999.00"), 12, 5, 10, "Electronics & Hardware", "Silicon Harbor Imports Ltd"),

                // 3. Low stock & Reorder recommendation items (currentStock <= minStockThreshold)
                new ProductDef("HP-LJP-4101", "HP LaserJet Pro MFP 4101fdw Printer", "High-speed wireless monochrome multifunction printer with dual-sided scanning.",
                        new BigDecimal("449.00"), 3, 8, 15, "Office Printing & Supplies", "Global Office Solutions"),

                new ProductDef("CISCO-CAT-1000", "Cisco Catalyst 1000 24-Port Gigabit Switch", "24x 10/100/1000 Ethernet ports, 4x 1G SFP uplinks, enterprise-grade L2 managed switch.",
                        new BigDecimal("689.00"), 2, 6, 10, "Networking & Infrastructure", "Vantage Network Systems Corp"),

                new ProductDef("HM-MOTIA-DESK", "Herman Miller Motia Standing Desk", "Motorized dual-leg adjustable height desk with touch programmable control paddle.",
                        new BigDecimal("1495.00"), 1, 4, 8, "Office Furniture", "Global Office Solutions"),

                new ProductDef("APC-SMART-1500", "APC Smart-UPS 1500VA LCD 120V UPS", "Pure sine wave battery backup with SmartConnect cloud remote monitoring.",
                        new BigDecimal("569.99"), 2, 5, 10, "Electronics & Hardware", "Apex Logistics & Tech Supply"),

                new ProductDef("SEAGATE-IW-8TB", "Seagate IronWolf Pro 8TB Enterprise HDD", "7200 RPM 256MB cache SATA 6Gb/s CMR hard drive built for 24/7 multi-user NAS workloads.",
                        new BigDecimal("199.99"), 4, 10, 25, "Storage & Memory", "Silicon Harbor Imports Ltd"),

                new ProductDef("ERGOHUMAN-CHAIR", "ErgoHuman High-Back Mesh Ergonomic Chair", "Full mesh executive chair with dynamic lumbar support and pneumatic tilt-lock mechanism.",
                        new BigDecimal("749.00"), 0, 6, 12, "Office Furniture", "Global Office Solutions")
        );

        for (ProductDef def : defs) {
            Product prod = productRepository.findBySkuIgnoreCase(def.sku())
                    .orElseGet(() -> {
                        Category category = cats.get(def.categoryName());
                        Supplier supplier = sups.get(def.supplierName());

                        if (category == null || supplier == null) {
                            log.warn("Cannot create product '{}' (SKU: {}): Category or Supplier missing.", def.name(), def.sku());
                            return null;
                        }

                        Product newProd = Product.builder()
                                .sku(def.sku())
                                .name(def.name())
                                .description(def.description())
                                .price(def.price())
                                .currentStock(def.currentStock())
                                .minStockThreshold(def.minStockThreshold())
                                .reorderQuantity(def.reorderQuantity())
                                .category(category)
                                .supplier(supplier)
                                .active(true)
                                .build();

                        Product saved = productRepository.save(newProd);
                        log.info("Created new demo product: SKU='{}', Stock={}", saved.getSku(), saved.getCurrentStock());
                        return saved;
                    });

            if (prod != null) {
                prodMap.put(prod.getSku(), prod);
            }
        }

        log.info("Products resolved/seeded: {} products mapped in total.", prodMap.size());
        return prodMap;
    }

    private void seedTransactions(Map<String, Product> products, Map<String, User> users) {
        User admin = users.get("admin");
        User sarah = users.get("sarah");
        User marcus = users.get("marcus");

        LocalDateTime now = LocalDateTime.now();

        record TxDef(String sku, User user, TransactionType type, int quantity, int prevStock, int newStock, String reason, LocalDateTime timestamp) {}

        List<TxDef> defs = List.of(
                // 13 Days ago
                new TxDef("DELL-LAT-5440", admin, TransactionType.STOCK_IN, 50, 0, 50, "Initial Q3 supplier restock PO #9401", now.minusDays(13).minusHours(2)),
                new TxDef("LOGI-MXM-3S", marcus, TransactionType.STOCK_IN, 100, 0, 100, "Bulk accessories warehouse intake PO #9402", now.minusDays(13).minusHours(5)),

                // 12 Days ago
                new TxDef("TPL-ARCH-AX55", sarah, TransactionType.STOCK_IN, 40, 0, 40, "Network hardware delivery PO #9405", now.minusDays(12).minusHours(1)),
                new TxDef("COR-VENG-DDR5", marcus, TransactionType.STOCK_IN, 70, 0, 70, "Memory kit bulk pallet intake PO #9408", now.minusDays(12).minusHours(4)),

                // 11 Days ago
                new TxDef("DELL-LAT-5440", sarah, TransactionType.STOCK_OUT, 3, 50, 47, "Sales department laptop provisioning", now.minusDays(11).minusHours(2)),
                new TxDef("LOGI-MXM-3S", marcus, TransactionType.STOCK_OUT, 5, 100, 95, "Engineering team workstation setup", now.minusDays(11).minusHours(6)),

                // 10 Days ago
                new TxDef("DELL-U2723QE", admin, TransactionType.STOCK_IN, 30, 0, 30, "Monitor consignment intake PO #9412", now.minusDays(10).minusHours(3)),
                new TxDef("ANKER-ELT-13", sarah, TransactionType.STOCK_IN, 45, 0, 45, "Thunderbolt dock delivery PO #9415", now.minusDays(10).minusHours(7)),

                // 9 Days ago
                new TxDef("SAMSUNG-990-2TB", marcus, TransactionType.STOCK_IN, 25, 0, 25, "Fast NVMe storage order PO #9420", now.minusDays(9).minusHours(2)),
                new TxDef("COR-VENG-DDR5", sarah, TransactionType.STOCK_OUT, 10, 70, 60, "Server memory upgrade dispatch", now.minusDays(9).minusHours(5)),

                // 8 Days ago
                new TxDef("SONY-WH1000XM5", admin, TransactionType.STOCK_IN, 35, 0, 35, "Executive audio peripherals PO #9425", now.minusDays(8).minusHours(1)),
                new TxDef("DELL-LAT-5440", marcus, TransactionType.STOCK_OUT, 2, 47, 45, "New employee onboarding equipment", now.minusDays(8).minusHours(4)),

                // 7 Days ago
                new TxDef("UBI-U6-PRO", sarah, TransactionType.STOCK_IN, 30, 0, 30, "Office WiFi 6 upgrade hardware PO #9430", now.minusDays(7).minusHours(2)),
                new TxDef("UBI-U6-PRO", marcus, TransactionType.STOCK_OUT, 6, 30, 24, "Floor 3 WiFi installation deployment", now.minusDays(7).minusHours(6)),

                // 6 Days ago
                new TxDef("KEYCHRON-Q1-PRO", admin, TransactionType.STOCK_IN, 20, 0, 20, "Mechanical keyboard intake PO #9435", now.minusDays(6).minusHours(3)),
                new TxDef("KEYCHRON-Q1-PRO", sarah, TransactionType.STOCK_OUT, 6, 20, 14, "Design team hardware distribution", now.minusDays(6).minusHours(7)),

                // 5 Days ago
                new TxDef("WD-RED-4TB", marcus, TransactionType.STOCK_IN, 20, 0, 20, "NAS storage supply batch PO #9440", now.minusDays(5).minusHours(2)),
                new TxDef("WD-RED-4TB", sarah, TransactionType.STOCK_OUT, 4, 20, 16, "Data center backup array expansion", now.minusDays(5).minusHours(5)),

                // 4 Days ago
                new TxDef("STEELCASE-GEST", admin, TransactionType.STOCK_IN, 10, 0, 10, "Executive furniture shipment PO #9445", now.minusDays(4).minusHours(1)),
                new TxDef("STEELCASE-GEST", marcus, TransactionType.STOCK_OUT, 2, 10, 8, "C-Suite office relocation dispatch", now.minusDays(4).minusHours(4)),

                // 3 Days ago
                new TxDef("FEL-POW-99CI", sarah, TransactionType.STOCK_IN, 15, 0, 15, "Facility shredders order PO #9450", now.minusDays(3).minusHours(2)),
                new TxDef("FEL-POW-99CI", marcus, TransactionType.STOCK_OUT, 4, 15, 11, "Accounting & Legal compliance deployment", now.minusDays(3).minusHours(6)),

                // 2 Days ago
                new TxDef("APPLE-MBP-14", admin, TransactionType.STOCK_IN, 15, 0, 15, "MacBook Pro hardware order PO #9455", now.minusDays(2).minusHours(1)),
                new TxDef("APPLE-MBP-14", sarah, TransactionType.STOCK_OUT, 3, 15, 12, "Senior Tech Lead equipment assignment", now.minusDays(2).minusHours(5)),
                new TxDef("LOGI-MXM-3S", marcus, TransactionType.STOCK_OUT, 10, 95, 85, "Remote workforce ergonomic distribution", now.minusDays(2).minusHours(8)),

                // 1 Day ago
                new TxDef("HP-LJP-4101", sarah, TransactionType.STOCK_OUT, 5, 8, 3, "Branch office replacement printer dispatch", now.minusDays(1).minusHours(3)),
                new TxDef("CISCO-CAT-1000", marcus, TransactionType.STOCK_OUT, 4, 6, 2, "Server rack switch replacement emergency dispatch", now.minusDays(1).minusHours(6)),
                new TxDef("APC-SMART-1500", admin, TransactionType.STOCK_OUT, 3, 5, 2, "Server room UPS installation", now.minusDays(1).minusHours(9)),

                // Today
                new TxDef("HM-MOTIA-DESK", sarah, TransactionType.STOCK_OUT, 3, 4, 1, "Executive suite desk fulfillment", now.minusHours(4)),
                new TxDef("SEAGATE-IW-8TB", marcus, TransactionType.STOCK_OUT, 6, 10, 4, "Enterprise storage array replacement", now.minusHours(2))
        );

        List<InventoryTransaction> toSave = new ArrayList<>();

        for (TxDef def : defs) {
            Product product = products.get(def.sku());
            User user = def.user();

            if (product == null || user == null) {
                log.warn("Skipping demo transaction for SKU '{}': product or user not available.", def.sku());
                continue;
            }

            // Check if this specific demo transaction already exists by referenceReason and product
            if (transactionRepository.existsByProductAndReferenceReason(product, def.reason())) {
                log.debug("Transaction for SKU '{}' with reason '{}' already exists. Skipping duplicate insert.", def.sku(), def.reason());
                continue;
            }

            InventoryTransaction tx = InventoryTransaction.builder()
                    .product(product)
                    .user(user)
                    .transactionType(def.type())
                    .quantity(def.quantity())
                    .previousStock(def.prevStock())
                    .newStock(def.newStock())
                    .referenceReason(def.reason())
                    .createdAt(def.timestamp())
                    .build();

            toSave.add(tx);
        }

        if (!toSave.isEmpty()) {
            transactionRepository.saveAll(toSave);
            log.info("Inserted {} new demo transactions.", toSave.size());
        } else {
            log.info("All demo transactions are already present. No new records inserted.");
        }
    }

    private void clearApplicationCachesSafely() {
        try {
            cacheManager.ifPresent(cm -> {
                for (String cacheName : cm.getCacheNames()) {
                    try {
                        Cache cache = cm.getCache(cacheName);
                        if (cache != null) {
                            cache.clear();
                        }
                    } catch (Exception e) {
                        log.warn("Could not clear cache '{}' during demo data initialization: {}", cacheName, e.getMessage());
                    }
                }
                log.info("Successfully cleared application cache regions following demo data seeding.");
            });
        } catch (Exception e) {
            log.warn("Cache eviction encountered an issue during startup: {}. Continuing startup safely.", e.getMessage());
        }
    }
}
