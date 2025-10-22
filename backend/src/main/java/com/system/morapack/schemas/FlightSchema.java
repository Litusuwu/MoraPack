package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FlightSchema {
    private Integer id;
    private String code;
    private String routeType;
    private Integer maxCapacity;
    private Double transportTimeDays;
    private Integer dailyFrequency;
    private String status;
    private LocalDateTime createdAt;
    private Integer airplaneId;
    private Integer originAirportId;
    private String originAirportCode;
    private Integer destinationAirportId;
    private String destinationAirportCode;

    // Legacy fields for algorithm compatibility
    private double frequencyPerDay;
    private AirportSchema originAirportSchema;
    private AirportSchema destinationAirportSchema;
    private int usedCapacity;
    private double transportTime;
    private double cost;
}
