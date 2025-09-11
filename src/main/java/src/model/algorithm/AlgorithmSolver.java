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

/**
 * Clase consolidada que implementa todos los algoritmos para MoraPack
 * Incluye: Greedy, Tabu Search, ALNS y algoritmo Híbrido
 */
@Getter
@Setter
public class AlgorithmSolver {
    
    private List<Package> packages;
    private List<Flight> availableFlights;
    private List<Warehouse> warehouses;
    private List<City> cities;
    private Random random;
    
    // Tabu Search parameters
    private LinkedList<TabuMove> tabuList;
    private int tabuListSize = Constants.TABU_LIST_SIZE;
    private int maxIterationsTabu = Constants.MAX_ITERATIONS;
    private int maxIterationsWithoutImprovement = Constants.MAX_ITERATIONS_WITHOUT_IMPROVEMENT;
    
    // ALNS parameters  
    private double[] destroyWeights;
    private double[] repairWeights;
    private int maxIterationsALNS = 1000;
    private double destructionRate = 0.3; // 30% of packages to destroy
    private double coolingRate = 0.99;
    private double initialTemperature = 1000.0;
    
    public AlgorithmSolver(List<Package> packages, List<Flight> availableFlights, 
                          List<Warehouse> warehouses, List<City> cities) {
        this.packages = packages;
        this.availableFlights = availableFlights;
        this.warehouses = warehouses;
        this.cities = cities;
        this.random = new Random();
        this.tabuList = new LinkedList<>();
        initializeALNSWeights();
    }
    
    /**
     * ALGORITMO GREEDY
     * Implementación del algoritmo constructivo greedy
     */
    public Solution searchGreedy() {
        System.out.println("🔍 Iniciando Algoritmo Greedy...");
        
        GreedyAlgorithm greedy = new GreedyAlgorithm(packages, availableFlights, warehouses, cities);
        Solution solution = greedy.solve();
        
        System.out.println("✅ Algoritmo Greedy completado");
        return solution;
    }
    
    /**
     * ALGORITMO TABU SEARCH
     * Implementación consolidada de búsqueda tabú con operadores optimizados
     */
    public Solution searchTabu() {
        System.out.println("🔍 Iniciando Tabu Search...");
        
        // Generar solución inicial greedy
        Solution currentSolution = generateInitialSolution();
        Solution bestSolution = new Solution(currentSolution);
        int iterationsWithoutImprovement = 0;
        
        for (int iteration = 0; iteration < maxIterationsTabu && 
             iterationsWithoutImprovement < maxIterationsWithoutImprovement; iteration++) {
            
            // Generar vecindario con múltiples operadores
            List<Solution> neighborhood = generateTabuNeighborhood(currentSolution);
            
            // Encontrar mejor candidato no-tabú
            Solution bestCandidate = findBestNonTabuCandidate(neighborhood);
            
            if (bestCandidate != null) {
                // Crear movimiento tabú
                TabuMove move = new TabuMove(currentSolution.getFitness(), 
                                           bestCandidate.getFitness(), iteration);
                addToTabuList(move);
                
                currentSolution = bestCandidate;
                
                // Actualizar mejor solución
                if (currentSolution.getFitness() < bestSolution.getFitness()) {
                    bestSolution = new Solution(currentSolution);
                    iterationsWithoutImprovement = 0;
                    System.out.printf("  ↗️ Mejora encontrada - Fitness: %.2f (iteración %d)%n", 
                                    bestSolution.getFitness(), iteration);
                } else {
                    iterationsWithoutImprovement++;
                }
            } else {
                iterationsWithoutImprovement++;
            }
            
            if (iteration % 100 == 0) {
                System.out.printf("  📊 Iteración %d - Mejor fitness: %.2f%n", 
                                iteration, bestSolution.getFitness());
            }
        }
        
        System.out.println("✅ Tabu Search completado");
        return bestSolution;
    }
    
