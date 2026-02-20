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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

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
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/orders - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders  = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    //Get order by id - /api/order/{id}
    @PutMapping("/{orderNumber}/fulfill")
    public ResponseEntity<OrderResponse> fulfillOrder(@PathVariable String orderNumber) {
        log.info("PUT /api/orders/{}/fulfill", orderNumber);
        OrderResponse response = orderService.fullfillOrder(orderNumber);
        return ResponseEntity.ok(response);
    }







}
