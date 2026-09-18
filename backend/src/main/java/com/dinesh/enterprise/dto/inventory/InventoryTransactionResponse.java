package com.dinesh.enterprise.dto.inventory;

import com.dinesh.enterprise.enums.InventoryTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionResponse {

    private Long id;
    private Long inventoryId;
    private Long productId;
    private String productName;
    private String productSku;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private InventoryTransactionType type;
    private Long quantity;
    private String referenceType;
    private String referenceId;
    private String notes;
    private Long previousQuantity;
    private Long newQuantity;
    private String createdBy;
    private LocalDateTime createdAt;
}
