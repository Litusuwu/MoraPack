package src.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Airport {
    private int id;
    private String codeIATA;
    private String alias;
    private int timezoneUTC;
    private String latitude;
    private String longitude;
    private double usedCapacity;
    private double maxCapacity;
    private City city;
    private AirportState state;
    private Warehouse warehouse;
    
    public Airport(int id, String codeIATA, String alias, City city) {
        this.id = id;
        this.codeIATA = codeIATA;
        this.alias = alias;
        this.city = city;
        this.state = AirportState.Avaiable;
        this.usedCapacity = 0.0;
        this.maxCapacity = Constants.WAREHOUSE_MIN_CAPACITY + 
                          (Math.random() * (Constants.WAREHOUSE_MAX_CAPACITY - Constants.WAREHOUSE_MIN_CAPACITY));
    }
    
    public boolean isOperational() {
        return state == AirportState.Avaiable;
    }
    
    public double getAvailableCapacity() {
        return maxCapacity - usedCapacity;
    }
}
