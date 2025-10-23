
package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.ScenarioParameters;
import com.system.morapack.dao.morapack_psql.service.ScenarioParametersService;
import com.system.morapack.schemas.ScenarioParametersSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScenarioParametersAdapter {

  private final ScenarioParametersService scenarioParametersService;

  private ScenarioParametersSchema toSchema(ScenarioParameters sp) {
    return ScenarioParametersSchema.builder()
        .id(sp.getId())
        .type(sp.getType())
        .timeWindowHours(sp.getTimeWindowHours())
        .policy(sp.getPolicy())
        .build();
  }

  private ScenarioParameters toEntity(ScenarioParametersSchema s) {
    return ScenarioParameters.builder()
        .id(s.getId())
        .type(s.getType())
        .timeWindowHours(s.getTimeWindowHours())
        .policy(s.getPolicy())
        .build();
  }

  // CRUD
  public ScenarioParametersSchema get(Integer id) {
    return toSchema(scenarioParametersService.get(id));
  }

  public List<ScenarioParametersSchema> fetch(List<Integer> ids) {
    return scenarioParametersService.fetch(ids).stream().map(this::toSchema).toList();
  }

  public ScenarioParametersSchema create(ScenarioParametersSchema req) {
    return toSchema(scenarioParametersService.create(toEntity(req)));
  }

  public List<ScenarioParametersSchema> bulkCreate(List<ScenarioParametersSchema> reqs) {
    var entities = reqs.stream().map(this::toEntity).toList();
    return scenarioParametersService.bulkCreate(entities).stream().map(this::toSchema).toList();
  }

  public ScenarioParametersSchema update(Integer id, ScenarioParametersSchema req) {
    return toSchema(scenarioParametersService.update(id, toEntity(req)));
  }

  public void delete(Integer id) { scenarioParametersService.delete(id); }

  public void bulkDelete(List<Integer> ids) { scenarioParametersService.bulkDelete(ids); }

  // Queries
  public List<ScenarioParametersSchema> getByType(String type) {
    return scenarioParametersService.getByType(type).stream().map(this::toSchema).toList();
  }
  public List<ScenarioParametersSchema> getByPolicy(String policy) {
    return scenarioParametersService.getByPolicy(policy).stream().map(this::toSchema).toList();
  }
  public List<ScenarioParametersSchema> getByTimeWindowRange(Double min, Double max) {
    return scenarioParametersService.getByTimeWindowRange(min, max).stream().map(this::toSchema).toList();
  }
}