    /**
     * ALGORITMO ALNS (Adaptive Large Neighborhood Search)
     * Implementación consolidada con múltiples operadores de destrucción/reparación
     */
    public Solution searchALNS() {
        System.out.println("🔍 Iniciando ALNS...");
        
        Solution currentSolution = generateInitialSolution();
        Solution bestSolution = new Solution(currentSolution);
        double temperature = initialTemperature;
        
        for (int iteration = 0; iteration < maxIterationsALNS; iteration++) {
            
            // Seleccionar operadores basado en pesos adaptativos
            int destroyOperator = selectOperator(destroyWeights);
            int repairOperator = selectOperator(repairWeights);
            
            // Aplicar destrucción y reparación
            Solution destroyedSolution = applyDestroyOperator(currentSolution, destroyOperator);
            Solution newSolution = applyRepairOperator(destroyedSolution, repairOperator);
            
            // Criterio de aceptación (Simulated Annealing)
            boolean accept = acceptSolution(currentSolution, newSolution, temperature);
            
            if (accept) {
                currentSolution = newSolution;
                
                // Actualizar mejor solución
                if (newSolution.getFitness() < bestSolution.getFitness()) {
                    bestSolution = new Solution(newSolution);
                    System.out.printf("  ↗️ Mejora encontrada - Fitness: %.2f (iteración %d)%n", 
                                    bestSolution.getFitness(), iteration);
                    
                    // Recompensar operadores exitosos
                    updateWeights(destroyOperator, repairOperator, true);
                }
            }
            
            // Actualizar pesos adaptativos
            if (iteration % 100 == 0) {
                updateWeights(destroyOperator, repairOperator, accept);
                System.out.printf("  📊 Iteración %d - Mejor fitness: %.2f, Temperatura: %.2f%n", 
                                iteration, bestSolution.getFitness(), temperature);
            }
            
            // Enfriar temperatura
            temperature *= coolingRate;
        }
        
        System.out.println("✅ ALNS completado");
        return bestSolution;
    }
    
    /**
     * ALGORITMO HÍBRIDO
     * Combina Greedy + ALNS + Tabu Search en secuencia
     */
    public Solution searchHybrid() {
        System.out.println("🔍 Iniciando Algoritmo Híbrido...");
        
        // Fase 1: Generar solución inicial con Greedy
        System.out.println("🔄 Fase 1: Construyendo solución inicial con Greedy...");
        searchGreedy();
        
        // Fase 2: Mejorar con ALNS
        System.out.println("🔄 Fase 2: Mejorando con ALNS...");
        AlgorithmSolver alnsSolver = new AlgorithmSolver(packages, availableFlights, warehouses, cities);
        alnsSolver.setMaxIterationsALNS(maxIterationsALNS / 2);
        alnsSolver.searchALNS();
        
        // Fase 3: Refinamiento final con Tabu Search
        System.out.println("🔄 Fase 3: Refinamiento final con Tabu Search...");
        AlgorithmSolver tabuSolver = new AlgorithmSolver(packages, availableFlights, warehouses, cities);
        tabuSolver.setMaxIterationsTabu(maxIterationsTabu / 2);
        Solution finalSolution = tabuSolver.searchTabu();
        
        System.out.println("✅ Algoritmo Híbrido completado");
        return finalSolution;
    }
    
    // ===================== MÉTODOS AUXILIARES CONSOLIDADOS =====================
    
    /**
     * Genera solución inicial usando algoritmo greedy mejorado
     */
    private Solution generateInitialSolution() {
        GreedyAlgorithm greedy = new GreedyAlgorithm(packages, availableFlights, warehouses, cities);
        return greedy.solve();
    }
    
    /**
     * Genera vecindario para Tabu Search usando múltiples operadores
     */
    private List<Solution> generateTabuNeighborhood(Solution current) {
        List<Solution> neighborhood = new ArrayList<>();
        
        // Operador 1: Intercambio de paquetes entre rutas
        neighborhood.addAll(generateSwapNeighbors(current, 5));
        
        // Operador 2: Reubicación de paquetes
        neighborhood.addAll(generateRelocateNeighbors(current, 5));
        
        // Operador 3: Cambio de ruta completa
        neighborhood.addAll(generateRouteChangeNeighbors(current, 3));
        
        return neighborhood;
    }
    
