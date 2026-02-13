package com.wms.ordermodule.controller;

import com.wms.ordermodule.dto.CreateOrderRequest;
import com.wms.ordermodule.dto.OrderResponse;
import com.wms.ordermodule.service.OrderService;
import com.wms.ordermodule.service.OrderServiceImpl;
import com.wms.ordermodule.service.PaymentService;
import com.wms.ordermodule.service.PaymentServiceImpl;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@CrossOrigin(origins = "*")
@Slf4j
public class OrderController {
    @Autowired
    private OrderServiceImpl orderService;
    @Autowired
    private PaymentServiceImpl paymentService;

    @PostMapping("/createOrder")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /api/orders - Creating order for customer: {}", request.getCustomerEmail());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
