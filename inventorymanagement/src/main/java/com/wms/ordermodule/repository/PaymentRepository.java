package com.wms.ordermodule.repository;

import com.wms.ordermodule.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :beforeDate")
    List<Payment> findStalePendingPayments(@Param("beforeDate") LocalDateTime beforeDate);

    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.retryCount < 3")
    List<Payment> findRetryablePayments();
}