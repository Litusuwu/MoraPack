package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.TrackingEvent;
import com.system.morapack.dao.morapack_psql.service.*;
import com.system.morapack.schemas.TrackingEventSchema;
import com.system.morapack.schemas.TrackingEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TrackingEventAdapter {

  private final TrackingEventService trackingEventService;
  private final CityService cityService;
  private final OrderService orderService;
  private final FlightSegmentService flightSegmentService;

  private TrackingEventSchema toSchema(TrackingEvent te) {
    return TrackingEventSchema.builder()
        .id(te.getId())
        .timestamp(te.getTimestamp())
        .type(te.getType())
        .cityId(te.getCity() != null ? te.getCity().getId() : null)
        .orderId(te.getOrder() != null ? te.getOrder().getId() : null)
        .segmentId(te.getSegment() != null ? te.getSegment().getId() : null)
        .createdAt(te.getCreatedAt())
        .build();
  }

  private TrackingEvent toEntity(TrackingEventSchema s) {
    var b = TrackingEvent.builder()
        .id(s.getId())
        .timestamp(s.getTimestamp())
        .type(s.getType())
        .createdAt(s.getCreatedAt());
    if (s.getCityId() != null)    b.city(cityService.getCity(s.getCityId()));
    if (s.getOrderId() != null)   b.order(orderService.getOrder(s.getOrderId()));
    if (s.getSegmentId() != null) b.segment(flightSegmentService.getFlightSegment(s.getSegmentId()));
    return b.build();
  }

  // CRUD mapeado a nombres reales del service
  public TrackingEventSchema get(Integer id) {
    return toSchema(trackingEventService.getTrackingEvent(id));
  }

  public List<TrackingEventSchema> fetch(List<Integer> ids) {
    return trackingEventService.fetchTrackingEvents(ids).stream().map(this::toSchema).toList();
  }

  public TrackingEventSchema create(TrackingEventSchema req) {
    return toSchema(trackingEventService.createTrackingEvent(toEntity(req)));
  }

  public List<TrackingEventSchema> bulkCreate(List<TrackingEventSchema> reqs) {
    var entities = reqs.stream().map(this::toEntity).toList();
    return trackingEventService.bulkCreateTrackingEvents(entities).stream().map(this::toSchema).toList();
  }

  public TrackingEventSchema update(Integer id, TrackingEventSchema req) {
    return toSchema(trackingEventService.updateTrackingEvent(id, toEntity(req)));
  }

  public void delete(Integer id) { trackingEventService.deleteTrackingEvent(id); }

  public void bulkDelete(List<Integer> ids) { trackingEventService.bulkDeleteTrackingEvents(ids); }

  // Queries
  public List<TrackingEventSchema> getByOrder(Integer orderId) {
    return trackingEventService.getByOrder(orderId).stream().map(this::toSchema).toList();
  }
  public TrackingEventSchema getLastByOrder(Integer orderId) {
    var te = trackingEventService.getLastByOrder(orderId);
    return te == null ? null : toSchema(te);
  }
  public List<TrackingEventSchema> getBySegment(Integer segmentId) {
    return trackingEventService.getBySegment(segmentId).stream().map(this::toSchema).toList();
  }
  public List<TrackingEventSchema> getByCity(Integer cityId) {
    return trackingEventService.getByCity(cityId).stream().map(this::toSchema).toList();
  }
  public List<TrackingEventSchema> getByType(TrackingEventType type) {
    return trackingEventService.getByType(type).stream().map(this::toSchema).toList();
  }
  public List<TrackingEventSchema> getByTimestampRange(LocalDateTime start, LocalDateTime end) {
    return trackingEventService.getByTimestampRange(start, end).stream().map(this::toSchema).toList();
  }
  public List<TrackingEventSchema> getByCreatedAtRange(LocalDateTime start, LocalDateTime end) {
    return trackingEventService.getByCreatedAtRange(start, end).stream().map(this::toSchema).toList();
  }
}