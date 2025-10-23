package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.Airplane;
import com.system.morapack.dao.morapack_psql.service.AirplaneService;
import com.system.morapack.schemas.AirplaneSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AirplaneAdapter {

  private final AirplaneService airplaneService;

  private AirplaneSchema mapToSchema(Airplane a) {
    return AirplaneSchema.builder()
        .id(a.getId())
        .registration(a.getRegistration())
        .model(a.getModel())
        .capacity(a.getCapacity())
        .status(a.getStatus())
        .createdAt(a.getCreatedAt())
        .build();
  }

  private Airplane mapToEntity(AirplaneSchema s) {
    return Airplane.builder()
        .id(s.getId())
        .registration(s.getRegistration())
        .model(s.getModel())
        .capacity(s.getCapacity())
        .status(s.getStatus())
        .createdAt(s.getCreatedAt())
        .build();
  }

  // CRUD
  public AirplaneSchema getAirplane(Integer id) { return mapToSchema(airplaneService.get(id)); }

  public List<AirplaneSchema> fetchAirplanes(List<Integer> ids) {
    return airplaneService.fetch(ids).stream().map(this::mapToSchema).collect(Collectors.toList());
  }

  public AirplaneSchema createAirplane(AirplaneSchema req) {
    return mapToSchema(airplaneService.create(mapToEntity(req)));
  }

  public List<AirplaneSchema> bulkCreateAirplanes(List<AirplaneSchema> reqs) {
    var entities = reqs.stream().map(this::mapToEntity).toList();
    return airplaneService.bulkCreate(entities).stream().map(this::mapToSchema).toList();
  }

  public AirplaneSchema updateAirplane(Integer id, AirplaneSchema req) {
    return mapToSchema(airplaneService.update(id, mapToEntity(req)));
  }

  public void deleteAirplane(Integer id) { airplaneService.delete(id); }

  public void bulkDeleteAirplanes(List<Integer> ids) { airplaneService.bulkDelete(ids); }

  // Queries útiles
  public List<AirplaneSchema> getByStatus(String status) {
    return airplaneService.getByStatus(status).stream().map(this::mapToSchema).toList();
  }

  public List<AirplaneSchema> getByCapacityAtLeast(Integer min) {
    return airplaneService.getByCapacityAtLeast(min).stream().map(this::mapToSchema).toList();
  }
}