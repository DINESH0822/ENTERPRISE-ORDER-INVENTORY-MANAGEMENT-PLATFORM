package com.dinesh.enterprise.repository;

import com.dinesh.enterprise.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByProductId(Long productId);

    Page<Inventory> findByProductId(Long productId, Pageable pageable);

    List<Inventory> findByWarehouseId(Long warehouseId);

    Page<Inventory> findByWarehouseId(Long warehouseId, Pageable pageable);

    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Query("SELECT COUNT(i) > 0 FROM Inventory i WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId")
    boolean existsByProductIdAndWarehouseId(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    @Query("""
            SELECT i FROM Inventory i
            LEFT JOIN FETCH i.product p
            LEFT JOIN FETCH i.warehouse w
            WHERE (i.quantity - i.reservedQuantity) <= i.reorderLevel
            """)
    Page<Inventory> findLowStock(Pageable pageable);

    @Query("""
            SELECT i FROM Inventory i
            LEFT JOIN FETCH i.product p
            LEFT JOIN FETCH i.warehouse w
            WHERE (:productId IS NULL OR i.product.id = :productId)
              AND (:warehouseId IS NULL OR i.warehouse.id = :warehouseId)
              AND (:lowStock IS NULL OR (:lowStock = true AND (i.quantity - i.reservedQuantity) <= i.reorderLevel))
            """)
    Page<Inventory> searchInventory(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("lowStock") Boolean lowStock,
            Pageable pageable);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= i.reorderLevel")
    long countLowStock();

    @Query("""
            SELECT i FROM Inventory i
            LEFT JOIN FETCH i.product p
            LEFT JOIN FETCH i.warehouse w
            WHERE (i.quantity - i.reservedQuantity) <= i.reorderLevel
            ORDER BY (i.quantity - i.reservedQuantity) ASC
            """)
    List<Inventory> findAllLowStockForExport();

    @Query("""
            SELECT i FROM Inventory i
            LEFT JOIN FETCH i.product p
            LEFT JOIN FETCH i.warehouse w
            ORDER BY i.id ASC
            """)
    List<Inventory> findAllForExport();
}
