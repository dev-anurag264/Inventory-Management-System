package com.wms.ordermodule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutOfStockItem {
    private String productSku;
    private String productName;
    private Integer requestedQuantity;
    private Integer availableQuantity;
}