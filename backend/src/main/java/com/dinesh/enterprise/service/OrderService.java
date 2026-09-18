package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.inventory.StockReleaseRequest;
import com.dinesh.enterprise.dto.inventory.StockReservationRequest;
import com.dinesh.enterprise.dto.order.CancelOrderRequest;
import com.dinesh.enterprise.dto.order.CreateOrderRequest;
import com.dinesh.enterprise.dto.order.OrderItemRequest;
import com.dinesh.enterprise.dto.order.OrderItemResponse;
import com.dinesh.enterprise.dto.order.OrderResponse;
import com.dinesh.enterprise.dto.order.OrderStatusHistoryResponse;
import com.dinesh.enterprise.dto.order.OrderStatusUpdateRequest;
import com.dinesh.enterprise.entity.Order;
import com.dinesh.enterprise.entity.OrderItem;
import com.dinesh.enterprise.entity.OrderStatusHistory;
import com.dinesh.enterprise.entity.Product;
import com.dinesh.enterprise.entity.User;
import com.dinesh.enterprise.entity.Warehouse;
import com.dinesh.enterprise.enums.OrderStatus;
import com.dinesh.enterprise.enums.ProductStatus;
import com.dinesh.enterprise.enums.WarehouseStatus;
import com.dinesh.enterprise.exception.BusinessException;
import com.dinesh.enterprise.exception.InvalidOrderStatusTransitionException;
import com.dinesh.enterprise.exception.OrderCancellationException;
import com.dinesh.enterprise.exception.OrderNotFoundException;
import com.dinesh.enterprise.exception.OrderOwnershipException;
import com.dinesh.enterprise.exception.ResourceNotFoundException;
import com.dinesh.enterprise.exception.UnauthorizedException;
import com.dinesh.enterprise.exception.WarehouseInactiveException;
import com.dinesh.enterprise.mapper.OrderMapper;
import com.dinesh.enterprise.repository.OrderRepository;
import com.dinesh.enterprise.repository.OrderStatusHistoryRepository;
import com.dinesh.enterprise.repository.ProductRepository;
import com.dinesh.enterprise.repository.UserRepository;
import com.dinesh.enterprise.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08"); // 8% Tax
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("15.00");

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    /**
     * Create a new customer order with atomic inventory reservation.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }

        User customer = getCurrentUser();
        String orderNumber = generateUniqueOrderNumber();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 1. Process and validate all order items
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() <= 0) {
                throw new BusinessException("Order item quantity must be greater than 0");
            }

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessException("Product " + product.getSku() + " is inactive or discontinued");
            }

            Warehouse warehouse = warehouseRepository.findById(itemReq.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + itemReq.getWarehouseId()));
            if (warehouse.getStatus() != WarehouseStatus.ACTIVE) {
                throw new WarehouseInactiveException("Warehouse " + warehouse.getCode() + " is currently INACTIVE");
            }

            // Historical price snapshot (from database, client price is ignored)
            BigDecimal unitPrice = product.getPrice();
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(itemSubtotal);

            // Reserve stock via InventoryService (throws InsufficientStockException on failure)
            inventoryService.reserveStock(StockReservationRequest.builder()
                    .productId(product.getId())
                    .warehouseId(warehouse.getId())
                    .quantity(itemReq.getQuantity())
                    .referenceType("ORDER")
                    .referenceId(orderNumber)
                    .notes("Stock reservation for order creation " + orderNumber)
                    .build());

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .warehouse(warehouse)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(itemSubtotal)
                    .build();

            orderItems.add(item);
        }

        // 2. Financial calculations
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : STANDARD_SHIPPING_FEE.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(tax).add(shippingFee).setScale(2, RoundingMode.HALF_UP);

        // 3. Persist Order entity
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .tax(tax)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress() != null ? request.getBillingAddress() : request.getShippingAddress())
                .notes(request.getNotes())
                .build();

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);

        // 4. Audit History
        recordStatusHistory(savedOrder, null, OrderStatus.PENDING, customer.getEmail(), "Order created successfully");

        log.info("Created order number={} customer={} total={}",
                savedOrder.getOrderNumber(), customer.getEmail(), savedOrder.getTotalAmount());

        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Update order status following the strict state machine workflow.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = findOrderById(orderId);
        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.getStatus();

        if (currentStatus == targetStatus) {
            return orderMapper.toResponse(order);
        }

        validateStatusTransition(currentStatus, targetStatus);

        String currentUserEmail = getCurrentUserEmail();

        // Perform side-effects based on transition
        if (targetStatus == OrderStatus.CANCELLED) {
            // Release inventory reservations
            releaseOrderInventoryReservations(order);
        } else if (targetStatus == OrderStatus.SHIPPED) {
            // Commit reserved stock as actual stock-out
            commitOrderInventoryShipment(order);
        }

        order.setStatus(targetStatus);
        Order savedOrder = orderRepository.save(order);

        recordStatusHistory(savedOrder, currentStatus, targetStatus, currentUserEmail, request.getReason());

        log.info("Updated order id={} number={} status from {} to {}",
                order.getId(), order.getOrderNumber(), currentStatus, targetStatus);

        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Cancel an order (Customer can cancel own order if PENDING/CONFIRMED; Admin can cancel any eligible order).
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, CancelOrderRequest request) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();

        // Enforce ownership if customer role
        if (isCustomerOnly(currentUser) && !order.getCustomer().getId().equals(currentUser.getId())) {
            throw new OrderOwnershipException("You do not have permission to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderCancellationException("Order " + order.getOrderNumber()
                    + " cannot be cancelled in status " + order.getStatus()
                    + ". Only PENDING or CONFIRMED orders can be cancelled.");
        }

        String reason = (request != null && request.getReason() != null)
                ? request.getReason()
                : "Cancelled by " + (isCustomerOnly(currentUser) ? "customer" : "admin");

        return updateOrderStatus(orderId, OrderStatusUpdateRequest.builder()
                .status(OrderStatus.CANCELLED)
                .reason(reason)
                .build());
    }

    /**
     * Get order details by ID (enforces customer ownership server-side).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = findOrderById(id);
        User currentUser = getCurrentUser();

        if (isCustomerOnly(currentUser) && !order.getCustomer().getId().equals(currentUser.getId())) {
            throw new OrderOwnershipException("You do not have permission to view this order");
        }

        return orderMapper.toResponse(order);
    }

    /**
     * Get orders belonging to the currently authenticated customer.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Pageable pageable) {
        User customer = getCurrentUser();
        Page<OrderResponse> page = orderRepository
                .findByCustomerId(customer.getId(), pageable)
                .map(orderMapper::toResponse);
        return PageResponse.of(page);
    }

    /**
     * Search all orders (Admin / Warehouse Manager / Support Agent).
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> searchOrders(OrderStatus status, Long customerId, Pageable pageable) {
        Page<OrderResponse> page = orderRepository
                .searchOrders(status, customerId, pageable)
                .map(orderMapper::toResponse);
        return PageResponse.of(page);
    }

    /**
     * Get order status change audit history.
     */
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(Long orderId) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();

        if (isCustomerOnly(currentUser) && !order.getCustomer().getId().equals(currentUser.getId())) {
            throw new OrderOwnershipException("You do not have permission to view history for this order");
        }

        return historyRepository.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(orderMapper::toHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get items for a specific order.
     */
    @Transactional(readOnly = true)
    public List<OrderItemResponse> getOrderItems(Long orderId) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();

        if (isCustomerOnly(currentUser) && !order.getCustomer().getId().equals(currentUser.getId())) {
            throw new OrderOwnershipException("You do not have permission to view items for this order");
        }

        return order.getItems().stream()
                .map(orderMapper::toItemResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Private Helpers & Validation
    // -------------------------------------------------------------------------

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        boolean valid = switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.PROCESSING || target == OrderStatus.CANCELLED;
            case PROCESSING -> target == OrderStatus.SHIPPED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidOrderStatusTransitionException(String.format(
                    "Invalid order status transition from %s to %s", current, target));
        }
    }

    private void releaseOrderInventoryReservations(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryService.releaseReservation(StockReleaseRequest.builder()
                    .productId(item.getProduct().getId())
                    .warehouseId(item.getWarehouse().getId())
                    .quantity(item.getQuantity())
                    .referenceType("ORDER")
                    .referenceId(order.getOrderNumber())
                    .notes("Reservation released for cancelled order " + order.getOrderNumber())
                    .build());
        }
    }

    private void commitOrderInventoryShipment(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryService.commitReservation(
                    item.getProduct().getId(),
                    item.getWarehouse().getId(),
                    item.getQuantity(),
                    "ORDER",
                    order.getOrderNumber(),
                    "Stock committed upon shipment of order " + order.getOrderNumber()
            );
        }
    }

    private void recordStatusHistory(Order order, OrderStatus prev, OrderStatus next, String changedBy, String reason) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(prev)
                .newStatus(next)
                .changedBy(changedBy)
                .reason(reason)
                .build();
        historyRepository.save(history);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private boolean isCustomerOnly(User user) {
        if (user.getRoles() == null) return true;
        return user.getRoles().stream()
                .allMatch(role -> "CUSTOMER".equalsIgnoreCase(role.getName()) || "ROLE_CUSTOMER".equalsIgnoreCase(role.getName()));
    }

    private String generateUniqueOrderNumber() {
        int year = LocalDateTime.now().getYear();
        Random random = new Random();
        String orderNum;
        do {
            int seq = random.nextInt(900000) + 100000;
            orderNum = String.format("ORD-%d-%06d", year, seq);
        } while (orderRepository.existsByOrderNumber(orderNum));
        return orderNum;
    }
}
