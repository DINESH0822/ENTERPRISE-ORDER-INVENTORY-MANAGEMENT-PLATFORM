package com.dinesh.enterprise.repository;

import com.dinesh.enterprise.config.JpaAuditingConfig;
import com.dinesh.enterprise.entity.Category;
import com.dinesh.enterprise.entity.Inventory;
import com.dinesh.enterprise.entity.InventoryTransaction;
import com.dinesh.enterprise.entity.Product;
import com.dinesh.enterprise.entity.Role;
import com.dinesh.enterprise.entity.User;
import com.dinesh.enterprise.entity.Warehouse;
import com.dinesh.enterprise.enums.InventoryTransactionType;
import com.dinesh.enterprise.enums.ProductStatus;
import com.dinesh.enterprise.enums.UserStatus;
import com.dinesh.enterprise.enums.WarehouseStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class JpaPersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should persist and retrieve Role entity")
    void testPersistRole() {
        Role role = Role.builder()
                .name("ADMIN")
                .build();
        Role saved = roleRepository.save(role);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("ADMIN");
        assertThat(roleRepository.findByName("ADMIN")).isPresent();
    }

    @Test
    @DisplayName("Should persist User entity with Role relationship")
    void testPersistUserWithRoles() {
        Role adminRole = roleRepository.save(Role.builder().name("ADMIN").build());
        Role customerRole = roleRepository.save(Role.builder().name("CUSTOMER").build());

        User user = User.builder()
                .firstName("Dinesh")
                .lastName("Murugan")
                .email("dinesh.enterprise@example.com")
                .password("$2a$10$hashedpasswordforpersistencecheck")
                .phone("+919876543210")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole, customerRole))
                .build();

        User saved = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findByEmailWithRoles("dinesh.enterprise@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Dinesh");
        assertThat(found.get().getRoles()).hasSize(2);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce User unique email constraint")
    void testUserUniqueEmailConstraint() {
        User user1 = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("password123")
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("password456")
                .status(UserStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should persist Category and associated Product relationship")
    void testPersistCategoryAndProduct() {
        Category category = Category.builder()
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .active(true)
                .build();
        Category savedCategory = categoryRepository.save(category);

        Product product = Product.builder()
                .sku("SKU-LAPTOP-001")
                .name("Enterprise Workstation Laptop 16-inch")
                .description("High performance developer laptop")
                .price(new BigDecimal("1499.99"))
                .costPrice(new BigDecimal("1100.00"))
                .status(ProductStatus.ACTIVE)
                .category(savedCategory)
                .build();

        Product savedProduct = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        Optional<Product> foundProduct = productRepository.findBySku("SKU-LAPTOP-001");
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getPrice()).isEqualByComparingTo("1499.99");
        assertThat(foundProduct.get().getCategory().getName()).isEqualTo("Electronics");
        assertThat(foundProduct.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce Product SKU unique constraint")
    void testProductUniqueSkuConstraint() {
        Category category = categoryRepository.save(Category.builder().name("Appliances").build());

        Product p1 = Product.builder()
                .sku("SKU-UNIQUE-001")
                .name("Product 1")
                .price(new BigDecimal("100.00"))
                .costPrice(new BigDecimal("80.00"))
                .category(category)
                .build();
        productRepository.saveAndFlush(p1);

        Product p2 = Product.builder()
                .sku("SKU-UNIQUE-001")
                .name("Product 2")
                .price(new BigDecimal("120.00"))
                .costPrice(new BigDecimal("90.00"))
                .category(category)
                .build();

        assertThatThrownBy(() -> productRepository.saveAndFlush(p2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should persist Warehouse entity")
    void testPersistWarehouse() {
        Warehouse warehouse = Warehouse.builder()
                .code("WH-HYD-01")
                .name("Hyderabad Central Fulfillment Center")
                .address("Hitec City, Phase 2")
                .city("Hyderabad")
                .state("Telangana")
                .country("India")
                .postalCode("500081")
                .status(WarehouseStatus.ACTIVE)
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        assertThat(saved.getId()).isNotNull();
        assertThat(warehouseRepository.findByCode("WH-HYD-01")).isPresent();
    }

    @Test
    @DisplayName("Should persist Inventory and enforce unique (Product, Warehouse) constraint")
    void testPersistInventoryAndUniqueness() {
        Category category = categoryRepository.save(Category.builder().name("Accessories").build());
        Product product = productRepository.save(Product.builder()
                .sku("SKU-MOUSE-001")
                .name("Wireless Ergonomic Mouse")
                .price(new BigDecimal("49.99"))
                .costPrice(new BigDecimal("25.00"))
                .category(category)
                .build());

        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-BLR-01")
                .name("Bangalore Logistics Hub")
                .build());

        Inventory inventory = Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .quantity(100L)
                .reservedQuantity(10L)
                .reorderLevel(20L)
                .build();

        Inventory saved = inventoryRepository.saveAndFlush(inventory);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getQuantity()).isEqualTo(100L);

        // Attempt duplicate (product, warehouse)
        Inventory duplicate = Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .quantity(50L)
                .reservedQuantity(0L)
                .reorderLevel(10L)
                .build();

        assertThatThrownBy(() -> inventoryRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should verify Inventory optimistic locking (@Version)")
    void testInventoryOptimisticLocking() {
        Category category = categoryRepository.save(Category.builder().name("Displays").build());
        Product product = productRepository.save(Product.builder()
                .sku("SKU-MONITOR-4K")
                .name("4K Ultra HD Monitor 27-inch")
                .price(new BigDecimal("399.99"))
                .costPrice(new BigDecimal("280.00"))
                .category(category)
                .build());

        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-DEL-01")
                .name("Delhi Hub")
                .build());

        Inventory inventory = inventoryRepository.saveAndFlush(Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .quantity(50L)
                .reservedQuantity(0L)
                .reorderLevel(5L)
                .build());

        Long initialVersion = inventory.getVersion();
        assertThat(initialVersion).isNotNull();

        // Direct update modifies version
        inventory.setQuantity(45L);
        inventory.setReservedQuantity(5L);
        Inventory updated = inventoryRepository.saveAndFlush(inventory);
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }

    @Test
    @DisplayName("Should persist InventoryTransaction ledger entry")
    void testPersistInventoryTransaction() {
        Category category = categoryRepository.save(Category.builder().name("Storage").build());
        Product product = productRepository.save(Product.builder()
                .sku("SKU-SSD-1TB")
                .name("NVMe SSD 1TB")
                .price(new BigDecimal("99.99"))
                .costPrice(new BigDecimal("60.00"))
                .category(category)
                .build());

        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-MUM-01")
                .name("Mumbai Logistics Node")
                .build());

        Inventory inventory = inventoryRepository.save(Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .quantity(200L)
                .reservedQuantity(0L)
                .reorderLevel(30L)
                .build());

        InventoryTransaction transaction = InventoryTransaction.builder()
                .inventory(inventory)
                .type(InventoryTransactionType.STOCK_IN)
                .quantity(200L)
                .referenceType("PO")
                .referenceId("PO-2026-00100")
                .notes("Initial stock consignment received")
                .build();

        InventoryTransaction savedTx = inventoryTransactionRepository.save(transaction);
        entityManager.flush();
        entityManager.clear();

        assertThat(savedTx.getId()).isNotNull();
        assertThat(savedTx.getCreatedAt()).isNotNull();
        assertThat(inventoryTransactionRepository.findByInventoryId(inventory.getId())).hasSize(1);
    }
}
