package com.wms.repository;

import com.wms.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByProductId(Long productId);

    Page<InventoryTransaction> findByProductId(Long productId, Pageable pageable);

    List<InventoryTransaction> findByTransactionType(InventoryTransaction.TransactionType transactionType);

    @Query("SELECT t FROM InventoryTransaction t WHERE t.product.id = :productId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<InventoryTransaction> findByProductAndDateRange(
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
