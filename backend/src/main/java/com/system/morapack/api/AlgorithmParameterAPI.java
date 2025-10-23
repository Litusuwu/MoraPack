package com.system.morapack.api;

import com.system.morapack.bll.controller.AlgorithmParameterController;
import com.system.morapack.schemas.AlgorithmParameterSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/algorithm-parameters")
@RequiredArgsConstructor
public class AlgorithmParameterAPI {

  private final AlgorithmParameterController controller;

  @GetMapping("/{id}")
  public ResponseEntity<AlgorithmParameterSchema> get(@PathVariable Integer id) {
    return ResponseEntity.ok(controller.get(id));
  }

  @GetMapping
  public ResponseEntity<List<AlgorithmParameterSchema>> list(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Integer planId,
      @RequestParam(required = false) String algorithmName) {
    if (planId != null && algorithmName != null)
      return ResponseEntity.ok(controller.getByAlgorithmAndPlan(algorithmName, planId));
    if (planId != null) return ResponseEntity.ok(controller.getByPlan(planId));
    if (algorithmName != null) return ResponseEntity.ok(controller.getByAlgorithm(algorithmName));
    return ResponseEntity.ok(controller.fetch(ids));
  }

  @PostMapping
  public ResponseEntity<AlgorithmParameterSchema> create(@RequestBody AlgorithmParameterSchema body) {
    return ResponseEntity.ok(controller.create(body));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<AlgorithmParameterSchema>> bulkCreate(@RequestBody List<AlgorithmParameterSchema> body) {
    return ResponseEntity.ok(controller.bulkCreate(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<AlgorithmParameterSchema> update(@PathVariable Integer id, @RequestBody AlgorithmParameterSchema body) {
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