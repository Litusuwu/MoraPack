package com.system.morapack.schemas;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class WarehouseSchema {
  private Integer id;
  private AirportSchema airportSchema; // optional, read-only hint
  private Integer maxCapacity;
  private Integer usedCapacity;
  private String name;
  private Boolean isMainWarehouse;
}