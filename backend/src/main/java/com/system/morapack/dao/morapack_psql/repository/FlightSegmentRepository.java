package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.FlightSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightSegmentRepository extends JpaRepository<FlightSegment, Integer> {
  List<FlightSegment> findByTravelPlan_Id(Integer planId);
  List<FlightSegment> findByOrder_Id(Integer orderId);
  List<FlightSegment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
  List<FlightSegment> findByIdIn(List<Integer> ids);
  List<FlightSegment> findByEstimateTimeDestinationBetween(LocalDateTime start, LocalDateTime end);
  @Modifying
  @Query("DELETE FROM FlightSegment f WHERE f.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}