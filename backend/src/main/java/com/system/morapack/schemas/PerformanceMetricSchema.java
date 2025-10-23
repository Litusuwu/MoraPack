package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class PerformanceMetricSchema {
  private Integer id;
  private String name;
  private Double value;
  private String period;
  private LocalDateTime createdAt;

  // Foreign keys
  private Integer planId;
  private Integer eventId;
  private Integer scenarioId;
  private Integer algorithmId;
}