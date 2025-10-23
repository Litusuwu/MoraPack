package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.WarehouseAdapter;
import com.system.morapack.schemas.WarehouseSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseController {

  private final WarehouseAdapter warehouseAdapter;

  public WarehouseSchema getWarehouse(Integer id) { return warehouseAdapter.getWarehouse(id); }

  public List<WarehouseSchema> fetchWarehouses(List<Integer> ids) { return warehouseAdapter.fetchWarehouses(ids); }

  public WarehouseSchema createWarehouse(WarehouseSchema req) { return warehouseAdapter.createWarehouse(req); }

  public List<WarehouseSchema> bulkCreateWarehouses(List<WarehouseSchema> reqs) { return warehouseAdapter.bulkCreateWarehouses(reqs); }

  public WarehouseSchema updateWarehouse(Integer id, WarehouseSchema req) { return warehouseAdapter.updateWarehouse(id, req); }

  public void deleteWarehouse(Integer id) { warehouseAdapter.deleteWarehouse(id); }

  public void bulkDeleteWarehouses(List<Integer> ids) { warehouseAdapter.bulkDeleteWarehouses(ids); }

  // Queries
  public WarehouseSchema getByAirport(Integer airportId) { return warehouseAdapter.getByAirport(airportId); }

  public List<WarehouseSchema> getMainWarehouses() { return warehouseAdapter.getMainWarehouses(); }

  public List<WarehouseSchema> getWithCapacityAtLeast(Integer min) { return warehouseAdapter.getWithCapacityAtLeast(min); }

  public List<WarehouseSchema> getWithUsedCapacityAtMost(Integer max) { return warehouseAdapter.getWithUsedCapacityAtMost(max); }
}