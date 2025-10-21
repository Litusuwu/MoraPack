package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.PerformanceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PerformanceMetricRepository extends JpaRepository<PerformanceMetric, Integer> {

  List<PerformanceMetric> findByIdIn(List<Integer> ids);

  List<PerformanceMetric> findByPlan_Id(Integer planId);
  List<PerformanceMetric> findByEvent_Id(Integer eventId);
  List<PerformanceMetric> findByScenario_Id(Integer scenarioId);
  List<PerformanceMetric> findByAlgorithm_Id(Integer algorithmId);

  List<PerformanceMetric> findByPeriod(String period);
  List<PerformanceMetric> findByName(String name);
  List<PerformanceMetric> findByNameAndPeriod(String name, String period);

  List<PerformanceMetric> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
  List<PerformanceMetric> findByValueBetween(Double min, Double max);

  PerformanceMetric findTopByPlan_IdAndPeriodOrderByCreatedAtDesc(Integer planId, String period);
  PerformanceMetric findTopByPlan_IdOrderByCreatedAtDesc(Integer planId);

  @Modifying
  @Query("DELETE FROM PerformanceMetric pm WHERE pm.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
