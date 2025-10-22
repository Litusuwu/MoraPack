package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.TravelPlanAdapter;
import com.system.morapack.schemas.TravelPlanSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelPlanController {

  private final TravelPlanAdapter travelPlanAdapter;

  public TravelPlanSchema getTravelPlan(Integer id) {
    return travelPlanAdapter.getTravelPlan(id);
  }

  public List<TravelPlanSchema> fetchTravelPlans(List<Integer> ids) {
    return travelPlanAdapter.fetchTravelPlans(ids);
  }

  public List<TravelPlanSchema> getByOrder(Integer orderId) {
    return travelPlanAdapter.getByOrder(orderId);
  }

  public List<TravelPlanSchema> getByStatus(String status) {
    return travelPlanAdapter.getByStatus(status);
  }

  public List<TravelPlanSchema> getByAlgorithm(String algorithm) {
    return travelPlanAdapter.getByAlgorithm(algorithm);
  }

  public List<TravelPlanSchema> getByPlanningDateRange(LocalDateTime start, LocalDateTime end) {
    return travelPlanAdapter.getByPlanningDateRange(start, end);
  }

  public TravelPlanSchema createTravelPlan(TravelPlanSchema request) {
    return travelPlanAdapter.createTravelPlan(request);
  }

  public List<TravelPlanSchema> bulkCreateTravelPlans(List<TravelPlanSchema> requests) {
    return travelPlanAdapter.bulkCreateTravelPlans(requests);
  }

  public TravelPlanSchema updateTravelPlan(Integer id, TravelPlanSchema request) {
    return travelPlanAdapter.updateTravelPlan(id, request);
  }

  public void deleteTravelPlan(Integer id) {
    travelPlanAdapter.deleteTravelPlan(id);
  }

  public void bulkDeleteTravelPlans(List<Integer> ids) {
    travelPlanAdapter.bulkDeleteTravelPlans(ids);
  }
}
