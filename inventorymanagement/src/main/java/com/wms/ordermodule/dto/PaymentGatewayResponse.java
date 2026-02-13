package com.wms.ordermodule.dto;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class PaymentGatewayResponse {
    private boolean success;
    private String transactionId;
    private String failureReason;
    private String rawResponse;
}
