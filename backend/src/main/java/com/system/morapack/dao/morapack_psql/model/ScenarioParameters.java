package com.system.morapack.dao.morapack_psql.model;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "scenario_parameters")
public class ScenarioParameters {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_parameter", nullable = false)
  private Integer id;

  @Column(name = "type", nullable = false, length = 64)
  private String type;

  @Column(name = "time_window_hours", nullable = false)
  private Double timeWindowHours;

  @Column(name = "policy", nullable = false, length = 128)
  private String policy;
}
