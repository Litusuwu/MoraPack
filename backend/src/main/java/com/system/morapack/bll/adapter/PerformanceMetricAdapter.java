package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.PerformanceMetric;
import com.system.morapack.dao.morapack_psql.service.*;
import com.system.morapack.schemas.PerformanceMetricSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PerformanceMetricAdapter {

  private final PerformanceMetricService metricService;
  private final TravelPlanService planService;
  private final TrackingEventService eventService;
  private final ScenarioParametersService scenarioService;
  private final AlgorithmParameterService algorithmService;

  private PerformanceMetricSchema toSchema(PerformanceMetric pm) {
    return PerformanceMetricSchema.builder()
        .id(pm.getId())
        .name(pm.getName())
        .value(pm.getValue())
        .period(pm.getPeriod())
        .createdAt(pm.getCreatedAt())
        .planId(pm.getPlan() != null ? pm.getPlan().getId() : null)
        .eventId(pm.getEvent() != null ? pm.getEvent().getId() : null)
        .scenarioId(pm.getScenario() != null ? pm.getScenario().getId() : null)
        .algorithmId(pm.getAlgorithm() != null ? pm.getAlgorithm().getId() : null)
        .build();
  }

  private PerformanceMetric toEntity(PerformanceMetricSchema s) {
    PerformanceMetric.PerformanceMetricBuilder b = PerformanceMetric.builder()
        .id(s.getId())
        .name(s.getName())
        .value(s.getValue())
        .period(s.getPeriod())
        .createdAt(s.getCreatedAt());

    if (s.getPlanId() != null) b.plan(planService.getTravelPlan(s.getPlanId()));
    if (s.getEventId() != null) b.event(eventService.getTrackingEvent(s.getEventId()));
    if (s.getScenarioId() != null) b.scenario(scenarioService.get(s.getScenarioId()));
    if (s.getAlgorithmId() != null) b.algorithm(algorithmService.get(s.getAlgorithmId()));
    return b.build();
  }

  // CRUD
  public PerformanceMetricSchema get(Integer id) { return toSchema(metricService.get(id)); }

  public List<PerformanceMetricSchema> fetch(List<Integer> ids) {
    return metricService.fetch(ids).stream().map(this::toSchema).toList();
  }

  public PerformanceMetricSchema create(PerformanceMetricSchema req) {
    return toSchema(metricService.create(toEntity(req)));
  }

  public List<PerformanceMetricSchema> bulkCreate(List<PerformanceMetricSchema> reqs) {
    var entities = reqs.stream().map(this::toEntity).toList();
    return metricService.bulkCreate(entities).stream().map(this::toSchema).toList();
  }

  public PerformanceMetricSchema update(Integer id, PerformanceMetricSchema req) {
    return toSchema(metricService.update(id, toEntity(req)));
  }

  public void delete(Integer id) { metricService.delete(id); }

  public void bulkDelete(List<Integer> ids) { metricService.bulkDelete(ids); }

  // Queries
  public List<PerformanceMetricSchema> getByPlan(Integer planId) {
    return metricService.getByPlan(planId).stream().map(this::toSchema).toList();
  }

  public List<PerformanceMetricSchema> getByEvent(Integer eventId) {
    return metricService.getByEvent(eventId).stream().map(this::toSchema).toList();
  }

  public List<PerformanceMetricSchema> getByScenario(Integer scenarioId) {
    return metricService.getByScenario(scenarioId).stream().map(this::toSchema).toList();
  }

  public List<PerformanceMetricSchema> getByAlgorithm(Integer algorithmId) {
    return metricService.getByAlgorithm(algorithmId).stream().map(this::toSchema).toList();
  }

  public List<PerformanceMetricSchema> getByPeriod(String period) {
    return metricService.getByPeriod(period).stream().map(this::toSchema).toList();
  }

  public List<PerformanceMetricSchema> getByName(String name) {
    return metricService.getByName(name).stream().map(this::toSchema).toList();
  }

  public List<PerformanceMetricSchema> getByNameAndPeriod(String name, String period) {
    return metricService.getByNameAndPeriod(name, period).stream().map(this::toSchema).toList();
  }

  public List<PerformanceMetricSchema> getByCreatedAtRange(LocalDateTime start, LocalDateTime end) {
    return metricService.getByCreatedAtRange(start, end).stream().map(this::toSchema).toList();
  }

  public List<PerformanceMetricSchema> getByValueRange(Double min, Double max) {
    return metricService.getByValueRange(min, max).stream().map(this::toSchema).toList();
  }

  public PerformanceMetricSchema getLastForPlanPeriod(Integer planId, String period) {
    var pm = metricService.getLastForPlanPeriod(planId, period);
    return pm == null ? null : toSchema(pm);
  }

  public PerformanceMetricSchema getLastForPlan(Integer planId) {
    var pm = metricService.getLastForPlan(planId);
    return pm == null ? null : toSchema(pm);
  }
}