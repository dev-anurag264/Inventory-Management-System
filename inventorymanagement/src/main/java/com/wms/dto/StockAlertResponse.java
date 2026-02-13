package com.wms.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertResponse {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private String alertType;
    private String message;
    private Integer currentQuantity;
    private Integer thresholdQuantity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
}
