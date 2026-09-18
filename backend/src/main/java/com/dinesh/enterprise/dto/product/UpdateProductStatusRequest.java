package com.dinesh.enterprise.dto.product;

import com.dinesh.enterprise.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for updating a product's status (ADMIN action).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductStatusRequest {

    @NotNull(message = "Status is required")
    private ProductStatus status;
}
