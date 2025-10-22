package com.system.morapack.api;

import com.system.morapack.bll.controller.FlightSegmentController;
import com.system.morapack.schemas.FlightSegmentSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/flight-segments")
@RequiredArgsConstructor
public class FlightSegmentAPI {

  private final FlightSegmentController flightSegmentController;

  @GetMapping("/{id}")
  public ResponseEntity<FlightSegmentSchema> getFlightSegment(@PathVariable Integer id) {
    return ResponseEntity.ok(flightSegmentController.getFlightSegment(id));
  }

  @GetMapping
  public ResponseEntity<List<FlightSegmentSchema>> getFlightSegments(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Integer planId,
      @RequestParam(required = false) Integer orderId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

    if (planId != null) {
      return ResponseEntity.ok(flightSegmentController.getByTravelPlan(planId));
    }

    if (orderId != null) {
      return ResponseEntity.ok(flightSegmentController.getByOrder(orderId));
    }

    if (startDate != null && endDate != null) {
      return ResponseEntity.ok(flightSegmentController.getByCreationRange(startDate, endDate));
    }

    return ResponseEntity.ok(flightSegmentController.fetchFlightSegments(ids));
  }

  @PostMapping
  public ResponseEntity<FlightSegmentSchema> createFlightSegment(@RequestBody FlightSegmentSchema segment) {
    return ResponseEntity.ok(flightSegmentController.createFlightSegment(segment));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<FlightSegmentSchema>> createFlightSegments(@RequestBody List<FlightSegmentSchema> segments) {
    return ResponseEntity.ok(flightSegmentController.bulkCreateFlightSegments(segments));
  }

  @PutMapping("/{id}")
  public ResponseEntity<FlightSegmentSchema> updateFlightSegment(@PathVariable Integer id, @RequestBody FlightSegmentSchema updates) {
    return ResponseEntity.ok(flightSegmentController.updateFlightSegment(id, updates));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteFlightSegment(@PathVariable Integer id) {
    flightSegmentController.deleteFlightSegment(id);
    return ResponseEntity.noContent().build();
  }
}
