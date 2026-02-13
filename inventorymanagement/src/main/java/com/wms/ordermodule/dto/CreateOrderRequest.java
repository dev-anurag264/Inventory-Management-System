package com.wms.ordermodule.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotBlank(message = "Customer name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String customerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;

    @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "Invalid phone format")
    private String customerPhone;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String shippingAddress;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Payment method is required")
    private String paymentMethod; // CREDIT_CARD, PAYPAL, etc.

    private String notes;

    @NotNull(message = "Shipping cost is required")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal shippingCost;
}
