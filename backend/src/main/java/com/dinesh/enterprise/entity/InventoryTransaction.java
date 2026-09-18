package com.dinesh.enterprise.entity;

import com.dinesh.enterprise.enums.InventoryTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "inventory_transactions",
    indexes = {
        @Index(name = "idx_inv_tx_inventory_id", columnList = "inventory_id"),
        @Index(name = "idx_inv_tx_type", columnList = "transaction_type"),
        @Index(name = "idx_inv_tx_reference", columnList = "reference_type, reference_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "inventory_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_inventory_tx_inventory_id")
    )
    private Inventory inventory;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private InventoryTransactionType type;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Size(max = 50)
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Size(max = 100)
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "previous_quantity")
    private Long previousQuantity;

    @Column(name = "new_quantity")
    private Long newQuantity;

    @Size(max = 100)
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
