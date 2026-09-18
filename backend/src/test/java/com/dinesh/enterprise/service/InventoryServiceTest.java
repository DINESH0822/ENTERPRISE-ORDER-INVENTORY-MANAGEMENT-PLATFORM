package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.inventory.InventoryResponse;
import com.dinesh.enterprise.dto.inventory.StockAdjustmentRequest;
import com.dinesh.enterprise.dto.inventory.StockInRequest;
import com.dinesh.enterprise.dto.inventory.StockOutRequest;
import com.dinesh.enterprise.dto.inventory.StockReleaseRequest;
import com.dinesh.enterprise.dto.inventory.StockReservationRequest;
import com.dinesh.enterprise.entity.Inventory;
import com.dinesh.enterprise.entity.InventoryTransaction;
import com.dinesh.enterprise.entity.Product;
import com.dinesh.enterprise.entity.Warehouse;
import com.dinesh.enterprise.enums.InventoryTransactionType;
import com.dinesh.enterprise.enums.ProductStatus;
import com.dinesh.enterprise.enums.WarehouseStatus;
import com.dinesh.enterprise.exception.BusinessException;
import com.dinesh.enterprise.exception.InsufficientStockException;
import com.dinesh.enterprise.exception.WarehouseInactiveException;
import com.dinesh.enterprise.mapper.InventoryMapper;
import com.dinesh.enterprise.repository.InventoryRepository;
import com.dinesh.enterprise.repository.InventoryTransactionRepository;
import com.dinesh.enterprise.repository.ProductRepository;
import com.dinesh.enterprise.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService — stock lifecycle & audit ledger unit tests")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryService inventoryService;

    @Captor
    private ArgumentCaptor<InventoryTransaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<Inventory> inventoryCaptor;

    private Product product;
    private Warehouse warehouse;
    private Warehouse inactiveWarehouse;
    private Inventory inventory;
    private InventoryResponse inventoryResponse;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(100L)
                .sku("SKU-TEST-001")
                .name("Test Industrial Tool")
                .price(new BigDecimal("99.99"))
                .status(ProductStatus.ACTIVE)
                .build();

        warehouse = Warehouse.builder()
                .id(200L)
                .code("WH-NYC-01")
                .name("New York Fulfillment Center")
                .status(WarehouseStatus.ACTIVE)
                .build();

        inactiveWarehouse = Warehouse.builder()
                .id(201L)
                .code("WH-INACTIVE")
                .name("Closed Warehouse")
                .status(WarehouseStatus.INACTIVE)
                .build();

        inventory = Inventory.builder()
                .id(500L)
                .product(product)
                .warehouse(warehouse)
                .quantity(100L)
                .reservedQuantity(20L)
                .reorderLevel(15L)
                .version(1L)
                .build();

        inventoryResponse = InventoryResponse.builder()
                .id(500L)
                .productId(100L)
                .productSku("SKU-TEST-001")
                .warehouseId(200L)
                .warehouseCode("WH-NYC-01")
                .quantity(100L)
                .reservedQuantity(20L)
                .availableQuantity(80L)
                .reorderLevel(15L)
                .isLowStock(false)
                .build();
    }

    // -------------------------------------------------------------------------
    // Stock In
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stockIn increases inventory quantity and records STOCK_IN transaction")
    void stockIn_success() {
        StockInRequest request = StockInRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .quantity(50L)
                .referenceType("PO")
                .referenceId("PO-12345")
                .notes("Initial shipment")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(100L, 200L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryMapper.toResponse(any())).thenReturn(inventoryResponse);

        InventoryResponse result = inventoryService.stockIn(request);

        assertThat(result).isNotNull();

        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue().getQuantity()).isEqualTo(150L);

        verify(transactionRepository).save(transactionCaptor.capture());
        InventoryTransaction tx = transactionCaptor.getValue();
        assertThat(tx.getType()).isEqualTo(InventoryTransactionType.STOCK_IN);
        assertThat(tx.getQuantity()).isEqualTo(50L);
        assertThat(tx.getPreviousQuantity()).isEqualTo(100L);
        assertThat(tx.getNewQuantity()).isEqualTo(150L);
        assertThat(tx.getReferenceId()).isEqualTo("PO-12345");
    }

    @Test
    @DisplayName("stockIn with inactive warehouse throws WarehouseInactiveException")
    void stockIn_inactiveWarehouse_throwsException() {
        StockInRequest request = StockInRequest.builder()
                .productId(100L)
                .warehouseId(201L)
                .quantity(50L)
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(201L)).thenReturn(Optional.of(inactiveWarehouse));

        assertThatThrownBy(() -> inventoryService.stockIn(request))
                .isInstanceOf(WarehouseInactiveException.class)
                .hasMessageContaining("INACTIVE");

        verify(inventoryRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("stockIn with zero quantity throws BusinessException")
    void stockIn_zeroQuantity_throwsBusinessException() {
        StockInRequest request = StockInRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .quantity(0L)
                .build();

        assertThatThrownBy(() -> inventoryService.stockIn(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than 0");
    }

    // -------------------------------------------------------------------------
    // Stock Out
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stockOut decreases inventory quantity and records STOCK_OUT transaction")
    void stockOut_success() {
        StockOutRequest request = StockOutRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .quantity(30L)
                .referenceType("SO")
                .referenceId("ORDER-999")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(100L, 200L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryMapper.toResponse(any())).thenReturn(inventoryResponse);

        InventoryResponse result = inventoryService.stockOut(request);

        assertThat(result).isNotNull();

        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue().getQuantity()).isEqualTo(70L);

        verify(transactionRepository).save(transactionCaptor.capture());
        InventoryTransaction tx = transactionCaptor.getValue();
        assertThat(tx.getType()).isEqualTo(InventoryTransactionType.STOCK_OUT);
        assertThat(tx.getQuantity()).isEqualTo(30L);
        assertThat(tx.getPreviousQuantity()).isEqualTo(100L);
        assertThat(tx.getNewQuantity()).isEqualTo(70L);
    }

    @Test
    @DisplayName("stockOut exceeding available quantity throws InsufficientStockException")
    void stockOut_exceedingAvailable_throwsInsufficientStock() {
        // total=100, reserved=20 -> available=80. Requesting 90 must fail.
        StockOutRequest request = StockOutRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .quantity(90L)
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(100L, 200L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.stockOut(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient available stock");

        verify(inventoryRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Stock Reservation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("reserveStock increases reservedQuantity without changing total quantity")
    void reserveStock_success() {
        StockReservationRequest request = StockReservationRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .quantity(30L)
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(100L, 200L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryMapper.toResponse(any())).thenReturn(inventoryResponse);

        inventoryService.reserveStock(request);

        verify(inventoryRepository).save(inventoryCaptor.capture());
        Inventory saved = inventoryCaptor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(100L); // Total unchanged
        assertThat(saved.getReservedQuantity()).isEqualTo(50L); // 20 + 30 = 50

        verify(transactionRepository).save(transactionCaptor.capture());
        InventoryTransaction tx = transactionCaptor.getValue();
        assertThat(tx.getType()).isEqualTo(InventoryTransactionType.RESERVATION);
        assertThat(tx.getQuantity()).isEqualTo(30L);
    }

    @Test
    @DisplayName("reserveStock exceeding available stock throws InsufficientStockException")
    void reserveStock_exceedingAvailable_throwsException() {
        // total=100, reserved=20 -> available=80. Requesting 85 to reserve must fail.
        StockReservationRequest request = StockReservationRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .quantity(85L)
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(100L, 200L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserveStock(request))
                .isInstanceOf(InsufficientStockException.class);
    }

    // -------------------------------------------------------------------------
    // Reservation Release
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("releaseReservation decreases reservedQuantity")
    void releaseReservation_success() {
        StockReleaseRequest request = StockReleaseRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .quantity(15L)
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(100L, 200L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryMapper.toResponse(any())).thenReturn(inventoryResponse);

        inventoryService.releaseReservation(request);

        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue().getReservedQuantity()).isEqualTo(5L); // 20 - 15 = 5

        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getType()).isEqualTo(InventoryTransactionType.RELEASE);
    }

    @Test
    @DisplayName("releaseReservation exceeding currently reserved quantity throws InsufficientStockException")
    void releaseReservation_exceedingReserved_throwsException() {
        // reserved=20. Requesting to release 25 must fail.
        StockReleaseRequest request = StockReleaseRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .quantity(25L)
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(100L, 200L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.releaseReservation(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Cannot release more than currently reserved");
    }

    // -------------------------------------------------------------------------
    // Stock Adjustment
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("adjustStock sets exact target quantity and records ADJUSTMENT transaction")
    void adjustStock_success() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .newQuantity(60L)
                .reason("Physical count reconciliation")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(100L, 200L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryMapper.toResponse(any())).thenReturn(inventoryResponse);

        inventoryService.adjustStock(request);

        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue().getQuantity()).isEqualTo(60L);

        verify(transactionRepository).save(transactionCaptor.capture());
        InventoryTransaction tx = transactionCaptor.getValue();
        assertThat(tx.getType()).isEqualTo(InventoryTransactionType.ADJUSTMENT);
        assertThat(tx.getNotes()).isEqualTo("Physical count reconciliation");
        assertThat(tx.getPreviousQuantity()).isEqualTo(100L);
        assertThat(tx.getNewQuantity()).isEqualTo(60L);
        assertThat(tx.getQuantity()).isEqualTo(40L); // diff |60 - 100| = 40
    }

    @Test
    @DisplayName("adjustStock below reserved quantity throws InsufficientStockException")
    void adjustStock_belowReserved_throwsException() {
        // reserved=20. Target 10 must fail.
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .productId(100L)
                .warehouseId(200L)
                .newQuantity(10L)
                .reason("Count reduction")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(100L, 200L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.adjustStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Cannot adjust stock below current reserved quantity");
    }

    // -------------------------------------------------------------------------
    // Low Stock Queries
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getLowStockInventory returns page of low stock items")
    void getLowStockInventory_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> page = new PageImpl<>(List.of(inventory), pageable, 1);

        when(inventoryRepository.findLowStock(pageable)).thenReturn(page);
        when(inventoryMapper.toResponse(inventory)).thenReturn(inventoryResponse);

        PageResponse<InventoryResponse> result = inventoryService.getLowStockInventory(pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}
