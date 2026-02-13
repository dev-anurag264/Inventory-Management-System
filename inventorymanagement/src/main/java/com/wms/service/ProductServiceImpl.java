package com.wms.service;

import com.wms.dto.DashboardStatsResponse;
import com.wms.dto.ProductRequest;
import com.wms.dto.ProductResponse;
import com.wms.dto.StockUpdateRequest;
import com.wms.entity.InventoryTransaction;
import com.wms.entity.Product;
import com.wms.entity.StockAlert;
import com.wms.repository.InventoryTransactionRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.StockAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
@EnableCaching

public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockAlertService alertService;
    @Autowired
    private StockAlertRepository stockAlertRepository;
    @Autowired
    private InventoryTransactionRepository transactionRepository;
    @Override
    @Transactional
    @CacheEvict(value = {"products","productStat"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product with SKU : {}", request.getSku());

      if(productRepository.findBySku(request.getSku()).isPresent()){
          System.out.println("Product with SKU" + request.getSku() + " already exists");
      }

        Product product = new Product();
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setQty(request.getQuantity());
        product.setReorderLevel(request.getReorderLevel());
        product.setMaxStockLevel(request.getMaxStockLevel());
        product.setPrice(request.getPrice());
        product.setStatus(Product.ProductStatus.ACTIVE);



        Product saved = productRepository.save(product);

        alertService.checkAndCreateAlert(saved);

        return mapToResponse(saved);

    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
        return mapToResponse(product);
    }

    @Override
    @Cacheable(value = "products", key = "#sku")
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + sku));
            return mapToResponse(product);

    }



    @Override
    public DashboardStatsResponse getDashBoardStats() {
         List<Product> allProducts = productRepository.findAll();
        long totalProducts = allProducts.size();
        long activeProducts = allProducts.stream()
                .filter(p -> p.getStatus() == Product.ProductStatus.ACTIVE)
                .count();
        long lowStockProducts = allProducts.stream()
                .filter(Product::isLowStock)
                .count();
        long outOfStockProducts = allProducts.stream()
                .filter(Product::isOutOfStock)
                .count();
        long activeAlerts = stockAlertRepository.findByStatus(StockAlert.AlertStatus.ACTIVE).size();
        BigDecimal totalValue = allProducts.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardStatsResponse(totalProducts, activeProducts,
                lowStockProducts, outOfStockProducts,
                activeAlerts, totalValue);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productStats"}, allEntries = true)
    public ProductResponse updateProduct(StockUpdateRequest request) {
        log.info("Updating stocks for SKU:{}, Type: {}", request.getSku(), request.getTransactionType());

        int maxRetries = 4;
        int retryCount = 0;

        while (retryCount < maxRetries){
            try {
                return attemptStockUpdate(request);
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                log.warn("Optimistic lock failure on stock update, retry {}/{}", retryCount, maxRetries);
                if (retryCount >= maxRetries) {
                    throw new RuntimeException("Failed to update stock after " + maxRetries + " retries", e);
                }
                try {
                    Thread.sleep(100 * retryCount); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }

            }
        }

        throw new RuntimeException("Failed to update Stocks");
    }

    @Override
    public ProductResponse attemptStockUpdate(StockUpdateRequest request) {
        Product product = productRepository.findBySkuWithLock(request.getSku()).
                orElseThrow(()->new RuntimeException("Product Not Found"+request.getSku()));

        int previousQty = product.getQty();
        InventoryTransaction.TransactionType transactionType = InventoryTransaction.TransactionType.valueOf(request.getTransactionType());
        log.info("Transaction Type: {}", transactionType);
        switch (transactionType){
            case STOCK_IN:
            case RETURN:
                product.setQty(product.getQty() + request.getQuantity());
                product.setLastRestocked(LocalDateTime.now());
                break;

            case STOCK_OUT:
            case DAMAGE:
                if (product.getAvailableQuantity() < request.getQuantity()) {
                    throw new RuntimeException(
                            String.format("Insufficient stock for %s. Available: %d, Requested: %d",
                                    product.getSku(), product.getAvailableQuantity(), request.getQuantity()));
                }
                product.setQty(product.getQty() - request.getQuantity());
                break;

            case ADJUSTMENT:
                product.setQty(request.getQuantity());
                break;
            case RESERVE:
                if (product.getAvailableQuantity() < request.getQuantity()) {
                    throw new RuntimeException("Cannot reserve " + request.getQuantity() +
                            " items. Only " + product.getAvailableQuantity() + " available");
                }
                product.setReservedQty(product.getReservedQty() + request.getQuantity());
                break;

            case RELEASE_RESERVATION:
                if (product.getReservedQty() < request.getQuantity()) {
                    throw new IllegalArgumentException("Cannot release " + request.getQuantity() +
                            " items. Only " + product.getReservedQty() + " reserved");
                }
                product.setReservedQty(product.getReservedQty() - request.getQuantity());
                break;
        }

        updateProductStatus(product);

        Product saved = productRepository.save(product);

        log.info("Stock updated successfully for SKU: {}, New quantity: {}",
                saved.getSku(), saved.getQty());
        alertService.checkAndCreateAlert(saved);

        return mapToResponse(saved);
    }


    public void updateProductStatus(Product product) {
        if (product.isOutOfStock()) {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        } else if (product.isLowStock()) {
            product.setStatus(Product.ProductStatus.LOW_STOCK);
        } else {
            product.setStatus(Product.ProductStatus.ACTIVE);
        }
    }

    @Override
    public void removeById(Long id) {
        productRepository.deleteById(id);
    }
    @Transactional
    @Override
    public void removeBySku(String sku) {
        Product product =productRepository.findBySku(sku).orElseThrow(()-> new RuntimeException("SKU Not found") );
        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setSku(product.getSku());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setCategory(product.getCategory());
        response.setQuantity(product.getQty());
        response.setReservedQuantity(product.getReservedQty());
        response.setAvailableQuantity(product.getAvailableQuantity());
        response.setReorderLevel(product.getReorderLevel());
        response.setMaxStockLevel(product.getMaxStockLevel());
        response.setPrice(product.getPrice());
        response.setStatus(product.getStatus());
        response.setLastRestocked(product.getLastRestocked());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        response.setLowStock(product.isLowStock());
        response.setOutOfStock(product.isOutOfStock());
        return response;
    }

    //Get all products
    @Cacheable(value = "products")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


}
