package com.system.morapack.dao.morapack_psql.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "routes")
public class Route {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @ManyToMany
  @JoinTable(
      name = "route_flights",
      joinColumns = @JoinColumn(name = "route_id"),
      inverseJoinColumns = @JoinColumn(name = "flight_id")
  )
  private List<Flight> flights;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "origin_city_id", nullable = false)
  private City originCity;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "destination_city_id", nullable = false)
  private City destinationCity;

  @Column(name = "total_time", nullable = false)
  private Double totalTime;

  @Column(name = "total_cost", nullable = false)
  private Double totalCost;

  @ManyToMany
  @JoinTable(
      name = "route_packages",
      joinColumns = @JoinColumn(name = "route_id"),
      inverseJoinColumns = @JoinColumn(name = "package_id")
  )
  private List<Package> packages;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "solution_id")
  private Solution solution;
}
