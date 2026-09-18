package com.dinesh.enterprise.controller;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.order.CancelOrderRequest;
import com.dinesh.enterprise.dto.order.CreateOrderRequest;
import com.dinesh.enterprise.dto.order.OrderItemResponse;
import com.dinesh.enterprise.dto.order.OrderResponse;
import com.dinesh.enterprise.dto.order.OrderStatusHistoryResponse;
import com.dinesh.enterprise.dto.order.OrderStatusUpdateRequest;
import com.dinesh.enterprise.enums.OrderStatus;
import com.dinesh.enterprise.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(orderService.searchOrders(status, customerId, pageable));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<PageResponse<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(orderService.getMyOrders(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancelOrder(id, request));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<OrderStatusHistoryResponse>> getOrderHistory(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderHistory(id));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItemResponse>> getOrderItems(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderItems(id));
    }

    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(Math.max(page, 0), safeSize, sort);
    }
}
