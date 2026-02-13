package com.wms.controller;

import com.wms.dto.DashboardStatsResponse;
import com.wms.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashBoardController {

    @Autowired
    private ProductService productService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        log.info("GET /api/dashboard/stats");
        DashboardStatsResponse stats = productService.getDashBoardStats();
        return ResponseEntity.ok(stats);
    }
}
