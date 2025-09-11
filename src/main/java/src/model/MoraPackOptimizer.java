package src.model;

import lombok.Getter;
import lombok.Setter;
import java.util.*;
import java.time.LocalDateTime;

/**
 * Main orchestrator class for MoraPack optimization system
 * Handles the three main scenarios: real-time operations, weekly simulation, and collapse simulation
 */
@Getter
@Setter
public class MoraPackOptimizer {
    
    private List<City> cities;
    private List<Airport> airports;
    private List<Warehouse> warehouses;
    private List<Flight> flights;
    private List<Package> packages;
    private List<Customer> customers;
    
    public MoraPackOptimizer() {
        initializeSystem();
    }
    
    /**
     * Initialize the MoraPack system with default data
     */
    private void initializeSystem() {
        cities = new ArrayList<>();
        airports = new ArrayList<>();
        warehouses = new ArrayList<>();
        flights = new ArrayList<>();
        packages = new ArrayList<>();
        customers = new ArrayList<>();
        
        createContinentsAndCities();
        createAirportsAndWarehouses();
        createFlights();
    }
    
    /**
     * Creates the main cities and continents for MoraPack operations
     */
    private void createContinentsAndCities() {
        // Main warehouse cities
        City lima = new City(1, "Lima", Continent.America);
        City brussels = new City(2, "Brussels", Continent.Europa);
        City baku = new City(3, "Baku", Continent.Asia);
        
        // Additional cities for comprehensive network
        City newYork = new City(4, "New York", Continent.America);
        City sãoPaulo = new City(5, "São Paulo", Continent.America);
        City paris = new City(6, "Paris", Continent.Europa);
        City london = new City(7, "London", Continent.Europa);
        City tokyo = new City(8, "Tokyo", Continent.Asia);
        City singapore = new City(9, "Singapore", Continent.Asia);
        
        cities.addAll(Arrays.asList(lima, brussels, baku, newYork, sãoPaulo, 
                                   paris, london, tokyo, singapore));
    }
    
    /**
     * Creates airports and warehouses for each city
     */
    private void createAirportsAndWarehouses() {
        int airportId = 1;
        int warehouseId = 1;
        
        for (City city : cities) {
            Airport airport = new Airport(airportId++, 
                                        city.getName().substring(0, 3).toUpperCase(), 
                                        city.getName() + " Airport", 
                                        city);
            
            boolean isMainWarehouse = city.getName().equals("Lima") || 
                                    city.getName().equals("Brussels") || 
                                    city.getName().equals("Baku");
            
            Warehouse warehouse = new Warehouse(warehouseId++, airport, 
                                              city.getName() + " Warehouse", 
                                              isMainWarehouse);
            
            airport.setWarehouse(warehouse);
            
            airports.add(airport);
            warehouses.add(warehouse);
        }
    }
    
    /**
     * Creates flight connections between cities
     */
    private void createFlights() {
        int flightId = 1;
        
        // Create flights between all city pairs (simplified approach)
        for (Airport origin : airports) {
            for (Airport destination : airports) {
                if (!origin.equals(destination)) {
                    // Determine frequency based on continent relationship
                    double frequency = (origin.getCity().getContinent() == 
                                      destination.getCity().getContinent()) ? 
                                      2.0 + Math.random() : // Same continent: 2-3 times per day
                                      1.0; // Different continent: once per day
                    
                    Flight flight = new Flight(flightId++, origin, destination, frequency);
                    flights.add(flight);
                }
            }
        }
    }
    
    /**
     * Scenario 1: Real-time operations simulation
     * Handles day-to-day operations with incoming orders
     */
    public Solution runRealTimeSimulation(List<Package> incomingPackages) {
        System.out.println("=== Running Real-Time Operations Simulation ===");
        
        // Update package list
        packages.addAll(incomingPackages);
        
        // Create and run Tabu Search
        TabuSearch optimizer = new TabuSearch(packages, flights, warehouses, cities);
        optimizer.setMaxIterations(500); // Reduced for real-time processing
        
        Solution solution = optimizer.solve();
        
        // Update system state based on solution
        updateSystemState(solution);
        
        System.out.println("Real-time simulation completed. Solution fitness: " + solution.getFitness());
        return solution;
    }
    
    /**
     * Scenario 2: Weekly simulation (30-90 minutes execution time)
     * Comprehensive optimization for weekly package distribution
     */
    public Solution runWeeklySimulation(int packageCount) {
        System.out.println("=== Running Weekly Simulation ===");
        System.out.println("Expected execution time: 30-90 minutes");
        
        // Generate weekly package load
        List<Package> weeklyPackages = generateWeeklyPackages(packageCount);
        packages.addAll(weeklyPackages);
        
        // Create and run comprehensive Tabu Search
        TabuSearch optimizer = new TabuSearch(packages, flights, warehouses, cities);
        optimizer.setMaxIterations(Constants.MAX_ITERATIONS);
        optimizer.setMaxIterationsWithoutImprovement(Constants.MAX_ITERATIONS_WITHOUT_IMPROVEMENT);
        
        long startTime = System.currentTimeMillis();
        Solution solution = optimizer.solve();
        long endTime = System.currentTimeMillis();
        
        double executionMinutes = (endTime - startTime) / (1000.0 * 60.0);
        System.out.println("Weekly simulation completed in " + String.format("%.2f", executionMinutes) + " minutes");
        System.out.println("Solution fitness: " + solution.getFitness());
        System.out.println("Packages processed: " + packages.size());
        System.out.println("Routes generated: " + solution.getRoutes().size());
        
        return solution;
    }
    
