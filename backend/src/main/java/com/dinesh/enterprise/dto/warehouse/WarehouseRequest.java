package com.dinesh.enterprise.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRequest {

    @NotBlank(message = "Warehouse code is required")
    @Size(max = 50, message = "Warehouse code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 150, message = "Warehouse name cannot exceed 150 characters")
    private String name;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    @Size(max = 30, message = "Postal code cannot exceed 30 characters")
    private String postalCode;
}
