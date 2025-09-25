package com.system.morapack.schemas.algorithm;

import com.system.morapack.schemas.Airport;
import com.system.morapack.schemas.City;
import com.system.morapack.schemas.Continent;
import com.system.morapack.schemas.Customer;
import com.system.morapack.schemas.Package;
import com.system.morapack.schemas.PackageStatus;
import com.system.morapack.schemas.Product;
import com.system.morapack.schemas.Status;

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
    private int productId = 1;

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
                // Format: dd-hh-mm-dest-###-IdClien
                // dd: días de prioridad (01/04/12/24)
                // hh: horas (01-23)
                // mm: minutos (01-59) 
                // dest: código aeropuerto destino
                // ###: cantidad de productos (001-999)
                // IdClien: ID cliente (7 posiciones numéricas)
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 6) {
                    int priorityDays = Integer.parseInt(parts[0]);
                    int hour = Integer.parseInt(parts[1]);
                    int minute = Integer.parseInt(parts[2]);
                    String destinationAirportCode = parts[3];
                    int productQuantity = Integer.parseInt(parts[4]); // Cantidad de productos en el paquete
                    int customerId = Integer.parseInt(parts[5]); // ID del cliente
                    
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
                        
                        // Set delivery deadline based on priority days
                        LocalDateTime deliveryDeadline;
                        switch (priorityDays) {
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
                        
                        // Create products for this package
                        ArrayList<Product> products = new ArrayList<>();
                        for (int i = 0; i < productQuantity; i++) {
                            Product product = new Product();
                            product.setId(productId++);
                            product.setStatus(Status.NOT_ASSIGNED); // Product not assigned initially
                            products.add(product);
                        }
                        pkg.setProducts(products);
                        
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
        // MoraPack has headquarters in Lima (Peru), Brussels (Belgium), and Baku (Azerbaijan)
        // Packages must start from one of these three locations with unlimited stock
        
        ArrayList<City> moraPackWarehouses = new ArrayList<>();
        
        // Find MoraPack warehouse cities
        for (Airport airport : airports) {
            City city = airport.getCity();
            String cityName = city.getName();
            
            if (cityName.equals("Lima") || cityName.equals("Bruselas") || cityName.equals("Baku") ||
                cityName.contains("Lima") || cityName.contains("Bruselas") || cityName.contains("Baku")) {
                // Prefer warehouses in different continent than destination to maximize coverage
                if (city.getContinent() != destinationContinent) {
                    moraPackWarehouses.add(city);
                }
            }
        }
        
        // If no warehouses in different continent, allow any MoraPack warehouse
        if (moraPackWarehouses.isEmpty()) {
            for (Airport airport : airports) {
                City city = airport.getCity();
                String cityName = city.getName();
                
                if (cityName.equals("Lima") || cityName.equals("Bruselas") || cityName.equals("Baku") ||
                    cityName.contains("Lima") || cityName.contains("Bruselas") || cityName.contains("Baku")) {
                    moraPackWarehouses.add(city);
                }
            }
        }
        
        // If somehow no MoraPack warehouses found (shouldn't happen), fallback to Lima
        if (moraPackWarehouses.isEmpty()) {
            System.err.println("Warning: No MoraPack warehouses found, using fallback");
            for (Airport airport : airports) {
                if (airport.getCity().getName().contains("Lima")) {
                    return airport.getCity();
                }
            }
        }
        
        // Return random MoraPack warehouse
        return moraPackWarehouses.get(random.nextInt(moraPackWarehouses.size()));
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