    /**
     * Operador de destrucción para ALNS
     */
    private Solution applyDestroyOperator(Solution solution, int operator) {
        Solution destroyed = new Solution(solution);
        int packagesToRemove = Math.max(1, (int)(packages.size() * destructionRate));
        
        List<Package> toRemove = new ArrayList<>();
        
        switch (operator) {
            case 0: // Random destruction
                toRemove = selectRandomPackages(destroyed, packagesToRemove);
                break;
            case 1: // Worst cost destruction  
                toRemove = selectWorstCostPackages(destroyed, packagesToRemove);
                break;
            case 2: // Related destruction (same destination)
                toRemove = selectRelatedPackages(destroyed, packagesToRemove);
                break;
        }
        
        removePackagesFromSolution(destroyed, toRemove);
        return destroyed;
    }
    
    /**
     * Operador de reparación para ALNS
     */
    private Solution applyRepairOperator(Solution solution, int operator) {
        // Obtener paquetes no asignados
        List<Package> unassigned = getUnassignedPackages(solution);
        
        switch (operator) {
            case 0: // Greedy repair
                return greedyRepair(solution, unassigned);
            case 1: // Best insertion repair
                return bestInsertionRepair(solution, unassigned);
            case 2: // Regret insertion repair
                return regretInsertionRepair(solution, unassigned);
        }
        
        return solution;
    }
    
    // ===================== OPERADORES ESPECÍFICOS =====================
    
    private List<Solution> generateSwapNeighbors(Solution current, int maxNeighbors) {
        List<Solution> neighbors = new ArrayList<>();
        List<Route> routes = current.getRoutes();
        int count = 0;
        
        for (int i = 0; i < routes.size() && count < maxNeighbors; i++) {
            for (int j = i + 1; j < routes.size() && count < maxNeighbors; j++) {
                Route route1 = routes.get(i);
                Route route2 = routes.get(j);
                
                if (!route1.getPackages().isEmpty() && !route2.getPackages().isEmpty()) {
                    Solution neighbor = new Solution(current);
                    if (swapPackagesBetweenRoutes(neighbor, route1, route2)) {
                        neighbors.add(neighbor);
                        count++;
                    }
                }
            }
        }
        
        return neighbors;
    }
    
    private List<Solution> generateRelocateNeighbors(Solution current, int maxNeighbors) {
        List<Solution> neighbors = new ArrayList<>();
        List<Route> routes = current.getRoutes();
        int count = 0;
        
        for (Route sourceRoute : routes) {
            if (count >= maxNeighbors) break;
            if (!sourceRoute.getPackages().isEmpty()) {
                for (Route targetRoute : routes) {
                    if (count >= maxNeighbors) break;
                    if (!sourceRoute.equals(targetRoute)) {
                        Solution neighbor = new Solution(current);
                        if (relocatePackage(neighbor, sourceRoute, targetRoute)) {
                            neighbors.add(neighbor);
                            count++;
                        }
                    }
                }
            }
        }
        
        return neighbors;
    }
    
    private List<Solution> generateRouteChangeNeighbors(Solution current, int maxNeighbors) {
        List<Solution> neighbors = new ArrayList<>();
        int count = 0;
        
        for (Route route : current.getRoutes()) {
            if (count >= maxNeighbors) break;
            
            // Intentar rutas alternativas
            List<Route> alternatives = findAlternativeRoutes(route.getOriginCity(), 
                                                           route.getDestinationCity(), 
                                                           route.getTotalPackages());
            
            for (Route altRoute : alternatives) {
                if (count >= maxNeighbors) break;
                Solution neighbor = new Solution(current);
                replaceRoute(neighbor, route, altRoute);
                neighbors.add(neighbor);
                count++;
            }
        }
        
        return neighbors;
    }
    
    // ===================== MÉTODOS DE UTILIDAD =====================
    
    private Warehouse findBestWarehouse(City destination) {
        return warehouses.stream()
                .filter(Warehouse::isMainWarehouse)
                .min(Comparator.comparingDouble(w -> calculateWarehouseScore(w, destination)))
                .orElse(warehouses.get(0));
    }
    
