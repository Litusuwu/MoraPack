package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.TravelPlan;
import com.system.morapack.dao.morapack_psql.repository.TravelPlanRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelPlanService {

  private final TravelPlanRepository travelPlanRepository;

  public TravelPlan getTravelPlan(Integer id) {
    return travelPlanRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Travel plan not found with id: " + id));
  }

  public List<TravelPlan> fetchTravelPlans(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) {
      return travelPlanRepository.findAll();
    }
    return travelPlanRepository.findByIdIn(ids);
  }

  public List<TravelPlan> getByOrder(Integer orderId) {
    return travelPlanRepository.findByOrder_Id(orderId);
  }

  public List<TravelPlan> getByStatus(String status) {
    return travelPlanRepository.findByStatus(status);
  }

  public List<TravelPlan> getByAlgorithm(String algorithm) {
    return travelPlanRepository.findBySelectedAlgorithm(algorithm);
  }

  public List<TravelPlan> getByPlanningDateRange(LocalDateTime start, LocalDateTime end) {
    return travelPlanRepository.findByPlanningDateBetween(start, end);
  }

  public TravelPlan createTravelPlan(TravelPlan travelPlan) {
    return travelPlanRepository.save(travelPlan);
  }

  public List<TravelPlan> bulkCreateTravelPlans(List<TravelPlan> travelPlans) {
    return travelPlanRepository.saveAll(travelPlans);
  }

  public TravelPlan updateTravelPlan(Integer id, TravelPlan updates) {
    TravelPlan plan = getTravelPlan(id);

    if (updates.getStatus() != null)
      plan.setStatus(updates.getStatus());
    if (updates.getSelectedAlgorithm() != null)
      plan.setSelectedAlgorithm(updates.getSelectedAlgorithm());
    if (updates.getDatasetVersion() != null)
      plan.setDatasetVersion(updates.getDatasetVersion());
    if (updates.getPlanningDate() != null)
      plan.setPlanningDate(updates.getPlanningDate());

    return travelPlanRepository.save(plan);
  }

  public void deleteTravelPlan(Integer id) {
    if (!travelPlanRepository.existsById(id)) {
      throw new EntityNotFoundException("Travel plan not found with id: " + id);
    }
    travelPlanRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteTravelPlans(List<Integer> ids) {
    travelPlanRepository.deleteAllByIdIn(ids);
  }
}
