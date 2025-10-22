package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.AirportAdapter;
import com.system.morapack.schemas.AirportSchema;
import com.system.morapack.schemas.AirportState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirportController {

  private final AirportAdapter airportAdapter;

  public AirportSchema getAirport(Integer id) {
    return airportAdapter.getAirport(id);
  }

  public List<AirportSchema> fetchAirports(List<Integer> ids) {
    return airportAdapter.fetchAirports(ids);
  }

  public AirportSchema getByCode(String codeIATA) {
    return airportAdapter.getByCode(codeIATA);
  }

  public List<AirportSchema> getByCity(Integer cityId) {
    return airportAdapter.getByCity(cityId);
  }

  public List<AirportSchema> getByState(AirportState state) {
    return airportAdapter.getByState(state);
  }

  public List<AirportSchema> getByTimezoneRange(Integer minTz, Integer maxTz) {
    return airportAdapter.getByTimezoneRange(minTz, maxTz);
  }

  public AirportSchema createAirport(AirportSchema request) {
    return airportAdapter.createAirport(request);
  }

  public List<AirportSchema> bulkCreateAirports(List<AirportSchema> requests) {
    return airportAdapter.bulkCreateAirports(requests);
  }

  public AirportSchema updateAirport(Integer id, AirportSchema request) {
    return airportAdapter.updateAirport(id, request);
  }

  public void deleteAirport(Integer id) {
    airportAdapter.deleteAirport(id);
  }

  public void bulkDeleteAirports(List<Integer> ids) {
    airportAdapter.bulkDeleteAirports(ids);
  }
}
