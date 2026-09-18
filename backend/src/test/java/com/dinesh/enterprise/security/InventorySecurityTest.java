package com.dinesh.enterprise.security;

import com.dinesh.enterprise.dto.inventory.StockInRequest;
import com.dinesh.enterprise.dto.warehouse.WarehouseRequest;
import com.dinesh.enterprise.service.InventoryService;
import com.dinesh.enterprise.service.WarehouseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Inventory Security & RBAC — role restriction integration tests")
class InventorySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseService warehouseService;

    @MockBean
    private InventoryService inventoryService;

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("CUSTOMER role is forbidden (403) from creating a warehouse")
    void customerRole_createWarehouse_forbidden() throws Exception {
        WarehouseRequest request = WarehouseRequest.builder()
                .code("WH-FORBIDDEN")
                .name("Customer Hub")
                .build();

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("CUSTOMER role is forbidden (403) from performing stock-in operations")
    void customerRole_stockIn_forbidden() throws Exception {
        StockInRequest request = StockInRequest.builder()
                .productId(1L)
                .warehouseId(1L)
                .quantity(100L)
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request is unauthorized (401)")
    void unauthenticatedRequest_unauthorized() throws Exception {
        StockInRequest request = StockInRequest.builder()
                .productId(1L)
                .warehouseId(1L)
                .quantity(100L)
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
