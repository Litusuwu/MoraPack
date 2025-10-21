package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {

  List<Warehouse> findByIdIn(List<Integer> ids);

  Optional<Warehouse> findByName(String name);
  boolean existsByName(String name);

  Warehouse findByAirport_Id(Integer airportId);

  List<Warehouse> findByIsMainWarehouse(Boolean isMain);
  List<Warehouse> findByMaxCapacityGreaterThanEqual(Integer minCapacity);
  List<Warehouse> findByUsedCapacityLessThanEqual(Integer maxUsed);

  @Modifying
  @Query("DELETE FROM Warehouse w WHERE w.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
