package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.ScenarioParametersAdapter;
import com.system.morapack.schemas.ScenarioParametersSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenarioParametersController {

  private final ScenarioParametersAdapter adapter;

  public ScenarioParametersSchema get(Integer id) { return adapter.get(id); }
  public List<ScenarioParametersSchema> fetch(List<Integer> ids) { return adapter.fetch(ids); }

  public ScenarioParametersSchema create(ScenarioParametersSchema req) { return adapter.create(req); }
  public List<ScenarioParametersSchema> bulkCreate(List<ScenarioParametersSchema> reqs) { return adapter.bulkCreate(reqs); }
  public ScenarioParametersSchema update(Integer id, ScenarioParametersSchema req) { return adapter.update(id, req); }
  public void delete(Integer id) { adapter.delete(id); }
  public void bulkDelete(List<Integer> ids) { adapter.bulkDelete(ids); }

  public List<ScenarioParametersSchema> getByType(String type) { return adapter.getByType(type); }
  public List<ScenarioParametersSchema> getByPolicy(String policy) { return adapter.getByPolicy(policy); }
  public List<ScenarioParametersSchema> getByTimeWindowRange(Double min, Double max) { return adapter.getByTimeWindowRange(min, max); }
}