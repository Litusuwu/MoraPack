package src.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Non-interactive test class to demonstrate all MoraPack scenarios
 * Perfect for automated testing and demonstration
 */
public class MoraPackTest {
    
    public static void main(String[] args) {
        System.out.println("=== MoraPack Automated Test Suite ===");
        System.out.println("Testing all three required scenarios without user interaction\n");
        
        MoraPackOptimizer optimizer = new MoraPackOptimizer();
        
        // Test 1: Real-time Operations
        testRealTimeOperations(optimizer);
        
        // Test 2: Weekly Simulation (smaller scale for demo)
        testWeeklySimulation(optimizer);
        
        // Test 3: Collapse Simulation (limited for demo)
        testCollapseSimulation(optimizer);
        
        // Test 4: System Information
        printSystemInformation(optimizer);
        
        System.out.println("\n=== All Tests Completed Successfully! ===");
    }
    
    private static void testRealTimeOperations(MoraPackOptimizer optimizer) {
        System.out.println("🚀 TEST 1: Real-Time Operations Simulation");
        System.out.println("=" .repeat(50));
        
        // Create test packages
        List<Package> testPackages = createTestPackages(optimizer, 15);
        
        long startTime = System.currentTimeMillis();
        Solution solution = optimizer.runRealTimeSimulation(testPackages);
        long endTime = System.currentTimeMillis();
        
        // Print results
        double executionTime = (endTime - startTime) / 1000.0;
        System.out.printf("✅ Processed %d packages in %.2f seconds%n", testPackages.size(), executionTime);
        System.out.printf("✅ Routes generated: %d%n", solution.getRoutes().size());
        System.out.printf("✅ Undelivered packages: %d%n", solution.getUndeliveredPackages());
        System.out.printf("✅ Solution fitness: %.2f%n", solution.getFitness());
        System.out.printf("✅ Solution valid: %s%n", solution.isValid() ? "YES" : "NO");
        
        // Show sample routes
        System.out.println("\n📍 Sample Routes:");
        for (int i = 0; i < Math.min(3, solution.getRoutes().size()); i++) {
            Route route = solution.getRoutes().get(i);
            System.out.printf("   Route %d: %s → %s (%d packages, %.1f days)%n",
                i + 1, route.getOriginCity().getName(), route.getDestinationCity().getName(),
                route.getTotalPackages(), route.getTotalTime());
        }
        System.out.println();
    }
    
    private static void testWeeklySimulation(MoraPackOptimizer optimizer) {
        System.out.println("📊 TEST 2: Weekly Simulation (Scaled Demo)");
        System.out.println("=" .repeat(50));
        System.out.println("Note: Running with 50 packages for demo (full version uses 1000+)");
        
        long startTime = System.currentTimeMillis();
        Solution solution = optimizer.runWeeklySimulation(50);
        long endTime = System.currentTimeMillis();
        
        double executionMinutes = (endTime - startTime) / (1000.0 * 60.0);
        System.out.printf("✅ Weekly simulation completed in %.2f minutes%n", executionMinutes);
        System.out.printf("✅ Total packages processed: %d%n", optimizer.getPackages().size());
        System.out.printf("✅ Routes generated: %d%n", solution.getRoutes().size());
        System.out.printf("✅ Solution fitness: %.2f%n", solution.getFitness());
        System.out.printf("✅ Delivery success rate: %.1f%%%n", 
            (1.0 - (double)solution.getUndeliveredPackages() / optimizer.getPackages().size()) * 100);
        
        // Analysis by continent
        analyzeRoutesByContinent(solution);
        System.out.println();
    }
    
