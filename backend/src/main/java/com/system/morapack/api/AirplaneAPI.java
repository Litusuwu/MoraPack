package com.system.morapack.api;

import com.system.morapack.bll.controller.AirplaneController;
import com.system.morapack.schemas.AirplaneSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airplanes")
@RequiredArgsConstructor
public class AirplaneAPI {

  private final AirplaneController airplaneController;

  @GetMapping("/{id}")
  public ResponseEntity<AirplaneSchema> get(@PathVariable Integer id) {
    return ResponseEntity.ok(airplaneController.getAirplane(id));
  }

  @GetMapping
  public ResponseEntity<List<AirplaneSchema>> list(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer minCapacity) {
    if (status != null) return ResponseEntity.ok(airplaneController.getByStatus(status));
    if (minCapacity != null) return ResponseEntity.ok(airplaneController.getByCapacityAtLeast(minCapacity));
    return ResponseEntity.ok(airplaneController.fetchAirplanes(ids));
  }

  @PostMapping
  public ResponseEntity<AirplaneSchema> create(@RequestBody AirplaneSchema body) {
    return ResponseEntity.ok(airplaneController.createAirplane(body));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<AirplaneSchema>> bulkCreate(@RequestBody List<AirplaneSchema> body) {
    return ResponseEntity.ok(airplaneController.bulkCreateAirplanes(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<AirplaneSchema> update(@PathVariable Integer id, @RequestBody AirplaneSchema body) {
    return ResponseEntity.ok(airplaneController.updateAirplane(id, body));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Integer id) {
    airplaneController.deleteAirplane(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> bulkDelete(@RequestParam List<Integer> ids) {
    airplaneController.bulkDeleteAirplanes(ids);
    return ResponseEntity.noContent().build();
  }
}