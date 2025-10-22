package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.FlightSegmentAdapter;
import com.system.morapack.schemas.FlightSegmentSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightSegmentController {

  private final FlightSegmentAdapter flightSegmentAdapter;

  public FlightSegmentSchema getFlightSegment(Integer id) {
    return flightSegmentAdapter.getFlightSegment(id);
  }

  public List<FlightSegmentSchema> fetchFlightSegments(List<Integer> ids) {
    return flightSegmentAdapter.fetchFlightSegments(ids);
  }

  public List<FlightSegmentSchema> getByTravelPlan(Integer planId) {
    return flightSegmentAdapter.getByTravelPlan(planId);
  }

  public List<FlightSegmentSchema> getByOrder(Integer orderId) {
    return flightSegmentAdapter.getByOrder(orderId);
  }

  public List<FlightSegmentSchema> getByCreationRange(LocalDateTime start, LocalDateTime end) {
    return flightSegmentAdapter.getByCreationRange(start, end);
  }

  public FlightSegmentSchema createFlightSegment(FlightSegmentSchema request) {
    return flightSegmentAdapter.createFlightSegment(request);
  }

  public List<FlightSegmentSchema> bulkCreateFlightSegments(List<FlightSegmentSchema> requests) {
    return flightSegmentAdapter.bulkCreateFlightSegments(requests);
  }

  public FlightSegmentSchema updateFlightSegment(Integer id, FlightSegmentSchema request) {
    return flightSegmentAdapter.updateFlightSegment(id, request);
  }

  public void deleteFlightSegment(Integer id) {
    flightSegmentAdapter.deleteFlightSegment(id);
  }

  public void bulkDeleteFlightSegments(List<Integer> ids) {
    flightSegmentAdapter.bulkDeleteFlightSegments(ids);
  }
}