    private double calculateWarehouseScore(Warehouse warehouse, City destination) {
        City warehouseCity = warehouse.getAirport().getCity();
        double continentPenalty = (warehouseCity.getContinent() == destination.getContinent()) ? 0 : 100;
        double utilizationPenalty = warehouse.getUtilizationRate() * 50;
        return continentPenalty + utilizationPenalty;
    }
    
    private Route createOptimalRoute(City origin, City destination, int packageCount) {
        Route route = new Route(origin, destination);
        
        // Buscar vuelo directo
        Flight directFlight = findBestFlight(origin, destination, packageCount);
        if (directFlight != null) {
            route.addFlight(directFlight);
            return route;
        }
        
        // Buscar ruta con una conexión
        for (City intermediate : cities) {
            if (!intermediate.equals(origin) && !intermediate.equals(destination)) {
                Flight first = findBestFlight(origin, intermediate, packageCount);
                Flight second = findBestFlight(intermediate, destination, packageCount);
                
                if (first != null && second != null) {
                    route.addFlight(first);
                    route.addFlight(second);
                    
                    if (route.isValidRoute()) {
                        return route;
                    } else {
                        route = new Route(origin, destination);
                    }
                }
            }
        }
        
        return null;
    }
    
    private Flight findBestFlight(City origin, City destination, int packageCount) {
        return availableFlights.stream()
                .filter(f -> f.getOriginAirport().getCity().equals(origin) &&
                           f.getDestinationAirport().getCity().equals(destination) &&
                           f.canAccommodatePackages(packageCount))
                .min(Comparator.comparing(Flight::getCost))
                .orElse(null);
    }
    
    // ===================== MÉTODOS TABU SEARCH =====================
    
    private Solution findBestNonTabuCandidate(List<Solution> neighborhood) {
        Solution bestCandidate = null;
        double bestFitness = Double.MAX_VALUE;
        
        for (Solution candidate : neighborhood) {
            TabuMove candidateMove = new TabuMove(0, candidate.getFitness(), 0);
            
            if (!isTabu(candidateMove) || candidate.getFitness() < bestFitness) {
                if (candidate.getFitness() < bestFitness) {
                    bestFitness = candidate.getFitness();
                    bestCandidate = candidate;
                }
            }
        }
        
        return bestCandidate;
    }
    
    private boolean isTabu(TabuMove move) {
        return tabuList.stream().anyMatch(tm -> 
            Math.abs(tm.toFitness - move.toFitness) < 0.01);
    }
    
    private void addToTabuList(TabuMove move) {
        tabuList.addFirst(move);
        if (tabuList.size() > tabuListSize) {
            tabuList.removeLast();
        }
    }
    
    // ===================== MÉTODOS ALNS =====================
    
    private void initializeALNSWeights() {
        destroyWeights = new double[]{1.0, 1.0, 1.0}; // 3 operadores de destrucción
        repairWeights = new double[]{1.0, 1.0, 1.0};  // 3 operadores de reparación
    }
    
