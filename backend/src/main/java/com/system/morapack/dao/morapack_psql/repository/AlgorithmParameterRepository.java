package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.AlgorithmParameter;
import com.system.morapack.dao.morapack_psql.model.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlgorithmParameterRepository extends JpaRepository<AlgorithmParameter, Integer> {

  List<AlgorithmParameter> findByIdIn(List<Integer> ids);

  List<AlgorithmParameter> findByPlan(TravelPlan plan);
  List<AlgorithmParameter> findByAlgorithmName(String algorithmName);
  List<AlgorithmParameter> findByAlgorithmNameAndPlan(String algorithmName, TravelPlan plan);
  Optional<AlgorithmParameter> findByAlgorithmNameAndParameterAndPlan(String algorithmName, String parameter, TravelPlan plan);

  boolean existsByAlgorithmNameAndParameterAndPlanIsNull(String algorithmName, String parameter);
  boolean existsByAlgorithmNameAndParameterAndPlan(String algorithmName, String parameter, TravelPlan plan);

  @Modifying
  @Query("DELETE FROM AlgorithmParameter ap WHERE ap.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
