package com.dinesh.enterprise.mapper;

import com.dinesh.enterprise.dto.order.OrderItemResponse;
import com.dinesh.enterprise.dto.order.OrderResponse;
import com.dinesh.enterprise.dto.order.OrderStatusHistoryResponse;
import com.dinesh.enterprise.entity.Order;
import com.dinesh.enterprise.entity.OrderItem;
import com.dinesh.enterprise.entity.OrderStatusHistory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(this::toItemResponse).collect(Collectors.toList())
                : Collections.emptyList();

        String customerName = null;
        if (order.getCustomer() != null) {
            String first = order.getCustomer().getFirstName() != null ? order.getCustomer().getFirstName() : "";
            String last = order.getCustomer().getLastName() != null ? order.getCustomer().getLastName() : "";
            customerName = (first + " " + last).trim();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerEmail(order.getCustomer() != null ? order.getCustomer().getEmail() : null)
                .customerName(customerName)
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .notes(order.getNotes())
                .items(itemResponses)
                .version(order.getVersion())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                .productSku(item.getProduct() != null ? item.getProduct().getSku() : null)
                .warehouseId(item.getWarehouse() != null ? item.getWarehouse().getId() : null)
                .warehouseCode(item.getWarehouse() != null ? item.getWarehouse().getCode() : null)
                .warehouseName(item.getWarehouse() != null ? item.getWarehouse().getName() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }

    public OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history) {
        if (history == null) {
            return null;
        }

        return OrderStatusHistoryResponse.builder()
                .id(history.getId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .changedBy(history.getChangedBy())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
