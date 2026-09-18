package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.inventory.InventoryResponse;
import com.dinesh.enterprise.dto.inventory.InventoryTransactionResponse;
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
import com.dinesh.enterprise.exception.ResourceNotFoundException;
import com.dinesh.enterprise.exception.WarehouseInactiveException;
import com.dinesh.enterprise.mapper.InventoryMapper;
import com.dinesh.enterprise.repository.InventoryRepository;
import com.dinesh.enterprise.repository.InventoryTransactionRepository;
import com.dinesh.enterprise.repository.ProductRepository;
import com.dinesh.enterprise.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryMapper inventoryMapper;

    /**
     * Receive stock into a warehouse for a product.
     */
    @Transactional
    public InventoryResponse stockIn(StockInRequest request) {
        if (request.getQuantity() <= 0) {
            throw new BusinessException("Stock-in quantity must be greater than 0");
        }

        Product product = validateProduct(request.getProductId());
        Warehouse warehouse = validateWarehouse(request.getWarehouseId());

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> Inventory.builder()
                        .product(product)
                        .warehouse(warehouse)
                        .quantity(0L)
                        .reservedQuantity(0L)
                        .reorderLevel(10L)
                        .build());

        long previousQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
        long newQty = previousQty + request.getQuantity();

        inventory.setQuantity(newQty);
        Inventory savedInventory = inventoryRepository.save(inventory);

        recordTransaction(
                savedInventory,
                InventoryTransactionType.STOCK_IN,
                request.getQuantity(),
                request.getReferenceType(),
                request.getReferenceId(),
                request.getNotes(),
                previousQty,
                newQty
        );

        log.info("Stock in: productId={}, warehouseId={}, qty={}, newTotal={}",
                product.getId(), warehouse.getId(), request.getQuantity(), newQty);

        return inventoryMapper.toResponse(savedInventory);
    }

    /**
     * Issue stock out of a warehouse for a product.
     */
    @Transactional
    public InventoryResponse stockOut(StockOutRequest request) {
        if (request.getQuantity() <= 0) {
            throw new BusinessException("Stock-out quantity must be greater than 0");
        }

        Product product = validateProduct(request.getProductId());
        Warehouse warehouse = validateWarehouse(request.getWarehouseId());

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record found for product ID " + product.getId()
                                + " in warehouse ID " + warehouse.getId()));

        long previousQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
        long reserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0L;
        long available = previousQty - reserved;

        if (request.getQuantity() > available) {
            throw new InsufficientStockException(String.format(
                    "Insufficient available stock for product %s in warehouse %s. Requested: %d, Available: %d",
                    product.getSku(), warehouse.getCode(), request.getQuantity(), available));
        }

        long newQty = previousQty - request.getQuantity();
        inventory.setQuantity(newQty);
        Inventory savedInventory = inventoryRepository.save(inventory);

        recordTransaction(
                savedInventory,
                InventoryTransactionType.STOCK_OUT,
                request.getQuantity(),
                request.getReferenceType(),
                request.getReferenceId(),
                request.getNotes(),
                previousQty,
                newQty
        );

        log.info("Stock out: productId={}, warehouseId={}, qty={}, newTotal={}",
                product.getId(), warehouse.getId(), request.getQuantity(), newQty);

        return inventoryMapper.toResponse(savedInventory);
    }

    /**
     * Adjust stock quantity to a specific target level.
     */
    @Transactional
    public InventoryResponse adjustStock(StockAdjustmentRequest request) {
        if (request.getNewQuantity() < 0) {
            throw new BusinessException("Target stock quantity cannot be negative");
        }

        Product product = validateProduct(request.getProductId());
        Warehouse warehouse = validateWarehouse(request.getWarehouseId());

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> Inventory.builder()
                        .product(product)
                        .warehouse(warehouse)
                        .quantity(0L)
                        .reservedQuantity(0L)
                        .reorderLevel(10L)
                        .build());

        long previousQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
        long reserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0L;
        long newQty = request.getNewQuantity();

        if (newQty < reserved) {
            throw new InsufficientStockException(String.format(
                    "Cannot adjust stock below current reserved quantity (%d) for product %s",
                    reserved, product.getSku()));
        }

        long diff = Math.abs(newQty - previousQty);

        inventory.setQuantity(newQty);
        Inventory savedInventory = inventoryRepository.save(inventory);

        recordTransaction(
                savedInventory,
                InventoryTransactionType.ADJUSTMENT,
                diff,
                request.getReferenceType(),
                request.getReferenceId(),
                request.getReason(),
                previousQty,
                newQty
        );

        log.info("Stock adjustment: productId={}, warehouseId={}, previousQty={}, newQty={}",
                product.getId(), warehouse.getId(), previousQty, newQty);

        return inventoryMapper.toResponse(savedInventory);
    }

    /**
     * Reserve inventory stock for an upcoming order.
     */
    @Transactional
    public InventoryResponse reserveStock(StockReservationRequest request) {
        if (request.getQuantity() <= 0) {
            throw new BusinessException("Reservation quantity must be greater than 0");
        }

        Product product = validateProduct(request.getProductId());
        Warehouse warehouse = validateWarehouse(request.getWarehouseId());

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record found for product ID " + product.getId()
                                + " in warehouse ID " + warehouse.getId()));

        long totalQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
        long reserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0L;
        long available = totalQty - reserved;

        if (request.getQuantity() > available) {
            throw new InsufficientStockException(String.format(
                    "Insufficient available stock to reserve. Requested: %d, Available: %d",
                    request.getQuantity(), available));
        }

        long newReserved = reserved + request.getQuantity();
        inventory.setReservedQuantity(newReserved);
        Inventory savedInventory = inventoryRepository.save(inventory);

        recordTransaction(
                savedInventory,
                InventoryTransactionType.RESERVATION,
                request.getQuantity(),
                request.getReferenceType(),
                request.getReferenceId(),
                request.getNotes(),
                totalQty,
                totalQty
        );

        log.info("Stock reserved: productId={}, warehouseId={}, reservedQty={}, newReservedTotal={}",
                product.getId(), warehouse.getId(), request.getQuantity(), newReserved);

        return inventoryMapper.toResponse(savedInventory);
    }

    /**
     * Release previously reserved inventory stock.
     */
    @Transactional
    public InventoryResponse releaseReservation(StockReleaseRequest request) {
        if (request.getQuantity() <= 0) {
            throw new BusinessException("Release quantity must be greater than 0");
        }

        Product product = validateProduct(request.getProductId());
        Warehouse warehouse = validateWarehouse(request.getWarehouseId());

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record found for product ID " + product.getId()
                                + " in warehouse ID " + warehouse.getId()));

        long totalQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
        long reserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0L;

        if (request.getQuantity() > reserved) {
            throw new InsufficientStockException(String.format(
                    "Cannot release more than currently reserved stock. Requested release: %d, Reserved: %d",
                    request.getQuantity(), reserved));
        }

        long newReserved = reserved - request.getQuantity();
        inventory.setReservedQuantity(newReserved);
        Inventory savedInventory = inventoryRepository.save(inventory);

        recordTransaction(
                savedInventory,
                InventoryTransactionType.RELEASE,
                request.getQuantity(),
                request.getReferenceType(),
                request.getReferenceId(),
                request.getNotes(),
                totalQty,
                totalQty
        );

        log.info("Stock release: productId={}, warehouseId={}, releasedQty={}, newReservedTotal={}",
                product.getId(), warehouse.getId(), request.getQuantity(), newReserved);

        return inventoryMapper.toResponse(savedInventory);
    }

    /**
     * Commit previously reserved inventory into an actual stock-out (e.g. on order shipment).
     * Reduces both total quantity and reserved quantity by quantity.
     */
    @Transactional
    public InventoryResponse commitReservation(
            Long productId, Long warehouseId, Long quantity, String referenceType, String referenceId, String notes) {
        if (quantity <= 0) {
            throw new BusinessException("Commit quantity must be greater than 0");
        }

        Product product = validateProduct(productId);
        Warehouse warehouse = validateWarehouse(warehouseId);

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record found for product ID " + product.getId()
                                + " in warehouse ID " + warehouse.getId()));

        long previousQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
        long reserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0L;

        if (quantity > reserved) {
            throw new InsufficientStockException(String.format(
                    "Cannot commit more than currently reserved stock. Requested: %d, Reserved: %d",
                    quantity, reserved));
        }

        long newQty = previousQty - quantity;
        long newReserved = reserved - quantity;

        inventory.setQuantity(newQty);
        inventory.setReservedQuantity(newReserved);
        Inventory savedInventory = inventoryRepository.save(inventory);

        recordTransaction(
                savedInventory,
                InventoryTransactionType.STOCK_OUT,
                quantity,
                referenceType,
                referenceId,
                notes != null ? notes : "Committed reserved stock upon shipment",
                previousQty,
                newQty
        );

        log.info("Stock committed: productId={}, warehouseId={}, qty={}, newTotal={}, newReserved={}",
                product.getId(), warehouse.getId(), quantity, newQty, newReserved);

        return inventoryMapper.toResponse(savedInventory);
    }

    /**
     * Read inventory by ID.
     */
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found with id: " + id));
        return inventoryMapper.toResponse(inventory);
    }

    /**
     * Read inventory by product & warehouse.
     */
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductAndWarehouse(Long productId, Long warehouseId) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory record not found for product ID " + productId + " and warehouse ID " + warehouseId));
        return inventoryMapper.toResponse(inventory);
    }

    /**
     * Search inventory with optional filtering by productId, warehouseId, and lowStock status.
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> searchInventory(
            Long productId, Long warehouseId, Boolean lowStock, Pageable pageable) {
        Page<InventoryResponse> page = inventoryRepository
                .searchInventory(productId, warehouseId, lowStock, pageable)
                .map(inventoryMapper::toResponse);
        return PageResponse.of(page);
    }

    /**
     * Get all low-stock inventory records.
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getLowStockInventory(Pageable pageable) {
        Page<InventoryResponse> page = inventoryRepository
                .findLowStock(pageable)
                .map(inventoryMapper::toResponse);
        return PageResponse.of(page);
    }

    /**
     * Search transaction ledger history.
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> searchTransactions(
            Long inventoryId, InventoryTransactionType type, Pageable pageable) {
        Page<InventoryTransactionResponse> page = transactionRepository
                .searchTransactions(inventoryId, type, pageable)
                .map(inventoryMapper::toTransactionResponse);
        return PageResponse.of(page);
    }

    /**
     * Get transaction history for a specific inventory ID.
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> getTransactionsByInventoryId(
            Long inventoryId, Pageable pageable) {
        if (!inventoryRepository.existsById(inventoryId)) {
            throw new ResourceNotFoundException("Inventory record not found with id: " + inventoryId);
        }
        Page<InventoryTransactionResponse> page = transactionRepository
                .findByInventoryId(inventoryId, pageable)
                .map(inventoryMapper::toTransactionResponse);
        return PageResponse.of(page);
    }

    /**
     * Get a single transaction by ID.
     */
    @Transactional(readOnly = true)
    public InventoryTransactionResponse getTransactionById(Long id) {
        InventoryTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory transaction not found with id: " + id));
        return inventoryMapper.toTransactionResponse(tx);
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private Product validateProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException("Product with ID " + productId + " is inactive or discontinued");
        }
        return product;
    }

    private Warehouse validateWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));
        if (warehouse.getStatus() != WarehouseStatus.ACTIVE) {
            throw new WarehouseInactiveException("Warehouse with ID " + warehouseId + " is currently INACTIVE");
        }
        return warehouse;
    }

    private void recordTransaction(
            Inventory inventory,
            InventoryTransactionType type,
            Long quantity,
            String referenceType,
            String referenceId,
            String notes,
            Long previousQty,
            Long newQty) {

        String currentUser = getCurrentUserEmail();

        InventoryTransaction transaction = InventoryTransaction.builder()
                .inventory(inventory)
                .type(type)
                .quantity(quantity)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .notes(notes)
                .previousQuantity(previousQty)
                .newQuantity(newQty)
                .createdBy(currentUser)
                .build();

        transactionRepository.save(transaction);
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}