    /**
     * Scenario 3: Collapse simulation
     * Stress test the system until operational collapse
     */
    public SimulationResult runCollapseSimulation() {
        System.out.println("=== Running Collapse Simulation ===");
        
        SimulationResult result = new SimulationResult();
        int batchSize = 100;
        int currentPackageLoad = 0;
        boolean systemCollapsed = false;
        
        while (!systemCollapsed) {
            currentPackageLoad += batchSize;
            
            // Generate package batch
            List<Package> batchPackages = generateRandomPackages(batchSize);
            packages.addAll(batchPackages);
            
            // Run optimization
            TabuSearch optimizer = new TabuSearch(packages, flights, warehouses, cities);
            optimizer.setMaxIterations(1000);
            
            Solution solution = optimizer.solve();
            
            // Check for system collapse conditions
            systemCollapsed = checkSystemCollapse(solution);
            
            result.addDataPoint(currentPackageLoad, solution.getFitness(), 
                               solution.getUndeliveredPackages(), systemCollapsed);
            
            System.out.println("Package load: " + currentPackageLoad + 
                             ", Fitness: " + solution.getFitness() + 
                             ", Undelivered: " + solution.getUndeliveredPackages());
            
            if (systemCollapsed) {
                System.out.println("System collapse detected at " + currentPackageLoad + " packages");
                break;
            }
            
            // Prevent infinite loop
            if (currentPackageLoad > 50000) {
                System.out.println("Maximum test load reached");
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Generates weekly package load for simulation
     */
    private List<Package> generateWeeklyPackages(int count) {
        List<Package> weeklyPackages = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            Customer customer = generateRandomCustomer();
            City destinationCity = cities.get(random.nextInt(cities.size()));
            
            Package pkg = new Package(packages.size() + i + 1, customer, destinationCity, 
                                    LocalDateTime.now().minusHours(random.nextInt(48)));
            weeklyPackages.add(pkg);
        }
        
        return weeklyPackages;
    }
    
    /**
     * Generates random packages for testing
     */
    private List<Package> generateRandomPackages(int count) {
        List<Package> randomPackages = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            Customer customer = generateRandomCustomer();
            City destinationCity = cities.get(random.nextInt(cities.size()));
            
            Package pkg = new Package(packages.size() + i + 1, customer, destinationCity, 
                                    LocalDateTime.now());
            randomPackages.add(pkg);
        }
        
        return randomPackages;
    }
    
    /**
     * Generates a random customer for testing
     */
    private Customer generateRandomCustomer() {
        Random random = new Random();
        int customerId = customers.size() + 1;
        City deliveryCity = cities.get(random.nextInt(cities.size()));
        
        Customer customer = new Customer(customerId, "Customer " + customerId, 
                                       "customer" + customerId + "@example.com", deliveryCity);
        
        // Assign warehouse to customer
        customer.assignOriginWarehouse(warehouses);
        customers.add(customer);
        
        return customer;
    }
    
    /**
     * Updates system state based on optimization solution
     */
    private void updateSystemState(Solution solution) {
        // Update flight capacities
        for (Route route : solution.getRoutes()) {
            for (Flight flight : route.getFlights()) {
                flight.setUsedCapacity(flight.getUsedCapacity() + route.getTotalPackages());
            }
        }
        
        // Update warehouse utilizations
        for (Warehouse warehouse : warehouses) {
            if (warehouse.isMainWarehouse()) {
                int packagesFromWarehouse = (int) packages.stream()
                    .filter(p -> p.getCustomer().getOriginWarehouse().equals(warehouse))
                    .count();
                warehouse.setUsedCapacity(packagesFromWarehouse);
            }
        }
    }
    
    /**
     * Checks if the system has collapsed under load
     */
    private boolean checkSystemCollapse(Solution solution) {
        // Define collapse conditions
        double maxAcceptableUndeliveredRate = 0.3; // 30% undelivered packages
        double undeliveredRate = (double) solution.getUndeliveredPackages() / packages.size();
        
        // Check warehouse capacity utilization
        boolean warehouseOverloaded = warehouses.stream()
            .anyMatch(w -> w.getUtilizationRate() > 1.0);
        
        // Check flight capacity utilization
        boolean flightsOverloaded = flights.stream()
            .anyMatch(f -> f.getUsedCapacity() > f.getMaxCapacity());
        
        return undeliveredRate > maxAcceptableUndeliveredRate || 
               warehouseOverloaded || flightsOverloaded;
    }
    
    /**
     * Inner class to store collapse simulation results
     */
    public static class SimulationResult {
        private List<Integer> packageLoads;
        private List<Double> fitnessValues;
        private List<Integer> undeliveredCounts;
        private List<Boolean> collapseFlags;
        
        public SimulationResult() {
            packageLoads = new ArrayList<>();
            fitnessValues = new ArrayList<>();
            undeliveredCounts = new ArrayList<>();
            collapseFlags = new ArrayList<>();
        }
        
        public void addDataPoint(int packageLoad, double fitness, int undelivered, boolean collapsed) {
            packageLoads.add(packageLoad);
            fitnessValues.add(fitness);
            undeliveredCounts.add(undelivered);
            collapseFlags.add(collapsed);
        }
        
        // Getters
        public List<Integer> getPackageLoads() { return packageLoads; }
        public List<Double> getFitnessValues() { return fitnessValues; }
        public List<Integer> getUndeliveredCounts() { return undeliveredCounts; }
        public List<Boolean> getCollapseFlags() { return collapseFlags; }
    }
}
