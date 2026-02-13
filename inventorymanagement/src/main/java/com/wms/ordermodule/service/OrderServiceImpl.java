package com.wms.ordermodule.service;

import com.wms.entity.Product;
import com.wms.ordermodule.dto.*;
import com.wms.ordermodule.entity.Order;
import com.wms.ordermodule.entity.OrderItem;
import com.wms.ordermodule.repository.OrderRepository;
import com.wms.repository.InventoryTransactionRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.StockAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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


        return null;
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
}
