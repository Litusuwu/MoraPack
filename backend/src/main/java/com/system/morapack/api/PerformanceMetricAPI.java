package com.system.morapack.api;

import com.system.morapack.bll.controller.PerformanceMetricController;
import com.system.morapack.schemas.PerformanceMetricSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/performance-metrics")
@RequiredArgsConstructor
public class PerformanceMetricAPI {

  private final PerformanceMetricController controller;

  @GetMapping("/{id}")
  public ResponseEntity<PerformanceMetricSchema> get(@PathVariable Integer id) {
    return ResponseEntity.ok(controller.get(id));
  }

  @GetMapping
  public ResponseEntity<List<PerformanceMetricSchema>> list(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Integer planId,
      @RequestParam(required = false) Integer eventId,
      @RequestParam(required = false) Integer scenarioId,
      @RequestParam(required = false) Integer algorithmId,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String period,
      @RequestParam(required = false) LocalDateTime startCreatedAt,
      @RequestParam(required = false) LocalDateTime endCreatedAt,
      @RequestParam(required = false) Double minValue,
      @RequestParam(required = false) Double maxValue) {

    if (planId != null && period != null) return ResponseEntity.ok(List.of(controller.getLastForPlanPeriod(planId, period)));
    if (planId != null && period == null) return ResponseEntity.ok(List.of(controller.getLastForPlan(planId)));
    if (planId != null) return ResponseEntity.ok(controller.getByPlan(planId));
    if (eventId != null) return ResponseEntity.ok(controller.getByEvent(eventId));
    if (scenarioId != null) return ResponseEntity.ok(controller.getByScenario(scenarioId));
    if (algorithmId != null) return ResponseEntity.ok(controller.getByAlgorithm(algorithmId));
    if (name != null && period != null) return ResponseEntity.ok(controller.getByNameAndPeriod(name, period));
    if (name != null) return ResponseEntity.ok(controller.getByName(name));
    if (period != null) return ResponseEntity.ok(controller.getByPeriod(period));
    if (startCreatedAt != null && endCreatedAt != null) return ResponseEntity.ok(controller.getByCreatedAtRange(startCreatedAt, endCreatedAt));
    if (minValue != null && maxValue != null) return ResponseEntity.ok(controller.getByValueRange(minValue, maxValue));
    return ResponseEntity.ok(controller.fetch(ids));
  }

  @PostMapping
  public ResponseEntity<PerformanceMetricSchema> create(@RequestBody PerformanceMetricSchema body) {
    return ResponseEntity.ok(controller.create(body));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<PerformanceMetricSchema>> bulkCreate(@RequestBody List<PerformanceMetricSchema> body) {
    return ResponseEntity.ok(controller.bulkCreate(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<PerformanceMetricSchema> update(@PathVariable Integer id, @RequestBody PerformanceMetricSchema body) {
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