package com.dinesh.enterprise.controller;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.inventory.InventoryResponse;
import com.dinesh.enterprise.dto.inventory.InventoryTransactionResponse;
import com.dinesh.enterprise.dto.inventory.StockAdjustmentRequest;
import com.dinesh.enterprise.dto.inventory.StockInRequest;
import com.dinesh.enterprise.dto.inventory.StockOutRequest;
import com.dinesh.enterprise.dto.inventory.StockReleaseRequest;
import com.dinesh.enterprise.dto.inventory.StockReservationRequest;
import com.dinesh.enterprise.enums.InventoryTransactionType;
import com.dinesh.enterprise.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<PageResponse<InventoryResponse>> getInventory(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(inventoryService.searchInventory(productId, warehouseId, lowStock, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponse> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<InventoryResponse>> getInventoryByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(inventoryService.searchInventory(productId, null, null, pageable));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<PageResponse<InventoryResponse>> getInventoryByWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(inventoryService.searchInventory(null, warehouseId, null, pageable));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<PageResponse<InventoryResponse>> getLowStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(inventoryService.getLowStockInventory(pageable));
    }

    @PostMapping("/stock-in")
    public ResponseEntity<InventoryResponse> stockIn(@Valid @RequestBody StockInRequest request) {
        return ResponseEntity.ok(inventoryService.stockIn(request));
    }

    @PostMapping("/stock-out")
    public ResponseEntity<InventoryResponse> stockOut(@Valid @RequestBody StockOutRequest request) {
        return ResponseEntity.ok(inventoryService.stockOut(request));
    }

    @PostMapping("/adjust")
    public ResponseEntity<InventoryResponse> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(request));
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(@Valid @RequestBody StockReservationRequest request) {
        return ResponseEntity.ok(inventoryService.reserveStock(request));
    }

    @PostMapping("/release")
    public ResponseEntity<InventoryResponse> releaseReservation(@Valid @RequestBody StockReleaseRequest request) {
        return ResponseEntity.ok(inventoryService.releaseReservation(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<InventoryTransactionResponse>> searchTransactions(
            @RequestParam(required = false) Long inventoryId,
            @RequestParam(required = false) InventoryTransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(inventoryService.searchTransactions(inventoryId, type, pageable));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<InventoryTransactionResponse> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getTransactionById(id));
    }

    @GetMapping("/{inventoryId}/transactions")
    public ResponseEntity<PageResponse<InventoryTransactionResponse>> getTransactionsByInventoryId(
            @PathVariable Long inventoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(inventoryService.getTransactionsByInventoryId(inventoryId, pageable));
    }

    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(Math.max(page, 0), safeSize, sort);
    }
}
