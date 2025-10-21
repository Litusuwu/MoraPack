package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Integer> {

  List<Route> findByIdIn(List<Integer> ids);
  List<Route> findByOriginCity_Id(Integer originCityId);
  List<Route> findByDestinationCity_Id(Integer destinationCityId);
  List<Route> findBySolution_Id(Integer solutionId);

  List<Route> findByTotalTimeBetween(Double min, Double max);
  List<Route> findByTotalCostBetween(Double min, Double max);

  // Relacionales (ManyToMany)
  @Query("SELECT r FROM Route r JOIN r.flights f WHERE f.id = :flightId")
  List<Route> findByFlightId(Integer flightId);

  @Query("SELECT r FROM Route r JOIN r.packages p WHERE p.id = :packageId")
  List<Route> findByPackageId(Integer packageId);

  boolean existsByOriginCity_IdAndDestinationCity_IdAndSolution_Id(Integer originId, Integer destId, Integer solutionId);

  @Modifying
  @Query("DELETE FROM Route r WHERE r.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
