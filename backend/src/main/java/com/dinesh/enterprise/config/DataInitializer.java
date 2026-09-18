package com.dinesh.enterprise.config;

import com.dinesh.enterprise.entity.Role;
import com.dinesh.enterprise.entity.User;
import com.dinesh.enterprise.enums.UserStatus;
import com.dinesh.enterprise.repository.RoleRepository;
import com.dinesh.enterprise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Application data initializer — runs on startup.
 *
 * <p>Seeds:</p>
 * <ul>
 *   <li>Default roles: ADMIN, CUSTOMER, WAREHOUSE_MANAGER, SUPPORT_AGENT</li>
 *   <li>Default admin user: admin@enterprise.com / Admin@1234</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final List<String> DEFAULT_ROLES = Arrays.asList(
            "ADMIN", "CUSTOMER", "WAREHOUSE_MANAGER", "SUPPORT_AGENT"
    );

    private static final String ADMIN_EMAIL = "admin@enterprise.com";
    private static final String ADMIN_PASSWORD = "Admin@1234";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.dinesh.enterprise.repository.CategoryRepository categoryRepository;
    private final com.dinesh.enterprise.repository.ProductRepository productRepository;
    private final com.dinesh.enterprise.repository.WarehouseRepository warehouseRepository;
    private final com.dinesh.enterprise.repository.InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        seedAdminUser();
        seedCatalogAndInventory();
    }

    /**
     * Seeds all default roles if they do not already exist.
     */
    private void seedRoles() {
        DEFAULT_ROLES.forEach(roleName -> {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder().name(roleName).build();
                roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            }
        });
    }

    /**
     * Seeds the default ADMIN user if no admin user exists yet.
     */
    private void seedAdminUser() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.debug("Admin user already exists — skipping seed.");
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found after seeding — check DataInitializer"));

        User admin = User.builder()
                .firstName("System")
                .lastName("Admin")
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();

        userRepository.save(admin);
        log.info("Seeded default ADMIN user: {}", ADMIN_EMAIL);
    }

    /**
     * Seeds initial categories, products, warehouses, and inventory stock if table is empty.
     */
    private void seedCatalogAndInventory() {
        if (categoryRepository.count() > 0) {
            return;
        }

        // 1. Categories
        com.dinesh.enterprise.entity.Category electronics = categoryRepository.save(
                com.dinesh.enterprise.entity.Category.builder()
                        .name("Electronics & Computing")
                        .description("High-performance workstations, computing hardware, and accessories")
                        .active(true)
                        .build()
        );

        com.dinesh.enterprise.entity.Category office = categoryRepository.save(
                com.dinesh.enterprise.entity.Category.builder()
                        .name("Office & Ergonomics")
                        .description("Professional ergonomic seating, motorized desks, and equipment")
                        .active(true)
                        .build()
        );

        com.dinesh.enterprise.entity.Category logistics = categoryRepository.save(
                com.dinesh.enterprise.entity.Category.builder()
                        .name("Logistics & Warehouse Supplies")
                        .description("Barcode scanners, label printers, and warehouse automation devices")
                        .active(true)
                        .build()
        );

        // 2. Warehouses
        com.dinesh.enterprise.entity.Warehouse whCentral = warehouseRepository.save(
                com.dinesh.enterprise.entity.Warehouse.builder()
                        .code("WH-CHI-01")
                        .name("Chicago Central Distribution Center")
                        .address("4500 Enterprise Pkwy")
                        .city("Chicago")
                        .state("IL")
                        .country("USA")
                        .postalCode("60601")
                        .status(com.dinesh.enterprise.enums.WarehouseStatus.ACTIVE)
                        .build()
        );

        com.dinesh.enterprise.entity.Warehouse whWest = warehouseRepository.save(
                com.dinesh.enterprise.entity.Warehouse.builder()
                        .code("WH-SEA-02")
                        .name("Seattle Pacific Logistics Hub")
                        .address("1200 Terminal Way")
                        .city("Seattle")
                        .state("WA")
                        .country("USA")
                        .postalCode("98101")
                        .status(com.dinesh.enterprise.enums.WarehouseStatus.ACTIVE)
                        .build()
        );

        // 3. Products & Stock
        List<com.dinesh.enterprise.entity.Product> sampleProducts = List.of(
                com.dinesh.enterprise.entity.Product.builder()
                        .sku("PROD-DELL-XPS16")
                        .name("Dell XPS 16 Developer Workstation")
                        .description("Core i9, 64GB DDR5 RAM, 2TB NVMe SSD, OLED 4K Display")
                        .price(new java.math.BigDecimal("2499.00"))
                        .costPrice(new java.math.BigDecimal("1800.00"))
                        .status(com.dinesh.enterprise.enums.ProductStatus.ACTIVE)
                        .category(electronics)
                        .build(),
                com.dinesh.enterprise.entity.Product.builder()
                        .sku("PROD-MX3S-MOUSE")
                        .name("Logitech MX Master 3S Wireless Mouse")
                        .description("Quiet click ergonomic wireless mouse with 8K DPI sensor")
                        .price(new java.math.BigDecimal("99.99"))
                        .costPrice(new java.math.BigDecimal("60.00"))
                        .status(com.dinesh.enterprise.enums.ProductStatus.ACTIVE)
                        .category(electronics)
                        .build(),
                com.dinesh.enterprise.entity.Product.builder()
                        .sku("PROD-SONY-WH1000")
                        .name("Sony WH-1000XM5 Wireless Headphones")
                        .description("Industry-leading active noise cancelling with 30-hour battery life")
                        .price(new java.math.BigDecimal("399.00"))
                        .costPrice(new java.math.BigDecimal("250.00"))
                        .status(com.dinesh.enterprise.enums.ProductStatus.ACTIVE)
                        .category(electronics)
                        .build(),
                com.dinesh.enterprise.entity.Product.builder()
                        .sku("PROD-CHAIR-ERG01")
                        .name("Aeron Mesh Ergonomic Task Chair")
                        .description("Fully adjustable lumbar support with breathable elastomeric suspension")
                        .price(new java.math.BigDecimal("850.00"))
                        .costPrice(new java.math.BigDecimal("500.00"))
                        .status(com.dinesh.enterprise.enums.ProductStatus.ACTIVE)
                        .category(office)
                        .build(),
                com.dinesh.enterprise.entity.Product.builder()
                        .sku("PROD-ZEBRA-SCAN")
                        .name("Zebra DS2208 2D Industrial Scanner")
                        .description("Rugged handheld barcode scanner for inventory scanning & order dispatch")
                        .price(new java.math.BigDecimal("210.00"))
                        .costPrice(new java.math.BigDecimal("130.00"))
                        .status(com.dinesh.enterprise.enums.ProductStatus.ACTIVE)
                        .category(logistics)
                        .build()
        );

        for (com.dinesh.enterprise.entity.Product p : sampleProducts) {
            com.dinesh.enterprise.entity.Product savedProduct = productRepository.save(p);

            // Seed stock in Central Warehouse
            inventoryRepository.save(
                    com.dinesh.enterprise.entity.Inventory.builder()
                            .product(savedProduct)
                            .warehouse(whCentral)
                            .quantity(100L)
                            .reservedQuantity(0L)
                            .reorderLevel(15L)
                            .build()
            );

            // Seed stock in West Warehouse
            inventoryRepository.save(
                    com.dinesh.enterprise.entity.Inventory.builder()
                            .product(savedProduct)
                            .warehouse(whWest)
                            .quantity(75L)
                            .reservedQuantity(0L)
                            .reorderLevel(10L)
                            .build()
            );
        }

        log.info("Seeded sample catalog categories, products, warehouses, and multi-warehouse stock.");
    }
}
