package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.AirplaneAdapter;
import com.system.morapack.schemas.AirplaneSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirplaneController {

  private final AirplaneAdapter airplaneAdapter;

  public AirplaneSchema getAirplane(Integer id) { return airplaneAdapter.getAirplane(id); }

  public List<AirplaneSchema> fetchAirplanes(List<Integer> ids) { return airplaneAdapter.fetchAirplanes(ids); }

  public AirplaneSchema createAirplane(AirplaneSchema req) { return airplaneAdapter.createAirplane(req); }

  public List<AirplaneSchema> bulkCreateAirplanes(List<AirplaneSchema> reqs) { return airplaneAdapter.bulkCreateAirplanes(reqs); }

  public AirplaneSchema updateAirplane(Integer id, AirplaneSchema req) { return airplaneAdapter.updateAirplane(id, req); }

  public void deleteAirplane(Integer id) { airplaneAdapter.deleteAirplane(id); }

  public void bulkDeleteAirplanes(List<Integer> ids) { airplaneAdapter.bulkDeleteAirplanes(ids); }

  public List<AirplaneSchema> getByStatus(String status) { return airplaneAdapter.getByStatus(status); }

  public List<AirplaneSchema> getByCapacityAtLeast(Integer min) { return airplaneAdapter.getByCapacityAtLeast(min); }
}