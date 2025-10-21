package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.AlgorithmParameter;
import com.system.morapack.dao.morapack_psql.model.TravelPlan;
import com.system.morapack.dao.morapack_psql.repository.AlgorithmParameterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlgorithmParameterService {

  private final AlgorithmParameterRepository repository;

  public AlgorithmParameter get(Integer id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("AlgorithmParameter not found with id: " + id));
  }

  public List<AlgorithmParameter> fetch(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return repository.findAll();
    return repository.findByIdIn(ids);
  }

  public AlgorithmParameter create(AlgorithmParameter ap) {
    normalize(ap);
    validate(ap);
    TravelPlan plan = ap.getPlan();
    boolean dup = (plan == null)
        ? repository.existsByAlgorithmNameAndParameterAndPlanIsNull(ap.getAlgorithmName(), ap.getParameter())
        : repository.existsByAlgorithmNameAndParameterAndPlan(ap.getAlgorithmName(), ap.getParameter(), plan);
    if (dup) throw new IllegalArgumentException("Parámetro duplicado para el plan");
    return repository.save(ap);
  }

  public List<AlgorithmParameter> bulkCreate(List<AlgorithmParameter> list) {
    list.forEach(this::normalize);
    list.forEach(this::validate);
    for (AlgorithmParameter ap : list) {
      TravelPlan plan = ap.getPlan();
      boolean dup = (plan == null)
          ? repository.existsByAlgorithmNameAndParameterAndPlanIsNull(ap.getAlgorithmName(), ap.getParameter())
          : repository.existsByAlgorithmNameAndParameterAndPlan(ap.getAlgorithmName(), ap.getParameter(), plan);
      if (dup) throw new IllegalArgumentException(
          "Parámetro duplicado: " + ap.getAlgorithmName() + " / " + ap.getParameter());
    }
    return repository.saveAll(list);
  }

  @Transactional
  public AlgorithmParameter update(Integer id, AlgorithmParameter patch) {
    AlgorithmParameter ap = get(id);

    if (patch.getAlgorithmName() != null) ap.setAlgorithmName(patch.getAlgorithmName());
    if (patch.getParameter() != null) ap.setParameter(patch.getParameter());
    if (patch.getValue() != null) ap.setValue(patch.getValue());
    if (patch.getPlan() != null) ap.setPlan(patch.getPlan());

    normalize(ap);
    validate(ap);

    TravelPlan plan = ap.getPlan();
    repository.findByAlgorithmNameAndParameterAndPlan(ap.getAlgorithmName(), ap.getParameter(), plan)
        .filter(existing -> !existing.getId().equals(ap.getId()))
        .ifPresent(existing -> { throw new IllegalArgumentException("Parámetro duplicado para el plan"); });

    return repository.save(ap);
  }

  public void delete(Integer id) {
    if (!repository.existsById(id)) throw new EntityNotFoundException("AlgorithmParameter not found with id: " + id);
    repository.deleteById(id);
  }

  @Transactional
  public void bulkDelete(List<Integer> ids) { repository.deleteAllByIdIn(ids); }

  public List<AlgorithmParameter> getByPlan(TravelPlan plan) { return repository.findByPlan(plan); }
  public List<AlgorithmParameter> getByAlgorithm(String algorithmName) { return repository.findByAlgorithmName(algorithmName); }
  public List<AlgorithmParameter> getByAlgorithmAndPlan(String algorithmName, TravelPlan plan) {
    return repository.findByAlgorithmNameAndPlan(algorithmName, plan);
  }

  private void normalize(AlgorithmParameter ap) {
    if (ap.getAlgorithmName() != null) ap.setAlgorithmName(ap.getAlgorithmName().trim());
    if (ap.getParameter() != null) ap.setParameter(ap.getParameter().trim());
    if (ap.getValue() != null) ap.setValue(ap.getValue().trim());
  }

  private void validate(AlgorithmParameter ap) {
    if (ap.getAlgorithmName() == null || ap.getAlgorithmName().isBlank())
      throw new IllegalArgumentException("algorithmName requerido");
    if (ap.getParameter() == null || ap.getParameter().isBlank())
      throw new IllegalArgumentException("parameter requerido");
    if (ap.getValue() == null || ap.getValue().isBlank())
      throw new IllegalArgumentException("value requerido");
  }
}
