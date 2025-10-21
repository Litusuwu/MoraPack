package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.City;
import com.system.morapack.schemas.Continent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {

  Optional<City> findByName(String name);
  List<City> findByContinent(Continent continent);
  List<City> findByIdIn(List<Integer> ids);

  boolean existsByName(String name);
  boolean existsByNameAndContinent(String name, Continent continent);

  @Modifying
  @Query("DELETE FROM City c WHERE c.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
