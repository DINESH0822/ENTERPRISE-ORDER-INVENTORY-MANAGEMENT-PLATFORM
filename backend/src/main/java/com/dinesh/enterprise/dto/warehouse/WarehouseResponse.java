package com.dinesh.enterprise.dto.warehouse;

import com.dinesh.enterprise.enums.WarehouseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {

    private Long id;
    private String code;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private WarehouseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
