package com.system.morapack.api;

import com.system.morapack.bll.controller.TravelPlanController;
import com.system.morapack.schemas.TravelPlanSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/travel-plans")
@RequiredArgsConstructor
public class TravelPlanAPI {

  private final TravelPlanController travelPlanController;

  @GetMapping("/{id}")
  public ResponseEntity<TravelPlanSchema> getTravelPlan(@PathVariable Integer id) {
    return ResponseEntity.ok(travelPlanController.getTravelPlan(id));
  }

  @GetMapping
  public ResponseEntity<List<TravelPlanSchema>> getTravelPlans(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Integer orderId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String algorithm,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

    if (orderId != null) {
      return ResponseEntity.ok(travelPlanController.getByOrder(orderId));
    }

    if (status != null) {
      return ResponseEntity.ok(travelPlanController.getByStatus(status));
    }

    if (algorithm != null) {
      return ResponseEntity.ok(travelPlanController.getByAlgorithm(algorithm));
    }

    if (startDate != null && endDate != null) {
      return ResponseEntity.ok(travelPlanController.getByPlanningDateRange(startDate, endDate));
    }

    return ResponseEntity.ok(travelPlanController.fetchTravelPlans(ids));
  }

  @PostMapping
  public ResponseEntity<TravelPlanSchema> createTravelPlan(@RequestBody TravelPlanSchema travelPlan) {
    return ResponseEntity.ok(travelPlanController.createTravelPlan(travelPlan));
  }

  @PutMapping("/{id}")
  public ResponseEntity<TravelPlanSchema> updateTravelPlan(@PathVariable Integer id, @RequestBody TravelPlanSchema updates) {
    return ResponseEntity.ok(travelPlanController.updateTravelPlan(id, updates));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTravelPlan(@PathVariable Integer id) {
    travelPlanController.deleteTravelPlan(id);
    return ResponseEntity.noContent().build();
  }
}
