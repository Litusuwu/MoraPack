package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.Order;
import com.system.morapack.dao.morapack_psql.model.Product;
import com.system.morapack.dao.morapack_psql.service.OrderService;
import com.system.morapack.dao.morapack_psql.service.ProductService;
import com.system.morapack.schemas.ProductSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductAdapter {

  private final ProductService productService;
  private final OrderService orderService;

  private ProductSchema mapToSchema(Product product) {
    return ProductSchema.builder()
        .id(product.getId())
        .name(product.getName())
        .weight(product.getWeight())
        .volume(product.getVolume())
        .creationDate(product.getCreationDate())
        .orderId(product.getOrder() != null ? product.getOrder().getId() : null)
        .build();
  }

  private Product mapToEntity(ProductSchema schema) {
    Product.ProductBuilder builder = Product.builder()
        .id(schema.getId())
        .name(schema.getName())
        .weight(schema.getWeight())
        .volume(schema.getVolume())
        .creationDate(schema.getCreationDate());

    if (schema.getOrderId() != null) {
      Order order = orderService.getOrder(schema.getOrderId());
      builder.order(order);
    }

    return builder.build();
  }

  public ProductSchema getProduct(Integer id) {
    Product product = productService.getProduct(id);
    return mapToSchema(product);
  }

  public List<ProductSchema> fetchProducts(List<Integer> ids) {
    return productService.fetchProducts(ids).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<ProductSchema> searchProductsByName(String name) {
    return productService.searchProductsByName(name).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<ProductSchema> getProductsByOrder(Integer orderId) {
    return productService.getProductsByOrder(orderId).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public ProductSchema createProduct(ProductSchema schema) {
    Product entity = mapToEntity(schema);
    return mapToSchema(productService.createProduct(entity));
  }

  public List<ProductSchema> bulkCreateProducts(List<ProductSchema> schemas) {
    List<Product> entities = schemas.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
    return productService.bulkCreateProducts(entities).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public ProductSchema updateProduct(Integer id, ProductSchema updates) {
    Product entityUpdates = mapToEntity(updates);
    return mapToSchema(productService.updateProduct(id, entityUpdates));
  }

  public void deleteProduct(Integer id) {
    productService.deleteProduct(id);
  }

  public void bulkDeleteProducts(List<Integer> ids) {
    productService.bulkDeleteProducts(ids);
  }
}
