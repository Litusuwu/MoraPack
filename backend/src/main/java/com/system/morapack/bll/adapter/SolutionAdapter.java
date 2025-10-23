package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.Route;
import com.system.morapack.dao.morapack_psql.model.Solution;
import com.system.morapack.dao.morapack_psql.service.RouteService;
import com.system.morapack.dao.morapack_psql.service.SolutionService;
import com.system.morapack.schemas.RouteSchema;
import com.system.morapack.schemas.SolutionSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SolutionAdapter {

  private final SolutionService solutionService;
  private final RouteService routeService;

  // ===== mapping =====
  private SolutionSchema toSchema(Solution s) {
    SolutionSchema out = new SolutionSchema();
    out.setTotalCost(s.getTotalCost());
    out.setTotalTime(s.getTotalTime());
    out.setUndeliveredPackages(s.getUndeliveredPackages());
    out.setFitness(s.getFitness());

    if (s.getRoutes() != null) {
      List<RouteSchema> rs = s.getRoutes().stream().map(this::toRouteSchemaMinimal).toList();
      out.setRouteSchemas(rs);
    }
    // packageRouteMap is transient in entity; omit or fill elsewhere.
    return out;
  }

  private RouteSchema toRouteSchemaMinimal(Route r) {
    RouteSchema rs = new RouteSchema();
    rs.setId(r.getId());
    rs.setTotalTime(r.getTotalTime());
    rs.setTotalCost(r.getTotalCost());
    return rs;
  }

  private Solution toEntity(SolutionSchema s) {
    Solution.SolutionBuilder b = Solution.builder()
        .totalCost(s.getTotalCost())
        .totalTime(s.getTotalTime())
        .undeliveredPackages(s.getUndeliveredPackages())
        .fitness(s.getFitness());

    if (s.getRouteSchemas() != null) {
      b.routes(s.getRouteSchemas().stream()
          .map(RouteSchema::getId)
          .distinct()
          .map(routeService::getRoute)
          .toList());
    }
    // packageRouteMap not persisted.
    return b.build();
  }

  // ===== facade =====
  public SolutionSchema getSolution(Integer id) {
    return toSchema(solutionService.getSolution(id));
  }

  public List<SolutionSchema> fetchSolutions(List<Integer> ids) {
    return solutionService.fetchSolutions(ids).stream().map(this::toSchema).toList();
  }

  public SolutionSchema createSolution(SolutionSchema req) {
    return toSchema(solutionService.createSolution(toEntity(req)));
  }

  public List<SolutionSchema> bulkCreateSolutions(List<SolutionSchema> reqs) {
    var entities = reqs.stream().map(this::toEntity).toList();
    return solutionService.bulkCreateSolutions(entities).stream().map(this::toSchema).toList();
  }

  public SolutionSchema updateSolution(Integer id, SolutionSchema req) {
    return toSchema(solutionService.updateSolution(id, toEntity(req)));
  }

  public void deleteSolution(Integer id) { solutionService.deleteSolution(id); }

  public void bulkDeleteSolutions(List<Integer> ids) { solutionService.bulkDeleteSolutions(ids); }

  // queries
  public List<SolutionSchema> getByCostRange(Double min, Double max) {
    return solutionService.getByCostRange(min, max).stream().map(this::toSchema).toList();
  }

  public List<SolutionSchema> getByTimeRange(Double min, Double max) {
    return solutionService.getByTimeRange(min, max).stream().map(this::toSchema).toList();
  }

  public List<SolutionSchema> getByFitnessRange(Double min, Double max) {
    return solutionService.getByFitnessRange(min, max).stream().map(this::toSchema).toList();
  }

  public List<SolutionSchema> getWithUndeliveredAtMost(Integer max) {
    return solutionService.getWithUndeliveredAtMost(max).stream().map(this::toSchema).toList();
  }
}