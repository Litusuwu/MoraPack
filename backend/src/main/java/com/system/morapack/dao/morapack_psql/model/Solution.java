package com.system.morapack.dao.morapack_psql.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "solutions")
public class Solution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @OneToMany(mappedBy = "solution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<Route> routes;

  // No se mapea directamente un Map<OrderSchema, RouteSchema>. Úsalo en memoria o modela una entidad intermedia.
  @Transient
  private Map<Package, Route> packageRouteMap;

  @Column(name = "total_cost", nullable = false)
  private Double totalCost;

  @Column(name = "total_time", nullable = false)
  private Double totalTime;

  @Column(name = "undelivered_packages", nullable = false)
  private Integer undeliveredPackages;

  @Column(name = "fitness", nullable = false)
  private Double fitness;
}
