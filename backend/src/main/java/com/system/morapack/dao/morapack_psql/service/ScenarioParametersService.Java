package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.ScenarioParameters;
import com.system.morapack.dao.morapack_psql.repository.ScenarioParametersRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenarioParametersService {

  private final ScenarioParametersRepository repository;

  public ScenarioParameters get(Integer id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("ScenarioParameters not found with id: " + id));
  }

  public List<ScenarioParameters> fetch(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return repository.findAll();
    return repository.findByIdIn(ids);
  }

  public ScenarioParameters create(ScenarioParameters sp) {
    validate(sp);
    if (repository.existsByTypeAndPolicy(sp.getType(), sp.getPolicy()))
      throw new IllegalArgumentException("Parámetro de escenario duplicado para type+policy");
    return repository.save(sp);
  }

  public List<ScenarioParameters> bulkCreate(List<ScenarioParameters> list) {
    list.forEach(this::validate);
    for (ScenarioParameters sp : list) {
      if (repository.existsByTypeAndPolicy(sp.getType(), sp.getPolicy()))
        throw new IllegalArgumentException("Duplicado: " + sp.getType() + " / " + sp.getPolicy());
    }
    return repository.saveAll(list);
  }

  @Transactional
  public ScenarioParameters update(Integer id, ScenarioParameters patch) {
    ScenarioParameters sp = get(id);

    if (patch.getType() != null) sp.setType(patch.getType());
    if (patch.getPolicy() != null) sp.setPolicy(patch.getPolicy());
    if (patch.getTimeWindowHours() != null) sp.setTimeWindowHours(patch.getTimeWindowHours());

    validate(sp);

    repository.findByTypeAndPolicy(sp.getType(), sp.getPolicy())
        .filter(existing -> !existing.getId().equals(sp.getId()))
        .ifPresent(existing -> { throw new IllegalArgumentException("Parámetro de escenario duplicado para type+policy"); });

    return repository.save(sp);
  }

  public void delete(Integer id) {
    if (!repository.existsById(id)) throw new EntityNotFoundException("ScenarioParameters not found with id: " + id);
    repository.deleteById(id);
  }

  @Transactional
  public void bulkDelete(List<Integer> ids) {
    repository.deleteAllByIdIn(ids);
  }

  public List<ScenarioParameters> getByType(String type) { return repository.findByType(type); }
  public List<ScenarioParameters> getByPolicy(String policy) { return repository.findByPolicy(policy); }
  public List<ScenarioParameters> getByTimeWindowRange(Double min, Double max) {
    return repository.findByTimeWindowHoursBetween(min, max);
  }

  private void validate(ScenarioParameters sp) {
    if (sp.getType() == null || sp.getType().isBlank())
      throw new IllegalArgumentException("type requerido");
    if (sp.getPolicy() == null || sp.getPolicy().isBlank())
      throw new IllegalArgumentException("policy requerido");
    if (sp.getTimeWindowHours() == null || sp.getTimeWindowHours() < 0)
      throw new IllegalArgumentException("timeWindowHours inválido");
  }
}
