// src/main/java/com/system/morapack/dao/morapack_psql/service/AirportService.java
package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.Airport;
import com.system.morapack.dao.morapack_psql.model.City;
import com.system.morapack.dao.morapack_psql.model.Warehouse;
import com.system.morapack.dao.morapack_psql.repository.AirportRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirportService {

  private final AirportRepository airportRepository;

  public Airport getAirport(Integer id) {
    return airportRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("AirportSchema not found with id: " + id));
  }

  public List<Airport> fetchAirports(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return airportRepository.findAll();
    return airportRepository.findByIdIn(ids);
  }

  public Airport createAirport(Airport a) {
    normalize(a);
    validate(a);
    if (airportRepository.existsByCodeIATA(a.getCodeIATA()))
      throw new IllegalArgumentException("AirportSchema code already exists: " + a.getCodeIATA());
    ensureWarehouseFree(a.getWarehouse());
    return airportRepository.save(a);
  }

  public List<Airport> bulkCreateAirports(List<Airport> list) {
    list.forEach(this::normalize);
    list.forEach(this::validate);
    // Chequeos simples
    for (Airport a : list) {
      if (airportRepository.existsByCodeIATA(a.getCodeIATA()))
        throw new IllegalArgumentException("AirportSchema code already exists: " + a.getCodeIATA());
      ensureWarehouseFree(a.getWarehouse());
    }
    return airportRepository.saveAll(list);
  }

  @Transactional
  public Airport updateAirport(Integer id, Airport patch) {
    Airport a = getAirport(id);

    if (patch.getCodeIATA() != null) a.setCodeIATA(patch.getCodeIATA());
    if (patch.getAlias() != null) a.setAlias(patch.getAlias());
    if (patch.getTimezoneUTC() != null) a.setTimezoneUTC(patch.getTimezoneUTC());
    if (patch.getLatitude() != null) a.setLatitude(patch.getLatitude());
    if (patch.getLongitude() != null) a.setLongitude(patch.getLongitude());
    if (patch.getCity() != null) a.setCity(patch.getCity());
    if (patch.getState() != null) a.setState(patch.getState());

    // Reasignación de warehouse con control 1:1
    if (patch.getWarehouse() != null) {
      if (a.getWarehouse() == null || !a.getWarehouse().getId().equals(patch.getWarehouse().getId())) {
        ensureWarehouseFree(patch.getWarehouse());
      }
      a.setWarehouse(patch.getWarehouse());
    }

    normalize(a);
    validate(a);

    // Unicidad codeIATA
    airportRepository.findByCodeIATA(a.getCodeIATA())
        .filter(found -> !found.getId().equals(a.getId()))
        .ifPresent(found -> { throw new IllegalArgumentException("AirportSchema code already exists: " + a.getCodeIATA()); });

    return airportRepository.save(a);
  }

  public void deleteAirport(Integer id) {
    if (!airportRepository.existsById(id)) {
      throw new EntityNotFoundException("AirportSchema not found with id: " + id);
    }
    airportRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteAirports(List<Integer> ids) {
    airportRepository.deleteAllByIdIn(ids);
  }

  public Airport getByCode(String codeIATA) {
    return airportRepository.findByCodeIATA(codeIATA)
        .orElseThrow(() -> new EntityNotFoundException("AirportSchema not found with code: " + codeIATA));
  }

  public List<Airport> getByCity(Integer cityId) {
    return airportRepository.findByCity_Id(cityId);
  }

  public List<Airport> getByState(com.system.morapack.schemas.AirportState state) {
    return airportRepository.findByState(state);
  }

  public List<Airport> getByTimezoneRange(Integer minTz, Integer maxTz) {
    return airportRepository.findByTimezoneUTCBetween(minTz, maxTz);
  }

  public List<Airport> getByWarehouse(Integer warehouseId) {
    return airportRepository.findByWarehouse_Id(warehouseId);
  }

  private void normalize(Airport a) {
    if (a.getCodeIATA() != null) a.setCodeIATA(a.getCodeIATA().trim().toUpperCase());
    if (a.getAlias() != null) a.setAlias(a.getAlias().trim());
  }

  private void validate(Airport a) {
    City c = a.getCity();
    if (a.getCodeIATA() == null || a.getCodeIATA().isBlank())
      throw new IllegalArgumentException("codeIATA requerido");
    if (a.getTimezoneUTC() == null)
      throw new IllegalArgumentException("timezoneUTC requerido");
    // Rango típico UTC −12..+14. Ajusta si usas otra convención.
    if (a.getTimezoneUTC() < -12 || a.getTimezoneUTC() > 14)
      throw new IllegalArgumentException("timezoneUTC fuera de rango");
    if (a.getLatitude() == null || a.getLatitude().isBlank())
      throw new IllegalArgumentException("latitude requerida");
    if (a.getLongitude() == null || a.getLongitude().isBlank())
      throw new IllegalArgumentException("longitude requerida");
    if (c == null || c.getId() == null)
      throw new IllegalArgumentException("city requerida");
    if (a.getState() == null)
      throw new IllegalArgumentException("state requerido");
  }

  private void ensureWarehouseFree(Warehouse w) {
    if (w == null || w.getId() == null) return;
    if (airportRepository.existsByWarehouse_Id(w.getId()))
      throw new IllegalArgumentException("warehouse ya está asignado a otro aeropuerto");
  }
}
