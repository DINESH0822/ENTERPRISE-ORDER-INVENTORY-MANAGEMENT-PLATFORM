package com.dinesh.enterprise.controller;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.warehouse.WarehouseRequest;
import com.dinesh.enterprise.dto.warehouse.WarehouseResponse;
import com.dinesh.enterprise.dto.warehouse.WarehouseStatusRequest;
import com.dinesh.enterprise.enums.WarehouseStatus;
import com.dinesh.enterprise.service.WarehouseService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private static final int MAX_PAGE_SIZE = 100;

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<WarehouseResponse>> getWarehouses(
            @RequestParam(required = false) WarehouseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, sort);

        return ResponseEntity.ok(warehouseService.getWarehouses(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<WarehouseResponse> getWarehouseByCode(@PathVariable String code) {
        return ResponseEntity.ok(warehouseService.getWarehouseByCode(code));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WarehouseResponse> updateWarehouseStatus(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseStatusRequest request) {
        return ResponseEntity.ok(warehouseService.updateWarehouseStatus(id, request.getStatus()));
    }
}
