package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.ScenarioParameters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScenarioParametersRepository extends JpaRepository<ScenarioParameters, Integer> {

  List<ScenarioParameters> findByIdIn(List<Integer> ids);

  Optional<ScenarioParameters> findByTypeAndPolicy(String type, String policy);
  List<ScenarioParameters> findByType(String type);
  List<ScenarioParameters> findByPolicy(String policy);
  List<ScenarioParameters> findByTimeWindowHoursBetween(Double min, Double max);

  @Modifying
  @Query("DELETE FROM ScenarioParameters sp WHERE sp.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);

  boolean existsByTypeAndPolicy(String type, String policy);
}
