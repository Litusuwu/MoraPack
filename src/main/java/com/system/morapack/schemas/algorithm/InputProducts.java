package com.system.morapack.schemas.algorithm;

import com.system.morapack.schemas.Airport;
import com.system.morapack.schemas.City;
import com.system.morapack.schemas.Continent;
import com.system.morapack.schemas.Customer;
import com.system.morapack.schemas.Package;
import com.system.morapack.schemas.PackageStatus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class InputProducts {
    private ArrayList<Package> packages;
    private final String filePath;
    private ArrayList<Airport> airports;
    private Map<String, Airport> airportMap;
    private Random random;

    public InputProducts(String filePath, ArrayList<Airport> airports) {
        this.filePath = filePath;
        this.packages = new ArrayList<>();
        this.airports = airports;
        this.random = new Random();
        createAirportMap();
    }

    private void createAirportMap() {
        this.airportMap = new HashMap<>();
        for (Airport airport : airports) {
            airportMap.put(airport.getCodeIATA(), airport);
        }
    }

    public ArrayList<Package> readProducts() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int packageId = 1;
            
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Parse product data
                // Format: PRIORITY(01/04/12/24) HOUR MINUTE DESTINATION_AIRPORT CUSTOMER_ID PACKAGE_ID
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 6) {
                    int priority = Integer.parseInt(parts[0]);
                    int hour = Integer.parseInt(parts[1]);
                    int minute = Integer.parseInt(parts[2]);
                    String destinationAirportCode = parts[3];
                    int customerId = Integer.parseInt(parts[4]);
                    // Package code is available in parts[5] but not used currently
                    
                    // Find destination airport
                    Airport destinationAirport = airportMap.get(destinationAirportCode);
                    
                    if (destinationAirport != null) {
                        // Create customer
                        Customer customer = new Customer();
                        customer.setId(customerId);
                        customer.setName("Customer " + customerId);
                        customer.setEmail("customer" + customerId + "@example.com");
                        customer.setDeliveryCity(destinationAirport.getCity());
                        
                        // Calculate order date and delivery deadline
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime orderDate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                        
                        // If the orderDate is in the past, set it to tomorrow
                        if (orderDate.isBefore(now)) {
                            orderDate = orderDate.plusDays(1);
                        }
                        
                        // Set delivery deadline based on priority (in days)
                        LocalDateTime deliveryDeadline;
                        switch (priority) {
                            case 1:  // Highest priority - 1 day
                                deliveryDeadline = orderDate.plus(1, ChronoUnit.DAYS);
                                break;
                            case 4:  // Medium priority - 4 days
                                deliveryDeadline = orderDate.plus(4, ChronoUnit.DAYS);
                                break;
                            case 12: // Low priority - 12 days
                                deliveryDeadline = orderDate.plus(12, ChronoUnit.DAYS);
                                break;
                            case 24: // Lowest priority - 24 days
                                deliveryDeadline = orderDate.plus(24, ChronoUnit.DAYS);
                                break;
                            default: // Default to 7 days
                                deliveryDeadline = orderDate.plus(7, ChronoUnit.DAYS);
                                break;
                        }
                        
                        // Create Package object
                        Package pkg = new Package();
                        pkg.setId(packageId++);
                        pkg.setCustomer(customer);
                        pkg.setDestinationCity(destinationAirport.getCity());
                        pkg.setOrderDate(orderDate);
                        pkg.setDeliveryDeadline(deliveryDeadline);
                        pkg.setStatus(PackageStatus.PENDING);
                        
                        // Assume the package starts at a random warehouse in a different continent
                        City currentLocation = getRandomWarehouseLocation(destinationAirport.getCity().getContinent());
                        pkg.setCurrentLocation(currentLocation);
                        
                        // Set priority based on delivery time window
                        double priorityValue = calculatePriority(orderDate, deliveryDeadline);
                        pkg.setPriority(priorityValue);
                        
                        packages.add(pkg);
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error reading product data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return packages;
    }
    
    private City getRandomWarehouseLocation(Continent destinationContinent) {
        // Choose a different continent than the destination
        Continent[] continents = Continent.values();
        Continent sourceContinent;
        do {
            sourceContinent = continents[random.nextInt(continents.length)];
        } while (sourceContinent == destinationContinent);
        
        // Find airports in the chosen continent
        ArrayList<Airport> continentAirports = new ArrayList<>();
        for (Airport airport : airports) {
            if (airport.getCity().getContinent() == sourceContinent) {
                continentAirports.add(airport);
            }
        }
        
        // If no airports found in other continents, pick any airport
        if (continentAirports.isEmpty()) {
            Airport randomAirport = airports.get(random.nextInt(airports.size()));
            return randomAirport.getCity();
        }
        
        // Return a random airport from the chosen continent
        Airport randomAirport = continentAirports.get(random.nextInt(continentAirports.size()));
        return randomAirport.getCity();
    }
    
    private double calculatePriority(LocalDateTime orderDate, LocalDateTime deliveryDeadline) {
        // Calculate priority based on time window
        long hours = ChronoUnit.HOURS.between(orderDate, deliveryDeadline);
        
        // Normalize priority: shorter delivery windows get higher priority (1.0 is highest)
        if (hours <= 24) {
            return 1.0; // Highest priority for 1-day delivery
        } else if (hours <= 96) {
            return 0.75; // High priority for 4-day delivery
        } else if (hours <= 288) {
            return 0.5; // Medium priority for 12-day delivery
        } else {
            return 0.25; // Low priority for 24-day delivery
        }
    }
}
