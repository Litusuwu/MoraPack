package com.system.morapack.schemas;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SolutionSchema {
    private List<RouteSchema> routeSchemas;
    private Map<OrderSchema, RouteSchema> packageRouteMap;
    private double totalCost;
    private double totalTime;
    private int undeliveredPackages;
    private double fitness;
}