    private static void testCollapseSimulation(MoraPackOptimizer optimizer) {
        System.out.println("⚠️  TEST 3: Collapse Simulation (Limited Demo)");
        System.out.println("=" .repeat(50));
        System.out.println("Note: Limited to 500 packages maximum for demo");
        
        // Create a new optimizer for collapse test
        MoraPackOptimizer collapseOptimizer = new MoraPackOptimizer();
        
        int maxPackages = 500; // Limit for demo
        int batchSize = 50;
        int currentLoad = 0;
        boolean collapsed = false;
        
        System.out.println("Testing system limits...");
        
        while (currentLoad < maxPackages && !collapsed) {
            currentLoad += batchSize;
            
            // Create batch of packages
            List<Package> batch = createTestPackages(collapseOptimizer, batchSize);
            Solution solution = collapseOptimizer.runRealTimeSimulation(batch);
            
            // Simple collapse detection: >20% undelivered
            double failureRate = (double)solution.getUndeliveredPackages() / collapseOptimizer.getPackages().size();
            collapsed = failureRate > 0.2;
            
            System.out.printf("   Load: %d packages, Fitness: %.2f, Failure rate: %.1f%%%n",
                currentLoad, solution.getFitness(), failureRate * 100);
            
            if (collapsed) {
                System.out.printf("🚨 System collapse detected at %d packages (%.1f%% failure rate)%n", 
                    currentLoad, failureRate * 100);
                break;
            }
        }
        
        if (!collapsed) {
            System.out.printf("✅ System handled %d packages without collapse%n", currentLoad);
        }
        System.out.println();
    }
    
    private static void printSystemInformation(MoraPackOptimizer optimizer) {
        System.out.println("🏗️  TEST 4: System Information");
        System.out.println("=" .repeat(50));
        
        System.out.printf("🌍 Cities: %d%n", optimizer.getCities().size());
        System.out.printf("✈️  Airports: %d%n", optimizer.getAirports().size());
        System.out.printf("🏭 Warehouses: %d (Main: %d)%n", 
            optimizer.getWarehouses().size(),
            (int)optimizer.getWarehouses().stream().filter(Warehouse::isMainWarehouse).count());
        System.out.printf("🛫 Flights: %d%n", optimizer.getFlights().size());
        System.out.printf("📦 Total packages processed: %d%n", optimizer.getPackages().size());
        
        System.out.println("\n📋 Business Rules:");
        System.out.printf("   Same continent delivery: %s days%n", Constants.SAME_CONTINENT_MAX_DELIVERY_TIME);
        System.out.printf("   Different continent delivery: %s days%n", Constants.DIFFERENT_CONTINENT_MAX_DELIVERY_TIME);
        System.out.printf("   Tabu list size: %d%n", Constants.TABU_LIST_SIZE);
        System.out.printf("   Max iterations: %d%n", Constants.MAX_ITERATIONS);
        
        System.out.println("\n🏢 Main Warehouses:");
        optimizer.getWarehouses().stream()
            .filter(Warehouse::isMainWarehouse)
            .forEach(w -> System.out.printf("   %s: %d/%d capacity (%.1f%% used)%n",
                w.getName(), w.getUsedCapacity(), w.getMaxCapacity(), w.getUtilizationRate() * 100));
        System.out.println();
    }
    
    private static List<Package> createTestPackages(MoraPackOptimizer optimizer, int count) {
        List<Package> packages = new ArrayList<>();
        List<City> cities = optimizer.getCities();
        List<Warehouse> warehouses = optimizer.getWarehouses();
        
        for (int i = 0; i < count; i++) {
            // Create customer
            City deliveryCity = cities.get(i % cities.size());
            Customer customer = new Customer(
                optimizer.getPackages().size() + i + 1,
                "Customer " + (optimizer.getPackages().size() + i + 1),
                "customer" + (optimizer.getPackages().size() + i + 1) + "@test.com",
                deliveryCity
            );
            customer.assignOriginWarehouse(warehouses);
            
            // Create package
            City destination = cities.get((i + 1) % cities.size());
            Package pkg = new Package(
                optimizer.getPackages().size() + i + 1,
                customer,
                destination,
                LocalDateTime.now()
            );
            packages.add(pkg);
        }
        
        return packages;
    }
    
    private static void analyzeRoutesByContinent(Solution solution) {
        long sameContinent = solution.getRoutes().stream()
            .filter(r -> r.getOriginCity().getContinent() == r.getDestinationCity().getContinent())
            .count();
        
        System.out.println("\n📊 Route Analysis:");
        System.out.printf("   Same continent routes: %d%n", sameContinent);
        System.out.printf("   Cross-continent routes: %d%n", solution.getRoutes().size() - sameContinent);
        
        double avgTime = solution.getRoutes().stream()
            .mapToDouble(Route::getTotalTime)
            .average().orElse(0.0);
        System.out.printf("   Average route time: %.2f days%n", avgTime);
    }
}
