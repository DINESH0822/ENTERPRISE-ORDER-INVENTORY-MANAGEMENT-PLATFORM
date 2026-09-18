package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.analytics.DashboardAnalyticsResponse;
import com.dinesh.enterprise.dto.inventory.InventoryResponse;
import com.dinesh.enterprise.entity.Inventory;
import com.dinesh.enterprise.entity.Order;
import com.dinesh.enterprise.enums.OrderStatus;
import com.dinesh.enterprise.mapper.InventoryMapper;
import com.dinesh.enterprise.repository.InventoryRepository;
import com.dinesh.enterprise.repository.OrderRepository;
import com.dinesh.enterprise.repository.ProductRepository;
import com.dinesh.enterprise.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getDashboardAnalytics() {
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        long totalOrders = orderRepository.count();

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status.name(), orderRepository.countByStatus(status));
        }

        long totalProducts = productRepository.count();
        long totalWarehouses = warehouseRepository.count();
        long lowStockCount = inventoryRepository.countLowStock();

        List<InventoryResponse> lowStockPreview = inventoryRepository.findLowStock(PageRequest.of(0, 5))
                .getContent()
                .stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());

        return DashboardAnalyticsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .ordersByStatus(ordersByStatus)
                .totalProducts(totalProducts)
                .totalWarehouses(totalWarehouses)
                .lowStockCount(lowStockCount)
                .lowStockPreview(lowStockPreview)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportLowStockCsv() {
        List<Inventory> lowStockItems = inventoryRepository.findAllLowStockForExport();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        writer.println("Inventory ID,Product SKU,Product Name,Warehouse Code,Warehouse Name,Quantity,Reserved Quantity,Available Quantity,Reorder Level,Low Stock");

        for (Inventory i : lowStockItems) {
            InventoryResponse dto = inventoryMapper.toResponse(i);
            writer.printf("%d,%s,%s,%s,%s,%d,%d,%d,%d,%b%n",
                    dto.getId(),
                    escapeCsv(dto.getProductSku()),
                    escapeCsv(dto.getProductName()),
                    escapeCsv(dto.getWarehouseCode()),
                    escapeCsv(dto.getWarehouseName()),
                    dto.getQuantity(),
                    dto.getReservedQuantity(),
                    dto.getAvailableQuantity(),
                    dto.getReorderLevel(),
                    dto.getIsLowStock()
            );
        }
        writer.flush();
        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] exportOrdersCsv() {
        List<Order> orders = orderRepository.findAllForExport();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        writer.println("Order ID,Order Number,Customer Name,Customer Email,Status,Subtotal,Tax,Shipping,Total Amount,Created At");

        for (Order o : orders) {
            String customerName = o.getCustomer() != null ? o.getCustomer().getFirstName() + " " + o.getCustomer().getLastName() : "N/A";
            String customerEmail = o.getCustomer() != null ? o.getCustomer().getEmail() : "N/A";

            writer.printf("%d,%s,%s,%s,%s,%.2f,%.2f,%.2f,%.2f,%s%n",
                    o.getId(),
                    escapeCsv(o.getOrderNumber()),
                    escapeCsv(customerName),
                    escapeCsv(customerEmail),
                    o.getStatus(),
                    o.getSubtotal(),
                    o.getTax(),
                    o.getShippingFee(),
                    o.getTotalAmount(),
                    o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
            );
        }
        writer.flush();
        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] exportInventoryCsv() {
        List<Inventory> items = inventoryRepository.findAllForExport();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        writer.println("Inventory ID,Product SKU,Product Name,Warehouse Code,Warehouse Name,Quantity,Reserved Quantity,Available Quantity,Reorder Level,Is Low Stock");

        for (Inventory i : items) {
            InventoryResponse dto = inventoryMapper.toResponse(i);
            writer.printf("%d,%s,%s,%s,%s,%d,%d,%d,%d,%b%n",
                    dto.getId(),
                    escapeCsv(dto.getProductSku()),
                    escapeCsv(dto.getProductName()),
                    escapeCsv(dto.getWarehouseCode()),
                    escapeCsv(dto.getWarehouseName()),
                    dto.getQuantity(),
                    dto.getReservedQuantity(),
                    dto.getAvailableQuantity(),
                    dto.getReorderLevel(),
                    dto.getIsLowStock()
            );
        }
        writer.flush();
        return out.toByteArray();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
