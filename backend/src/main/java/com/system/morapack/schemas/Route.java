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
public class Route {
    private int id;
    private List<Flight> flights;
    private City originCity;
    private City destinationCity;
    private double totalTime;
    private double totalCost;
    private List<Package> packages;
}
