package com.wms.ordermodule.service;

import com.wms.ordermodule.dto.PaymentGatewayResponse;
import com.wms.ordermodule.dto.PaymentRequest;
import com.wms.ordermodule.dto.PaymentResponse;
import com.wms.ordermodule.entity.Order;
import com.wms.ordermodule.entity.Payment;
import com.wms.ordermodule.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentGatewayServiceImpl paymentGatewayService;

    public PaymentResponse processPayment(Order order, PaymentRequest request) {
        log.info("Processing payment for order: {}", order.getOrderNumber());

        // Create payment record
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency("INR");
        payment.setPaymentMethod(Payment.PaymentMethod.valueOf(request.getPaymentMethod()));
        payment.setStatus(Payment.PaymentStatus.PROCESSING);

        Payment savedPayment = paymentRepository.save(payment);

        PaymentGatewayResponse gatewayResponse =
                paymentGatewayService.processPayment(request, order.getTotalAmount());

        if (gatewayResponse.isSuccess()) {
            // Payment successful
            savedPayment.setStatus(Payment.PaymentStatus.COMPLETED);
            savedPayment.setTransactionId(gatewayResponse.getTransactionId());
            savedPayment.setProviderResponse(gatewayResponse.getRawResponse());

            log.info("Payment successful for order: {}, transaction: {}",
                    order.getOrderNumber(), gatewayResponse.getTransactionId());
        } else {
            // Payment failed
            savedPayment.setStatus(Payment.PaymentStatus.FAILED);
            savedPayment.setFailureReason(gatewayResponse.getFailureReason());
            savedPayment.setRetryCount(savedPayment.getRetryCount() + 1);

            log.error("Payment failed for order: {}, reason: {}",
                    order.getOrderNumber(), gatewayResponse.getFailureReason());

            throw new RuntimeException(gatewayResponse.getFailureReason());
        }
        return mapToResponse(savedPayment);
    }
    public PaymentResponse getPaymentByOrderNumber(String orderNumber) {
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder().getOrderNumber().equals(orderNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setTransactionId(payment.getTransactionId());
        response.setOrderNumber(payment.getOrder().getOrderNumber());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setPaymentMethod(payment.getPaymentMethod().toString());
        response.setStatus(payment.getStatus().toString());
        response.setFailureReason(payment.getFailureReason());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}
