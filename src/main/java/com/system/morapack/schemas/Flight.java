package src.schemas;

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
    private double transportTime; // in days
    private double cost;
    
    public Flight(int id, Airport originAirport, Airport destinationAirport, double frequencyPerDay) {
        this.id = id;
        this.originAirport = originAirport;
        this.destinationAirport = destinationAirport;
        this.frequencyPerDay = frequencyPerDay;
        this.usedCapacity = 0;
        calculateCapacityAndTime();
    }
    
    private void calculateCapacityAndTime() {
        boolean sameContinent = originAirport.getCity().getContinent() == 
                               destinationAirport.getCity().getContinent();
        
        if (sameContinent) {
            // Same continent flight
            this.maxCapacity = Constants.SAME_CONTINENT_MIN_CAPACITY + 
                              (int)(Math.random() * (Constants.SAME_CONTINENT_MAX_CAPACITY - 
                                                   Constants.SAME_CONTINENT_MIN_CAPACITY));
            this.transportTime = Constants.SAME_CONTINENT_TRANSPORT_TIME;
        } else {
            // Different continent flight
            this.maxCapacity = Constants.DIFFERENT_CONTINENT_MIN_CAPACITY + 
                              (int)(Math.random() * (Constants.DIFFERENT_CONTINENT_MAX_CAPACITY - 
                                                   Constants.DIFFERENT_CONTINENT_MIN_CAPACITY));
            this.transportTime = Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME;
        }
        
        // Base cost calculation (can be enhanced with more sophisticated pricing)
        this.cost = transportTime * 100 + maxCapacity * 0.1;
    }
    
    public int getAvailableCapacity() {
        return maxCapacity - usedCapacity;
    }
    
    public boolean canAccommodatePackages(int packageCount) {
        return getAvailableCapacity() >= packageCount;
    }
    
    public void addPackages(int packageCount) {
        if (canAccommodatePackages(packageCount)) {
            usedCapacity += packageCount;
        } else {
            throw new IllegalArgumentException("Flight cannot accommodate " + packageCount + " packages");
        }
    }
    
    public void removePackages(int packageCount) {
        if (usedCapacity >= packageCount) {
            usedCapacity -= packageCount;
        } else {
            throw new IllegalArgumentException("Cannot remove more packages than currently loaded");
        }
    }
    
    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
    
    public void setTransportTime(double transportTime) {
        this.transportTime = transportTime;
    }
    
    public void setCost(double cost) {
        this.cost = cost;
    }
}
