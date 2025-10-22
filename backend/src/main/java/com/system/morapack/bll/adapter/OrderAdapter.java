package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.City;
import com.system.morapack.dao.morapack_psql.model.Customer;
import com.system.morapack.dao.morapack_psql.model.Order;
import com.system.morapack.dao.morapack_psql.service.CityService;
import com.system.morapack.dao.morapack_psql.service.CustomerService;
import com.system.morapack.dao.morapack_psql.service.OrderService;
import com.system.morapack.schemas.OrderSchema;
import com.system.morapack.schemas.PackageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderAdapter {

  private final OrderService orderService;
  private final CityService cityService;
  private final CustomerService customerService;

  private OrderSchema mapToSchema(Order order) {
    return OrderSchema.builder()
        .id(order.getId())
        .name(order.getName())
        .originCityId(order.getOrigin() != null ? order.getOrigin().getId() : null)
        .originCityName(order.getOrigin() != null ? order.getOrigin().getName() : null)
        .destinationCityId(order.getDestination() != null ? order.getDestination().getId() : null)
        .destinationCityName(order.getDestination() != null ? order.getDestination().getName() : null)
        .deliveryDate(order.getDeliveryDate())
        .status(order.getStatus())
        .pickupTimeHours(order.getPickupTimeHours())
        .creationDate(order.getCreationDate())
        .updatedAt(order.getUpdatedAt())
        .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
        .build();
  }

  private Order mapToEntity(OrderSchema schema) {
    Order.OrderBuilder builder = Order.builder()
        .id(schema.getId())
        .name(schema.getName())
        .deliveryDate(schema.getDeliveryDate())
        .status(schema.getStatus() != null ? schema.getStatus() : PackageStatus.PENDING)
        .pickupTimeHours(schema.getPickupTimeHours());

    if (schema.getOriginCityId() != null) {
      City origin = cityService.getCity(schema.getOriginCityId());
      builder.origin(origin);
    }

    if (schema.getDestinationCityId() != null) {
      City destination = cityService.getCity(schema.getDestinationCityId());
      builder.destination(destination);
    }

    if (schema.getCustomerId() != null) {
      Customer customer = customerService.getCustomer(schema.getCustomerId());
      builder.customer(customer);
    }

    return builder.build();
  }

  public OrderSchema getOrder(Integer id) {
    Order order = orderService.getOrder(id);
    return mapToSchema(order);
  }

  public List<OrderSchema> fetchOrders(List<Integer> ids) {
    return orderService.fetchOrders(ids).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public OrderSchema createOrder(OrderSchema schema) {
    Order entity = mapToEntity(schema);
    return mapToSchema(orderService.createOrder(entity));
  }

  public List<OrderSchema> bulkCreateOrders(List<OrderSchema> schemas) {
    List<Order> entities = schemas.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
    return orderService.bulkCreateOrders(entities).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public OrderSchema updateOrder(Integer id, OrderSchema updates) {
    Order entityUpdates = mapToEntity(updates);
    return mapToSchema(orderService.updateOrder(id, entityUpdates));
  }

  public OrderSchema updateStatus(Integer id, PackageStatus status) {
    return mapToSchema(orderService.updateStatus(id, status));
  }

  public void deleteOrder(Integer id) {
    orderService.deleteOrder(id);
  }

  public void bulkDeleteOrders(List<Integer> ids) {
    orderService.bulkDeleteOrders(ids);
  }

  public List<OrderSchema> getOrdersByDeliveryDateRange(LocalDateTime start, LocalDateTime end) {
    return orderService.getOrdersByDeliveryDateRange(start, end).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<OrderSchema> getOrdersByStatus(PackageStatus status) {
    return orderService.getOrdersByStatus(status).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }
}
