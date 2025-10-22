package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.ProductAdapter;
import com.system.morapack.schemas.ProductSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductController {

  private final ProductAdapter productAdapter;

  public ProductSchema getProduct(Integer id) {
    return productAdapter.getProduct(id);
  }

  public List<ProductSchema> fetchProducts(List<Integer> ids) {
    return productAdapter.fetchProducts(ids);
  }

  public List<ProductSchema> searchProductsByName(String name) {
    return productAdapter.searchProductsByName(name);
  }

  public List<ProductSchema> getProductsByOrder(Integer orderId) {
    return productAdapter.getProductsByOrder(orderId);
  }

  public ProductSchema createProduct(ProductSchema request) {
    return productAdapter.createProduct(request);
  }

  public List<ProductSchema> bulkCreateProducts(List<ProductSchema> requests) {
    return productAdapter.bulkCreateProducts(requests);
  }

  public ProductSchema updateProduct(Integer id, ProductSchema request) {
    return productAdapter.updateProduct(id, request);
  }

  public void deleteProduct(Integer id) {
    productAdapter.deleteProduct(id);
  }

  public void bulkDeleteProducts(List<Integer> ids) {
    productAdapter.bulkDeleteProducts(ids);
  }
}
