package com.system.morapack.schemas;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class ScenarioParametersSchema {
  private Integer id;
  private String type;
  private Double timeWindowHours;
  private String policy;
}