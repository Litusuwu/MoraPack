package src.schemas;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Getter
@Setter
public class Solution {
    private List<Route> routes;
    private Map<Package, Route> packageRouteMap;
    private double totalCost;
    private double totalTime;
    private int undeliveredPackages;
    private double fitness;
    
    public Solution() {
        this.routes = new ArrayList<>();
        this.packageRouteMap = new HashMap<>();
        this.totalCost = 0.0;
        this.totalTime = 0.0;
        this.undeliveredPackages = 0;
        this.fitness = Double.MAX_VALUE;
    }
    
    public Solution(Solution other) {
        // Deep copy constructor for Tabu Search
        this.routes = new ArrayList<>();
        this.packageRouteMap = new HashMap<>();
        
        for (Route route : other.routes) {
            Route newRoute = new Route(route.getOriginCity(), route.getDestinationCity());
            newRoute.setFlights(new ArrayList<>(route.getFlights()));
            newRoute.setPackages(new ArrayList<>(route.getPackages()));
            newRoute.setTotalTime(route.getTotalTime());
            newRoute.setTotalCost(route.getTotalCost());
            this.routes.add(newRoute);
        }
        
        this.packageRouteMap.putAll(other.packageRouteMap);
        this.totalCost = other.totalCost;
        this.totalTime = other.totalTime;
        this.undeliveredPackages = other.undeliveredPackages;
        this.fitness = other.fitness;
    }
    
    public void addRoute(Route route) {
        routes.add(route);
        updateMetrics();
    }
    
    public void assignPackageToRoute(Package pkg, Route route) {
        packageRouteMap.put(pkg, route);
        route.addPackage(pkg);
        updateMetrics();
    }
    
    public void updateMetrics() {
        totalCost = 0.0;
        totalTime = 0.0;
        undeliveredPackages = 0;
        
        for (Route route : routes) {
            totalCost += route.getTotalCost();
            if (route.getTotalTime() > totalTime) {
                totalTime = route.getTotalTime();
            }
            
            // Count packages that exceed delivery deadline
            if (!route.isValidRoute()) {
                undeliveredPackages += route.getPackages().size();
            }
        }
        
        calculateFitness();
    }
    
    private void calculateFitness() {
        // Fitness function: minimize cost + penalize undelivered packages
        double penaltyFactor = 1000.0;
        fitness = totalCost + (undeliveredPackages * penaltyFactor);
    }
    
    public boolean isValid() {
        // Check if all packages are assigned and all routes are valid
        for (Route route : routes) {
            if (!route.isValidRoute()) {
                return false;
            }
        }
        return true;
    }
}
