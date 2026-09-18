package com.dinesh.enterprise.repository;

import com.dinesh.enterprise.entity.Order;
import com.dinesh.enterprise.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.orderNumber = :orderNumber")
    boolean existsByOrderNumber(@Param("orderNumber") String orderNumber);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.customer c
            WHERE (:status IS NULL OR o.status = :status)
              AND (:customerId IS NULL OR c.id = :customerId)
            """)
    Page<Order> searchOrders(
            @Param("status") OrderStatus status,
            @Param("customerId") Long customerId,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> com.dinesh.enterprise.enums.OrderStatus.CANCELLED")
    BigDecimal calculateTotalRevenue();

    long countByStatus(OrderStatus status);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.customer c
            ORDER BY o.createdAt DESC
            """)
    List<Order> findAllForExport();
}
