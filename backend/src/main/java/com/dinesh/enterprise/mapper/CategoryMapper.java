package com.dinesh.enterprise.mapper;

import com.dinesh.enterprise.dto.category.CategoryResponse;
import com.dinesh.enterprise.entity.Category;
import org.springframework.stereotype.Component;

/**
 * Maps Category entity to CategoryResponse DTO.
 */
@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .productCount(category.getProducts() != null ? category.getProducts().size() : 0)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