    private int selectOperator(double[] weights) {
        double total = Arrays.stream(weights).sum();
        double random = this.random.nextDouble() * total;
        
        double cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (random <= cumulative) {
                return i;
            }
        }
        return weights.length - 1;
    }
    
    private boolean acceptSolution(Solution current, Solution newSolution, double temperature) {
        if (newSolution.getFitness() < current.getFitness()) {
            return true; // Mejor solución, siempre aceptar
        }
        
        if (temperature <= 0) return false;
        
        double delta = newSolution.getFitness() - current.getFitness();
        double probability = Math.exp(-delta / temperature);
        return random.nextDouble() < probability;
    }
    
    private void updateWeights(int destroyOp, int repairOp, boolean improvement) {
        double reward = improvement ? 1.5 : 0.8;
        destroyWeights[destroyOp] = Math.max(0.1, destroyWeights[destroyOp] * reward);
        repairWeights[repairOp] = Math.max(0.1, repairWeights[repairOp] * reward);
    }
    
    // ===================== OPERADORES DE DESTRUCCIÓN/REPARACIÓN =====================
    
    private List<Package> selectRandomPackages(Solution solution, int count) {
        List<Package> allPackages = getAllAssignedPackages(solution);
        Collections.shuffle(allPackages, random);
        return allPackages.subList(0, Math.min(count, allPackages.size()));
    }
    
    private List<Package> selectWorstCostPackages(Solution solution, int count) {
        List<Package> allPackages = getAllAssignedPackages(solution);
        allPackages.sort((p1, p2) -> {
            Route r1 = solution.getPackageRouteMap().get(p1);
            Route r2 = solution.getPackageRouteMap().get(p2);
            return Double.compare(r2.getTotalCost(), r1.getTotalCost());
        });
        return allPackages.subList(0, Math.min(count, allPackages.size()));
    }
    
    private List<Package> selectRelatedPackages(Solution solution, int count) {
        List<Package> allPackages = getAllAssignedPackages(solution);
        if (allPackages.isEmpty()) return new ArrayList<>();
        
        // Seleccionar un paquete inicial aleatoriamente
        Package seed = allPackages.get(random.nextInt(allPackages.size()));
        List<Package> related = new ArrayList<>();
        related.add(seed);
        
        // Encontrar paquetes relacionados (mismo destino o ruta similar)
        for (Package pkg : allPackages) {
            if (related.size() >= count) break;
            if (!related.contains(pkg) && 
                pkg.getDestinationCity().equals(seed.getDestinationCity())) {
                related.add(pkg);
            }
        }
        
        return related;
    }
    
    private Solution greedyRepair(Solution solution, List<Package> unassigned) {
        for (Package pkg : unassigned) {
            insertPackageGreedy(solution, pkg);
        }
        return solution;
    }
    
    private Solution bestInsertionRepair(Solution solution, List<Package> unassigned) {
        for (Package pkg : unassigned) {
            insertPackageBestPosition(solution, pkg);
        }
        return solution;
    }
    
    private Solution regretInsertionRepair(Solution solution, List<Package> unassigned) {
        while (!unassigned.isEmpty()) {
            Package bestPkg = null;
            double maxRegret = -1;
            
            for (Package pkg : unassigned) {
                double regret = calculateRegret(solution, pkg);
                if (regret > maxRegret) {
                    maxRegret = regret;
                    bestPkg = pkg;
                }
            }
            
            if (bestPkg != null) {
                insertPackageBestPosition(solution, bestPkg);
                unassigned.remove(bestPkg);
            } else {
                break;
            }
        }
        return solution;
    }
    
    // ===================== MÉTODOS DE UTILIDAD ADICIONALES =====================
    
    private List<Package> getAllAssignedPackages(Solution solution) {
        return new ArrayList<>(solution.getPackageRouteMap().keySet());
    }
    
    private List<Package> getUnassignedPackages(Solution solution) {
        Set<Package> assigned = solution.getPackageRouteMap().keySet();
        return packages.stream()
                .filter(p -> !assigned.contains(p))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    private void removePackagesFromSolution(Solution solution, List<Package> toRemove) {
        for (Package pkg : toRemove) {
            Route route = solution.getPackageRouteMap().get(pkg);
            if (route != null) {
                route.getPackages().remove(pkg);
                solution.getPackageRouteMap().remove(pkg);
            }
        }
        solution.updateMetrics();
    }
    
    private void insertPackageGreedy(Solution solution, Package pkg) {
        Warehouse bestWarehouse = findBestWarehouse(pkg.getDestinationCity());
        City origin = bestWarehouse.getAirport().getCity();
        
        Route newRoute = createOptimalRoute(origin, pkg.getDestinationCity(), 1);
        if (newRoute != null) {
            solution.assignPackageToRoute(pkg, newRoute);
            solution.addRoute(newRoute);
        }
    }
    
    private void insertPackageBestPosition(Solution solution, Package pkg) {
        Route bestRoute = null;
        double bestCostIncrease = Double.MAX_VALUE;
        
        // Intentar insertar en rutas existentes
        for (Route route : solution.getRoutes()) {
            if (route.getDestinationCity().equals(pkg.getDestinationCity())) {
                double costBefore = route.getTotalCost();
                route.addPackage(pkg);
                double costAfter = route.getTotalCost();
                double increase = costAfter - costBefore;
                
                if (increase < bestCostIncrease) {
                    bestCostIncrease = increase;
                    bestRoute = route;
                }
                
                route.getPackages().remove(pkg); // Remover temporalmente
            }
        }
        
        if (bestRoute != null) {
            solution.assignPackageToRoute(pkg, bestRoute);
        } else {
            // Crear nueva ruta si no se puede insertar en existentes
            insertPackageGreedy(solution, pkg);
        }
    }
    
    private double calculateRegret(Solution solution, Package pkg) {
        List<Double> costs = new ArrayList<>();
        
        for (Route route : solution.getRoutes()) {
            if (route.getDestinationCity().equals(pkg.getDestinationCity())) {
                double costBefore = route.getTotalCost();
                route.addPackage(pkg);
                double costAfter = route.getTotalCost();
                costs.add(costAfter - costBefore);
                route.getPackages().remove(pkg);
            }
        }
        
        if (costs.size() < 2) return 0;
        
        Collections.sort(costs);
        return costs.get(1) - costs.get(0); // Diferencia entre segunda mejor y mejor opción
    }
    
    private boolean swapPackagesBetweenRoutes(Solution solution, Route route1, Route route2) {
        if (route1.getPackages().isEmpty() || route2.getPackages().isEmpty()) {
            return false;
        }
        
        Package pkg1 = route1.getPackages().get(0);
        Package pkg2 = route2.getPackages().get(0);
        
        if (route1.getDestinationCity().equals(pkg2.getDestinationCity()) &&
            route2.getDestinationCity().equals(pkg1.getDestinationCity())) {
            
            route1.getPackages().remove(pkg1);
            route1.getPackages().add(pkg2);
            route2.getPackages().remove(pkg2);
            route2.getPackages().add(pkg1);
            
            solution.getPackageRouteMap().put(pkg1, route2);
            solution.getPackageRouteMap().put(pkg2, route1);
            
            solution.updateMetrics();
            return true;
        }
        return false;
    }
    
    private boolean relocatePackage(Solution solution, Route sourceRoute, Route targetRoute) {
        if (sourceRoute.getPackages().isEmpty()) {
            return false;
        }
        
        Package pkg = sourceRoute.getPackages().get(0);
        
        if (targetRoute.getDestinationCity().equals(pkg.getDestinationCity())) {
            sourceRoute.getPackages().remove(pkg);
            targetRoute.getPackages().add(pkg);
            solution.getPackageRouteMap().put(pkg, targetRoute);
            solution.updateMetrics();
            return true;
        }
        return false;
    }
    
    private void replaceRoute(Solution solution, Route oldRoute, Route newRoute) {
        int routeIndex = solution.getRoutes().indexOf(oldRoute);
        if (routeIndex >= 0) {
            solution.getRoutes().set(routeIndex, newRoute);
            
            for (Package pkg : oldRoute.getPackages()) {
                newRoute.addPackage(pkg);
                solution.getPackageRouteMap().put(pkg, newRoute);
            }
            
            solution.updateMetrics();
        }
    }
    
    private List<Route> findAlternativeRoutes(City origin, City destination, int packageCount) {
        List<Route> alternatives = new ArrayList<>();
        
        // Intentar diferentes ciudades intermedias
        for (City intermediate : cities) {
            if (!intermediate.equals(origin) && !intermediate.equals(destination)) {
                Route altRoute = createOptimalRoute(origin, destination, packageCount);
                if (altRoute != null) {
                    alternatives.add(altRoute);
                }
            }
        }
        
        return alternatives;
    }
    
    // ===================== CLASE INTERNA PARA MOVIMIENTOS TABÚ =====================
    
    private static class TabuMove {
        double toFitness;
        
        TabuMove(double fromFitness, double toFitness, int iteration) {
            this.toFitness = toFitness;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TabuMove tabuMove = (TabuMove) obj;
            return Double.compare(tabuMove.toFitness, toFitness) == 0;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(toFitness);
        }
    }
}
