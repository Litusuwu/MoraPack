package com.system.morapack.api;

import com.system.morapack.bll.controller.AlertController;
import com.system.morapack.schemas.AlertSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertAPI {

  private final AlertController alertController;

  @GetMapping("/{id}")
  public ResponseEntity<AlertSchema> getAlert(@PathVariable Integer id) {
    return ResponseEntity.ok(alertController.getAlert(id));
  }

  @GetMapping
  public ResponseEntity<List<AlertSchema>> getAlerts(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer orderId,
      @RequestParam(required = false) String search) {

    if (status != null) {
      return ResponseEntity.ok(alertController.getAlertsByStatus(status));
    }

    if (orderId != null) {
      return ResponseEntity.ok(alertController.getAlertsByOrder(orderId));
    }

    if (search != null) {
      return ResponseEntity.ok(alertController.searchAlerts(search));
    }

    return ResponseEntity.ok(alertController.fetchAlerts(ids));
  }

  @PostMapping
  public ResponseEntity<AlertSchema> createAlert(@RequestBody AlertSchema alert) {
    return ResponseEntity.ok(alertController.createAlert(alert));
  }

  @PutMapping("/{id}")
  public ResponseEntity<AlertSchema> updateAlert(@PathVariable Integer id, @RequestBody AlertSchema updates) {
    return ResponseEntity.ok(alertController.updateAlert(id, updates));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAlert(@PathVariable Integer id) {
    alertController.deleteAlert(id);
    return ResponseEntity.noContent().build();
  }
}
