package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.Order;
import com.system.morapack.dao.morapack_psql.repository.OrderRepository;
import com.system.morapack.schemas.PackageStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;

  public Order getOrder(Integer id) {
    return orderRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
  }

  public List<Order> fetchOrders(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) {
      return orderRepository.findAll();
    }
    return orderRepository.findByIdIn(ids);
  }

  public Order createOrder(Order order) {
    if (orderRepository.existsByName(order.getName())) {
      throw new IllegalArgumentException("Order already exists: " + order.getName());
    }
    return orderRepository.save(order);
  }

  public List<Order> bulkCreateOrders(List<Order> orders) {
    return orderRepository.saveAll(orders);
  }

  public Order updateOrder(Integer id, Order updates) {
    Order order = getOrder(id);

    if (updates.getName() != null)
      order.setName(updates.getName());
    if (updates.getDeliveryDate() != null)
      order.setDeliveryDate(updates.getDeliveryDate());
    if (updates.getPickupTimeHours() != null)
      order.setPickupTimeHours(updates.getPickupTimeHours());
    if (updates.getStatus() != null)
      order.setStatus(updates.getStatus());

    return orderRepository.save(order);
  }

  public Order updateStatus(Integer id, PackageStatus status) {
    Order order = getOrder(id);
    order.setStatus(status);
    return orderRepository.save(order);
  }

  public void deleteOrder(Integer id) {
    if (!orderRepository.existsById(id)) {
      throw new EntityNotFoundException("Order not found with id: " + id);
    }
    orderRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteOrders(List<Integer> ids) {
    orderRepository.deleteAllByIdIn(ids);
  }

  public List<Order> getOrdersByDeliveryDateRange(LocalDateTime start, LocalDateTime end) {
    return orderRepository.findByDeliveryDateBetween(start, end);
  }

  public List<Order> getOrdersByStatus(PackageStatus status) {
    return orderRepository.findByStatus(status);
  }
}
