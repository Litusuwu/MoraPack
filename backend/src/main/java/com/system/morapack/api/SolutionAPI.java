package com.system.morapack.api;

import com.system.morapack.bll.controller.SolutionController;
import com.system.morapack.schemas.SolutionSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solutions")
@RequiredArgsConstructor
public class SolutionAPI {

  private final SolutionController controller;

  @GetMapping("/{id}")
  public ResponseEntity<SolutionSchema> get(@PathVariable Integer id) {
    return ResponseEntity.ok(controller.getSolution(id));
  }

  @GetMapping
  public ResponseEntity<List<SolutionSchema>> list(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Double minCost,
      @RequestParam(required = false) Double maxCost,
      @RequestParam(required = false) Double minTime,
      @RequestParam(required = false) Double maxTime,
      @RequestParam(required = false) Double minFitness,
      @RequestParam(required = false) Double maxFitness,
      @RequestParam(required = false) Integer maxUndelivered) {

    if (minCost != null && maxCost != null) return ResponseEntity.ok(controller.getByCostRange(minCost, maxCost));
    if (minTime != null && maxTime != null) return ResponseEntity.ok(controller.getByTimeRange(minTime, maxTime));
    if (minFitness != null && maxFitness != null) return ResponseEntity.ok(controller.getByFitnessRange(minFitness, maxFitness));
    if (maxUndelivered != null) return ResponseEntity.ok(controller.getWithUndeliveredAtMost(maxUndelivered));
    return ResponseEntity.ok(controller.fetchSolutions(ids));
  }

  @PostMapping
  public ResponseEntity<SolutionSchema> create(@RequestBody SolutionSchema body) {
    return ResponseEntity.ok(controller.createSolution(body));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<SolutionSchema>> bulkCreate(@RequestBody List<SolutionSchema> body) {
    return ResponseEntity.ok(controller.bulkCreateSolutions(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<SolutionSchema> update(@PathVariable Integer id, @RequestBody SolutionSchema body) {
    return ResponseEntity.ok(controller.updateSolution(id, body));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Integer id) {
    controller.deleteSolution(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> bulkDelete(@RequestParam List<Integer> ids) {
    controller.bulkDeleteSolutions(ids);
    return ResponseEntity.noContent().build();
  }
}