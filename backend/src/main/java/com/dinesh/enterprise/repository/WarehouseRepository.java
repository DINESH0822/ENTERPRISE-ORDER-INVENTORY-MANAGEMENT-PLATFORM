package com.dinesh.enterprise.repository;

import com.dinesh.enterprise.entity.Warehouse;
import com.dinesh.enterprise.enums.WarehouseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    @Query("SELECT w FROM Warehouse w WHERE w.code = :code")
    Optional<Warehouse> findByCode(@Param("code") String code);

    @Query("SELECT COUNT(w) > 0 FROM Warehouse w WHERE w.code = :code")
    boolean existsByCode(@Param("code") String code);

    List<Warehouse> findByStatus(WarehouseStatus status);

    Page<Warehouse> findByStatus(WarehouseStatus status, Pageable pageable);
}
