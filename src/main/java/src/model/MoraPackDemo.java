package src.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Demonstration class for MoraPack optimization system
 * Shows how to use the three main scenarios
 */
public class MoraPackDemo {
    
    public static void main(String[] args) {
        MoraPackOptimizer optimizer = new MoraPackOptimizer();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== MoraPack Optimization System Demo ===");
        System.out.println("This system implements a Tabu Search algorithm for logistics optimization");
        System.out.println();
        
        while (true) {
            printMenu();
            int choice = scanner.nextInt();
            
            switch (choice) {
                case 1:
                    runRealTimeDemo(optimizer);
                    break;
                case 2:
                    runWeeklyDemo(optimizer, scanner);
                    break;
                case 3:
                    runCollapseDemo(optimizer);
                    break;
                case 4:
                    printSystemInfo(optimizer);
                    break;
                case 5:
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
            
            System.out.println();
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            scanner.nextLine();
        }
    }
    
    private static void printMenu() {
        System.out.println("=== MoraPack System Menu ===");
        System.out.println("1. Real-time Operations Simulation");
        System.out.println("2. Weekly Simulation (30-90 minutes)");
        System.out.println("3. Collapse Simulation");
        System.out.println("4. System Information");
        System.out.println("5. Exit");
        System.out.print("Choose an option: ");
    }
    
    private static void runRealTimeDemo(MoraPackOptimizer optimizer) {
        System.out.println("\n=== Real-Time Operations Demo ===");
        
        // Generate a small batch of packages for real-time processing
        List<Package> incomingPackages = new ArrayList<>();
        
        // Create some sample customers and packages
        List<City> cities = optimizer.getCities();
        List<Warehouse> warehouses = optimizer.getWarehouses();
        
        for (int i = 0; i < 10; i++) {
            Customer customer = new Customer(i + 1, "Customer " + (i + 1), 
                                           "customer" + (i + 1) + "@email.com", 
                                           cities.get(i % cities.size()));
            
            // Assign warehouse to customer
            customer.assignOriginWarehouse(warehouses);
            
            City destination = cities.get((i + 1) % cities.size());
            Package pkg = new Package(i + 1, customer, destination, LocalDateTime.now());
            incomingPackages.add(pkg);
        }
        
        System.out.println("Processing " + incomingPackages.size() + " new packages...");
        
        long startTime = System.currentTimeMillis();
        Solution solution = optimizer.runRealTimeSimulation(incomingPackages);
        long endTime = System.currentTimeMillis();
        
        printSolutionSummary(solution, (endTime - startTime) / 1000.0);
    }
    
    private static void runWeeklyDemo(MoraPackOptimizer optimizer, Scanner scanner) {
        System.out.println("\n=== Weekly Simulation Demo ===");
        System.out.print("Enter number of packages to process (recommended: 100-1000): ");
        int packageCount = scanner.nextInt();
        
        System.out.println("Starting weekly simulation with " + packageCount + " packages...");
        System.out.println("This may take 30-90 minutes depending on the package count and system performance.");
        System.out.println("You can monitor progress in the console output.");
        
        Solution solution = optimizer.runWeeklySimulation(packageCount);
        
        printDetailedSolutionSummary(solution);
    }
    
    private static void runCollapseDemo(MoraPackOptimizer optimizer) {
        System.out.println("\n=== Collapse Simulation Demo ===");
        System.out.println("This simulation will gradually increase package load until system collapse...");
        
        MoraPackOptimizer.SimulationResult result = optimizer.runCollapseSimulation();
        
        System.out.println("\n=== Collapse Simulation Results ===");
        System.out.println("Data points collected: " + result.getPackageLoads().size());
        
        if (!result.getPackageLoads().isEmpty()) {
            int lastIndex = result.getPackageLoads().size() - 1;
            System.out.println("Maximum packages processed: " + result.getPackageLoads().get(lastIndex));
            System.out.println("Final fitness value: " + String.format("%.2f", result.getFitnessValues().get(lastIndex)));
            System.out.println("Final undelivered packages: " + result.getUndeliveredCounts().get(lastIndex));
            
            // Print some key milestones
            System.out.println("\nKey Milestones:");
            for (int i = 0; i < result.getPackageLoads().size(); i += Math.max(1, result.getPackageLoads().size() / 10)) {
                System.out.printf("Packages: %d, Fitness: %.2f, Undelivered: %d%n",
                    result.getPackageLoads().get(i),
                    result.getFitnessValues().get(i),
                    result.getUndeliveredCounts().get(i));
            }
        }
    }
    
    private static void printSystemInfo(MoraPackOptimizer optimizer) {
        System.out.println("\n=== System Information ===");
        System.out.println("Cities: " + optimizer.getCities().size());
        System.out.println("Airports: " + optimizer.getAirports().size());
        System.out.println("Warehouses: " + optimizer.getWarehouses().size());
        System.out.println("Available Flights: " + optimizer.getFlights().size());
        System.out.println("Total Packages Processed: " + optimizer.getPackages().size());
        
        System.out.println("\n=== Business Rules ===");
        System.out.println("Same Continent Delivery Time: " + Constants.SAME_CONTINENT_MAX_DELIVERY_TIME + " days");
        System.out.println("Different Continent Delivery Time: " + Constants.DIFFERENT_CONTINENT_MAX_DELIVERY_TIME + " days");
        System.out.println("Same Continent Transport Time: " + Constants.SAME_CONTINENT_TRANSPORT_TIME + " days");
        System.out.println("Different Continent Transport Time: " + Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME + " days");
        
        System.out.println("\n=== Algorithm Parameters ===");
        System.out.println("Tabu List Size: " + Constants.TABU_LIST_SIZE);
        System.out.println("Maximum Iterations: " + Constants.MAX_ITERATIONS);
        System.out.println("Max Iterations Without Improvement: " + Constants.MAX_ITERATIONS_WITHOUT_IMPROVEMENT);
        
        System.out.println("\n=== Main Warehouses ===");
        long mainWarehouses = optimizer.getWarehouses().stream()
                .filter(Warehouse::isMainWarehouse)
                .count();
        System.out.println("Number of main warehouses: " + mainWarehouses);
        
        optimizer.getWarehouses().stream()
                .filter(Warehouse::isMainWarehouse)
                .forEach(w -> System.out.println("- " + w.getName() + 
                    " (Capacity: " + w.getMaxCapacity() + 
                    ", Used: " + w.getUsedCapacity() + 
                    ", Utilization: " + String.format("%.1f%%", w.getUtilizationRate() * 100) + ")"));
    }
    
    private static void printSolutionSummary(Solution solution, double executionTimeSeconds) {
        System.out.println("\n=== Solution Summary ===");
        System.out.println("Execution Time: " + String.format("%.2f", executionTimeSeconds) + " seconds");
        System.out.println("Total Routes: " + solution.getRoutes().size());
        System.out.println("Solution Fitness: " + String.format("%.2f", solution.getFitness()));
        System.out.println("Total Cost: " + String.format("%.2f", solution.getTotalCost()));
        System.out.println("Maximum Route Time: " + String.format("%.2f", solution.getTotalTime()) + " days");
        System.out.println("Undelivered Packages: " + solution.getUndeliveredPackages());
        System.out.println("Solution Valid: " + (solution.isValid() ? "YES" : "NO"));
        
        if (!solution.getRoutes().isEmpty()) {
            System.out.println("\n=== Route Details ===");
            for (int i = 0; i < Math.min(5, solution.getRoutes().size()); i++) {
                Route route = solution.getRoutes().get(i);
                System.out.printf("Route %d: %s -> %s (%d packages, %.2f days, %d flights)%n",
                    i + 1,
                    route.getOriginCity().getName(),
                    route.getDestinationCity().getName(),
                    route.getTotalPackages(),
                    route.getTotalTime(),
                    route.getFlights().size());
            }
            
            if (solution.getRoutes().size() > 5) {
                System.out.println("... and " + (solution.getRoutes().size() - 5) + " more routes");
            }
        }
    }
    
    private static void printDetailedSolutionSummary(Solution solution) {
        printSolutionSummary(solution, 0.0);
        
        System.out.println("\n=== Detailed Analysis ===");
        
        // Analyze routes by continent
        long sameContinent = solution.getRoutes().stream()
                .filter(r -> r.getOriginCity().getContinent() == r.getDestinationCity().getContinent())
                .count();
        long differentContinent = solution.getRoutes().size() - sameContinent;
        
        System.out.println("Same Continent Routes: " + sameContinent);
        System.out.println("Different Continent Routes: " + differentContinent);
        
        // Analyze delivery times
        double avgTime = solution.getRoutes().stream()
                .mapToDouble(Route::getTotalTime)
                .average()
                .orElse(0.0);
        
        System.out.println("Average Route Time: " + String.format("%.2f", avgTime) + " days");
        
        // Count routes exceeding time limits
        long violatingRoutes = solution.getRoutes().stream()
                .filter(r -> !r.isValidRoute())
                .count();
        
        System.out.println("Routes Violating Time Constraints: " + violatingRoutes);
    }
}
