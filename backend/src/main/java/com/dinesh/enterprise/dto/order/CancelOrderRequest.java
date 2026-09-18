package com.dinesh.enterprise.dto.order;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    private String reason;
}
