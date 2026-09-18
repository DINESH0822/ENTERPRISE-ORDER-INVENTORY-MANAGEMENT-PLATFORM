package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.analytics.DashboardAnalyticsResponse;
import com.dinesh.enterprise.entity.Inventory;
import com.dinesh.enterprise.entity.Order;
import com.dinesh.enterprise.entity.Product;
import com.dinesh.enterprise.entity.Warehouse;
import com.dinesh.enterprise.enums.OrderStatus;
import com.dinesh.enterprise.mapper.InventoryMapper;
import com.dinesh.enterprise.repository.InventoryRepository;
import com.dinesh.enterprise.repository.OrderRepository;
import com.dinesh.enterprise.repository.ProductRepository;
import com.dinesh.enterprise.repository.WarehouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService — Dashboard metrics and CSV export tests")
class AnalyticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Spy
    private InventoryMapper inventoryMapper = new InventoryMapper();

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("getDashboardAnalytics should return calculated metrics and status counts")
    void getDashboardAnalytics_Success() {
        when(orderRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("1500.00"));
        when(orderRepository.count()).thenReturn(10L);
        when(productRepository.count()).thenReturn(25L);
        when(warehouseRepository.count()).thenReturn(3L);
        when(inventoryRepository.countLowStock()).thenReturn(2L);
        when(orderRepository.countByStatus(any(OrderStatus.class))).thenReturn(1L);
        when(inventoryRepository.findLowStock(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        DashboardAnalyticsResponse response = analyticsService.getDashboardAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(response.getTotalOrders()).isEqualTo(10L);
        assertThat(response.getTotalProducts()).isEqualTo(25L);
        assertThat(response.getTotalWarehouses()).isEqualTo(3L);
        assertThat(response.getLowStockCount()).isEqualTo(2L);
        assertThat(response.getOrdersByStatus()).containsKeys("PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");
    }

    @Test
    @DisplayName("exportLowStockCsv should return valid CSV byte array")
    void exportLowStockCsv_Success() {
        Product p = Product.builder().id(1L).sku("PROD-001").name("Widget A").build();
        Warehouse w = Warehouse.builder().id(1L).code("WH-01").name("Main Warehouse").build();
        Inventory inv = Inventory.builder()
                .id(100L)
                .product(p)
                .warehouse(w)
                .quantity(10L)
                .reservedQuantity(2L)
                .reorderLevel(15L)
                .build();

        when(inventoryRepository.findAllLowStockForExport()).thenReturn(List.of(inv));

        byte[] csvBytes = analyticsService.exportLowStockCsv();
        String csvContent = new String(csvBytes);

        assertThat(csvContent).contains("Inventory ID,Product SKU,Product Name");
        assertThat(csvContent).contains("100,PROD-001,Widget A,WH-01,Main Warehouse,10,2,8,15,true");
    }

    @Test
    @DisplayName("exportOrdersCsv should return formatted order report CSV")
    void exportOrdersCsv_Success() {
        Order order = Order.builder()
                .id(50L)
                .orderNumber("ORD-2026-000001")
                .status(OrderStatus.CONFIRMED)
                .subtotal(new BigDecimal("100.00"))
                .tax(new BigDecimal("8.00"))
                .shippingFee(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("118.00"))
                .build();

        when(orderRepository.findAllForExport()).thenReturn(List.of(order));

        byte[] csvBytes = analyticsService.exportOrdersCsv();
        String csvContent = new String(csvBytes);

        assertThat(csvContent).contains("Order ID,Order Number,Customer Name");
        assertThat(csvContent).contains("50,ORD-2026-000001");
        assertThat(csvContent).contains("CONFIRMED");
    }
}
