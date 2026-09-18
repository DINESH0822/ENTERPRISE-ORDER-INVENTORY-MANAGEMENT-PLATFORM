package com.dinesh.enterprise.dto.order;

import com.dinesh.enterprise.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryResponse {

    private Long id;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String changedBy;
    private String reason;
    private LocalDateTime createdAt;
}
