package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.category.CategoryRequest;
import com.dinesh.enterprise.dto.category.CategoryResponse;
import com.dinesh.enterprise.entity.Category;
import com.dinesh.enterprise.exception.DuplicateResourceException;
import com.dinesh.enterprise.exception.ResourceNotFoundException;
import com.dinesh.enterprise.mapper.CategoryMapper;
import com.dinesh.enterprise.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for product category management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Creates a new category. Name must be unique.
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.getName());
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Created category: {}", saved.getName());
        return categoryMapper.toResponse(saved);
    }

    /**
     * Returns all active categories (public endpoint).
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByActiveTrue()
                .stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all categories including inactive ones (ADMIN).
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single category by ID.
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(findCategoryById(id));
    }

    /**
     * Updates an existing category's name and/or description.
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategoryById(id);

        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.getName());
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category saved = categoryRepository.save(category);
        log.info("Updated category id={}: name={}", id, saved.getName());
        return categoryMapper.toResponse(saved);
    }

    /**
     * Soft-deletes a category by setting active = false.
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryById(id);
        category.setActive(false);
        categoryRepository.save(category);
        log.info("Deactivated category id={}", id);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
}
