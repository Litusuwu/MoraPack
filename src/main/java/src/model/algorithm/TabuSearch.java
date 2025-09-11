package src.model.algorithm;

import src.model.City;
import src.model.Constants;
import src.model.Flight;
import src.model.Package;
import src.model.Route;
import src.model.Solution;
import src.model.Warehouse;
import lombok.Getter;
import lombok.Setter;
import java.util.*;

@Getter
@Setter
public class TabuSearch {
    private List<Package> packages;
    private List<Flight> availableFlights;
    private List<Warehouse> warehouses;
    private List<City> cities;
    private LinkedList<Move> tabuList;
    private Solution bestSolution;
    private Solution currentSolution;
    private int tabuListSize;
    private int maxIterations;
    private int maxIterationsWithoutImprovement;
    private Solution initialSolution; // For hybrid algorithms
    
    public TabuSearch(List<Package> packages, List<Flight> availableFlights, 
                     List<Warehouse> warehouses, List<City> cities) {
        this.packages = packages;
        this.availableFlights = availableFlights;
        this.warehouses = warehouses;
        this.cities = cities;
        this.tabuList = new LinkedList<>();
        this.tabuListSize = Constants.TABU_LIST_SIZE;
        this.maxIterations = Constants.MAX_ITERATIONS;
        this.maxIterationsWithoutImprovement = Constants.MAX_ITERATIONS_WITHOUT_IMPROVEMENT;
    }
    
    public void setBestSolution(Solution solution) {
        this.bestSolution = new Solution(solution);
        this.currentSolution = new Solution(solution);
    }
    
    public Solution solve() {
        // Initialize with greedy solution
        currentSolution = generateInitialSolution();
        bestSolution = new Solution(currentSolution);
        
        int iterationsWithoutImprovement = 0;
        
        for (int iteration = 0; iteration < maxIterations && 
             iterationsWithoutImprovement < maxIterationsWithoutImprovement; iteration++) {
            
            // Generate neighborhood solutions
            List<Solution> neighborhood = generateNeighborhood(currentSolution);
            
            // Find best non-tabu solution in neighborhood
            Solution bestCandidate = findBestCandidate(neighborhood);
            
            if (bestCandidate != null) {
                // Create move and add to tabu list
                Move move = createMove(currentSolution, bestCandidate);
                addToTabuList(move);
                
                // Update current solution
                currentSolution = bestCandidate;
                
                // Update best solution if improvement found
                if (currentSolution.getFitness() < bestSolution.getFitness()) {
                    bestSolution = new Solution(currentSolution);
                    iterationsWithoutImprovement = 0;
                } else {
                    iterationsWithoutImprovement++;
                }
            } else {
                iterationsWithoutImprovement++;
            }
            
            // Optional: Print progress
            if (iteration % 100 == 0) {
                System.out.println("Iteration " + iteration + ": Best fitness = " + 
                                 bestSolution.getFitness());
            }
        }
        
        return bestSolution;
    }
    
    private Solution generateInitialSolution() {
        Solution solution = new Solution();
        
        // Group packages by destination city for efficient routing
        Map<City, List<Package>> packagesByDestination = new HashMap<>();
        for (Package pkg : packages) {
            packagesByDestination.computeIfAbsent(pkg.getDestinationCity(), 
                                                 k -> new ArrayList<>()).add(pkg);
        }
        
        // Create routes for each destination
        for (Map.Entry<City, List<Package>> entry : packagesByDestination.entrySet()) {
            City destination = entry.getKey();
            List<Package> destPackages = entry.getValue();
            
            // Find best warehouse to serve this destination
            Warehouse bestWarehouse = findBestWarehouse(destination);
            City origin = bestWarehouse.getAirport().getCity();
            
            // Create route from warehouse to destination
            Route route = createRoute(origin, destination, destPackages.size());
            
            if (route != null && route.isValidRoute()) {
                for (Package pkg : destPackages) {
                    solution.assignPackageToRoute(pkg, route);
                }
                solution.addRoute(route);
            }
        }
        
        return solution;
    }
    
    private Warehouse findBestWarehouse(City destination) {
        Warehouse bestWarehouse = null;
        double bestScore = Double.MAX_VALUE;
        
        for (Warehouse warehouse : warehouses) {
            if (warehouse.isMainWarehouse()) {
                // Calculate score based on continent proximity and capacity
                double score = calculateWarehouseScore(warehouse, destination);
                if (score < bestScore) {
                    bestScore = score;
                    bestWarehouse = warehouse;
                }
            }
        }
        
        return bestWarehouse != null ? bestWarehouse : warehouses.get(0);
    }
    
