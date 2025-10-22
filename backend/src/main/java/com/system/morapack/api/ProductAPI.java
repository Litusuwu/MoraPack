package com.system.morapack.api;

import com.system.morapack.bll.controller.ProductController;
import com.system.morapack.schemas.ProductSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductAPI {

  private final ProductController productController;

  @GetMapping("/{id}")
  public ResponseEntity<ProductSchema> getProduct(@PathVariable Integer id) {
    return ResponseEntity.ok(productController.getProduct(id));
  }

  @GetMapping
  public ResponseEntity<List<ProductSchema>> getProducts(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer orderId) {

    if (name != null) {
      return ResponseEntity.ok(productController.searchProductsByName(name));
    }

    if (orderId != null) {
      return ResponseEntity.ok(productController.getProductsByOrder(orderId));
    }

    return ResponseEntity.ok(productController.fetchProducts(ids));
  }

  @PostMapping
  public ResponseEntity<ProductSchema> createProduct(@RequestBody ProductSchema product) {
    return ResponseEntity.ok(productController.createProduct(product));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<ProductSchema>> createProducts(@RequestBody List<ProductSchema> products) {
    return ResponseEntity.ok(productController.bulkCreateProducts(products));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProductSchema> updateProduct(@PathVariable Integer id, @RequestBody ProductSchema updates) {
    return ResponseEntity.ok(productController.updateProduct(id, updates));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
    productController.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }
}
