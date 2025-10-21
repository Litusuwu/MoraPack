// src/main/java/com/system/morapack/dao/morapack_psql/service/TrackingEventService.java
package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.TrackingEvent;
import com.system.morapack.dao.morapack_psql.repository.TrackingEventRepository;
import com.system.morapack.schemas.TrackingEventType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingEventService {

  private final TrackingEventRepository trackingEventRepository;

  public TrackingEvent getTrackingEvent(Integer id) {
    return trackingEventRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("TrackingEvent not found with id: " + id));
  }

  public List<TrackingEvent> fetchTrackingEvents(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return trackingEventRepository.findAll();
    return trackingEventRepository.findByIdIn(ids);
  }

  public TrackingEvent createTrackingEvent(TrackingEvent te) {
    validate(te);
    if (te.getCreatedAt() == null) te.setCreatedAt(LocalDateTime.now());
    return trackingEventRepository.save(te);
  }

  public List<TrackingEvent> bulkCreateTrackingEvents(List<TrackingEvent> events) {
    events.forEach(this::ensureCreatedAtAndValidate);
    return trackingEventRepository.saveAll(events);
  }

  @Transactional
  public TrackingEvent updateTrackingEvent(Integer id, TrackingEvent patch) {
    TrackingEvent te = getTrackingEvent(id);

    if (patch.getTimestamp() != null) te.setTimestamp(patch.getTimestamp());
    if (patch.getType() != null) te.setType(patch.getType());
    if (patch.getCity() != null) te.setCity(patch.getCity());
    if (patch.getOrder() != null) te.setOrder(patch.getOrder());
    if (patch.getSegment() != null) te.setSegment(patch.getSegment());
    if (patch.getCreatedAt() != null) te.setCreatedAt(patch.getCreatedAt());

    validate(te);
    return trackingEventRepository.save(te);
  }

  public void deleteTrackingEvent(Integer id) {
    if (!trackingEventRepository.existsById(id)) {
      throw new EntityNotFoundException("TrackingEvent not found with id: " + id);
    }
    trackingEventRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteTrackingEvents(List<Integer> ids) {
    trackingEventRepository.deleteAllByIdIn(ids);
  }

  public List<TrackingEvent> getByOrder(Integer orderId) {
    return trackingEventRepository.findByOrder_Id(orderId);
  }

  public TrackingEvent getLastByOrder(Integer orderId) {
    return trackingEventRepository.findTopByOrder_IdOrderByTimestampDesc(orderId);
  }

  public List<TrackingEvent> getBySegment(Integer segmentId) {
    return trackingEventRepository.findBySegment_Id(segmentId);
  }

  public List<TrackingEvent> getByCity(Integer cityId) {
    return trackingEventRepository.findByCity_Id(cityId);
  }

  public List<TrackingEvent> getByType(TrackingEventType type) {
    return trackingEventRepository.findByType(type);
  }

  public List<TrackingEvent> getByTimestampRange(LocalDateTime start, LocalDateTime end) {
    return trackingEventRepository.findByTimestampBetween(start, end);
  }

  public List<TrackingEvent> getByCreatedAtRange(LocalDateTime start, LocalDateTime end) {
    return trackingEventRepository.findByCreatedAtBetween(start, end);
  }

  // Reglas mínimas
  private void ensureCreatedAtAndValidate(TrackingEvent te) {
    if (te.getCreatedAt() == null) te.setCreatedAt(LocalDateTime.now());
    validate(te);
  }

  private void validate(TrackingEvent te) {
    if (te.getTimestamp() == null) throw new IllegalArgumentException("timestamp is required");
    if (te.getType() == null) throw new IllegalArgumentException("type is required");
    if (te.getCreatedAt() == null) throw new IllegalArgumentException("createdAt is required");
    // city/order/segment son opcionales
  }
}
