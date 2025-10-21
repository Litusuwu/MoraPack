package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Integer> {
  List<TravelPlan> findByOrder_Id(Integer orderId);
  List<TravelPlan> findByStatus(String status);
  List<TravelPlan> findBySelectedAlgorithm(String selectedAlgorithm);
  List<TravelPlan> findByPlanningDateBetween(LocalDateTime start, LocalDateTime end);
  List<TravelPlan> findByIdIn(List<Integer> ids);
  @Modifying
  @Query("DELETE FROM TravelPlan t WHERE t.idPlan IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
