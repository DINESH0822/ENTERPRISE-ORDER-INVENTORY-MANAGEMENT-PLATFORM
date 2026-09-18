package com.dinesh.enterprise.dto.warehouse;

import com.dinesh.enterprise.enums.WarehouseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStatusRequest {

    @NotNull(message = "Status is required")
    private WarehouseStatus status;
}
