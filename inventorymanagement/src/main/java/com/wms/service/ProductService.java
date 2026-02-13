package com.wms.service;

import com.wms.dto.DashboardStatsResponse;
import com.wms.dto.ProductRequest;
import com.wms.dto.ProductResponse;
import com.wms.dto.StockUpdateRequest;
import com.wms.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    public ProductResponse createProduct(ProductRequest request);
    public ProductResponse getProductById(Long id);
    public ProductResponse getProductBySku(String sku);
    public DashboardStatsResponse getDashBoardStats();
    public ProductResponse updateProduct(StockUpdateRequest request);
    ProductResponse attemptStockUpdate(StockUpdateRequest request);

    public void updateProductStatus(Product product);
    public List<ProductResponse> getLowStockProducts();
    public void removeById(Long id);

    public void removeBySku(String sku);
}
