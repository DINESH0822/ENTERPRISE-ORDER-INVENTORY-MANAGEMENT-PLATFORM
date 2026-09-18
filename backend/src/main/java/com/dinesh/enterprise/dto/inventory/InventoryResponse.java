package com.dinesh.enterprise.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long quantity;
    private Long reservedQuantity;
    private Long availableQuantity;
    private Long reorderLevel;
    private Boolean isLowStock;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
