package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.SolutionAdapter;
import com.system.morapack.schemas.SolutionSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SolutionController {

  private final SolutionAdapter solutionAdapter;

  public SolutionSchema getSolution(Integer id) { return solutionAdapter.getSolution(id); }

  public List<SolutionSchema> fetchSolutions(List<Integer> ids) { return solutionAdapter.fetchSolutions(ids); }

  public SolutionSchema createSolution(SolutionSchema req) { return solutionAdapter.createSolution(req); }

  public List<SolutionSchema> bulkCreateSolutions(List<SolutionSchema> reqs) { return solutionAdapter.bulkCreateSolutions(reqs); }

  public SolutionSchema updateSolution(Integer id, SolutionSchema req) { return solutionAdapter.updateSolution(id, req); }

  public void deleteSolution(Integer id) { solutionAdapter.deleteSolution(id); }

  public void bulkDeleteSolutions(List<Integer> ids) { solutionAdapter.bulkDeleteSolutions(ids); }

  // queries
  public List<SolutionSchema> getByCostRange(Double min, Double max) { return solutionAdapter.getByCostRange(min, max); }
  public List<SolutionSchema> getByTimeRange(Double min, Double max) { return solutionAdapter.getByTimeRange(min, max); }
  public List<SolutionSchema> getByFitnessRange(Double min, Double max) { return solutionAdapter.getByFitnessRange(min, max); }
  public List<SolutionSchema> getWithUndeliveredAtMost(Integer max) { return solutionAdapter.getWithUndeliveredAtMost(max); }
}