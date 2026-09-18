package com.dinesh.enterprise.dto.order;

import com.dinesh.enterprise.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerEmail;
    private String customerName;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String billingAddress;
    private String notes;
    private List<OrderItemResponse> items;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
