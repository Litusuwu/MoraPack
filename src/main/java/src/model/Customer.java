package src.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Customer {
    private int id;
    private String name;
    private String email;
    private City deliveryCity;
    private Warehouse originWarehouse; // Which warehouse will serve this customer
    
    public Customer(int id, String name, String email, City deliveryCity) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.deliveryCity = deliveryCity;
        // originWarehouse will be assigned later via setOriginWarehouse()
    }
    
    public Customer(int id, String name, String email, City deliveryCity, Warehouse originWarehouse) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.deliveryCity = deliveryCity;
        this.originWarehouse = originWarehouse;
    }
    
    public void assignOriginWarehouse(List<Warehouse> availableWarehouses) {
        // Find the best warehouse based on delivery city continent
        Warehouse bestWarehouse = null;
        
        // First priority: same continent main warehouse
        for (Warehouse warehouse : availableWarehouses) {
            if (warehouse.isMainWarehouse() && 
                warehouse.getAirport().getCity().getContinent() == deliveryCity.getContinent()) {
                bestWarehouse = warehouse;
                break;
            }
        }
        
        // Fallback: any main warehouse
        if (bestWarehouse == null) {
            bestWarehouse = availableWarehouses.stream()
                    .filter(Warehouse::isMainWarehouse)
                    .findFirst()
                    .orElse(null);
        }
        
        // Final fallback: any warehouse
        if (bestWarehouse == null && !availableWarehouses.isEmpty()) {
            bestWarehouse = availableWarehouses.get(0);
        }
        
        this.originWarehouse = bestWarehouse;
    }
}
