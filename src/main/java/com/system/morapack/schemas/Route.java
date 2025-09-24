package src.schemas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Route {
    private int id;
    private List<Flight> flights;
    private City originCity;
    private City destinationCity;
    private double totalTime; // in days
    private double totalCost;
    private List<Package> packages;
    
    public Route(City originCity, City destinationCity) {
        this.originCity = originCity;
        this.destinationCity = destinationCity;
        this.flights = new ArrayList<>();
        this.packages = new ArrayList<>();
        this.totalTime = 0.0;
        this.totalCost = 0.0;
    }
    
    public void addFlight(Flight flight) {
        flights.add(flight);
        calculateTotalTime();
    }
    
    public void addPackage(Package pkg) {
        packages.add(pkg);
    }
    
    private void calculateTotalTime() {
        totalTime = 0.0;
        for (Flight flight : flights) {
            // Add transport time based on continent constraints
            if (flight.getOriginAirport().getCity().getContinent() == 
                flight.getDestinationAirport().getCity().getContinent()) {
                totalTime += 0.5; // Same continent: 0.5 days
            } else {
                totalTime += 1.0; // Different continent: 1 day
            }
        }
    }
    
    public boolean isValidRoute() {
        // Check if route meets deadline constraints
        boolean sameContinent = originCity.getContinent() == destinationCity.getContinent();
        double maxAllowedTime = sameContinent ? 2.0 : 3.0;
        return totalTime <= maxAllowedTime;
    }
    
    public int getTotalPackages() {
        return packages.size();
    }
}
