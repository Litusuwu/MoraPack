package com.system.morapack.dao.morapack_psql.model;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "algorithm_parameters")
public class AlgorithmParameter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_parameter", nullable = false)
  private Integer id;

  @Column(name = "algorithm_name", nullable = false, length = 120)
  private String algorithmName;

  @Column(name = "parameter", nullable = false, length = 120)
  private String parameter;

  @Column(name = "value", nullable = false, length = 256)
  private String value;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id")
  private Plan plan;
}
