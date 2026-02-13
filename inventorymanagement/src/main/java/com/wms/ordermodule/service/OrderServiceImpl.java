package com.wms.ordermodule.service;

import com.wms.entity.InventoryTransaction;
import com.wms.entity.Product;
import com.wms.exception.InsufficientStockException;
import com.wms.exception.ProductNotFoundException;
import com.wms.ordermodule.dto.*;
import com.wms.ordermodule.entity.Order;
import com.wms.ordermodule.entity.OrderItem;
import com.wms.ordermodule.repository.OrderRepository;
import com.wms.repository.InventoryTransactionRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.StockAlertRepository;
import com.wms.service.StockAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentServiceImpl paymentService;
    @Autowired
    private StockAlertService alertService;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value={"products","productStats"}, allEntries = true)
    public OrderResponse createOrder(CreateOrderRequest request){
        OrderValidationResult validationResult =validateOrderInventory(request);
        if(!validationResult.isValid()){
            log.error("Order Validation failed for Order {}", validationResult.getErrors());
                throw new RuntimeException();
        }

        //Create Order entity
        Order order = buildOrder(request);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);

        Order save = orderRepository.save(order);
        log.info("Order created {}, status PENDING", order.getOrderNumber());

        try{
            reserveInventoryForOrder(order);
            log.info("Reserving stock for order {}",  order.getOrderNumber());
            save.setStatus(Order.OrderStatus.PAYMENT_PENDING);
        }catch (InsufficientStockException e){
            save.setStatus(Order.OrderStatus.CANCELLED);
            save.setPaymentFailureReason("Insufficient Stocks");
            orderRepository.save(order);
            throw e;
        }

        try {
            PaymentRequest paymentReq = new PaymentRequest();
            paymentReq.setOrderNumber(save.getOrderNumber());
            paymentReq.setPaymentMethod(request.getPaymentMethod());

            PaymentResponse paymentResponse = paymentService.processPayment(save, paymentReq);

            //For successful payment
            save.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            save.setPaymentTransactionId(paymentResponse.getTransactionId());
            save.setPaymentDate(LocalDateTime.now());
            save.setStatus(Order.OrderStatus.CONFIRMED);

            log.info("Payment successful for order: {}, transaction: {}",
                    save.getOrderNumber(), paymentResponse.getTransactionId());

            triggerFulfillmentProcess(save);
        }catch (Exception e){
            //payment failed - roll back
            log.error("Payment failed for order: {}", save.getOrderNumber(), e);

            save.setPaymentStatus(Order.PaymentStatus.FAILED);
            save.setPaymentFailureReason(e.getMessage());
            save.setStatus(Order.OrderStatus.PAYMENT_FAILED);

            // Release reserved inventory
            releaseInventoryReservations(save);

            orderRepository.save(save);
            throw new RuntimeException("Payment Failed");
        }
        Order finalOrder = orderRepository.save(save);

        //send alert to dashboard


        return mapToResponse(finalOrder);
    }

    public OrderValidationResult validateOrderInventory(CreateOrderRequest request) {
        List<String> errors = new ArrayList<>();
        List<OutOfStockItem> outOfStockItems = new ArrayList<>();

        for (OrderItemRequest item : request.getItems()) {
            Product product = productRepository.findBySku(item.getProductSku())
                    .orElse(null);

            if (product == null) {
                errors.add("Product not found: " + item.getProductSku());
                continue;
            }

            if (product.getStatus() == Product.ProductStatus.DISCONTINUED) {
                errors.add("Product discontinued: " + item.getProductSku());
                continue;
            }

            int available = product.getAvailableQuantity();
            if (available < item.getQuantity()) {
                errors.add(String.format("Insufficient stock for %s: requested %d, available %d",
                        product.getName(), item.getQuantity(), available));

                outOfStockItems.add(new OutOfStockItem(
                        product.getSku(),
                        product.getName(),
                        item.getQuantity(),
                        available
                ));
            }
        }

        return new OrderValidationResult(errors.isEmpty(), errors, outOfStockItems);
    }

    public Order buildOrder(CreateOrderRequest request){
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCost(request.getShippingCost());
        order.setNotes(request.getNotes());
        order.setPaymentMethod(request.getPaymentMethod());

        //Build order for items - List
        for(OrderItemRequest items : request.getItems()){
            Product product = productRepository.findBySku(items.getProductSku()).
                    orElseThrow(()-> new RuntimeException("Product not found"));
            OrderItem item = new OrderItem();
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setProduct(product);
            item.setQuantity(items.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.calculateTotalPrice();

            order.addOrderItem(item);
        }
        //Calculate total\
        order.calculateTotals();

        return order;

    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    @Transactional
    public void reserveInventoryForOrder(Order order){
        log.info("Reserving stocks for order no. {}", order.getOrderNumber());
        for(OrderItem i: order.getOrderItems()){
            Product product = productRepository.findBySkuWithLock(i.getProductSku()).orElseThrow(() -> new ProductNotFoundException(i.getProductSku()));
            // double check
            if (product.getAvailableQuantity() < i.getQuantity()) {
                throw new InsufficientStockException(
                        String.format("Insufficient stock for %s during reservation. Available: %d, Requested: %d",
                                product.getName(), product.getAvailableQuantity(), i.getQuantity())
                );
            }

            //Reserve stock
            int previousReserved = product.getReservedQty();
            product.setReservedQty(previousReserved + i.getQuantity());
            productRepository.save(product);

            //record reservation transaction
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProduct(product);
            transaction.setTransactionType(InventoryTransaction.TransactionType.RESERVE);
            transaction.setQuantity(i.getQuantity());
            transaction.setPreviousQuantity(previousReserved);
            transaction.setNewQuantity(product.getReservedQty());
            transaction.setReferenceId(order.getOrderNumber());
            transaction.setPerformedBy("system");
            transaction.setNotes("Reservation done for Order, Final Stocks will reflect after successful payment" + order.getOrderNumber());

            InventoryTransaction saved= inventoryTransactionRepository.save(transaction);

            i.setReservationId(saved.getId());
            i.setStatus(OrderItem.ItemStatus.RESERVED);
            log.info("Reserved {} units of {} for order {}",
                    i.getQuantity(), product.getSku(), order.getOrderNumber());
        }

    }

    @Transactional
    private void releaseInventoryReservations(Order order) {
        log.info("Releasing inventory reservations for order: {}", order.getOrderNumber());

        for (OrderItem item : order.getOrderItems()) {
            if (item.getStatus() == OrderItem.ItemStatus.RESERVED) {
                Product product = productRepository.findBySkuWithLock(item.getProductSku())
                        .orElseThrow(() -> new ProductNotFoundException("Product not found"));

                int previousReserved = product.getReservedQty();
                product.setReservedQty(Math.max(0, previousReserved - item.getQuantity()));
                productRepository.save(product);

                // Record release transaction
                InventoryTransaction release = new InventoryTransaction();
                release.setProduct(product);
                release.setTransactionType(InventoryTransaction.TransactionType.RELEASE_RESERVATION);
                release.setQuantity(item.getQuantity());
                release.setPreviousQuantity(previousReserved);
                release.setNewQuantity(product.getReservedQty());
                release.setReferenceId(order.getOrderNumber());
                release.setPerformedBy("SYSTEM");
                release.setNotes("Released due to order failure/cancellation");
                inventoryTransactionRepository.save(release);

                item.setStatus(OrderItem.ItemStatus.CANCELLED);

                log.info("Released {} units of {} from order {}",
                        item.getQuantity(), product.getSku(), order.getOrderNumber());
            }
        }
    }

    @Async("taskExecutor")
    private void triggerFulfillmentProcess(Order order) {
        try {
            // Simulate warehouse notification
            Thread.sleep(2000);
            log.info("Warehouse notified for order: {}", order.getOrderNumber());

            // In production: Send to warehouse management system, print packing slip, etc.

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Fulfillment notification interrupted", e);
        }
    }
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setShippingAddress(order.getShippingAddress());
        response.setSubtotal(order.getSubtotal());
        response.setTax(order.getTax());
        response.setShippingCost(order.getShippingCost());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus().toString());
        response.setPaymentStatus(order.getPaymentStatus().toString());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentTransactionId(order.getPaymentTransactionId());
        response.setPaymentDate(order.getPaymentDate());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        response.setItems(order.getOrderItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList()));

        return response;
    }
    private OrderItemResponse mapItemToResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductSku(item.getProductSku());
        response.setProductName(item.getProductName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setTotalPrice(item.getTotalPrice());
        response.setStatus(item.getStatus().toString());
        response.setFulfilledQuantity(item.getFulfilledQuantity());
        return response;
    }

    private OrderSummaryResponse mapToSummary(Order order) {
        return new OrderSummaryResponse(
                order.getOrderNumber(),
                order.getStatus().toString(),
                order.getTotalAmount(),
                order.getOrderItems().size(),
                order.getCreatedAt()
        );
    }
}
