package com.wms.controller;

import com.wms.dto.ProductRequest;
import com.wms.dto.ProductResponse;
import com.wms.dto.StockUpdateRequest;
import com.wms.entity.Product;
import com.wms.repository.ProductRepository;
import com.wms.service.ProductServiceImpl;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@CrossOrigin(origins = "*")
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductServiceImpl productService;

    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/product-details")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest product){
        ProductResponse response =  productService.createProduct(product);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/get-all-products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        if(products!=null){
            return ResponseEntity.ok(products);
        }
       return ResponseEntity.noContent().build();
    }

    @GetMapping("/get-product/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id){
       ProductResponse product = productService.getProductById(id);
       return  ResponseEntity.ok(product);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        log.info("GET /api/products/sku/{} - Fetching product by SKU", sku);
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductById(@PathVariable Long id){
        productService.removeById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("sku/{sku}")
    public  ResponseEntity<Void> deleteProductBySku(@PathVariable String sku){
         productService.removeBySku(sku);
         return ResponseEntity.noContent().build();
    }

    @PutMapping("/stock")
    public ResponseEntity<ProductResponse> updateStock(@Valid @RequestBody StockUpdateRequest request) {
        log.info("PUT /api/products/stock - Updating stock for SKU: {}", request.getSku());
        ProductResponse response = productService.updateProduct(request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts() {
        log.info("GET /api/products/low-stock");
        List<ProductResponse> products = productService.getLowStockProducts();
        return ResponseEntity.ok(products);
    }
}
