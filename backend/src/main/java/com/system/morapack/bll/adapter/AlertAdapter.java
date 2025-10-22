package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.Alert;
import com.system.morapack.dao.morapack_psql.model.Order;
import com.system.morapack.dao.morapack_psql.service.AlertService;
import com.system.morapack.dao.morapack_psql.service.OrderService;
import com.system.morapack.schemas.AlertSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AlertAdapter {

  private final AlertService alertService;
  private final OrderService orderService;

  private AlertSchema mapToSchema(Alert alert) {
    return AlertSchema.builder()
        .id(alert.getId())
        .description(alert.getDescription())
        .status(alert.getStatus())
        .generationDate(alert.getGenerationDate())
        .orderId(alert.getOrder() != null ? alert.getOrder().getId() : null)
        .orderName(alert.getOrder() != null ? alert.getOrder().getName() : null)
        .build();
  }

  private Alert mapToEntity(AlertSchema schema) {
    Alert.AlertBuilder builder = Alert.builder()
        .id(schema.getId())
        .description(schema.getDescription())
        .status(schema.getStatus())
        .generationDate(schema.getGenerationDate());

    if (schema.getOrderId() != null) {
      Order order = orderService.getOrder(schema.getOrderId());
      builder.order(order);
    }

    return builder.build();
  }

  public AlertSchema getAlert(Integer id) {
    Alert alert = alertService.getAlert(id);
    return mapToSchema(alert);
  }

  public List<AlertSchema> fetchAlerts(List<Integer> ids) {
    return alertService.fetchAlerts(ids).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<AlertSchema> getAlertsByStatus(String status) {
    return alertService.getAlertsByStatus(status).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<AlertSchema> getAlertsByOrder(Integer orderId) {
    return alertService.getAlertsByOrder(orderId).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<AlertSchema> searchAlerts(String query) {
    return alertService.searchAlerts(query).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public AlertSchema createAlert(AlertSchema schema) {
    Alert entity = mapToEntity(schema);
    return mapToSchema(alertService.createAlert(entity));
  }

  public List<AlertSchema> bulkCreateAlerts(List<AlertSchema> schemas) {
    List<Alert> entities = schemas.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
    return alertService.bulkCreateAlerts(entities).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public AlertSchema updateAlert(Integer id, AlertSchema updates) {
    Alert entityUpdates = mapToEntity(updates);
    return mapToSchema(alertService.updateAlert(id, entityUpdates));
  }

  public void deleteAlert(Integer id) {
    alertService.deleteAlert(id);
  }

  public void bulkDeleteAlerts(List<Integer> ids) {
    alertService.bulkDeleteAlerts(ids);
  }
}
