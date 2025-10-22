package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanSchema {
    private Integer id;
    private LocalDateTime planningDate;
    private String status;
    private String selectedAlgorithm;
    private String datasetVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer orderId;
    private String orderName;
    private List<FlightSegmentSchema> flightSegments;
}
