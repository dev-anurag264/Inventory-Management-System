package com.wms.repository;
import com.wms.entity.StockAlert;
import com.wms.entity.StockAlert.AlertStatus;
import com.wms.entity.StockAlert.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findByStatus(AlertStatus status);

    List<StockAlert> findByProductId(Long productId);

    @Query("SELECT a FROM StockAlert a WHERE a.product.id = :productId AND a.alertType = :alertType AND a.status = 'ACTIVE'")
    Optional<StockAlert> findActiveAlertByProductAndType(Long productId, AlertType alertType);

    @Query("SELECT a FROM StockAlert a WHERE a.status = 'ACTIVE' ORDER BY a.createdAt DESC")
    List<StockAlert> findAllActiveAlerts();
}
