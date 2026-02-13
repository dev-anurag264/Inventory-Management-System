package com.wms.dto;

import java.time.LocalDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryTransactionResponse {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private String transactionType;
    private Integer quantity;
    private Integer previousQuantity;
    private Integer newQuantity;
    private String notes;
    private String referenceId;
    private String performedBy;
    private LocalDateTime transactionDate;
}
