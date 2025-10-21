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
public class OrderSchema {
    private int id;
    private CustomerSchema customerSchema;
    private CitySchema destinationCitySchema;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDeadline;
    private PackageStatus status;
    private CitySchema currentLocation;
    private RouteSchema assignedRouteSchema;
    private double priority;
    private ArrayList<ProductSchema> productSchemas;
}
