package com.dinesh.enterprise.mapper;

import com.dinesh.enterprise.dto.inventory.InventoryResponse;
import com.dinesh.enterprise.dto.inventory.InventoryTransactionResponse;
import com.dinesh.enterprise.entity.Inventory;
import com.dinesh.enterprise.entity.InventoryTransaction;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        long qty = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
        long reserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0L;
        long available = qty - reserved;
        long reorder = inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 0L;
        boolean lowStock = available <= reorder;

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                .productSku(inventory.getProduct() != null ? inventory.getProduct().getSku() : null)
                .warehouseId(inventory.getWarehouse() != null ? inventory.getWarehouse().getId() : null)
                .warehouseCode(inventory.getWarehouse() != null ? inventory.getWarehouse().getCode() : null)
                .warehouseName(inventory.getWarehouse() != null ? inventory.getWarehouse().getName() : null)
                .quantity(qty)
                .reservedQuantity(reserved)
                .availableQuantity(available)
                .reorderLevel(reorder)
                .isLowStock(lowStock)
                .version(inventory.getVersion())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public InventoryTransactionResponse toTransactionResponse(InventoryTransaction tx) {
        if (tx == null) {
            return null;
        }

        Inventory inv = tx.getInventory();
        Long productId = null;
        String productName = null;
        String productSku = null;
        Long warehouseId = null;
        String warehouseCode = null;
        String warehouseName = null;

        if (inv != null) {
            if (inv.getProduct() != null) {
                productId = inv.getProduct().getId();
                productName = inv.getProduct().getName();
                productSku = inv.getProduct().getSku();
            }
            if (inv.getWarehouse() != null) {
                warehouseId = inv.getWarehouse().getId();
                warehouseCode = inv.getWarehouse().getCode();
                warehouseName = inv.getWarehouse().getName();
            }
        }

        return InventoryTransactionResponse.builder()
                .id(tx.getId())
                .inventoryId(inv != null ? inv.getId() : null)
                .productId(productId)
                .productName(productName)
                .productSku(productSku)
                .warehouseId(warehouseId)
                .warehouseCode(warehouseCode)
                .warehouseName(warehouseName)
                .type(tx.getType())
                .quantity(tx.getQuantity())
                .referenceType(tx.getReferenceType())
                .referenceId(tx.getReferenceId())
                .notes(tx.getNotes())
                .previousQuantity(tx.getPreviousQuantity())
                .newQuantity(tx.getNewQuantity())
                .createdBy(tx.getCreatedBy())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
