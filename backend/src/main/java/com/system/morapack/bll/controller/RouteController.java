package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.RouteAdapter;
import com.system.morapack.schemas.RouteSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteController {

  private final RouteAdapter routeAdapter;

  public RouteSchema getRoute(Integer id) { return routeAdapter.getRoute(id); }

  public List<RouteSchema> fetchRoutes(List<Integer> ids) { return routeAdapter.fetchRoutes(ids); }

  public List<RouteSchema> getByOrigin(Integer originCityId) { return routeAdapter.getByOrigin(originCityId); }

  public List<RouteSchema> getByDestination(Integer destinationCityId) { return routeAdapter.getByDestination(destinationCityId); }

  public List<RouteSchema> getBySolution(Integer solutionId) { return routeAdapter.getBySolution(solutionId); }

  public List<RouteSchema> getByFlight(Integer flightId) { return routeAdapter.getByFlight(flightId); }

  public List<RouteSchema> getByPackage(Integer packageId) { return routeAdapter.getByPackage(packageId); }

  public List<RouteSchema> getByTimeRange(Double min, Double max) { return routeAdapter.getByTimeRange(min, max); }

  public List<RouteSchema> getByCostRange(Double min, Double max) { return routeAdapter.getByCostRange(min, max); }

  public RouteSchema createRoute(RouteSchema request) { return routeAdapter.createRoute(request); }

  public List<RouteSchema> bulkCreateRoutes(List<RouteSchema> requests) { return routeAdapter.bulkCreateRoutes(requests); }

  public RouteSchema updateRoute(Integer id, RouteSchema request) { return routeAdapter.updateRoute(id, request); }

  public void deleteRoute(Integer id) { routeAdapter.deleteRoute(id); }

  public void bulkDeleteRoutes(List<Integer> ids) { routeAdapter.bulkDeleteRoutes(ids); }
}