package com.wms.controller;

import com.wms.dto.StockAlertResponse;
import com.wms.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AlertController {
    @Autowired
    private StockAlertService stockAlertService;

    //getActiveAlerts

    @GetMapping
    public ResponseEntity<List<StockAlertResponse>> getActiveAlerts() {
        log.info("GET /api/alerts - Fetching active alerts");
        List<StockAlertResponse> alerts = stockAlertService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }
    //getActiveAlertsByProduct
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockAlertResponse>> getAlertsByProduct(@PathVariable Long productId) {
        log.info("GET /api/alerts/product/{}", productId);
        List<StockAlertResponse> alerts = stockAlertService.getAlertsByProduct(productId);
        return ResponseEntity.ok(alerts);
    }

    @PutMapping("/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable Long alertId,
            @RequestParam String acknowledgedBy) {
        log.info("PUT /api/alerts/{}/acknowledge - Acknowledged by: {}", alertId, acknowledgedBy);
        stockAlertService.acknowledgeAlerts(alertId, acknowledgedBy);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{alertId}/dismiss")
    public ResponseEntity<Void> dismissAlert(@PathVariable Long alertId) {
        log.info("PUT /api/alerts/{}/dismiss", alertId);
        stockAlertService.dismissAlerts(alertId);
        return ResponseEntity.ok().build();
    }
    //Acknowledge Alerts
    //Dismiss Alerts



}
