package com.dinesh.enterprise.repository;

import com.dinesh.enterprise.entity.Product;
import com.dinesh.enterprise.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.sku = :sku")
    boolean existsBySku(@Param("sku") String sku);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByStatus(ProductStatus status);

    /**
     * Dynamic search with all optional filters. Null parameters are ignored.
     * Uses LEFT JOIN FETCH on category to avoid N+1 when rendering categoryName.
     */
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN p.category c
            WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:sku IS NULL OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :sku, '%')))
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:status IS NULL OR p.status = :status)
            """)
    Page<Product> searchProducts(
            @Param("name") String name,
            @Param("sku") String sku,
            @Param("categoryId") Long categoryId,
            @Param("status") ProductStatus status,
            Pageable pageable);
}
