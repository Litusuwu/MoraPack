package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.Warehouse;
import com.system.morapack.dao.morapack_psql.service.WarehouseService;
import com.system.morapack.schemas.AirportSchema;
import com.system.morapack.schemas.WarehouseSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WarehouseAdapter {

  private final WarehouseService warehouseService;

  private WarehouseSchema toSchema(Warehouse w) {
    WarehouseSchema s = new WarehouseSchema();
    s.setId(w.getId());
    s.setName(w.getName());
    s.setMaxCapacity(w.getMaxCapacity());
    s.setUsedCapacity(w.getUsedCapacity());
    s.setIsMainWarehouse(w.getIsMainWarehouse());

    // Airport is the owning side on Airport entity. Expose read-only info.
    if (w.getAirport() != null) {
      AirportSchema a = new AirportSchema();
      a.setId(w.getAirport().getId());
      a.setCodeIATA(w.getAirport().getCodeIATA());
      s.setAirportSchema(a);
    }
    return s;
  }

  // Do NOT set airport here; Airport owns the 1:1 via airport.warehouse.
  private Warehouse toEntity(WarehouseSchema s) {
    return Warehouse.builder()
        .id(s.getId())
        .name(s.getName())
        .maxCapacity(s.getMaxCapacity())
        .usedCapacity(s.getUsedCapacity())
        .isMainWarehouse(Boolean.TRUE.equals(s.getIsMainWarehouse()))
        .build();
  }

  // CRUD
  public WarehouseSchema getWarehouse(Integer id) {
    return toSchema(warehouseService.getWarehouse(id));
  }

  public List<WarehouseSchema> fetchWarehouses(List<Integer> ids) {
    return warehouseService.fetchWarehouses(ids).stream().map(this::toSchema).toList();
  }

  public WarehouseSchema createWarehouse(WarehouseSchema req) {
    return toSchema(warehouseService.createWarehouse(toEntity(req)));
  }

  public List<WarehouseSchema> bulkCreateWarehouses(List<WarehouseSchema> reqs) {
    var entities = reqs.stream().map(this::toEntity).toList();
    return warehouseService.bulkCreateWarehouses(entities).stream().map(this::toSchema).toList();
  }

  public WarehouseSchema updateWarehouse(Integer id, WarehouseSchema req) {
    return toSchema(warehouseService.updateWarehouse(id, toEntity(req)));
  }

  public void deleteWarehouse(Integer id) { warehouseService.deleteWarehouse(id); }

  public void bulkDeleteWarehouses(List<Integer> ids) { warehouseService.bulkDeleteWarehouses(ids); }

  // Queries
  public WarehouseSchema getByAirport(Integer airportId) {
    var w = warehouseService.getByAirport(airportId);
    return w == null ? null : toSchema(w);
  }

  public List<WarehouseSchema> getMainWarehouses() {
    return warehouseService.getMainWarehouses().stream().map(this::toSchema).toList();
  }

  public List<WarehouseSchema> getWithCapacityAtLeast(Integer min) {
    return warehouseService.getWithCapacityAtLeast(min).stream().map(this::toSchema).toList();
  }

  public List<WarehouseSchema> getWithUsedCapacityAtMost(Integer max) {
    return warehouseService.getWithUsedCapacityAtMost(max).stream().map(this::toSchema).toList();
  }
}