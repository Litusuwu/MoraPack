package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.TrackingEvent;
import com.system.morapack.schemas.TrackingEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Integer> {

  List<TrackingEvent> findByIdIn(List<Integer> ids);

  List<TrackingEvent> findByOrder_Id(Integer orderId);
  List<TrackingEvent> findBySegment_Id(Integer segmentId);
  List<TrackingEvent> findByCity_Id(Integer cityId);
  List<TrackingEvent> findByType(TrackingEventType type);

  List<TrackingEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
  List<TrackingEvent> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

  TrackingEvent findTopByOrder_IdOrderByTimestampDesc(Integer orderId);

  @Modifying
  @Query("DELETE FROM TrackingEvent te WHERE te.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}