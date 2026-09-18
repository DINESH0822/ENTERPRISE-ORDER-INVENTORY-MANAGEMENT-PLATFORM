package com.dinesh.enterprise.dto.analytics;

import com.dinesh.enterprise.dto.inventory.InventoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsResponse {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private Map<String, Long> ordersByStatus;
    private long totalProducts;
    private long totalWarehouses;
    private long lowStockCount;
    private List<InventoryResponse> lowStockPreview;
}
