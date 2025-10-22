package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSegmentSchema {
    private Integer id;
    private LocalDateTime estimateTimeDestination;
    private LocalDateTime estimatedTimeArrival;
    private Integer reservedCapacity;
    private LocalDateTime createdAt;
    private Integer planId;
    private Integer orderId;
    private String orderName;
}
