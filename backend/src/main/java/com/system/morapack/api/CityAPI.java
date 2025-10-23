package com.system.morapack.api;

import com.system.morapack.bll.controller.CityController;
import com.system.morapack.schemas.CitySchema;
import com.system.morapack.schemas.Continent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityAPI {

  private final CityController cityController;

  @GetMapping("/{id}")
  public ResponseEntity<CitySchema> get(@PathVariable Integer id) {
    return ResponseEntity.ok(cityController.getCity(id));
  }

  @GetMapping
  public ResponseEntity<List<CitySchema>> list(
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Continent continent) {
    if (name != null) return ResponseEntity.ok(List.of(cityController.getByName(name)));
    if (continent != null) return ResponseEntity.ok(cityController.getByContinent(continent));
    return ResponseEntity.ok(cityController.fetchCities(ids));
  }

  @PostMapping
  public ResponseEntity<CitySchema> create(@RequestBody CitySchema body) {
    return ResponseEntity.ok(cityController.createCity(body));
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<CitySchema>> bulkCreate(@RequestBody List<CitySchema> body) {
    return ResponseEntity.ok(cityController.bulkCreateCities(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<CitySchema> update(@PathVariable Integer id, @RequestBody CitySchema body) {
    return ResponseEntity.ok(cityController.updateCity(id, body));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Integer id) {
    cityController.deleteCity(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> bulkDelete(@RequestParam List<Integer> ids) {
    cityController.bulkDeleteCities(ids);
    return ResponseEntity.noContent().build();
  }
}