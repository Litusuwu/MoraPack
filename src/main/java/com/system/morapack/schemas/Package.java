package src.schemas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Package {
    private int id;
    private Customer customer;
    private City destinationCity;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDeadline;
    private PackageStatus status;
    private City currentLocation;
    private Route assignedRoute;
    private double priority; // For optimization purposes
    
    public Package(int id, Customer customer, City destinationCity, LocalDateTime orderDate) {
        this.id = id;
        this.customer = customer;
        this.destinationCity = destinationCity;
        this.orderDate = orderDate;
        this.status = PackageStatus.PENDING;
        this.priority = 1.0;
        calculateDeliveryDeadline();
    }
    
    private void calculateDeliveryDeadline() {
        // Calculate deadline based on continent constraints
        if (customer.getOriginWarehouse().getAirport().getCity().getContinent() == 
            destinationCity.getContinent()) {
            // Same continent: 2 days maximum
            this.deliveryDeadline = orderDate.plusDays(2);
        } else {
            // Different continent: 3 days maximum
            this.deliveryDeadline = orderDate.plusDays(3);
        }
    }
}
