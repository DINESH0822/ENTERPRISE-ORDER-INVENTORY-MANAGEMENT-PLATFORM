package com.dinesh.enterprise.controller;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.product.ProductRequest;
import com.dinesh.enterprise.dto.product.ProductResponse;
import com.dinesh.enterprise.dto.product.UpdateProductStatusRequest;
import com.dinesh.enterprise.enums.ProductStatus;
import com.dinesh.enterprise.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for product catalog management.
 *
 * <ul>
 *   <li>POST   /api/v1/products                — create (ADMIN)</li>
 *   <li>GET    /api/v1/products                — search/filter with pagination (public)</li>
 *   <li>GET    /api/v1/products/{id}           — get by ID (public)</li>
 *   <li>GET    /api/v1/products/sku/{sku}      — get by SKU (public)</li>
 *   <li>PUT    /api/v1/products/{id}           — update (ADMIN)</li>
 *   <li>PATCH  /api/v1/products/{id}/status    — status change (ADMIN)</li>
 *   <li>DELETE /api/v1/products/{id}           — delete (ADMIN)</li>
 * </ul>
 *
 * <p>The GET /api/v1/products endpoint supports the following optional query parameters:</p>
 * <ul>
 *   <li>name       — partial, case-insensitive name search</li>
 *   <li>sku        — partial, case-insensitive SKU search</li>
 *   <li>categoryId — filter by category</li>
 *   <li>status     — filter by product status (ACTIVE, INACTIVE, DISCONTINUED)</li>
 *   <li>page       — zero-based page index (default 0)</li>
 *   <li>size       — page size (default 20, max 100)</li>
 *   <li>sortBy     — field to sort by (default "id")</li>
 *   <li>direction  — "asc" or "desc" (default "asc")</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    /**
     * Unified list + search endpoint. All filter parameters are optional.
     * Returns an empty page (not an error) when no products match.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        // Cap page size to prevent abuse
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, sort);

        return ResponseEntity.ok(
                productService.searchProducts(name, sku, categoryId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        return ResponseEntity.ok(productService.updateProductStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
