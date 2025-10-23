package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.TrackingEventAdapter;
import com.system.morapack.schemas.TrackingEventSchema;
import com.system.morapack.schemas.TrackingEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingEventController {

  private final TrackingEventAdapter adapter;

  public TrackingEventSchema get(Integer id) { return adapter.get(id); }
  public List<TrackingEventSchema> fetch(List<Integer> ids) { return adapter.fetch(ids); }
  public TrackingEventSchema create(TrackingEventSchema req) { return adapter.create(req); }
  public List<TrackingEventSchema> bulkCreate(List<TrackingEventSchema> reqs) { return adapter.bulkCreate(reqs); }
  public TrackingEventSchema update(Integer id, TrackingEventSchema req) { return adapter.update(id, req); }
  public void delete(Integer id) { adapter.delete(id); }
  public void bulkDelete(List<Integer> ids) { adapter.bulkDelete(ids); }

  public List<TrackingEventSchema> getByOrder(Integer orderId) { return adapter.getByOrder(orderId); }
  public TrackingEventSchema getLastByOrder(Integer orderId) { return adapter.getLastByOrder(orderId); }
  public List<TrackingEventSchema> getBySegment(Integer segmentId) { return adapter.getBySegment(segmentId); }
  public List<TrackingEventSchema> getByCity(Integer cityId) { return adapter.getByCity(cityId); }
  public List<TrackingEventSchema> getByType(TrackingEventType type) { return adapter.getByType(type); }
  public List<TrackingEventSchema> getByTimestampRange(LocalDateTime start, LocalDateTime end) { return adapter.getByTimestampRange(start, end); }
  public List<TrackingEventSchema> getByCreatedAtRange(LocalDateTime start, LocalDateTime end) { return adapter.getByCreatedAtRange(start, end); }
}