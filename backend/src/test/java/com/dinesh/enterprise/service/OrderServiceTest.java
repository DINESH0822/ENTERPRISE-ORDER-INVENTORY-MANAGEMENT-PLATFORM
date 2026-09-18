package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.inventory.InventoryResponse;
import com.dinesh.enterprise.dto.inventory.StockReservationRequest;
import com.dinesh.enterprise.dto.order.CancelOrderRequest;
import com.dinesh.enterprise.dto.order.CreateOrderRequest;
import com.dinesh.enterprise.dto.order.OrderItemRequest;
import com.dinesh.enterprise.dto.order.OrderResponse;
import com.dinesh.enterprise.dto.order.OrderStatusHistoryResponse;
import com.dinesh.enterprise.dto.order.OrderStatusUpdateRequest;
import com.dinesh.enterprise.entity.Order;
import com.dinesh.enterprise.entity.OrderItem;
import com.dinesh.enterprise.entity.OrderStatusHistory;
import com.dinesh.enterprise.entity.Product;
import com.dinesh.enterprise.entity.Role;
import com.dinesh.enterprise.entity.User;
import com.dinesh.enterprise.entity.Warehouse;
import com.dinesh.enterprise.enums.OrderStatus;
import com.dinesh.enterprise.enums.ProductStatus;
import com.dinesh.enterprise.enums.WarehouseStatus;
import com.dinesh.enterprise.exception.BusinessException;
import com.dinesh.enterprise.exception.InsufficientStockException;
import com.dinesh.enterprise.exception.InvalidOrderStatusTransitionException;
import com.dinesh.enterprise.exception.OrderCancellationException;
import com.dinesh.enterprise.exception.OrderOwnershipException;
import com.dinesh.enterprise.exception.WarehouseInactiveException;
import com.dinesh.enterprise.mapper.OrderMapper;
import com.dinesh.enterprise.repository.OrderRepository;
import com.dinesh.enterprise.repository.OrderStatusHistoryRepository;
import com.dinesh.enterprise.repository.ProductRepository;
import com.dinesh.enterprise.repository.UserRepository;
import com.dinesh.enterprise.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService — order lifecycle, state machine & inventory integration unit tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository historyRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<OrderStatusHistory> historyCaptor;

    private User customer;
    private User otherCustomer;
    private User admin;
    private Product product;
    private Warehouse warehouse;
    private Order order;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        Role customerRole = Role.builder().id(1L).name("CUSTOMER").build();
        Role adminRole = Role.builder().id(2L).name("ADMIN").build();

        customer = User.builder()
                .id(10L)
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .roles(Set.of(customerRole))
                .build();

        otherCustomer = User.builder()
                .id(11L)
                .email("other.user@example.com")
                .firstName("Other")
                .lastName("User")
                .roles(Set.of(customerRole))
                .build();

        admin = User.builder()
                .id(1L)
                .email("admin@enterprise.com")
                .firstName("System")
                .lastName("Admin")
                .roles(Set.of(adminRole))
                .build();

        product = Product.builder()
                .id(100L)
                .sku("SKU-LAPTOP-001")
                .name("Enterprise Laptop")
                .price(new BigDecimal("100.00"))
                .status(ProductStatus.ACTIVE)
                .build();

        warehouse = Warehouse.builder()
                .id(200L)
                .code("WH-NYC-01")
                .name("New York Hub")
                .status(WarehouseStatus.ACTIVE)
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(product)
                .warehouse(warehouse)
                .quantity(2L)
                .unitPrice(new BigDecimal("100.00"))
                .subtotal(new BigDecimal("200.00"))
                .build();

        order = Order.builder()
                .id(500L)
                .orderNumber("ORD-2026-000001")
                .customer(customer)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("200.00"))
                .tax(new BigDecimal("16.00"))
                .shippingFee(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("231.00"))
                .shippingAddress("123 Main St, New York, NY")
                .billingAddress("123 Main St, New York, NY")
                .items(List.of(item))
                .build();
        item.setOrder(order);

        orderResponse = OrderResponse.builder()
                .id(500L)
                .orderNumber("ORD-2026-000001")
                .customerId(10L)
                .customerEmail("john.doe@example.com")
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("200.00"))
                .tax(new BigDecimal("16.00"))
                .shippingFee(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("231.00"))
                .build();
    }

    private void mockAuthentication(User user) {
        SecurityContextHolder.setContext(securityContext);
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.isAuthenticated()).thenReturn(true);
        org.mockito.Mockito.lenient().when(authentication.getPrincipal()).thenReturn(user.getEmail());
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn(user.getEmail());
        org.mockito.Mockito.lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // -------------------------------------------------------------------------
    // Order Creation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createOrder calculates prices, reserves inventory, and initializes PENDING order")
    void createOrder_success() {
        mockAuthentication(customer);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(OrderItemRequest.builder()
                        .productId(100L)
                        .warehouseId(200L)
                        .quantity(2L)
                        .build()))
                .shippingAddress("123 Main St, New York, NY")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryService.reserveStock(any(StockReservationRequest.class))).thenReturn(InventoryResponse.builder().build());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("ORD-2026-000001");

        verify(inventoryService).reserveStock(any(StockReservationRequest.class));
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getSubtotal()).isEqualTo(new BigDecimal("200.00"));
        assertThat(savedOrder.getTax()).isEqualTo(new BigDecimal("16.00")); // 200 * 0.08 = 16.00
        assertThat(savedOrder.getShippingFee()).isEqualTo(new BigDecimal("15.00")); // subtotal < 500
        assertThat(savedOrder.getTotalAmount()).isEqualTo(new BigDecimal("231.00"));

        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getNewStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("createOrder with empty items throws BusinessException")
    void createOrder_emptyItems_throwsException() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of())
                .shippingAddress("123 Main St")
                .build();

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    @DisplayName("createOrder when inventory reservation fails throws InsufficientStockException and rolls back")
    void createOrder_inventoryShortage_throwsException() {
        mockAuthentication(customer);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(OrderItemRequest.builder()
                        .productId(100L)
                        .warehouseId(200L)
                        .quantity(50L)
                        .build()))
                .shippingAddress("123 Main St")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(warehouse));
        when(inventoryService.reserveStock(any(StockReservationRequest.class)))
                .thenThrow(new InsufficientStockException("Insufficient stock"));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Order Status State Machine
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateOrderStatus PENDING -> CONFIRMED succeeds")
    void updateOrderStatus_pendingToConfirmed_success() {
        mockAuthentication(admin);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponse(any())).thenReturn(orderResponse);

        orderService.updateOrderStatus(500L, OrderStatusUpdateRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .reason("Payment verified")
                .build());

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("updateOrderStatus PROCESSING -> SHIPPED commits reserved stock as stock-out")
    void updateOrderStatus_processingToShipped_commitsStock() {
        mockAuthentication(admin);
        order.setStatus(OrderStatus.PROCESSING);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponse(any())).thenReturn(orderResponse);

        orderService.updateOrderStatus(500L, OrderStatusUpdateRequest.builder()
                .status(OrderStatus.SHIPPED)
                .reason("Handed over to carrier")
                .build());

        verify(inventoryService).commitReservation(eq(100L), eq(200L), eq(2L), eq("ORDER"), eq("ORD-2026-000001"), any());
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("updateOrderStatus invalid transition DELIVERED -> CANCELLED throws InvalidOrderStatusTransitionException")
    void updateOrderStatus_invalidTransition_throwsException() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(500L, OrderStatusUpdateRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build()))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("from DELIVERED to CANCELLED");
    }

    // -------------------------------------------------------------------------
    // Order Cancellation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cancelOrder releases inventory reservation for eligible PENDING order")
    void cancelOrder_success_releasesReservation() {
        mockAuthentication(customer);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponse(any())).thenReturn(orderResponse);

        orderService.cancelOrder(500L, CancelOrderRequest.builder().reason("Changed mind").build());

        verify(inventoryService).releaseReservation(any());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelOrder for SHIPPED order throws OrderCancellationException")
    void cancelOrder_shippedOrder_throwsException() {
        mockAuthentication(customer);
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(500L, null))
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("SHIPPED");

        verify(inventoryService, never()).releaseReservation(any());
    }

    // -------------------------------------------------------------------------
    // Customer Ownership Checks
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getOrderById when customer accesses another customer's order throws OrderOwnershipException")
    void getOrderById_otherCustomer_throwsOrderOwnershipException() {
        mockAuthentication(otherCustomer);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById(500L))
                .isInstanceOf(OrderOwnershipException.class)
                .hasMessageContaining("do not have permission");
    }

    @Test
    @DisplayName("getOrderById when owning customer accesses own order succeeds")
    void getOrderById_owningCustomer_success() {
        mockAuthentication(customer);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.getOrderById(500L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(500L);
    }
}
