// src/main/java/com/system/morapack/dao/morapack_psql/service/SolutionService.java
package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.Route;
import com.system.morapack.dao.morapack_psql.model.Solution;
import com.system.morapack.dao.morapack_psql.repository.SolutionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolutionService {

  private final SolutionRepository solutionRepository;

  public Solution getSolution(Integer id) {
    return solutionRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("SolutionSchema not found with id: " + id));
  }

  public List<Solution> fetchSolutions(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return solutionRepository.findAll();
    return solutionRepository.findByIdIn(ids);
  }

  public Solution createSolution(Solution s) {
    normalizeRoutesBackRef(s);
    validate(s);
    return solutionRepository.save(s);
  }

  public List<Solution> bulkCreateSolutions(List<Solution> list) {
    list.forEach(this::normalizeRoutesBackRef);
    list.forEach(this::validate);
    return solutionRepository.saveAll(list);
  }

  @Transactional
  public Solution updateSolution(Integer id, Solution patch) {
    Solution s = getSolution(id);

    if (patch.getTotalCost() != null) s.setTotalCost(patch.getTotalCost());
    if (patch.getTotalTime() != null) s.setTotalTime(patch.getTotalTime());
    if (patch.getUndeliveredPackages() != null) s.setUndeliveredPackages(patch.getUndeliveredPackages());
    if (patch.getFitness() != null) s.setFitness(patch.getFitness());

    // Reemplazo de rutas si vienen en el patch
    if (patch.getRoutes() != null) {
      // orphanRemoval=true: limpiar y volver a asociar
      if (s.getRoutes() == null) s.setRoutes(new ArrayList<>());
      s.getRoutes().clear();
      for (Route r : patch.getRoutes()) {
        r.setSolution(s);
        s.getRoutes().add(r);
      }
    }

    validate(s);
    return solutionRepository.save(s);
  }

  public void deleteSolution(Integer id) {
    if (!solutionRepository.existsById(id)) {
      throw new EntityNotFoundException("SolutionSchema not found with id: " + id);
    }
    solutionRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteSolutions(List<Integer> ids) {
    solutionRepository.deleteAllByIdIn(ids);
  }

  public List<Solution> getByCostRange(Double min, Double max) {
    return solutionRepository.findByTotalCostBetween(min, max);
  }

  public List<Solution> getByTimeRange(Double min, Double max) {
    return solutionRepository.findByTotalTimeBetween(min, max);
  }

  public List<Solution> getByFitnessRange(Double min, Double max) {
    return solutionRepository.findByFitnessBetween(min, max);
  }

  public List<Solution> getWithUndeliveredAtMost(Integer max) {
    return solutionRepository.findByUndeliveredPackagesLessThanEqual(max);
  }

  private void normalizeRoutesBackRef(Solution s) {
    if (s.getRoutes() == null) return;
    for (Route r : s.getRoutes()) r.setSolution(s);
  }

  private void validate(Solution s) {
    if (s.getTotalCost() == null || s.getTotalCost() < 0) throw new IllegalArgumentException("totalCost inválido");
    if (s.getTotalTime() == null || s.getTotalTime() < 0) throw new IllegalArgumentException("totalTime inválido");
    if (s.getUndeliveredPackages() == null || s.getUndeliveredPackages() < 0)
      throw new IllegalArgumentException("undeliveredPackages inválido");
    if (s.getFitness() == null) throw new IllegalArgumentException("fitness requerido");
  }
}
