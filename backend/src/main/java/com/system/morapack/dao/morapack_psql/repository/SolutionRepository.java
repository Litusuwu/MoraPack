package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Integer> {

  List<Solution> findByIdIn(List<Integer> ids);

  List<Solution> findByTotalCostBetween(Double min, Double max);
  List<Solution> findByTotalTimeBetween(Double min, Double max);
  List<Solution> findByFitnessBetween(Double min, Double max);
  List<Solution> findByUndeliveredPackagesLessThanEqual(Integer maxUndelivered);

  @Modifying
  @Query("DELETE FROM Solution s WHERE s.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
