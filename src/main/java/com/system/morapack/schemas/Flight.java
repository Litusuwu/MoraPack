package com.system.morapack.schemas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Flight {
    private int id;
    private double frequencyPerDay;
    private Airport originAirport;
    private Airport destinationAirport;
    private int maxCapacity;
    private int usedCapacity;
    private double transportTime;
    private double cost;
}
