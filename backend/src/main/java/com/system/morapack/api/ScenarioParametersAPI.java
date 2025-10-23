package com.system.morapack.api;

import com.system.morapack.bll.controller.ScenarioParametersController;
import com.system.morapack.schemas.ScenarioParametersSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenario-parameters")
@RequiredArgsConstructor
public class ScenarioParametersAPI {

  private final ScenarioParametersController controller;

  @GetMapping("/{id}")
  public ResponseEntity<ScenarioParametersSchema> get(@PathVariable Integer id) {
    return ResponseEntity.ok(controller.get(id));
  }

  @GetMapping
  public ResponseEntity<List<ScenarioParametersSchema>> list(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String policy,
      @RequestParam(required = false) Double minHours,
      @RequestParam(required = false) Double maxHours) {
    if (type != null) return ResponseEntity.ok(controller.getByType(type));
    if (policy != null) return ResponseEntity.ok(controller.getByPolicy(policy));
    if (minHours != null && maxHours != null) return ResponseEntity.ok(controller.getByTimeWindowRange(minHours, maxHours));
    return ResponseEntity.ok(controller.fetch(ids));
  }

  @PostMapping
  public ResponseEntity<ScenarioParametersSchema> create(@RequestBody ScenarioParametersSchema body) {
    return ResponseEntity.ok(controller.create(body));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<ScenarioParametersSchema>> bulkCreate(@RequestBody List<ScenarioParametersSchema> body) {
    return ResponseEntity.ok(controller.bulkCreate(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ScenarioParametersSchema> update(@PathVariable Integer id, @RequestBody ScenarioParametersSchema body) {
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