package com.dinesh.enterprise.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "New quantity is required")
    @PositiveOrZero(message = "New quantity cannot be negative")
    private Long newQuantity;

    @NotBlank(message = "Adjustment reason is mandatory")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    @Size(max = 50, message = "Reference type cannot exceed 50 characters")
    private String referenceType;

    @Size(max = 100, message = "Reference ID cannot exceed 100 characters")
    private String referenceId;
}