    private double calculateWarehouseScore(Warehouse warehouse, City destination) {
        City warehouseCity = warehouse.getAirport().getCity();
        
        // Prefer same continent (lower score is better)
        double continentPenalty = (warehouseCity.getContinent() == destination.getContinent()) ? 0 : 100;
        
        // Consider warehouse utilization
        double utilizationPenalty = warehouse.getUtilizationRate() * 50;
        
        return continentPenalty + utilizationPenalty;
    }
    
    private Route createRoute(City origin, City destination, int packageCount) {
        Route route = new Route(origin, destination);
        
        // Find direct flight if available
        Flight directFlight = findFlight(origin, destination);
        if (directFlight != null && directFlight.canAccommodatePackages(packageCount)) {
            route.addFlight(directFlight);
            directFlight.addPackages(packageCount);
            return route;
        }
        
        // Find route with connections (simple approach - one connection)
        for (City intermediate : cities) {
            if (!intermediate.equals(origin) && !intermediate.equals(destination)) {
                Flight firstFlight = findFlight(origin, intermediate);
                Flight secondFlight = findFlight(intermediate, destination);
                
                if (firstFlight != null && secondFlight != null &&
                    firstFlight.canAccommodatePackages(packageCount) &&
                    secondFlight.canAccommodatePackages(packageCount)) {
                    
                    route.addFlight(firstFlight);
                    route.addFlight(secondFlight);
                    
                    if (route.isValidRoute()) {
                        firstFlight.addPackages(packageCount);
                        secondFlight.addPackages(packageCount);
                        return route;
                    } else {
                        // Reset route if invalid
                        route = new Route(origin, destination);
                    }
                }
            }
        }
        
        return null; // No valid route found
    }
    
    private Flight findFlight(City origin, City destination) {
        return availableFlights.stream()
                .filter(f -> f.getOriginAirport().getCity().equals(origin) &&
                           f.getDestinationAirport().getCity().equals(destination) &&
                           f.getAvailableCapacity() > 0)
                .min(Comparator.comparing(Flight::getCost))
                .orElse(null);
    }
    
    private List<Solution> generateNeighborhood(Solution current) {
        List<Solution> neighborhood = new ArrayList<>();
        
        // Generate neighborhood using different move operators
        neighborhood.addAll(generateSwapMoves(current));
        neighborhood.addAll(generateRelocateMoves(current));
        neighborhood.addAll(generateRouteChangeMoves(current));
        
        return neighborhood;
    }
    
    private List<Solution> generateSwapMoves(Solution current) {
        List<Solution> moves = new ArrayList<>();
        List<Route> routes = current.getRoutes();
        
        // Swap packages between different routes
        for (int i = 0; i < routes.size(); i++) {
            for (int j = i + 1; j < routes.size(); j++) {
                Route route1 = routes.get(i);
                Route route2 = routes.get(j);
                
                if (!route1.getPackages().isEmpty() && !route2.getPackages().isEmpty()) {
                    Solution neighbor = new Solution(current);
                    // Implement package swapping logic
                    swapPackagesBetweenRoutes(neighbor, route1, route2);
                    moves.add(neighbor);
                }
            }
        }
        
        return moves;
    }
    
    private List<Solution> generateRelocateMoves(Solution current) {
        List<Solution> moves = new ArrayList<>();
        List<Route> routes = current.getRoutes();
        
        // Relocate packages from one route to another
        for (Route sourceRoute : routes) {
            if (!sourceRoute.getPackages().isEmpty()) {
                for (Route targetRoute : routes) {
                    if (!sourceRoute.equals(targetRoute)) {
                        Solution neighbor = new Solution(current);
                        relocatePackage(neighbor, sourceRoute, targetRoute);
                        moves.add(neighbor);
                    }
                }
            }
        }
        
        return moves;
    }
    
    private List<Solution> generateRouteChangeMoves(Solution current) {
        List<Solution> moves = new ArrayList<>();
        
        // Try to change the flights used in existing routes
        for (Route route : current.getRoutes()) {
            List<Route> alternativeRoutes = findAlternativeRoutes(route);
            for (Route altRoute : alternativeRoutes) {
                Solution neighbor = new Solution(current);
                replaceRoute(neighbor, route, altRoute);
                moves.add(neighbor);
            }
        }
        
        return moves;
    }
    
