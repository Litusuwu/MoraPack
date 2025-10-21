package com.system.morapack.dao.morapack_psql.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "warehouses")
public class Warehouse {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  // Relación bidireccional con AirportSchema.
  // AirportSchema es el lado propietario con @JoinColumn(name = "warehouse_id")
  @OneToOne(mappedBy = "warehouse", fetch = FetchType.LAZY)
  private Airport airport;

  @Column(name = "max_capacity", nullable = false)
  private Integer maxCapacity;

  @Column(name = "used_capacity", nullable = false)
  private Integer usedCapacity;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "is_main_warehouse", nullable = false)
  private Boolean isMainWarehouse;
}
