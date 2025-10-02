package com.system.morapack.schemas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;

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
    private double priority;
    private ArrayList<Product> products;
}
