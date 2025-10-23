package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.AlgorithmParameter;
import com.system.morapack.dao.morapack_psql.model.TravelPlan;
import com.system.morapack.dao.morapack_psql.service.AlgorithmParameterService;
import com.system.morapack.dao.morapack_psql.service.TravelPlanService;
import com.system.morapack.schemas.AlgorithmParameterSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AlgorithmParameterAdapter {

  private final AlgorithmParameterService service;
  private final TravelPlanService planService;

  private AlgorithmParameterSchema toSchema(AlgorithmParameter ap) {
    return AlgorithmParameterSchema.builder()
        .id(ap.getId())
        .algorithmName(ap.getAlgorithmName())
        .parameter(ap.getParameter())
        .value(ap.getValue())
        .planId(ap.getPlan() != null ? ap.getPlan().getId() : null)
        .build();
  }

  private AlgorithmParameter toEntity(AlgorithmParameterSchema s) {
    AlgorithmParameter.AlgorithmParameterBuilder b = AlgorithmParameter.builder()
        .id(s.getId())
        .algorithmName(s.getAlgorithmName())
        .parameter(s.getParameter())
        .value(s.getValue());
    if (s.getPlanId() != null) {
      TravelPlan plan = planService.getTravelPlan(s.getPlanId());
      b.plan(plan);
    }
    return b.build();
  }

  // CRUD
  public AlgorithmParameterSchema get(Integer id) { return toSchema(service.get(id)); }

  public List<AlgorithmParameterSchema> fetch(List<Integer> ids) {
    return service.fetch(ids).stream().map(this::toSchema).toList();
  }

  public AlgorithmParameterSchema create(AlgorithmParameterSchema req) {
    return toSchema(service.create(toEntity(req)));
  }

  public List<AlgorithmParameterSchema> bulkCreate(List<AlgorithmParameterSchema> reqs) {
    var entities = reqs.stream().map(this::toEntity).toList();
    return service.bulkCreate(entities).stream().map(this::toSchema).toList();
  }

  public AlgorithmParameterSchema update(Integer id, AlgorithmParameterSchema req) {
    return toSchema(service.update(id, toEntity(req)));
  }

  public void delete(Integer id) { service.delete(id); }

  public void bulkDelete(List<Integer> ids) { service.bulkDelete(ids); }

  // Queries
  public List<AlgorithmParameterSchema> getByPlan(Integer planId) {
    TravelPlan plan = planService.getTravelPlan(planId);
    return service.getByPlan(plan).stream().map(this::toSchema).toList();
  }

  public List<AlgorithmParameterSchema> getByAlgorithm(String algorithmName) {
    return service.getByAlgorithm(algorithmName).stream().map(this::toSchema).toList();
  }

  public List<AlgorithmParameterSchema> getByAlgorithmAndPlan(String algorithmName, Integer planId) {
    TravelPlan plan = planService.getTravelPlan(planId);
    return service.getByAlgorithmAndPlan(algorithmName, plan).stream().map(this::toSchema).toList();
  }
}