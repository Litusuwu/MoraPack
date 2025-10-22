package com.system.morapack.api;

import com.system.morapack.bll.controller.AirportController;
import com.system.morapack.schemas.AirportSchema;
import com.system.morapack.schemas.AirportState;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airports")
@RequiredArgsConstructor
public class AirportAPI {

  private final AirportController airportController;

  @GetMapping("/{id}")
  public ResponseEntity<AirportSchema> getAirport(@PathVariable Integer id) {
    return ResponseEntity.ok(airportController.getAirport(id));
  }

  @GetMapping("/code/{codeIATA}")
  public ResponseEntity<AirportSchema> getAirportByCode(@PathVariable String codeIATA) {
    return ResponseEntity.ok(airportController.getByCode(codeIATA));
  }

  @GetMapping
  public ResponseEntity<List<AirportSchema>> getAirports(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Integer cityId,
      @RequestParam(required = false) AirportState state,
      @RequestParam(required = false) Integer minTimezone,
      @RequestParam(required = false) Integer maxTimezone) {

    if (cityId != null) {
      return ResponseEntity.ok(airportController.getByCity(cityId));
    }

    if (state != null) {
      return ResponseEntity.ok(airportController.getByState(state));
    }

    if (minTimezone != null && maxTimezone != null) {
      return ResponseEntity.ok(airportController.getByTimezoneRange(minTimezone, maxTimezone));
    }

    return ResponseEntity.ok(airportController.fetchAirports(ids));
  }

  @PostMapping
  public ResponseEntity<AirportSchema> createAirport(@RequestBody AirportSchema airport) {
    return ResponseEntity.ok(airportController.createAirport(airport));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<AirportSchema>> createAirports(@RequestBody List<AirportSchema> airports) {
    return ResponseEntity.ok(airportController.bulkCreateAirports(airports));
  }

  @PutMapping("/{id}")
  public ResponseEntity<AirportSchema> updateAirport(@PathVariable Integer id, @RequestBody AirportSchema updates) {
    return ResponseEntity.ok(airportController.updateAirport(id, updates));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAirport(@PathVariable Integer id) {
    airportController.deleteAirport(id);
    return ResponseEntity.noContent().build();
  }
}
