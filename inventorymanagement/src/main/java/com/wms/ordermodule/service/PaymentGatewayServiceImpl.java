package com.wms.ordermodule.service;

import com.wms.ordermodule.dto.PaymentGatewayResponse;
import com.wms.ordermodule.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class PaymentGatewayServiceImpl {

    public PaymentGatewayResponse processPayment(PaymentRequest request,
                                                 BigDecimal amount){
      log.info("Transaction for {} currently in process", amount);
      try{
          Thread.sleep(1000);

          //90% success rate
          boolean success = Math.random() > 0.1;
          if(success){
              String txnId = "TXN-"+ UUID.randomUUID().toString();
              log.info("Payment success with transaction id {}", txnId);

              return PaymentGatewayResponse.builder().
                      success(true).transactionId(txnId).
                      rawResponse("{\"status\":\"success\",\"transaction_id\":\"" + txnId + "\"}")
                      .build();

          }else{
              return PaymentGatewayResponse.builder().
                      success(false).failureReason("Insufficient Funds / Card Not Acceptable").
                      rawResponse("{\"status\":\"failed\"").
                      build();
          }

      }catch (InterruptedException e){
        Thread.currentThread().interrupt();
        return PaymentGatewayResponse.builder().success(false).failureReason("Payment Gateway Timeout").
                build();
      }
    }
    public PaymentGatewayResponse refundPayment(String transactionId, BigDecimal amount) {
        log.info("Processing refund for transaction: {}, amount: {}", transactionId, amount);

        try {
            Thread.sleep(500);

            // Simulate refund - always successful in demo
            String refundId = "REFUND-" + UUID.randomUUID().toString();

            return PaymentGatewayResponse.builder()
                    .success(true)
                    .transactionId(refundId)
                    .rawResponse("{\"status\":\"refunded\",\"refund_id\":\"" + refundId + "\"}")
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .failureReason("Refund gateway timeout")
                    .build();
        }
    }

}


// PaymentGatewayResponse.java
