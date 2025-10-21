package com.system.morapack.dao.morapack_psql.model;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cliente")
public class City {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer idCiudad;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "continent", nullable = false)
  private String continent;
}
