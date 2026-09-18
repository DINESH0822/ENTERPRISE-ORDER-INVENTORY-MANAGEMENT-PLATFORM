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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Warehouse already exists with code: " + request.getCode());
        }

        Warehouse warehouse = Warehouse.builder()
                .code(request.getCode())
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .status(WarehouseStatus.ACTIVE)
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Created warehouse code={} id={}", saved.getCode(), saved.getId());
        return warehouseMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        return warehouseMapper.toResponse(findWarehouseById(id));
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseByCode(String code) {
        Warehouse warehouse = warehouseRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with code: " + code));
        return warehouseMapper.toResponse(warehouse);
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseResponse> getWarehouses(WarehouseStatus status, Pageable pageable) {
        Page<WarehouseResponse> page;
        if (status != null) {
            page = warehouseRepository.findByStatus(status, pageable).map(warehouseMapper::toResponse);
        } else {
            page = warehouseRepository.findAll(pageable).map(warehouseMapper::toResponse);
        }
        return PageResponse.of(page);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        Warehouse warehouse = findWarehouseById(id);

        if (!warehouse.getCode().equalsIgnoreCase(request.getCode())
                && warehouseRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Warehouse already exists with code: " + request.getCode());
        }

        warehouse.setCode(request.getCode());
        warehouse.setName(request.getName());
        warehouse.setAddress(request.getAddress());
        warehouse.setCity(request.getCity());
        warehouse.setState(request.getState());
        warehouse.setCountry(request.getCountry());
        warehouse.setPostalCode(request.getPostalCode());

        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Updated warehouse id={}", id);
        return warehouseMapper.toResponse(saved);
    }

    @Transactional
    public WarehouseResponse updateWarehouseStatus(Long id, WarehouseStatus newStatus) {
        Warehouse warehouse = findWarehouseById(id);
        warehouse.setStatus(newStatus);
        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Updated status of warehouse id={} to {}", id, newStatus);
        return warehouseMapper.toResponse(saved);
    }

    private Warehouse findWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
    }
}
