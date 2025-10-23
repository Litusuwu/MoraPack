package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class TrackingEventSchema {
  private Integer id;
  private LocalDateTime timestamp;
  private TrackingEventType type;
  private Integer cityId;
  private Integer orderId;
  private Integer segmentId;
  private LocalDateTime createdAt;
}