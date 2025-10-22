package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.FlightSegment;
import com.system.morapack.dao.morapack_psql.model.Order;
import com.system.morapack.dao.morapack_psql.model.TravelPlan;
import com.system.morapack.dao.morapack_psql.service.FlightSegmentService;
import com.system.morapack.dao.morapack_psql.service.OrderService;
import com.system.morapack.dao.morapack_psql.service.TravelPlanService;
import com.system.morapack.schemas.FlightSegmentSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FlightSegmentAdapter {

  private final FlightSegmentService flightSegmentService;
  private final TravelPlanService travelPlanService;
  private final OrderService orderService;

  private FlightSegmentSchema mapToSchema(FlightSegment segment) {
    return FlightSegmentSchema.builder()
        .id(segment.getId())
        .estimateTimeDestination(segment.getEstimateTimeDestination())
        .estimatedTimeArrival(segment.getEstimatedTimeArrival())
        .reservedCapacity(segment.getReservedCapacity())
        .createdAt(segment.getCreatedAt())
        .planId(segment.getTravelPlan() != null ? segment.getTravelPlan().getId() : null)
        .orderId(segment.getOrder() != null ? segment.getOrder().getId() : null)
        .orderName(segment.getOrder() != null ? segment.getOrder().getName() : null)
        .build();
  }

  private FlightSegment mapToEntity(FlightSegmentSchema schema) {
    FlightSegment.FlightSegmentBuilder builder = FlightSegment.builder()
        .id(schema.getId())
        .estimateTimeDestination(schema.getEstimateTimeDestination())
        .estimatedTimeArrival(schema.getEstimatedTimeArrival())
        .reservedCapacity(schema.getReservedCapacity())
        .createdAt(schema.getCreatedAt());

    if (schema.getPlanId() != null) {
      TravelPlan plan = travelPlanService.getTravelPlan(schema.getPlanId());
      builder.travelPlan(plan);
    }

    if (schema.getOrderId() != null) {
      Order order = orderService.getOrder(schema.getOrderId());
      builder.order(order);
    }

    return builder.build();
  }

  public FlightSegmentSchema getFlightSegment(Integer id) {
    FlightSegment segment = flightSegmentService.getFlightSegment(id);
    return mapToSchema(segment);
  }

  public List<FlightSegmentSchema> fetchFlightSegments(List<Integer> ids) {
    return flightSegmentService.fetchFlightSegments(ids).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<FlightSegmentSchema> getByTravelPlan(Integer planId) {
    return flightSegmentService.getByTravelPlan(planId).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<FlightSegmentSchema> getByOrder(Integer orderId) {
    return flightSegmentService.getByOrder(orderId).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public List<FlightSegmentSchema> getByCreationRange(LocalDateTime start, LocalDateTime end) {
    return flightSegmentService.getByCreationRange(start, end).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public FlightSegmentSchema createFlightSegment(FlightSegmentSchema schema) {
    FlightSegment entity = mapToEntity(schema);
    return mapToSchema(flightSegmentService.createFlightSegment(entity));
  }

  public List<FlightSegmentSchema> bulkCreateFlightSegments(List<FlightSegmentSchema> schemas) {
    List<FlightSegment> entities = schemas.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
    return flightSegmentService.bulkCreateFlightSegments(entities).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public FlightSegmentSchema updateFlightSegment(Integer id, FlightSegmentSchema updates) {
    FlightSegment entityUpdates = mapToEntity(updates);
    return mapToSchema(flightSegmentService.updateFlightSegment(id, entityUpdates));
  }

  public void deleteFlightSegment(Integer id) {
    flightSegmentService.deleteFlightSegment(id);
  }

  public void bulkDeleteFlightSegments(List<Integer> ids) {
    flightSegmentService.bulkDeleteFlightSegments(ids);
  }
}
