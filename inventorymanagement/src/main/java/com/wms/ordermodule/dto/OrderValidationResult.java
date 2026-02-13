package com.wms.ordermodule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<OutOfStockItem> outOfStockItems;
}