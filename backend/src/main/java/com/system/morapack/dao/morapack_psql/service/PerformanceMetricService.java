package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.PerformanceMetric;
import com.system.morapack.dao.morapack_psql.repository.PerformanceMetricRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceMetricService {

  private final PerformanceMetricRepository repository;

  public PerformanceMetric get(Integer id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("PerformanceMetric not found with id: " + id));
  }

  public List<PerformanceMetric> fetch(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return repository.findAll();
    return repository.findByIdIn(ids);
  }

  public PerformanceMetric create(PerformanceMetric pm) {
    ensureCreatedAt(pm);
    validate(pm);
    return repository.save(pm);
  }

  public List<PerformanceMetric> bulkCreate(List<PerformanceMetric> list) {
    list.forEach(this::ensureCreatedAt);
    list.forEach(this::validate);
    return repository.saveAll(list);
  }

  @Transactional
  public PerformanceMetric update(Integer id, PerformanceMetric patch) {
    PerformanceMetric pm = get(id);

    if (patch.getName() != null) pm.setName(patch.getName());
    if (patch.getValue() != null) pm.setValue(patch.getValue());
    if (patch.getPeriod() != null) pm.setPeriod(patch.getPeriod());
    if (patch.getCreatedAt() != null) pm.setCreatedAt(patch.getCreatedAt());

    if (patch.getPlan() != null) pm.setPlan(patch.getPlan());
    if (patch.getEvent() != null) pm.setEvent(patch.getEvent());
    if (patch.getScenario() != null) pm.setScenario(patch.getScenario());
    if (patch.getAlgorithm() != null) pm.setAlgorithm(patch.getAlgorithm());

    validate(pm);
    return repository.save(pm);
  }

  public void delete(Integer id) {
    if (!repository.existsById(id)) {
      throw new EntityNotFoundException("PerformanceMetric not found with id: " + id);
    }
    repository.deleteById(id);
  }

  @Transactional
  public void bulkDelete(List<Integer> ids) {
    repository.deleteAllByIdIn(ids);
  }

  public List<PerformanceMetric> getByPlan(Integer planId) { return repository.findByPlan_Id(planId); }
  public List<PerformanceMetric> getByEvent(Integer eventId) { return repository.findByEvent_Id(eventId); }
  public List<PerformanceMetric> getByScenario(Integer scenarioId) { return repository.findByScenario_Id(scenarioId); }
  public List<PerformanceMetric> getByAlgorithm(Integer algorithmId) { return repository.findByAlgorithm_Id(algorithmId); }
  public List<PerformanceMetric> getByPeriod(String period) { return repository.findByPeriod(period); }
  public List<PerformanceMetric> getByName(String name) { return repository.findByName(name); }
  public List<PerformanceMetric> getByNameAndPeriod(String name, String period) { return repository.findByNameAndPeriod(name, period); }
  public List<PerformanceMetric> getByCreatedAtRange(LocalDateTime start, LocalDateTime end) { return repository.findByCreatedAtBetween(start, end); }
  public List<PerformanceMetric> getByValueRange(Double min, Double max) { return repository.findByValueBetween(min, max); }
  public PerformanceMetric getLastForPlanPeriod(Integer planId, String period) { return repository.findTopByPlan_IdAndPeriodOrderByCreatedAtDesc(planId, period); }
  public PerformanceMetric getLastForPlan(Integer planId) { return repository.findTopByPlan_IdOrderByCreatedAtDesc(planId); }

  // Reglas
  private void ensureCreatedAt(PerformanceMetric pm) {
    if (pm.getCreatedAt() == null) pm.setCreatedAt(LocalDateTime.now());
  }

  private void validate(PerformanceMetric pm) {
    if (pm.getName() == null || pm.getName().isBlank())
      throw new IllegalArgumentException("name requerido");
    if (pm.getValue() == null)
      throw new IllegalArgumentException("value requerido");
    if (pm.getPeriod() == null || pm.getPeriod().isBlank())
      throw new IllegalArgumentException("period requerido");
    if (pm.getCreatedAt() == null)
      throw new IllegalArgumentException("createdAt requerido");
    // plan/event/scenario/algorithm son opcionales.
  }
}
