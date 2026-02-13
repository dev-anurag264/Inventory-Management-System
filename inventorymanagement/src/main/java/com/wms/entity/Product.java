package com.wms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="products",indexes ={
        @Index(name = "idx_sku", columnList = "sku"),
        @Index(name = "idx_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false)
    private Integer qty = 0;
    @Column(nullable = false, length = 100)
    private String category;
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQty = 0;

    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel = 10;

    @Column(name = "max_stock_level", nullable = false)
    private Integer maxStockLevel = 1000;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "last_restocked")
    private LocalDateTime lastRestocked;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //Optimistic Locking
    @Version
    private Long version;

    public Integer getAvailableQuantity() {
        return qty - reservedQty;
    }

    public boolean isLowStock() {
        return getAvailableQuantity() <= reorderLevel;
    }

    public boolean isOutOfStock() {
        return getAvailableQuantity() <= 0;
    }

    public boolean canFulfillOrder(Integer requestedQuantity) {
        return getAvailableQuantity() >= requestedQuantity;
    }

    public enum ProductStatus {
        ACTIVE, DISCONTINUED, OUT_OF_STOCK, LOW_STOCK
    }
}
