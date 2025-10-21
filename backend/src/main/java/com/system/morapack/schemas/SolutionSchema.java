package com.system.morapack.schemas;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Solution {
    private List<Route> routes;
    private Map<OrderSchema, Route> packageRouteMap;
    private double totalCost;
    private double totalTime;
    private int undeliveredPackages;
    private double fitness;
}
