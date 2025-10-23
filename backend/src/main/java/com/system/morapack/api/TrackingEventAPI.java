package com.system.morapack.api;

import com.system.morapack.bll.controller.TrackingEventController;
import com.system.morapack.schemas.TrackingEventSchema;
import com.system.morapack.schemas.TrackingEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tracking-events")
@RequiredArgsConstructor
public class TrackingEventAPI {

  private final TrackingEventController controller;

  @GetMapping("/{id}")
  public ResponseEntity<TrackingEventSchema> get(@PathVariable Integer id) {
    return ResponseEntity.ok(controller.get(id));
  }

  @GetMapping
  public ResponseEntity<List<TrackingEventSchema>> list(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Integer orderId,
      @RequestParam(required = false) Integer segmentId,
      @RequestParam(required = false) Integer cityId,
      @RequestParam(required = false) TrackingEventType type,
      @RequestParam(required = false) LocalDateTime startTs,
      @RequestParam(required = false) LocalDateTime endTs,
      @RequestParam(required = false) LocalDateTime startCreated,
      @RequestParam(required = false) LocalDateTime endCreated) {

    if (orderId != null) return ResponseEntity.ok(controller.getByOrder(orderId));
    if (segmentId != null) return ResponseEntity.ok(controller.getBySegment(segmentId));
    if (cityId != null) return ResponseEntity.ok(controller.getByCity(cityId));
    if (type != null) return ResponseEntity.ok(controller.getByType(type));
    if (startTs != null && endTs != null) return ResponseEntity.ok(controller.getByTimestampRange(startTs, endTs));
    if (startCreated != null && endCreated != null) return ResponseEntity.ok(controller.getByCreatedAtRange(startCreated, endCreated));
    return ResponseEntity.ok(controller.fetch(ids));
  }

  @GetMapping("/orders/{orderId}/last")
  public ResponseEntity<TrackingEventSchema> lastForOrder(@PathVariable Integer orderId) {
    return ResponseEntity.ok(controller.getLastByOrder(orderId));
  }

  @PostMapping
  public ResponseEntity<TrackingEventSchema> create(@RequestBody TrackingEventSchema body) {
    return ResponseEntity.ok(controller.create(body));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<TrackingEventSchema>> bulkCreate(@RequestBody List<TrackingEventSchema> body) {
    return ResponseEntity.ok(controller.bulkCreate(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<TrackingEventSchema> update(@PathVariable Integer id, @RequestBody TrackingEventSchema body) {
    return ResponseEntity.ok(controller.update(id, body));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Integer id) {
    controller.delete(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> bulkDelete(@RequestParam List<Integer> ids) {
    controller.bulkDelete(ids);
    return ResponseEntity.noContent().build();
  }
}