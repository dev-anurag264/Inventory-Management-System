package com.wms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions", indexes = {
        @Index(name = "idx_product_id", columnList = "product_id"),
        @Index(name = "idx_transaction_date", columnList = "transaction_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "previous_quantity", nullable = false)
    private Integer previousQuantity;

    @Column(name = "new_quantity", nullable = false)
    private Integer newQuantity;

    @Column(length = 500)
    private String notes;

    @Column(name = "reference_id", length = 100)
    private String referenceId; // Order ID, Purchase Order ID, etc.

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "transaction_date", updatable = false)
    private LocalDateTime transactionDate;


    public enum TransactionType {
        STOCK_IN,           // Receiving new stock
        STOCK_OUT,          // Fulfilling orders
        ADJUSTMENT,         // Manual adjustments
        RETURN,             // Customer returns
        DAMAGE,             // Damaged goods
        TRANSFER,           // Warehouse transfers
        RESERVE,            // Reserve for pending orders
        RELEASE_RESERVATION // Release reserved stock
    }

}
