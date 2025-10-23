package com.system.morapack.api;

import com.system.morapack.bll.controller.WarehouseController;
import com.system.morapack.schemas.WarehouseSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseAPI {

  private final WarehouseController warehouseController;

  @GetMapping("/{id}")
  public ResponseEntity<WarehouseSchema> get(@PathVariable Integer id) {
    return ResponseEntity.ok(warehouseController.getWarehouse(id));
  }

  @GetMapping
  public ResponseEntity<List<WarehouseSchema>> list(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Integer airportId,
      @RequestParam(required = false) Boolean mainOnly,
      @RequestParam(required = false) Integer minCapacity,
      @RequestParam(required = false) Integer maxUsed) {

    if (airportId != null) return ResponseEntity.ok(List.of(warehouseController.getByAirport(airportId)));
    if (Boolean.TRUE.equals(mainOnly)) return ResponseEntity.ok(warehouseController.getMainWarehouses());
    if (minCapacity != null) return ResponseEntity.ok(warehouseController.getWithCapacityAtLeast(minCapacity));
    if (maxUsed != null) return ResponseEntity.ok(warehouseController.getWithUsedCapacityAtMost(maxUsed));
    return ResponseEntity.ok(warehouseController.fetchWarehouses(ids));
  }

  @PostMapping
  public ResponseEntity<WarehouseSchema> create(@RequestBody WarehouseSchema body) {
    return ResponseEntity.ok(warehouseController.createWarehouse(body));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<WarehouseSchema>> bulkCreate(@RequestBody List<WarehouseSchema> body) {
    return ResponseEntity.ok(warehouseController.bulkCreateWarehouses(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<WarehouseSchema> update(@PathVariable Integer id, @RequestBody WarehouseSchema body) {
    return ResponseEntity.ok(warehouseController.updateWarehouse(id, body));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Integer id) {
    warehouseController.deleteWarehouse(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> bulkDelete(@RequestParam List<Integer> ids) {
    warehouseController.bulkDeleteWarehouses(ids);
    return ResponseEntity.noContent().build();
  }
}