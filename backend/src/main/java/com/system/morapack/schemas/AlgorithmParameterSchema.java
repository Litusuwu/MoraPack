package com.system.morapack.schemas;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class AlgorithmParameterSchema {
  private Integer id;
  private String algorithmName;
  private String parameter;
  private String value;
  private Integer planId; // TravelPlan FK
}