package com.dinesh.enterprise.mapper;

import com.dinesh.enterprise.dto.product.ProductResponse;
import com.dinesh.enterprise.entity.Product;
import org.springframework.stereotype.Component;

/**
 * Maps Product entity to ProductResponse DTO.
 */
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .costPrice(product.getCostPrice())
                .status(product.getStatus())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
