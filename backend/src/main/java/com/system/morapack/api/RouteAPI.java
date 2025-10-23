package com.system.morapack.api;

import com.system.morapack.bll.controller.RouteController;
import com.system.morapack.schemas.RouteSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteAPI {

  private final RouteController routeController;

  @GetMapping("/{id}")
  public ResponseEntity<RouteSchema> get(@PathVariable Integer id) {
    return ResponseEntity.ok(routeController.getRoute(id));
  }

  @GetMapping
  public ResponseEntity<List<RouteSchema>> list(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Integer originCityId,
      @RequestParam(required = false) Integer destinationCityId,
      @RequestParam(required = false) Integer solutionId,
      @RequestParam(required = false) Integer flightId,
      @RequestParam(required = false) Integer packageId,
      @RequestParam(required = false) Double minTime,
      @RequestParam(required = false) Double maxTime,
      @RequestParam(required = false) Double minCost,
      @RequestParam(required = false) Double maxCost) {

    if (originCityId != null) return ResponseEntity.ok(routeController.getByOrigin(originCityId));
    if (destinationCityId != null) return ResponseEntity.ok(routeController.getByDestination(destinationCityId));
    if (solutionId != null) return ResponseEntity.ok(routeController.getBySolution(solutionId));
    if (flightId != null) return ResponseEntity.ok(routeController.getByFlight(flightId));
    if (packageId != null) return ResponseEntity.ok(routeController.getByPackage(packageId));
    if (minTime != null && maxTime != null) return ResponseEntity.ok(routeController.getByTimeRange(minTime, maxTime));
    if (minCost != null && maxCost != null) return ResponseEntity.ok(routeController.getByCostRange(minCost, maxCost));
    return ResponseEntity.ok(routeController.fetchRoutes(ids));
  }

  @PostMapping
  public ResponseEntity<RouteSchema> create(@RequestBody RouteSchema body) {
    return ResponseEntity.ok(routeController.createRoute(body));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<RouteSchema>> bulkCreate(@RequestBody List<RouteSchema> body) {
    return ResponseEntity.ok(routeController.bulkCreateRoutes(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<RouteSchema> update(@PathVariable Integer id, @RequestBody RouteSchema body) {
    return ResponseEntity.ok(routeController.updateRoute(id, body));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Integer id) {
    routeController.deleteRoute(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> bulkDelete(@RequestParam List<Integer> ids) {
    routeController.bulkDeleteRoutes(ids);
    return ResponseEntity.noContent().build();
  }
}