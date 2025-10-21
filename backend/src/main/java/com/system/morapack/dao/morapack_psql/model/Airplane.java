package com.system.morapack.dao.morapack_psql.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "airplanes")
public class Airplane {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @Column(name = "registration", nullable = false, length = 120)
  private String registration;

  @Column(name = "model", nullable = false, length = 120)
  private String model;

  @Column(name = "capacity", nullable = false)
  private Integer capacity;

  @Column(name = "status", nullable = false, length = 64)
  private String status;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
