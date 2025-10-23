package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class AirplaneSchema {
  private Integer id;
  private String registration;
  private String model;
  private Integer capacity;
  private String status;
  private LocalDateTime createdAt;
}