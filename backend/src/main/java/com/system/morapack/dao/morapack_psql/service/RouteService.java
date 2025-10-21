package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.City;
import com.system.morapack.dao.morapack_psql.model.Flight;
import com.system.morapack.dao.morapack_psql.model.Order;
import com.system.morapack.dao.morapack_psql.model.Route;
import com.system.morapack.dao.morapack_psql.repository.RouteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

  private final RouteRepository routeRepository;

  public Route getRoute(Integer id) {
    return routeRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("RouteSchema not found with id: " + id));
  }

  public List<Route> fetchRoutes(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return routeRepository.findAll();
    return routeRepository.findByIdIn(ids);
  }

  public Route createRoute(Route route) {
    validateRoute(route);

    Integer solId = route.getSolution() != null ? route.getSolution().getId() : null;
    Integer originId = route.getOriginCity() != null ? route.getOriginCity().getId() : null;
    Integer destId = route.getDestinationCity() != null ? route.getDestinationCity().getId() : null;

    if (solId != null && originId != null && destId != null &&
        routeRepository.existsByOriginCity_IdAndDestinationCity_IdAndSolution_Id(originId, destId, solId)) {
      throw new IllegalArgumentException("RouteSchema already exists for origin-destination-solution");
    }
    return routeRepository.save(route);
  }

  public List<Route> bulkCreateRoutes(List<Route> routes) {
    for (Route r : routes) validateRoute(r);
    return routeRepository.saveAll(routes);
  }

  /**
   * Actualiza campos escalares y reemplaza colecciones solo si vienen no nulas.
   * Para ManyToMany se recomienda traer la entidad y setear la lista completa.
   */
  @Transactional
  public Route updateRoute(Integer id, Route patch) {
    Route r = getRoute(id);

    if (patch.getTotalTime() != null) r.setTotalTime(patch.getTotalTime());
    if (patch.getTotalCost() != null) r.setTotalCost(patch.getTotalCost());
    if (patch.getOriginCity() != null) r.setOriginCity(patch.getOriginCity());
    if (patch.getDestinationCity() != null) r.setDestinationCity(patch.getDestinationCity());
    if (patch.getSolution() != null) r.setSolution(patch.getSolution());

    // Colecciones ManyToMany: reemplazo completo si se envían
    if (patch.getFlights() != null) {
      r.getFlights().clear();
      r.getFlights().addAll(patch.getFlights().stream().distinct().toList());
    }
    if (patch.getPackages() != null) {
      r.getPackages().clear();
      r.getPackages().addAll(patch.getPackages().stream().distinct().toList());
    }

    validateRoute(r);
    return routeRepository.save(r);
  }

  public void deleteRoute(Integer id) {
    if (!routeRepository.existsById(id)) {
      throw new EntityNotFoundException("RouteSchema not found with id: " + id);
    }
    routeRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteRoutes(List<Integer> ids) {
    routeRepository.deleteAllByIdIn(ids);
  }

  // Extras q pueden servir :v
  public List<Route> getByOrigin(Integer originCityId) {
    return routeRepository.findByOriginCity_Id(originCityId);
  }

  public List<Route> getByDestination(Integer destinationCityId) {
    return routeRepository.findByDestinationCity_Id(destinationCityId);
  }

  public List<Route> getBySolution(Integer solutionId) {
    return routeRepository.findBySolution_Id(solutionId);
  }

  public List<Route> getByFlight(Integer flightId) {
    return routeRepository.findByFlightId(flightId);
  }

  public List<Route> getByPackage(Integer packageId) {
    return routeRepository.findByPackageId(packageId);
  }

  public List<Route> getByTimeRange(Double min, Double max) {
    return routeRepository.findByTotalTimeBetween(min, max);
  }

  public List<Route> getByCostRange(Double min, Double max) {
    return routeRepository.findByTotalCostBetween(min, max);
  }

  //validacion :v
  private void validateRoute(Route r) {
    City o = r.getOriginCity();
    City d = r.getDestinationCity();
    if (o == null || d == null) throw new IllegalArgumentException("Origin and destination are required");
    if (o.getId() != null && d.getId() != null && o.getId().equals(d.getId()))
      throw new IllegalArgumentException("Origin and destination must differ");
    if (r.getTotalTime() == null || r.getTotalTime() < 0) throw new IllegalArgumentException("totalTime invalid");
    if (r.getTotalCost() == null || r.getTotalCost() < 0) throw new IllegalArgumentException("totalCost invalid");

    if (r.getFlights() == null) r.setFlights(new java.util.ArrayList<Flight>());
    if (r.getPackages() == null) r.setPackages(new java.util.ArrayList<Order>());
  }
}
