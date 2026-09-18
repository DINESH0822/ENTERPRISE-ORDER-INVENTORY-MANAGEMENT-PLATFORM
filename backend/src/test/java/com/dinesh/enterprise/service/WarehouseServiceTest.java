package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.warehouse.WarehouseRequest;
import com.dinesh.enterprise.dto.warehouse.WarehouseResponse;
import com.dinesh.enterprise.entity.Warehouse;
import com.dinesh.enterprise.enums.WarehouseStatus;
import com.dinesh.enterprise.exception.DuplicateResourceException;
import com.dinesh.enterprise.exception.ResourceNotFoundException;
import com.dinesh.enterprise.mapper.WarehouseMapper;
import com.dinesh.enterprise.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseService — CRUD & business rule unit tests")
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseMapper warehouseMapper;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse warehouse;
    private WarehouseResponse warehouseResponse;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder()
                .id(1L)
                .code("WH-MAIN-01")
                .name("Main Logistics Hub")
                .city("New York")
                .country("USA")
                .status(WarehouseStatus.ACTIVE)
                .build();

        warehouseResponse = WarehouseResponse.builder()
                .id(1L)
                .code("WH-MAIN-01")
                .name("Main Logistics Hub")
                .city("New York")
                .country("USA")
                .status(WarehouseStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("createWarehouse success creates and returns warehouse DTO")
    void createWarehouse_success() {
        WarehouseRequest request = WarehouseRequest.builder()
                .code("WH-MAIN-01")
                .name("Main Logistics Hub")
                .city("New York")
                .country("USA")
                .build();

        when(warehouseRepository.existsByCode("WH-MAIN-01")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);
        when(warehouseMapper.toResponse(warehouse)).thenReturn(warehouseResponse);

        WarehouseResponse result = warehouseService.createWarehouse(request);

        assertThat(result.getCode()).isEqualTo("WH-MAIN-01");
        assertThat(result.getStatus()).isEqualTo(WarehouseStatus.ACTIVE);
    }

    @Test
    @DisplayName("createWarehouse duplicate code throws DuplicateResourceException")
    void createWarehouse_duplicateCode_throwsConflict() {
        WarehouseRequest request = WarehouseRequest.builder()
                .code("WH-MAIN-01")
                .name("Duplicate Hub")
                .build();

        when(warehouseRepository.existsByCode("WH-MAIN-01")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.createWarehouse(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("WH-MAIN-01");

        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("getWarehouseById valid ID returns warehouse DTO")
    void getWarehouseById_validId_returnsWarehouse() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseMapper.toResponse(warehouse)).thenReturn(warehouseResponse);

        WarehouseResponse result = warehouseService.getWarehouseById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getWarehouseById missing ID throws ResourceNotFoundException")
    void getWarehouseById_missingId_throwsNotFound() {
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getWarehouseById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateWarehouseStatus updates warehouse status to INACTIVE")
    void updateWarehouseStatus_deactivatesWarehouse() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);
        when(warehouseMapper.toResponse(warehouse)).thenReturn(warehouseResponse);

        WarehouseResponse result = warehouseService.updateWarehouseStatus(1L, WarehouseStatus.INACTIVE);

        assertThat(result).isNotNull();
        verify(warehouseRepository).save(warehouse);
        assertThat(warehouse.getStatus()).isEqualTo(WarehouseStatus.INACTIVE);
    }

    @Test
    @DisplayName("getWarehouses with pagination returns page of warehouses")
    void getWarehouses_paginated() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Warehouse> page = new PageImpl<>(List.of(warehouse), pageable, 1);

        when(warehouseRepository.findAll(pageable)).thenReturn(page);
        when(warehouseMapper.toResponse(warehouse)).thenReturn(warehouseResponse);

        PageResponse<WarehouseResponse> result = warehouseService.getWarehouses(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
