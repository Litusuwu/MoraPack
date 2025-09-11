package src.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Warehouse {
    private int id;
    private Airport airport;
    private int maxCapacity;
    private int usedCapacity;
    private String name;
    private boolean isMainWarehouse; // Lima, Brussels, or Baku
    
    public Warehouse(int id, Airport airport, String name, boolean isMainWarehouse) {
        this.id = id;
        this.airport = airport;
        this.name = name;
        this.isMainWarehouse = isMainWarehouse;
        this.usedCapacity = 0;
        
        // Set capacity based on warehouse type
        if (isMainWarehouse) {
            this.maxCapacity = Constants.WAREHOUSE_MAX_CAPACITY;
        } else {
            this.maxCapacity = Constants.WAREHOUSE_MIN_CAPACITY + 
                              (int)(Math.random() * (Constants.WAREHOUSE_MAX_CAPACITY - 
                                                   Constants.WAREHOUSE_MIN_CAPACITY));
        }
    }
    
    public int getAvailableCapacity() {
        return maxCapacity - usedCapacity;
    }
    
    public boolean canStorePackages(int packageCount) {
        return getAvailableCapacity() >= packageCount;
    }
    
    public void storePackages(int packageCount) {
        if (canStorePackages(packageCount)) {
            usedCapacity += packageCount;
        } else {
            throw new IllegalArgumentException("Warehouse cannot store " + packageCount + " packages");
        }
    }
    
    public void removePackages(int packageCount) {
        if (usedCapacity >= packageCount) {
            usedCapacity -= packageCount;
        } else {
            throw new IllegalArgumentException("Cannot remove more packages than currently stored");
        }
    }
    
    public double getUtilizationRate() {
        return (double) usedCapacity / maxCapacity;
    }
    
    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
}
