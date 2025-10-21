package com.system.morapack.schemas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RouteSchema {
    private int id;
    private List<FlightSchema> flightSchemas;
    private CitySchema originCitySchema;
    private CitySchema destinationCitySchema;
    private double totalTime;
    private double totalCost;
    private List<OrderSchema> orderSchemas;
}
