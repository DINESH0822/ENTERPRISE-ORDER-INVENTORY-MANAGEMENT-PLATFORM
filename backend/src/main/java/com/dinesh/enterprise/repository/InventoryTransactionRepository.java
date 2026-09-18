package com.dinesh.enterprise.repository;

import com.dinesh.enterprise.entity.InventoryTransaction;
import com.dinesh.enterprise.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByInventoryId(Long inventoryId);

    Page<InventoryTransaction> findByInventoryId(Long inventoryId, Pageable pageable);

    List<InventoryTransaction> findByType(InventoryTransactionType type);

    Page<InventoryTransaction> findByType(InventoryTransactionType type, Pageable pageable);

    @Query("""
            SELECT tx FROM InventoryTransaction tx
            JOIN FETCH tx.inventory inv
            JOIN FETCH inv.product p
            JOIN FETCH inv.warehouse w
            WHERE (:inventoryId IS NULL OR inv.id = :inventoryId)
              AND (:type IS NULL OR tx.type = :type)
            """)
    Page<InventoryTransaction> searchTransactions(
            @Param("inventoryId") Long inventoryId,
            @Param("type") InventoryTransactionType type,
            Pageable pageable);
}
