package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.AlgorithmParameterAdapter;
import com.system.morapack.schemas.AlgorithmParameterSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlgorithmParameterController {

  private final AlgorithmParameterAdapter adapter;

  public AlgorithmParameterSchema get(Integer id) { return adapter.get(id); }
  public List<AlgorithmParameterSchema> fetch(List<Integer> ids) { return adapter.fetch(ids); }

  public AlgorithmParameterSchema create(AlgorithmParameterSchema req) { return adapter.create(req); }
  public List<AlgorithmParameterSchema> bulkCreate(List<AlgorithmParameterSchema> reqs) { return adapter.bulkCreate(reqs); }

  public AlgorithmParameterSchema update(Integer id, AlgorithmParameterSchema req) { return adapter.update(id, req); }

  public void delete(Integer id) { adapter.delete(id); }
  public void bulkDelete(List<Integer> ids) { adapter.bulkDelete(ids); }

  public List<AlgorithmParameterSchema> getByPlan(Integer planId) { return adapter.getByPlan(planId); }
  public List<AlgorithmParameterSchema> getByAlgorithm(String algorithmName) { return adapter.getByAlgorithm(algorithmName); }
  public List<AlgorithmParameterSchema> getByAlgorithmAndPlan(String algorithmName, Integer planId) {
    return adapter.getByAlgorithmAndPlan(algorithmName, planId);
  }
}