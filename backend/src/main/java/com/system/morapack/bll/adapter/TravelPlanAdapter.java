package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.Order;
import com.system.morapack.dao.morapack_psql.model.TravelPlan;
import com.system.morapack.dao.morapack_psql.service.OrderService;
import com.system.morapack.dao.morapack_psql.service.TravelPlanService;
import com.system.morapack.schemas.TravelPlanSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TravelPlanAdapter {

  private final TravelPlanService travelPlanService;
  private final OrderService orderService;

  private TravelPlanSchema mapToSchema(TravelPlan travelPlan) {
    return TravelPlanSchema.builder()
        .id(travelPlan.getId())
        .planningDate(travelPlan.getPlanningDate())
        .status(travelPlan.getStatus())
        .selectedAlgorithm(travelPlan.getSelectedAlgorithm())
        .datasetVersion(travelPlan.getDatasetVersion())
        .createdAt(travelPlan.getCreatedAt())
        .updatedAt(travelPlan.getUpdatedAt())
        .orderId(travelPlan.getOrder() != null ? travelPlan.getOrder().getId() : null)
        .orderName(travelPlan.getOrder() != null ? travelPlan.getOrder().getName() : null)
        .build();
  }

  private TravelPlan mapToEntity(TravelPlanSchema schema) {
    TravelPlan.TravelPlanBuilder builder = TravelPlan.builder()
        .id(schema.getId())
        .planningDate(schema.getPlanningDate())
        .status(schema.getStatus())
        .selectedAlgorithm(schema.getSelectedAlgorithm())
        .datasetVersion(schema.getDatasetVersion())
        .createdAt(schema.getCreatedAt())
        .updatedAt(schema.getUpdatedAt());

    if (schema.getOrderId() != null) {
      Order order = orderService.getOrder(schema.getOrderId());
      builder.order(order);
    }

    return builder.build();
  }

  public TravelPlanSchema getTravelPlan(Integer id) {
    TravelPlan travelPlan = travelPlanService.getTravelPlan(id);
    return mapToSchema(travelPlan);
  }

  public List<TravelPlanSchema> fetchTravelPlans(List<Integer> ids) {
    return travelPlanService.fetchTravelPlans(ids).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<TravelPlanSchema> getByOrder(Integer orderId) {
    return travelPlanService.getByOrder(orderId).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<TravelPlanSchema> getByStatus(String status) {
    return travelPlanService.getByStatus(status).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<TravelPlanSchema> getByAlgorithm(String algorithm) {
    return travelPlanService.getByAlgorithm(algorithm).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<TravelPlanSchema> getByPlanningDateRange(LocalDateTime start, LocalDateTime end) {
    return travelPlanService.getByPlanningDateRange(start, end).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public TravelPlanSchema createTravelPlan(TravelPlanSchema schema) {
    TravelPlan entity = mapToEntity(schema);
    return mapToSchema(travelPlanService.createTravelPlan(entity));
  }

  public List<TravelPlanSchema> bulkCreateTravelPlans(List<TravelPlanSchema> schemas) {
    List<TravelPlan> entities = schemas.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
    return travelPlanService.bulkCreateTravelPlans(entities).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public TravelPlanSchema updateTravelPlan(Integer id, TravelPlanSchema updates) {
    TravelPlan entityUpdates = mapToEntity(updates);
    return mapToSchema(travelPlanService.updateTravelPlan(id, entityUpdates));
  }

  public void deleteTravelPlan(Integer id) {
    travelPlanService.deleteTravelPlan(id);
  }

  public void bulkDeleteTravelPlans(List<Integer> ids) {
    travelPlanService.bulkDeleteTravelPlans(ids);
  }
}
