package com.wms.service;

import com.wms.dto.StockAlertResponse;
import com.wms.entity.Product;
import com.wms.entity.StockAlert;
import com.wms.repository.StockAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StockAlertService {
    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Async("taskExecutor")
    @Transactional
    public void checkAndCreateAlert(Product product) {
        log.info("Checking for product:{}", product.getSku());

        try {

            //Products out of stock
            if(product.isOutOfStock()){
                createOrUpdateAlert(product,
                        StockAlert.AlertType.OUT_OF_STOCK,
                        String.format("Product %s (%s) is OUT OF STOCK! Current: %d, Reserved: %d",
                                product.getName(), product.getSku(),
                                product.getQty(), product.getReservedQty()),
                                product.getQty(),0);


            }else{
                resolveAlert(product, StockAlert.AlertType.OUT_OF_STOCK);
            }
            //Product with less stock
            if(product.isLowStock() && !product.isOutOfStock()){
                createOrUpdateAlert(
                        product,
                        StockAlert.AlertType.LOW_STOCK,
                        String.format(
                                "Product %s (%s) is running LOW ON STOCKS Current: %d, Reserved: %d",
                                product.getName(), product.getSku(),
                                product.getQty(), product.getReorderLevel()),
                        product.getAvailableQuantity(), product.getReorderLevel());

            }else{
                resolveAlert(product, StockAlert.AlertType.LOW_STOCK);
            }

            //Check for last restocked level
            if (product.getAvailableQuantity() <= product.getReorderLevel() &&
                    product.getAvailableQuantity() > 0) {
                createOrUpdateAlert(product, StockAlert.AlertType.REORDER_POINT_REACHED,
                        String.format("Product %s (%s) has reached REORDER POINT. Available: %d, Reorder Level: %d",
                                product.getName(), product.getSku(),
                                product.getAvailableQuantity(), product.getReorderLevel()),
                        product.getAvailableQuantity(), product.getReorderLevel());
            }

            // Check for overstock
            if (product.getQty() > product.getMaxStockLevel()) {
                createOrUpdateAlert(product, StockAlert.AlertType.OVERSTOCK,
                        String.format("Product %s (%s) is OVERSTOCKED. Current: %d, Max Level: %d",
                                product.getName(), product.getSku(),
                                product.getQty(), product.getMaxStockLevel()),
                        product.getQty(), product.getMaxStockLevel());
            } else {
                resolveAlert(product, StockAlert.AlertType.OVERSTOCK);
            }

            log.info("Alert check completed for product: {}", product.getSku());

        } catch (Exception e) {
            log.error("Error checking alerts for product {}: {}", product.getSku(), e.getMessage(), e);
        }

    }

    public void createOrUpdateAlert(Product product, StockAlert.AlertType alertType,
                                    String message, Integer currQty, Integer thresholdQty) {

        Optional<StockAlert> existingAlert = stockAlertRepository
                .findActiveAlertByProductAndType(product.getId(), alertType);

        if (existingAlert.isEmpty()) {
            StockAlert alert = new StockAlert();
            alert.setProduct(product);
            alert.setAlertType(alertType);
            alert.setMessage(message);
            alert.setCurrentQuantity(currQty);
            alert.setThresholdQuantity(thresholdQty);
            alert.setStatus(StockAlert.AlertStatus.ACTIVE);
            stockAlertRepository.save(alert);
            log.info("Resolved {} Alert for product {}", alertType, product.getSku());

            //Send notification
            sendNotification(alert);
        }
    }

    @Async("taskExecutor")
    public void sendNotification(StockAlert alert) {
        // Simulate sending email/SMS/webhook notification
        log.info("NOTIFICATION SENT - Type: {}, Product: {}, Message: {}",
                alert.getAlertType(), alert.getProduct().getSku(), alert.getMessage());
    }
    private void resolveAlert(Product product, StockAlert.AlertType alertType) {
        Optional<StockAlert> existingAlert = stockAlertRepository
                .findActiveAlertByProductAndType(product.getId(), alertType);

        existingAlert.ifPresent(alert -> {
            alert.setStatus(StockAlert.AlertStatus.RESOLVED);
            alert.setResolvedAt(LocalDateTime.now());
            stockAlertRepository.save(alert);
            log.info("Resolved {} alert for product: {}", alertType, product.getSku());
        });
    }

    public List<StockAlertResponse> getActiveAlerts(){
        return stockAlertRepository.findAllActiveAlerts().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<StockAlertResponse> getAlertsByProduct(Long productId){
        return stockAlertRepository.findByProductId(productId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    @Transactional
    public void acknowledgeAlerts(Long alertId, String acknowledgedBy){
        StockAlert alert = stockAlertRepository.findById(alertId).orElseThrow(
                () -> new RuntimeException("Alert Not Found")
        );

        alert.setStatus(StockAlert.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(acknowledgedBy);
    }

    @Transactional
    public void dismissAlerts(Long alertId){
        StockAlert alert = stockAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        alert.setStatus(StockAlert.AlertStatus.DISMISSED);
        stockAlertRepository.save(alert);
        log.info("Alert {} dismissed", alertId);
    }


    private StockAlertResponse mapToResponse(StockAlert alert){
        StockAlertResponse response = new StockAlertResponse();
        response.setId(alert.getId());
        response.setProductId(alert.getProduct().getId());
        response.setProductSku(alert.getProduct().getSku());
        response.setProductName(alert.getProduct().getName());
        response.setAlertType(alert.getAlertType().toString());
        response.setMessage(alert.getMessage());
        response.setCurrentQuantity(alert.getCurrentQuantity());
        response.setThresholdQuantity(alert.getThresholdQuantity());
        response.setStatus(alert.getStatus().toString());
        response.setCreatedAt(alert.getCreatedAt());
        response.setAcknowledgedAt(alert.getAcknowledgedAt());
        response.setAcknowledgedBy(alert.getAcknowledgedBy());
        return response;
    }


}