    private void swapPackagesBetweenRoutes(Solution solution, Route route1, Route route2) {
        // Simple implementation: swap one package from each route
        if (!route1.getPackages().isEmpty() && !route2.getPackages().isEmpty()) {
            Package pkg1 = route1.getPackages().get(0);
            Package pkg2 = route2.getPackages().get(0);
            
            // Check if swap is feasible (destination compatibility)
            if (route1.getDestinationCity().equals(pkg2.getDestinationCity()) &&
                route2.getDestinationCity().equals(pkg1.getDestinationCity())) {
                
                route1.getPackages().remove(pkg1);
                route1.getPackages().add(pkg2);
                route2.getPackages().remove(pkg2);
                route2.getPackages().add(pkg1);
                
                solution.getPackageRouteMap().put(pkg1, route2);
                solution.getPackageRouteMap().put(pkg2, route1);
                
                solution.updateMetrics();
            }
        }
    }
    
    private void relocatePackage(Solution solution, Route sourceRoute, Route targetRoute) {
        if (!sourceRoute.getPackages().isEmpty()) {
            Package pkg = sourceRoute.getPackages().get(0);
            
            // Check if relocation is feasible
            if (targetRoute.getDestinationCity().equals(pkg.getDestinationCity())) {
                sourceRoute.getPackages().remove(pkg);
                targetRoute.getPackages().add(pkg);
                solution.getPackageRouteMap().put(pkg, targetRoute);
                solution.updateMetrics();
            }
        }
    }
    
    private void replaceRoute(Solution solution, Route oldRoute, Route newRoute) {
        int routeIndex = solution.getRoutes().indexOf(oldRoute);
        if (routeIndex >= 0) {
            solution.getRoutes().set(routeIndex, newRoute);
            
            // Update package-route mappings
            for (Package pkg : oldRoute.getPackages()) {
                newRoute.addPackage(pkg);
                solution.getPackageRouteMap().put(pkg, newRoute);
            }
            
            solution.updateMetrics();
        }
    }
    
    private List<Route> findAlternativeRoutes(Route currentRoute) {
        List<Route> alternatives = new ArrayList<>();
        
        // Find alternative flight combinations for the same origin-destination
        City origin = currentRoute.getOriginCity();
        City destination = currentRoute.getDestinationCity();
        int packageCount = currentRoute.getTotalPackages();
        
        // Try different intermediate cities
        for (City intermediate : cities) {
            if (!intermediate.equals(origin) && !intermediate.equals(destination)) {
                Route altRoute = createRoute(origin, destination, packageCount);
                if (altRoute != null && !altRoute.equals(currentRoute)) {
                    alternatives.add(altRoute);
                }
            }
        }
        
        return alternatives;
    }
    
    private Solution findBestCandidate(List<Solution> neighborhood) {
        Solution bestCandidate = null;
        double bestFitness = Double.MAX_VALUE;
        
        for (Solution candidate : neighborhood) {
            Move candidateMove = createMove(currentSolution, candidate);
            
            // Check if move is not tabu or satisfies aspiration criteria
            if (!isTabu(candidateMove) || 
                (candidate.getFitness() < bestSolution.getFitness())) {
                
                if (candidate.getFitness() < bestFitness) {
                    bestFitness = candidate.getFitness();
                    bestCandidate = candidate;
                }
            }
        }
        
        return bestCandidate;
    }
    
    private Move createMove(Solution from, Solution to) {
        // Simple move representation - could be enhanced
        return new Move(from.getFitness(), to.getFitness(), System.currentTimeMillis());
    }
    
    private boolean isTabu(Move move) {
        return tabuList.contains(move);
    }
    
    private void addToTabuList(Move move) {
        tabuList.addFirst(move);
        if (tabuList.size() > tabuListSize) {
            tabuList.removeLast();
        }
    }
    
    // Inner class to represent moves
    private static class Move {
        private double fromFitness;
        private double toFitness;
        @SuppressWarnings("unused") // Used for future enhancements
        private long timestamp;
        
        public Move(double fromFitness, double toFitness, long timestamp) {
            this.fromFitness = fromFitness;
            this.toFitness = toFitness;
            this.timestamp = timestamp;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Move move = (Move) obj;
            return Double.compare(move.fromFitness, fromFitness) == 0 &&
                   Double.compare(move.toFitness, toFitness) == 0;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(fromFitness, toFitness);
        }
    }
}
