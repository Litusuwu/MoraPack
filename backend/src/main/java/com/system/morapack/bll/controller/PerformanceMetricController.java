package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.PerformanceMetricAdapter;
import com.system.morapack.schemas.PerformanceMetricSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceMetricController {

  private final PerformanceMetricAdapter adapter;

  public PerformanceMetricSchema get(Integer id) { return adapter.get(id); }

  public List<PerformanceMetricSchema> fetch(List<Integer> ids) { return adapter.fetch(ids); }

  public PerformanceMetricSchema create(PerformanceMetricSchema req) { return adapter.create(req); }

  public List<PerformanceMetricSchema> bulkCreate(List<PerformanceMetricSchema> reqs) { return adapter.bulkCreate(reqs); }

  public PerformanceMetricSchema update(Integer id, PerformanceMetricSchema req) { return adapter.update(id, req); }

  public void delete(Integer id) { adapter.delete(id); }

  public void bulkDelete(List<Integer> ids) { adapter.bulkDelete(ids); }

  // Queries
  public List<PerformanceMetricSchema> getByPlan(Integer planId) { return adapter.getByPlan(planId); }
  public List<PerformanceMetricSchema> getByEvent(Integer eventId) { return adapter.getByEvent(eventId); }
  public List<PerformanceMetricSchema> getByScenario(Integer scenarioId) { return adapter.getByScenario(scenarioId); }
  public List<PerformanceMetricSchema> getByAlgorithm(Integer algorithmId) { return adapter.getByAlgorithm(algorithmId); }
  public List<PerformanceMetricSchema> getByPeriod(String period) { return adapter.getByPeriod(period); }
  public List<PerformanceMetricSchema> getByName(String name) { return adapter.getByName(name); }
  public List<PerformanceMetricSchema> getByNameAndPeriod(String name, String period) { return adapter.getByNameAndPeriod(name, period); }
  public List<PerformanceMetricSchema> getByCreatedAtRange(LocalDateTime start, LocalDateTime end) { return adapter.getByCreatedAtRange(start, end); }
  public List<PerformanceMetricSchema> getByValueRange(Double min, Double max) { return adapter.getByValueRange(min, max); }
  public PerformanceMetricSchema getLastForPlanPeriod(Integer planId, String period) { return adapter.getLastForPlanPeriod(planId, period); }
  public PerformanceMetricSchema getLastForPlan(Integer planId) { return adapter.getLastForPlan(planId); }
}