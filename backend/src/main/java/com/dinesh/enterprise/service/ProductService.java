package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.product.ProductRequest;
import com.dinesh.enterprise.dto.product.ProductResponse;
import com.dinesh.enterprise.entity.Category;
import com.dinesh.enterprise.entity.Product;
import com.dinesh.enterprise.enums.ProductStatus;
import com.dinesh.enterprise.exception.DuplicateResourceException;
import com.dinesh.enterprise.exception.ResourceNotFoundException;
import com.dinesh.enterprise.mapper.ProductMapper;
import com.dinesh.enterprise.repository.CategoryRepository;
import com.dinesh.enterprise.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for product catalog management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    /**
     * Creates a new product. SKU must be globally unique.
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product already exists with SKU: " + request.getSku());
        }

        Category category = resolveCategory(request.getCategoryId());

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .costPrice(request.getCostPrice())
                .status(ProductStatus.ACTIVE)
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        log.info("Created product SKU={} id={}", saved.getSku(), saved.getId());
        return productMapper.toResponse(saved);
    }

    /**
     * Returns a paginated list of products with optional filtering by name, SKU, categoryId and status.
     * Any null parameter is treated as "no filter" for that field.
     * Empty results return an empty page (not an error).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(
            String name,
            String sku,
            Long categoryId,
            ProductStatus status,
            Pageable pageable) {

        // If a categoryId filter is requested, verify the category exists to give a clear 404 instead of empty result
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }

        Page<ProductResponse> page = productRepository
                .searchProducts(name, sku, categoryId, status, pageable)
                .map(productMapper::toResponse);

        return PageResponse.of(page);
    }

    /**
     * Returns a single product by ID.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findProductById(id));
    }

    /**
     * Returns a single product by SKU.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return productMapper.toResponse(product);
    }

    /**
     * Updates an existing product's details.
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductById(id);

        if (!product.getSku().equalsIgnoreCase(request.getSku())
                && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product already exists with SKU: " + request.getSku());
        }

        Category category = resolveCategory(request.getCategoryId());

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCostPrice(request.getCostPrice());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        log.info("Updated product id={}", id);
        return productMapper.toResponse(saved);
    }

    /**
     * Updates a product's status (ACTIVE, INACTIVE, DISCONTINUED).
     */
    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatus newStatus) {
        Product product = findProductById(id);
        product.setStatus(newStatus);
        Product saved = productRepository.save(product);
        log.info("Updated status of product id={} to {}", id, newStatus);
        return productMapper.toResponse(saved);
    }

    /**
     * Hard-deletes a product by ID.
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        productRepository.delete(product);
        log.info("Deleted product id={}", id);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    /**
     * Resolves a category by ID. Returns null if categoryId is null (uncategorised product).
     * Throws ResourceNotFoundException if the category does not exist.
     */
    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + categoryId));
    }
}
