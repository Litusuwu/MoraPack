package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.Airport;
import com.system.morapack.schemas.AirportState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Integer> {

  List<Airport> findByIdIn(List<Integer> ids);

  Optional<Airport> findByCodeIATA(String codeIATA);
  boolean existsByCodeIATA(String codeIATA);

  boolean existsByWarehouse_Id(Integer warehouseId);
  List<Airport> findByWarehouse_Id(Integer warehouseId);

  List<Airport> findByCity_Id(Integer cityId);
  List<Airport> findByState(AirportState state);
  List<Airport> findByTimezoneUTCBetween(Integer minTz, Integer maxTz);

  @Modifying
  @Query("DELETE FROM Airport a WHERE a.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
