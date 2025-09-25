package com.system.morapack.schemas.algorithm;

import com.system.morapack.schemas.Airport;
import com.system.morapack.schemas.City;
import com.system.morapack.schemas.Flight;
import com.system.morapack.schemas.Package;
import com.system.morapack.config.Constants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Solution {
    private HashMap<HashMap<Package, ArrayList<Flight>>, Integer> solution;
    private HashMap<ArrayList<HashMap<Package, ArrayList<Flight>>>, Integer> solutionSpace;
    private InputAirports inputAirports;
    private InputData inputData;
    private InputProducts inputProducts;
    private ArrayList<Airport> airports;
    private ArrayList<Flight> flights;
    private ArrayList<Package> packages;
    // Mapa para rastrear la ocupación de almacenes por destino
    private HashMap<Airport, Integer> warehouseOccupancy;
    // Matriz temporal para validar capacidad de almacenes por minuto [aeropuerto][minuto_del_dia]
    private HashMap<Airport, int[]> temporalWarehouseOccupancy;
    // Generador de números aleatorios para diversificar soluciones
    private HashMap<HashMap<Package, ArrayList<Flight>>, Integer> bestSolution;
    private Random random;
    
    // Variables para ALNS
    private ALNSDestruction destructionOperators;
    private ALNSRepair repairOperators;
    private double[][] operatorWeights; // Pesos de operadores [destrucción][reparación]
    private double[][] operatorScores;  // Puntajes de operadores [destrucción][reparación]
    private int[][] operatorUsage;      // Contador de uso de operadores [destrucción][reparación]
    private double temperature;
    private double coolingRate;
    private int maxIterations;
    private int segmentSize;

    
    public Solution() {
        this.inputAirports = new InputAirports(Constants.AIRPORT_INFO_FILE_PATH);
        this.solution = new HashMap<>();
        this.solutionSpace = new HashMap<>();
        this.airports = inputAirports.readAirports();
        this.inputData = new InputData(Constants.FLIGHTS_FILE_PATH, this.airports);
        this.flights = inputData.readFlights();
        this.inputProducts = new InputProducts(Constants.PRODUCTS_FILE_PATH, this.airports);
        this.packages = inputProducts.readProducts();
        this.warehouseOccupancy = new HashMap<>();
        this.temporalWarehouseOccupancy = new HashMap<>();
        // Inicializar generador de números aleatorios con semilla basada en tiempo actual
        this.random = new Random(System.currentTimeMillis());
        
        // Inicializar operadores ALNS
        this.destructionOperators = new ALNSDestruction();
        this.repairOperators = new ALNSRepair(airports, flights, warehouseOccupancy);
        
        // Inicializar parámetros ALNS
        initializeALNSParameters();
        
        // Inicializar ocupación de almacenes
        initializeWarehouseOccupancy();
        initializeTemporalWarehouseOccupancy();
    }
    
    /**
     * Inicializa los parámetros del algoritmo ALNS
     */
    private void initializeALNSParameters() {
        // Número de operadores de destrucción y reparación
        int numDestructionOps = 4; // random, geographic, timeBased, congestedRoute
        int numRepairOps = 4;      // greedy, regret, timeBased, capacityBased
        
        // Inicializar matrices de pesos, puntajes y uso
        this.operatorWeights = new double[numDestructionOps][numRepairOps];
        this.operatorScores = new double[numDestructionOps][numRepairOps];
        this.operatorUsage = new int[numDestructionOps][numRepairOps];
        
        // Inicializar pesos uniformemente (1.0 para todos)
        for (int i = 0; i < numDestructionOps; i++) {
            for (int j = 0; j < numRepairOps; j++) {
                this.operatorWeights[i][j] = 1.0;
                this.operatorScores[i][j] = 0.0;
                this.operatorUsage[i][j] = 0;
            }
        }
        
        // Parámetros del algoritmo
        this.temperature = 1000.0;        // Temperatura inicial
        this.coolingRate = 0.995;         // Tasa de enfriamiento
        this.maxIterations = 10;          // Máximo número de iteraciones (para demostración)
        this.segmentSize = 10;            // Tamaño del segmento para actualizar pesos
    }

    public void solve() {
        // 1. Inicialización
        System.out.println("Iniciando solución ALNS");
        System.out.println("Lectura de aeropuertos");
        System.out.println("Aeropuertos leídos: " + this.airports.size());
        System.out.println("Lectura de vuelos");
        System.out.println("Vuelos leídos: " + this.flights.size());
        System.out.println("Lectura de productos");
        System.out.println("Productos leídos: " + this.packages.size());
        
        // 2. Generar una solución inicial s_actual
        System.out.println("\n=== GENERANDO SOLUCIÓN INICIAL ===");
        this.generateInitialSolution();
        
        // Validar solución generada
        System.out.println("Validando solución...");
        boolean isValid = this.isSolutionValid();
        System.out.println("Solución válida: " + (isValid ? "SÍ" : "NO"));
        
        // Mostrar descripción de la solución inicial
        this.printSolutionDescription(1);
        
        // 3. Establecer s_mejor = s_actual
        bestSolution = new HashMap<>(solution);
        
        // 4. Ejecutar algoritmo ALNS
        System.out.println("\n=== INICIANDO ALGORITMO ALNS ===");
        runALNSAlgorithm();
        
        // 5. Mostrar resultado final
        System.out.println("\n=== RESULTADO FINAL ALNS ===");
        this.printSolutionDescription(2);
    }
    
    /**
     * Ejecuta el algoritmo ALNS (Adaptive Large Neighborhood Search)
     */
    private void runALNSAlgorithm() {
        // Obtener la solución actual y su peso
        HashMap<Package, ArrayList<Flight>> currentSolution = null;
        int currentWeight = Integer.MAX_VALUE;
        
        for (Map.Entry<HashMap<Package, ArrayList<Flight>>, Integer> entry : solution.entrySet()) {
            currentSolution = new HashMap<>(entry.getKey());
            currentWeight = entry.getValue();
            break;
        }
        
        if (currentSolution == null) {
            System.out.println("Error: No se pudo obtener la solución inicial");
            return;
        }
        
        System.out.println("Peso de solución inicial: " + currentWeight);
        
        int bestWeight = currentWeight;
        int improvements = 0;
        int noImprovementCount = 0;
        
        // Bucle principal ALNS
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            System.out.println("ALNS Iteración " + iteration + "/" + maxIterations);
            
            // Seleccionar operadores basado en pesos
            int[] selectedOps = selectOperators();
            int destructionOp = selectedOps[0];
            int repairOp = selectedOps[1];
            System.out.println("  Operadores seleccionados: Destrucción=" + destructionOp + ", Reparación=" + repairOp);
            
            // Crear copia de la solución actual
            HashMap<Package, ArrayList<Flight>> tempSolution = new HashMap<>(currentSolution);
            
            // Aplicar operador de destrucción
            System.out.println("  Aplicando operador de destrucción...");
            long startTime = System.currentTimeMillis();
            ALNSDestruction.DestructionResult destructionResult = applyDestructionOperator(
                tempSolution, destructionOp);
            long endTime = System.currentTimeMillis();
            System.out.println("  Operador de destrucción completado en " + (endTime - startTime) + "ms");
            
            if (destructionResult == null || destructionResult.getDestroyedPackages().isEmpty()) {
                System.out.println("  No se pudo destruir nada, continuando...");
                continue; // No se pudo destruir nada
            }
            System.out.println("  Paquetes destruidos: " + destructionResult.getDestroyedPackages().size());
            
            // Aplicar operador de reparación
            ALNSRepair.RepairResult repairResult = applyRepairOperator(
                tempSolution, repairOp, destructionResult.getDestroyedPackages());
            
            if (repairResult == null || !repairResult.isSuccess()) {
                continue; // No se pudo reparar
            }
            
            // Evaluar nueva solución
            int tempWeight = calculateSolutionWeight(tempSolution);
            
            // Actualizar contador de uso
            operatorUsage[destructionOp][repairOp]++;
            
            // Criterio de aceptación
            boolean accepted = false;
            if (tempWeight < currentWeight) {
                // Mejor solución encontrada
                currentSolution = tempSolution;
                currentWeight = tempWeight;
                accepted = true;
                
                if (tempWeight < bestWeight) {
                    // Nueva mejor solución global
                    bestWeight = tempWeight;
                    bestSolution.clear();
                    bestSolution.put(currentSolution, currentWeight);
                    operatorScores[destructionOp][repairOp] += 100; // Puntaje alto
                    improvements++;
                    noImprovementCount = 0;
                    System.out.println("Iteración " + iteration + ": Nueva mejor solución! Peso: " + bestWeight);
                } else {
                    // Mejor que actual pero no global
                    operatorScores[destructionOp][repairOp] += 50; // Puntaje medio
                }
            } else {
                // Aceptación por Simulated Annealing
                double probability = Math.exp((currentWeight - tempWeight) / temperature);
                if (random.nextDouble() < probability) {
                    currentSolution = tempSolution;
                    currentWeight = tempWeight;
                    accepted = true;
                    operatorScores[destructionOp][repairOp] += 10; // Puntaje bajo
                }
            }
            
            if (!accepted) {
                noImprovementCount++;
            }
            
            // Actualizar pesos cada segmentSize iteraciones
            if ((iteration + 1) % segmentSize == 0) {
                updateOperatorWeights();
                temperature *= coolingRate;
            }
            
            // Parada temprana si no hay mejoras
            if (noImprovementCount > 50) {
                System.out.println("Parada temprana en iteración " + iteration + " (sin mejoras)");
                break;
            }
        }
        
        // Actualizar la solución final
        solution.clear();
        solution.putAll(bestSolution);
        
        System.out.println("ALNS completado:");
        System.out.println("  Mejoras encontradas: " + improvements);
        System.out.println("  Peso final: " + bestWeight);
        System.out.println("  Temperatura final: " + temperature);
    }
    
    /**
     * Selecciona operadores de destrucción y reparación basado en sus pesos
     */
    private int[] selectOperators() {
        try {
            System.out.println("    Seleccionando operadores...");
            // Selección por ruleta basada en pesos
            double totalWeight = 0.0;
            for (int i = 0; i < operatorWeights.length; i++) {
                for (int j = 0; j < operatorWeights[i].length; j++) {
                    totalWeight += operatorWeights[i][j];
                }
            }
            
            System.out.println("    Peso total: " + totalWeight);
            double randomValue = random.nextDouble() * totalWeight;
            double cumulativeWeight = 0.0;
            
            for (int i = 0; i < operatorWeights.length; i++) {
                for (int j = 0; j < operatorWeights[i].length; j++) {
                    cumulativeWeight += operatorWeights[i][j];
                    if (randomValue <= cumulativeWeight) {
                        System.out.println("    Operadores seleccionados: " + i + ", " + j);
                        return new int[]{i, j};
                    }
                }
            }
            
            // Fallback: seleccionar el primero
            System.out.println("    Usando fallback: 0, 0");
            return new int[]{0, 0};
        } catch (Exception e) {
            System.out.println("    Error en selección de operadores: " + e.getMessage());
            e.printStackTrace();
            return new int[]{0, 0};
        }
    }
    
    /**
     * Aplica el operador de destrucción seleccionado
     */
    private ALNSDestruction.DestructionResult applyDestructionOperator(
            HashMap<Package, ArrayList<Flight>> solution, int operatorIndex) {
        try {
            switch (operatorIndex) {
                case 0: // Random Destroy
                    System.out.println("    Ejecutando randomDestroy...");
                    return destructionOperators.randomDestroy(solution, 0.4, 100, 200);
                case 1: // Geographic Destroy
                    System.out.println("    Ejecutando geographicDestroy...");
                    return destructionOperators.geographicDestroy(solution, 0.4, 100, 200);
                case 2: // Time Based Destroy
                    System.out.println("    Ejecutando timeBasedDestroy...");
                    return destructionOperators.timeBasedDestroy(solution, 0.4, 100, 200);
                case 3: // Congested Route Destroy - OPTIMIZADO
                    System.out.println("    Ejecutando congestedRouteDestroy (optimizado)...");
                    return destructionOperators.congestedRouteDestroy(solution, 0.4, 100, 200);
                default:
                    System.out.println("    Ejecutando randomDestroy (default)...");
                    return destructionOperators.randomDestroy(solution, 0.4, 100, 600);
            }
        } catch (Exception e) {
            System.out.println("    Error en operador de destrucción: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Aplica el operador de reparación seleccionado
     */
    private ALNSRepair.RepairResult applyRepairOperator(
            HashMap<Package, ArrayList<Flight>> solution, int operatorIndex,
            ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages) {
        
        // Los operadores de reparación esperan los Map.Entry completos
        switch (operatorIndex) {
            case 0: // Greedy Repair
                return repairOperators.greedyRepair(solution, destroyedPackages);
            case 1: // Regret Repair
                return repairOperators.regretRepair(solution, destroyedPackages, 2); // regretLevel = 2
            case 2: // Time Based Repair
                return repairOperators.timeBasedRepair(solution, destroyedPackages);
            case 3: // Capacity Based Repair
                return repairOperators.capacityBasedRepair(solution, destroyedPackages);
            default:
                return repairOperators.greedyRepair(solution, destroyedPackages);
        }
    }
    
    /**
     * Actualiza los pesos de los operadores basado en sus puntajes
     */
    private void updateOperatorWeights() {
        double lambda = 0.1; // Factor de aprendizaje
        
        for (int i = 0; i < operatorScores.length; i++) {
            for (int j = 0; j < operatorScores[i].length; j++) {
                if (operatorUsage[i][j] > 0) {
                    double averageScore = operatorScores[i][j] / operatorUsage[i][j];
                    operatorWeights[i][j] = (1 - lambda) * operatorWeights[i][j] + 
                                          lambda * averageScore;
                    
                    // Reiniciar puntajes y contador
                    operatorScores[i][j] = 0.0;
                    operatorUsage[i][j] = 0;
                }
            }
        }
    }
    
    public void generateInitialSolution() {
        System.out.println("Generating initial solution using optimized greedy approach...");
        
        // Crear estructura de solución temporal
        HashMap<Package, ArrayList<Flight>> currentSolution = new HashMap<>();
        
        // Ordenar paquetes con un componente aleatorio
        ArrayList<Package> sortedPackages = new ArrayList<>(packages);
        
        // Decidir aleatoriamente entre diferentes estrategias de ordenamiento
        int sortStrategy = 0; // Añadido una estrategia más
        
        switch (sortStrategy) {
            case 0:
                // Ordenamiento por deadline (original)
                System.out.println("Estrategia de ordenamiento: Por deadline optimizado");
                sortedPackages.sort((p1, p2) -> p1.getDeliveryDeadline().compareTo(p2.getDeliveryDeadline()));
                break;
            case 1:
                // Ordenamiento por prioridad
                System.out.println("Estrategia de ordenamiento: Por prioridad");
                sortedPackages.sort((p1, p2) -> Double.compare(p2.getPriority(), p1.getPriority()));
                break;
            case 2:
                // Ordenamiento por distancia entre continentes
                System.out.println("Estrategia de ordenamiento: Por distancia entre continentes");
                sortedPackages.sort((p1, p2) -> {
                    boolean p1DiffContinent = p1.getCurrentLocation().getContinent() != p1.getDestinationCity().getContinent();
                    boolean p2DiffContinent = p2.getCurrentLocation().getContinent() != p2.getDestinationCity().getContinent();
                    return Boolean.compare(p1DiffContinent, p2DiffContinent);
                });
                break;
            case 3:
                // Ordenamiento por margen de tiempo (más urgentes primero)
                System.out.println("Estrategia de ordenamiento: Por margen de tiempo");
                sortedPackages.sort((p1, p2) -> {
                    LocalDateTime now = LocalDateTime.now();
                    long p1Margin = ChronoUnit.HOURS.between(now, p1.getDeliveryDeadline());
                    long p2Margin = ChronoUnit.HOURS.between(now, p2.getDeliveryDeadline());
                    return Long.compare(p1Margin, p2Margin);
                });
                break;
            case 4:
                // Ordenamiento aleatorio
                System.out.println("Estrategia de ordenamiento: Aleatorio");
                Collections.shuffle(sortedPackages, random);
                break;
        }
        
        // Usar algoritmo optimizado con ventanas de tiempo y reasignación dinámica
        int assignedPackages = generateOptimizedSolution(currentSolution, sortedPackages);
        
        // Calcular el peso/costo de esta solución
        int solutionWeight = calculateSolutionWeight(currentSolution);
        
        // Almacenar la solución con su peso
        solution.put(currentSolution, solutionWeight);
        
        System.out.println("Initial solution generated: " + assignedPackages + "/" + packages.size() + " packages assigned");
        System.out.println("Solution weight: " + solutionWeight);
    }
    
    /**
     * Genera una solución optimizada usando ventanas de tiempo y reasignación dinámica
     * para aprovechar mejor la liberación de espacio en almacenes.
     * 
     * @param currentSolution solución actual
     * @param sortedPackages paquetes ordenados por estrategia
     * @return número de paquetes asignados
     */
    private int generateOptimizedSolution(HashMap<Package, ArrayList<Flight>> currentSolution, 
                                        ArrayList<Package> sortedPackages) {
        int assignedPackages = 0;
        int maxIterations = 3; // Máximo número de iteraciones para reasignación
        
        System.out.println("Iniciando algoritmo optimizado con " + maxIterations + " iteraciones...");
        
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            if (iteration > 0) {
                System.out.println("Iteración " + iteration + " - Reasignación dinámica...");
                // En iteraciones posteriores, intentar reasignar paquetes no asignados
                ArrayList<Package> unassignedPackages = new ArrayList<>();
                for (Package pkg : sortedPackages) {
                    if (!currentSolution.containsKey(pkg)) {
                        unassignedPackages.add(pkg);
                    }
                }
                sortedPackages = unassignedPackages;
            }
            
            int iterationAssigned = 0;
            
            for (Package pkg : sortedPackages) {
                Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
                if (destinationAirport == null) continue;
                
                int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
                
                // Intentar asignar el paquete usando diferentes estrategias
                ArrayList<Flight> bestRoute = findBestRouteWithTimeWindows(pkg, currentSolution);
                
                if (bestRoute != null && isRouteValid(pkg, bestRoute)) {
                    // Primero validar temporalmente sin actualizar capacidades
                    if (canAssignWithSpaceOptimization(pkg, bestRoute, currentSolution)) {
                        // Si la validación temporal pasa, entonces actualizar capacidades
                        currentSolution.put(pkg, bestRoute);
                        assignedPackages++;
                        iterationAssigned++;
                        
                        // Actualizar capacidades DESPUÉS de la validación
                        updateFlightCapacities(bestRoute, productCount);
                        incrementWarehouseOccupancy(destinationAirport, productCount);
                        
                        if (iteration > 0) {
                            System.out.println("  Reasignado paquete " + pkg.getId() + " en iteración " + iteration);
                        }
                    }
                }
            }
            
            System.out.println("  Iteración " + iteration + " completada: " + iterationAssigned + " paquetes asignados");
            
            // Si no se asignaron paquetes en esta iteración, no hay punto en continuar
            if (iterationAssigned == 0) {
                break;
            }
        }
        
        return assignedPackages;
    }
    
    /**
     * Encuentra la mejor ruta considerando ventanas de tiempo y liberación de espacio
     */
    private ArrayList<Flight> findBestRouteWithTimeWindows(Package pkg, 
                                                          HashMap<Package, ArrayList<Flight>> currentSolution) {
        // Primero intentar con el método original
        ArrayList<Flight> originalRoute = findBestRoute(pkg);
        
        // Si no funciona, intentar con diferentes horarios de salida
        if (originalRoute == null || !canAssignWithSpaceOptimization(pkg, originalRoute, currentSolution)) {
            return findRouteWithDelayedDeparture(pkg, currentSolution);
        }
        
        return originalRoute;
    }
    
    /**
     * Encuentra una ruta con horarios de salida retrasados para aprovechar liberación de espacio
     */
    private ArrayList<Flight> findRouteWithDelayedDeparture(Package pkg, 
                                                           HashMap<Package, ArrayList<Flight>> currentSolution) {
        // Intentar con diferentes horarios de salida (cada 2 horas)
        for (int delayHours = 2; delayHours <= 12; delayHours += 2) {
            // Simular un paquete con horario de salida retrasado
            Package delayedPkg = createDelayedPackage(pkg, delayHours);
            if (delayedPkg == null) continue;
            
            ArrayList<Flight> route = findBestRoute(delayedPkg);
            if (route != null && canAssignWithSpaceOptimization(delayedPkg, route, currentSolution)) {
                return route;
            }
        }
        
        return null;
    }
    
    /**
     * Crea un paquete con horario de salida retrasado para probar diferentes ventanas de tiempo
     */
    private Package createDelayedPackage(Package originalPkg, int delayHours) {
        // Verificar si el retraso no viola el deadline
        LocalDateTime delayedOrderDate = originalPkg.getOrderDate().plusHours(delayHours);
        if (delayedOrderDate.isAfter(originalPkg.getDeliveryDeadline())) {
            return null; // El retraso violaría el deadline
        }
        
        // Crear una copia del paquete con el nuevo horario
        Package delayedPkg = new Package();
        delayedPkg.setId(originalPkg.getId());
        delayedPkg.setCustomer(originalPkg.getCustomer());
        delayedPkg.setDestinationCity(originalPkg.getDestinationCity());
        delayedPkg.setOrderDate(delayedOrderDate);
        delayedPkg.setDeliveryDeadline(originalPkg.getDeliveryDeadline());
        delayedPkg.setCurrentLocation(originalPkg.getCurrentLocation());
        delayedPkg.setProducts(originalPkg.getProducts());
        delayedPkg.setPriority(originalPkg.getPriority());
        
        return delayedPkg;
    }
    
    /**
     * Verifica si se puede asignar un paquete considerando la optimización de espacio
     * y la liberación temporal de almacenes usando validación temporal estricta
     */
    private boolean canAssignWithSpaceOptimization(Package pkg, ArrayList<Flight> route,
                                                  HashMap<Package, ArrayList<Flight>> currentSolution) {
        // Usar validación temporal pero solo para el paquete actual
        // No considerar todos los paquetes simultáneamente para evitar ser demasiado restrictivo
        return validateSinglePackageTemporalFlow(pkg, route);
    }
    
    /**
     * Valida el flujo temporal de un solo paquete sin considerar otros paquetes simultáneamente
     * Esto permite una validación más flexible que considera que los paquetes pueden esperar
     */
    private boolean validateSinglePackageTemporalFlow(Package pkg, ArrayList<Flight> route) {
        if (route == null || route.isEmpty()) {
            // Paquete ya en destino - solo verificar capacidad final
            Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
            if (destinationAirport == null) return false;
            
            int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
            int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
            int maxCapacity = destinationAirport.getWarehouse().getMaxCapacity();
            
            return (currentOccupancy + productCount) <= maxCapacity;
        }
        
        // Para rutas con vuelos, verificar capacidades de vuelos y destino final
        Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
        if (destinationAirport == null) return false;
        
        int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
        
        // Verificar capacidad de vuelos
        for (Flight flight : route) {
            if (flight.getUsedCapacity() + productCount > flight.getMaxCapacity()) {
                return false; // Vuelo no tiene capacidad
            }
        }
        
        // Verificar capacidad del almacén de destino
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        int maxCapacity = destinationAirport.getWarehouse().getMaxCapacity();
        
        return (currentOccupancy + productCount) <= maxCapacity;
    }
    
    /**
     * Verifica si habrá capacidad disponible en el futuro debido a liberación de espacio
     */
    private boolean checkFutureCapacityAvailability(Package pkg, ArrayList<Flight> route,
                                                   HashMap<Package, ArrayList<Flight>> currentSolution) {
        if (route == null || route.isEmpty()) return false;
        
        // Calcular cuándo llegará este paquete a su destino
        int arrivalTime = calculateArrivalTime(pkg, route);
        Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
        int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
        
        // Verificar qué paquetes se liberarán antes de la llegada de este paquete
        int futureAvailableCapacity = calculateFutureAvailableCapacity(destinationAirport, arrivalTime, currentSolution);
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        int maxCapacity = destinationAirport.getWarehouse().getMaxCapacity();
        
        return (currentOccupancy + productCount - futureAvailableCapacity) <= maxCapacity;
    }
    
    /**
     * Calcula cuándo llegará un paquete a su destino final
     */
    private int calculateArrivalTime(Package pkg, ArrayList<Flight> route) {
        int currentTime = getPackageStartTime(pkg);
        
        for (Flight flight : route) {
            int waitingTime = 120; // 2 horas de espera
            int flightDuration = (int)(flight.getTransportTime() * 60);
            currentTime += waitingTime + flightDuration;
            
            if (route.indexOf(flight) < route.size() - 1) {
                currentTime += 120; // 2 horas de conexión
            }
        }
        
        return currentTime;
    }
    
    /**
     * Calcula la capacidad que estará disponible en el futuro debido a liberación de espacio
     */
    private int calculateFutureAvailableCapacity(Airport airport, int arrivalTime, 
                                               HashMap<Package, ArrayList<Flight>> currentSolution) {
        int availableCapacity = 0;
        
        // Buscar paquetes que se liberarán antes de la llegada
        for (Map.Entry<Package, ArrayList<Flight>> entry : currentSolution.entrySet()) {
            Package assignedPkg = entry.getKey();
            ArrayList<Flight> assignedRoute = entry.getValue();
            
            if (assignedRoute != null && !assignedRoute.isEmpty()) {
                Airport assignedDestination = getAirportByCity(assignedPkg.getDestinationCity());
                if (assignedDestination.equals(airport)) {
                    int assignedArrivalTime = calculateArrivalTime(assignedPkg, assignedRoute);
                    int assignedPickupTime = assignedArrivalTime + (Constants.CUSTOMER_PICKUP_MAX_HOURS * 60);
                    
                    // Si este paquete se liberará antes de que llegue el nuevo paquete
                    if (assignedPickupTime <= arrivalTime) {
                        int assignedProductCount = assignedPkg.getProducts() != null ? assignedPkg.getProducts().size() : 1;
                        availableCapacity += assignedProductCount;
                    }
                }
            }
        }
        
        return availableCapacity;
    }
    
    private ArrayList<Flight> findBestRoute(Package pkg) {
        City origin = pkg.getCurrentLocation();
        City destination = pkg.getDestinationCity();
        
        // Si ya está en la ciudad destino, no necesita vuelos
        if (origin.equals(destination)) {
            return new ArrayList<>();
        }
        
        // Introducir aleatoriedad en el orden de búsqueda de rutas
        ArrayList<ArrayList<Flight>> validRoutes = new ArrayList<>();
        ArrayList<String> routeTypes = new ArrayList<>();
        ArrayList<Double> routeScores = new ArrayList<>(); // Puntajes para cada ruta
        
        // 1. Buscar ruta directa
        ArrayList<Flight> directRoute = findDirectRoute(origin, destination);
        if (directRoute != null && isRouteValid(pkg, directRoute)) {
            validRoutes.add(directRoute);
            routeTypes.add("directa");
            
            // Calcular margen de tiempo para la ruta directa
            double directScore = calculateRouteTimeMargin(pkg, directRoute);
            routeScores.add(directScore);
        }
        
        // 2. Buscar ruta con una escala
        ArrayList<Flight> oneStopRoute = findOneStopRoute(origin, destination);
        if (oneStopRoute != null && isRouteValid(pkg, oneStopRoute)) {
            validRoutes.add(oneStopRoute);
            routeTypes.add("una escala");
            
            // Calcular margen de tiempo para la ruta con una escala
            double oneStopScore = calculateRouteTimeMargin(pkg, oneStopRoute);
            routeScores.add(oneStopScore);
        }
        
        // 3. Buscar ruta con dos escalas
        ArrayList<Flight> twoStopRoute = findTwoStopRoute(origin, destination);
        if (twoStopRoute != null && isRouteValid(pkg, twoStopRoute)) {
            validRoutes.add(twoStopRoute);
            routeTypes.add("dos escalas");
            
            // Calcular margen de tiempo para la ruta con dos escalas
            double twoStopScore = calculateRouteTimeMargin(pkg, twoStopRoute);
            routeScores.add(twoStopScore);
        }
        
        // Si no hay rutas válidas, intentar un segundo pase con menos restricciones
        if (validRoutes.isEmpty()) {
            // Podríamos implementar un reintento con criterios más flexibles aquí
            return null;
        }
        
        // Seleccionar una ruta basada en probabilidad ponderada por margen de tiempo
        int totalRoutes = validRoutes.size();
        int selectedIndex;
        
        if (totalRoutes > 1) {
            // Calcular probabilidades basadas en puntajes
            double totalScore = 0;
            for (double score : routeScores) {
                totalScore += score;
            }
            
            if (totalScore > 0) {
                // Selección con probabilidad proporcional al margen de tiempo
                double rand = random.nextDouble() * totalScore;
                double cumulativeScore = 0;
                selectedIndex = 0;
                
                for (int i = 0; i < routeScores.size(); i++) {
                    cumulativeScore += routeScores.get(i);
                    if (rand <= cumulativeScore) {
                        selectedIndex = i;
                        break;
                    }
                }
            } else {
                // Si todos los puntajes son 0 o negativos, selección aleatoria simple
                selectedIndex = random.nextInt(totalRoutes);
            }
        } else {
            // Solo hay una ruta disponible
            selectedIndex = 0;
        }
        
        return validRoutes.get(selectedIndex);
    }
    
    /**
     * Calcula un puntaje para una ruta basado en el margen de tiempo antes del deadline
     * Rutas con mayor margen reciben puntajes más altos
     */
    private double calculateRouteTimeMargin(Package pkg, ArrayList<Flight> route) {
        double totalTime = 0;
        
        // Calcular tiempo total de la ruta
        for (Flight flight : route) {
            totalTime += flight.getTransportTime();
        }
        
        // Añadir penalizaciones
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0; // Conexiones
        }
        
        // Penalización por continente
        boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == pkg.getDestinationCity().getContinent();
        if (sameContinentRoute) {
            totalTime += Constants.SAME_CONTINENT_TRANSPORT_TIME;
        } else {
            totalTime += Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME;
        }
        
        // Calcular margen antes del deadline (en horas)
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDeadline = ChronoUnit.HOURS.between(now, pkg.getDeliveryDeadline());
        double margin = hoursUntilDeadline - totalTime;
        
        // Convertir margen a un puntaje (mayor margen = mayor puntaje)
        // Puntaje mínimo es 1 para rutas válidas
        return Math.max(margin * 10, 1.0);
    }
    
    private ArrayList<Flight> findDirectRoute(City origin, City destination) {
        Airport originAirport = getAirportByCity(origin);
        Airport destinationAirport = getAirportByCity(destination);
        
        if (originAirport == null || destinationAirport == null) {
            return null;
        }
        
        // Buscar vuelo directo
        for (Flight flight : flights) {
            if (flight.getOriginAirport().equals(originAirport) && 
                flight.getDestinationAirport().equals(destinationAirport) &&
                flight.getUsedCapacity() < flight.getMaxCapacity()) {
                
                ArrayList<Flight> route = new ArrayList<>();
                route.add(flight);
                return route;
            }
        }
        
        return null;
    }
    
    private ArrayList<Flight> findOneStopRoute(City origin, City destination) {
        Airport originAirport = getAirportByCity(origin);
        Airport destinationAirport = getAirportByCity(destination);
        
        if (originAirport == null || destinationAirport == null) {
            return null;
        }
        
        // Crear una lista de aeropuertos intermedios potenciales y barajarla
        ArrayList<Airport> potentialIntermediates = new ArrayList<>();
        for (Airport airport : airports) {
            if (!airport.equals(originAirport) && !airport.equals(destinationAirport)) {
                potentialIntermediates.add(airport);
            }
        }
        
        // Barajar la lista para explorar aeropuertos intermedios en orden aleatorio
        Collections.shuffle(potentialIntermediates, random);
        
        // Buscar escala intermedia
        for (Airport intermediateAirport : potentialIntermediates) {
            
            // Buscar vuelo de origen a intermedio
            Flight firstFlight = null;
            for (Flight flight : flights) {
                if (flight.getOriginAirport().equals(originAirport) && 
                    flight.getDestinationAirport().equals(intermediateAirport) &&
                    flight.getUsedCapacity() < flight.getMaxCapacity()) {
                    firstFlight = flight;
                    break;
                }
            }
            
            if (firstFlight == null) continue;
            
            // Buscar vuelo de intermedio a destino
            Flight secondFlight = null;
            for (Flight flight : flights) {
                if (flight.getOriginAirport().equals(intermediateAirport) && 
                    flight.getDestinationAirport().equals(destinationAirport) &&
                    flight.getUsedCapacity() < flight.getMaxCapacity()) {
                    secondFlight = flight;
                    break;
                }
            }
            
            if (secondFlight != null) {
                ArrayList<Flight> route = new ArrayList<>();
                route.add(firstFlight);
                route.add(secondFlight);
                return route;
            }
        }
        
        return null;
    }
    
    private ArrayList<Flight> findTwoStopRoute(City origin, City destination) {
        Airport originAirport = getAirportByCity(origin);
        Airport destinationAirport = getAirportByCity(destination);
        
        if (originAirport == null || destinationAirport == null) {
            return null;
        }
        
        // Crear listas de posibles aeropuertos intermedios
        ArrayList<Airport> firstIntermediates = new ArrayList<>();
        for (Airport airport : airports) {
            if (!airport.equals(originAirport) && !airport.equals(destinationAirport)) {
                firstIntermediates.add(airport);
            }
        }
        
        // Barajar la primera lista de intermedios
        Collections.shuffle(firstIntermediates, random);
        
        // Limitar la búsqueda a un subconjunto aleatorio para mejorar rendimiento
        int maxFirstIntermediates = Math.min(10, firstIntermediates.size());
        
        // Buscar ruta con dos escalas intermedias
        for (int i = 0; i < maxFirstIntermediates; i++) {
            Airport firstIntermediate = firstIntermediates.get(i);
            
            // Crear y barajar lista de segundos intermedios
            ArrayList<Airport> secondIntermediates = new ArrayList<>();
            for (Airport airport : airports) {
                if (!airport.equals(originAirport) && 
                    !airport.equals(destinationAirport) &&
                    !airport.equals(firstIntermediate)) {
                    secondIntermediates.add(airport);
                }
            }
            
            Collections.shuffle(secondIntermediates, random);
            
            // Limitar también la búsqueda del segundo intermedio
            int maxSecondIntermediates = Math.min(10, secondIntermediates.size());
            
            for (int j = 0; j < maxSecondIntermediates; j++) {
                Airport secondIntermediate = secondIntermediates.get(j);
                
                // Buscar primer vuelo: origen -> primera escala
                Flight firstFlight = null;
                for (Flight flight : flights) {
                    if (flight.getOriginAirport().equals(originAirport) && 
                        flight.getDestinationAirport().equals(firstIntermediate) &&
                        flight.getUsedCapacity() < flight.getMaxCapacity()) {
                        firstFlight = flight;
                        break;
                    }
                }
                
                if (firstFlight == null) continue;
                
                // Buscar segundo vuelo: primera escala -> segunda escala
                Flight secondFlight = null;
                for (Flight flight : flights) {
                    if (flight.getOriginAirport().equals(firstIntermediate) && 
                        flight.getDestinationAirport().equals(secondIntermediate) &&
                        flight.getUsedCapacity() < flight.getMaxCapacity()) {
                        secondFlight = flight;
                        break;
                    }
                }
                
                if (secondFlight == null) continue;
                
                // Buscar tercer vuelo: segunda escala -> destino
                Flight thirdFlight = null;
                for (Flight flight : flights) {
                    if (flight.getOriginAirport().equals(secondIntermediate) && 
                        flight.getDestinationAirport().equals(destinationAirport) &&
                        flight.getUsedCapacity() < flight.getMaxCapacity()) {
                        thirdFlight = flight;
                        break;
                    }
                }
                
                if (thirdFlight != null) {
                    ArrayList<Flight> route = new ArrayList<>();
                    route.add(firstFlight);
                    route.add(secondFlight);
                    route.add(thirdFlight);
                    
                    // Verificar que la ruta total no exceda límites de tiempo
                    double totalTime = firstFlight.getTransportTime() + 
                                      secondFlight.getTransportTime() + 
                                      thirdFlight.getTransportTime();
                    
                    // Agregar penalización por múltiples escalas (tiempo de conexión)
                    totalTime += 2.0; // 2 horas adicionales por cada escala
                    
                    // Si la ruta es demasiado larga, continuar buscando
                    if (totalTime > Constants.DIFFERENT_CONTINENT_MAX_DELIVERY_TIME * 24) {
                        continue;
                    }
                    
                    return route;
                }
            }
        }
        
        return null;
    }
    
    private Airport getAirportByCity(City city) {
        for (Airport airport : airports) {
            if (airport.getCity().equals(city)) {
                return airport;
            }
        }
        return null;
    }
    
    private boolean isRouteValid(Package pkg, ArrayList<Flight> route) {
        if (route == null || route.isEmpty()) {
            return pkg.getCurrentLocation().equals(pkg.getDestinationCity());
        }
        
        // Verificar capacidad de vuelos
        for (Flight flight : route) {
            if (flight.getUsedCapacity() >= flight.getMaxCapacity()) {
                return false;
            }
        }
        
        // Verificar que la ruta sea continua
        for (int i = 0; i < route.size() - 1; i++) {
            if (!route.get(i).getDestinationAirport().equals(route.get(i + 1).getOriginAirport())) {
                return false;
            }
        }
        
        // Verificar restricciones de tiempo
        return isDeadlineRespected(pkg, route);
    }
    
    private boolean isDeadlineRespected(Package pkg, ArrayList<Flight> route) {
        double totalTime = 0;
        
        for (Flight flight : route) {
            totalTime += flight.getTransportTime();
        }
        
        // Añadir penalización por conexiones (2 horas por conexión)
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0;
        }
        
        // Agregar tiempo de traslado según continente
        City origin = pkg.getCurrentLocation();
        City destination = pkg.getDestinationCity();
        
        boolean sameContinentRoute = origin.getContinent() == destination.getContinent();
        
        if (sameContinentRoute) {
            totalTime += Constants.SAME_CONTINENT_TRANSPORT_TIME;
        } else {
            totalTime += Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME;
        }
        
        // Factor de seguridad aleatorio (1-10%) para asegurar entregas a tiempo
        // Más margen de seguridad para rutas complejas o intercontinentales
        double safetyMargin = 0.0;
        if (random != null) { // Verificar que random esté inicializado
            int complexityFactor = route.size() + (sameContinentRoute ? 0 : 2);
            safetyMargin = 0.01 * (1 + random.nextInt(complexityFactor * 3));
            totalTime = totalTime * (1.0 + safetyMargin); // Aumentar tiempo estimado para ser conservadores
        }
        
        // Convertir tiempo límite a horas para comparar
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDeadline = ChronoUnit.HOURS.between(now, pkg.getDeliveryDeadline());
        
        return totalTime <= hoursUntilDeadline;
    }
    
    private void updateFlightCapacities(ArrayList<Flight> route, int productCount) {
        for (Flight flight : route) {
            flight.setUsedCapacity(flight.getUsedCapacity() + productCount);
        }
    }
    
    private int calculateSolutionWeight(HashMap<Package, ArrayList<Flight>> solutionMap) {
        // El peso de la solución considera múltiples factores:
        // 1. Número total de paquetes asignados (maximizar)
        // 2. Número total de productos transportados (maximizar) - NUEVO
        // 3. Tiempo total de entrega (minimizar)
        // 4. Utilización de capacidad de vuelos (maximizar)
        // 5. Cumplimiento de deadlines (maximizar)
        // 6. Margen de seguridad antes de deadline (maximizar)
        
        int totalPackages = solutionMap.size();
        int totalProducts = 0; // NUEVO: contador de productos
        double totalDeliveryTime = 0;
        int onTimeDeliveries = 0;
        double totalCapacityUtilization = 0;
        int totalFlightsUsed = 0;
        double totalDeliveryMargin = 0; // Margen total antes del deadline
        
        // Calcular métricas
        for (Map.Entry<Package, ArrayList<Flight>> entry : solutionMap.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            // Contar productos en este paquete
            int packageProducts = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
            totalProducts += packageProducts;
            
            // Tiempo total de la ruta
            double routeTime = 0;
            for (Flight flight : route) {
                routeTime += flight.getTransportTime();
                totalCapacityUtilization += (double) flight.getUsedCapacity() / flight.getMaxCapacity();
                totalFlightsUsed++;
            }
            
            // Añadir penalización por conexiones
            if (route.size() > 1) {
                routeTime += (route.size() - 1) * 2.0; // 2 horas por cada conexión
            }
            
            totalDeliveryTime += routeTime;
            
            // Verificar si llega a tiempo y calcular margen
            if (isDeadlineRespected(pkg, route)) {
                onTimeDeliveries++;
                
                // Calcular margen de tiempo antes del deadline (en horas)
                LocalDateTime estimatedDelivery = pkg.getOrderDate().plusHours((long)routeTime);
                double marginHours = ChronoUnit.HOURS.between(estimatedDelivery, pkg.getDeliveryDeadline());
                totalDeliveryMargin += marginHours;
            }
        }
        
        // Fórmula de peso que combina múltiples objetivos
        // Maximizar paquetes asignados y entregas a tiempo, minimizar tiempo promedio
        double avgDeliveryTime = totalPackages > 0 ? totalDeliveryTime / totalPackages : 0;
        double avgCapacityUtilization = totalFlightsUsed > 0 ? totalCapacityUtilization / totalFlightsUsed : 0;
        double onTimeRate = totalPackages > 0 ? (double) onTimeDeliveries / totalPackages : 0;
        double avgDeliveryMargin = onTimeDeliveries > 0 ? totalDeliveryMargin / onTimeDeliveries : 0;
        
        // Peso final con énfasis extremo en entregas a tiempo
        int weight = (int) (
            totalPackages * 500 +             // Más paquetes asignados = mejor
            totalProducts * 50 +              // Más productos transportados = mejor (NUEVO)
            onTimeRate * 5000 +               // Entregas a tiempo con MÁXIMA prioridad
            Math.min(avgDeliveryMargin * 100, 1000) + // Premiar margen de seguridad (máx 1000)
            avgCapacityUtilization * 200 -    // Mayor utilización = mejor
            avgDeliveryTime * 100             // Menos tiempo promedio = mejor
        );
        
        // Penalización SEVERA si hay entregas tardías
        if (onTimeRate < 1.0) {
            // Reducir drásticamente el peso si hay entregas tardías
            weight = (int)(weight * Math.pow(onTimeRate, 3)); // Penalización cúbica
        }
        
        return weight;
    }

    public boolean isSolutionValid() {
        if (solution.isEmpty()) {
            return false;
        }
        
        // Obtener la solución actual
        HashMap<Package, ArrayList<Flight>> currentSolution = solution.keySet().iterator().next();
        
        // Verificar que todos los paquetes asignados tengan rutas válidas
        for (Map.Entry<Package, ArrayList<Flight>> entry : currentSolution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            if (!isRouteValid(pkg, route)) {
                return false;
            }
        }
        
        // Validación temporal de capacidades de almacenes
        if (!isTemporalSolutionValid(currentSolution)) {
            System.out.println("Solution violates temporal warehouse capacity constraints");
            return false;
        }
        
        return true;
    }
    
    public boolean isSolutionCapacityValid() {
        if (solution.isEmpty()) {
            return false;
        }
        
        // Crear mapa de uso de capacidad por vuelo
        HashMap<Flight, Integer> flightUsage = new HashMap<>();
        
        // Obtener la solución actual
        HashMap<Package, ArrayList<Flight>> currentSolution = solution.keySet().iterator().next();
        
        // Contar cuántos paquetes usan cada vuelo
        for (ArrayList<Flight> route : currentSolution.values()) {
            for (Flight flight : route) {
                flightUsage.put(flight, flightUsage.getOrDefault(flight, 0) + 1);
            }
        }
        
        // Verificar que ningún vuelo exceda su capacidad
        for (Map.Entry<Flight, Integer> entry : flightUsage.entrySet()) {
            if (entry.getValue() > entry.getKey().getMaxCapacity()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Imprime una descripción detallada de la solución actual.
     * Muestra estadísticas generales y las rutas asignadas a cada paquete.
     * 
     * @param detailLevel nivel de detalle (1: resumen, 2: rutas principales, 3: todas las rutas)
     */
    public void printSolutionDescription(int detailLevel) {
        if (solution.isEmpty()) {
            System.out.println("No hay solución disponible para mostrar.");
            return;
        }
        
        // Obtener la solución actual y su peso
        HashMap<Package, ArrayList<Flight>> currentSolution = solution.keySet().iterator().next();
        int solutionWeight = solution.get(currentSolution);
        
        // Calcular total de productos
        int totalProductsAssigned = 0;
        int totalProductsInSystem = 0;
        for (Package pkg : packages) {
            int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
            totalProductsInSystem += productCount;
            if (currentSolution.containsKey(pkg)) {
                totalProductsAssigned += productCount;
            }
        }
        
        // Estadísticas generales
        System.out.println("\n========== DESCRIPCIÓN DE LA SOLUCIÓN ==========");
        System.out.println("Peso de la solución: " + solutionWeight);
        System.out.println("Paquetes asignados: " + currentSolution.size() + "/" + packages.size());
        System.out.println("Productos transportados: " + totalProductsAssigned + "/" + totalProductsInSystem);
        
        // Calcular estadísticas adicionales
        int directRoutes = 0;
        int oneStopRoutes = 0;
        int twoStopRoutes = 0;
        int sameContinentRoutes = 0;
        int differentContinentRoutes = 0;
        int onTimeDeliveries = 0;
        
        for (Map.Entry<Package, ArrayList<Flight>> entry : currentSolution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            // Contar tipos de rutas
            if (route.size() == 1) directRoutes++;
            else if (route.size() == 2) oneStopRoutes++;
            else if (route.size() == 3) twoStopRoutes++;
            
            // Contar rutas por continente
            if (pkg.getCurrentLocation().getContinent() == pkg.getDestinationCity().getContinent()) {
                sameContinentRoutes++;
            } else {
                differentContinentRoutes++;
            }
            
            // Contar entregas a tiempo
            if (isDeadlineRespected(pkg, route)) {
                onTimeDeliveries++;
            }
        }
        
        // Mostrar estadísticas detalladas
        System.out.println("\n----- Estadísticas de Rutas -----");
        System.out.println("Rutas directas: " + directRoutes + " (" + formatPercentage(directRoutes, currentSolution.size()) + "%)");
        System.out.println("Rutas con 1 escala: " + oneStopRoutes + " (" + formatPercentage(oneStopRoutes, currentSolution.size()) + "%)");
        System.out.println("Rutas con 2 escalas: " + twoStopRoutes + " (" + formatPercentage(twoStopRoutes, currentSolution.size()) + "%)");
        System.out.println("Rutas en mismo continente: " + sameContinentRoutes + " (" + formatPercentage(sameContinentRoutes, currentSolution.size()) + "%)");
        System.out.println("Rutas entre continentes: " + differentContinentRoutes + " (" + formatPercentage(differentContinentRoutes, currentSolution.size()) + "%)");
        System.out.println("Entregas a tiempo: " + onTimeDeliveries + " (" + formatPercentage(onTimeDeliveries, currentSolution.size()) + "% de asignados)");
        System.out.println("Entregas a tiempo del total: " + onTimeDeliveries + "/" + packages.size() + " (" + formatPercentage(onTimeDeliveries, packages.size()) + "%)");
        
        int unassignedPackages = packages.size() - currentSolution.size();
        if (unassignedPackages > 0) {
            System.out.println("Paquetes no asignados: " + unassignedPackages + "/" + packages.size() + " (" + formatPercentage(unassignedPackages, packages.size()) + "%)");
            System.out.println("Razón principal: Capacidad de almacenes insuficiente");
        }
        
        // Mostrar estadísticas de ocupación de almacenes
        System.out.println("\n----- Ocupación de Almacenes -----");
        int totalWarehouseCapacity = 0;
        int totalWarehouseOccupancy = 0;
        int warehousesAtCapacity = 0;
        
        for (Map.Entry<Airport, Integer> entry : warehouseOccupancy.entrySet()) {
            Airport airport = entry.getKey();
            int occupancy = entry.getValue();
            
            if (airport.getWarehouse() != null) {
                int maxCapacity = airport.getWarehouse().getMaxCapacity();
                totalWarehouseCapacity += maxCapacity;
                totalWarehouseOccupancy += occupancy;
                
                if (occupancy >= maxCapacity) {
                    warehousesAtCapacity++;
                }
                
                // Mostrar almacenes con alta ocupación (>80%)
                double occupancyPercentage = (occupancy * 100.0) / maxCapacity;
                if (occupancyPercentage > 80.0) {
                    System.out.println("  " + airport.getCity().getName() + ": " + occupancy + "/" + maxCapacity + 
                                      " (" + String.format("%.1f", occupancyPercentage) + "%)");
                }
            }
        }
        
        double avgOccupancyPercentage = totalWarehouseCapacity > 0 ? 
            (totalWarehouseOccupancy * 100.0) / totalWarehouseCapacity : 0.0;
        
        System.out.println("Ocupación promedio de almacenes: " + String.format("%.1f", avgOccupancyPercentage) + "%");
        System.out.println("Almacenes llenos: " + warehousesAtCapacity + "/" + airports.size());
        
        // Mostrar información de picos temporales si la validación temporal está disponible
        if (temporalWarehouseOccupancy != null && !temporalWarehouseOccupancy.isEmpty()) {
            System.out.println("\n----- Picos de Ocupación Temporal -----");
            for (Airport airport : airports) {
                if (airport.getWarehouse() != null) {
                    int[] peakInfo = findPeakOccupancy(airport);
                    int peakMinute = peakInfo[0];
                    int maxOccupancy = peakInfo[1];
                    
                    if (maxOccupancy > 0) {
                        int peakHour = peakMinute / 60;
                        int peakMin = peakMinute % 60;
                        double peakPercentage = (maxOccupancy * 100.0) / airport.getWarehouse().getMaxCapacity();
                        
                        if (peakPercentage > 50.0) { // Mostrar solo aeropuertos con picos significativos
                            System.out.println("  " + airport.getCity().getName() + 
                                              " - Pico: " + maxOccupancy + "/" + airport.getWarehouse().getMaxCapacity() + 
                                              " (" + String.format("%.1f", peakPercentage) + "%) a las " + 
                                              String.format("%02d:%02d", peakHour, peakMin));
                        }
                    }
                }
            }
        }
        
        // Si el nivel de detalle es bajo, terminar aquí
        if (detailLevel < 2) {
            return;
        }
        
        // Mostrar rutas por prioridad
        System.out.println("\n----- Rutas por Prioridad -----");
        
        // Ordenar paquetes por prioridad
        List<Package> sortedPackages = new ArrayList<>(currentSolution.keySet());
        sortedPackages.sort((p1, p2) -> {
            // Primero por prioridad (mayor a menor)
            int priorityCompare = Double.compare(p2.getPriority(), p1.getPriority());
            if (priorityCompare != 0) return priorityCompare;
            
            // Luego por deadline (más cercano primero)
            return p1.getDeliveryDeadline().compareTo(p2.getDeliveryDeadline());
        });
        
        // Mostrar rutas de alta prioridad o todas según el nivel de detalle
        int routesToShow = detailLevel == 2 ? Math.min(10, sortedPackages.size()) : sortedPackages.size();
        
        for (int i = 0; i < routesToShow; i++) {
            Package pkg = sortedPackages.get(i);
            ArrayList<Flight> route = currentSolution.get(pkg);
            
            System.out.println("\nPaquete #" + pkg.getId() + 
                              " (Prioridad: " + String.format("%.2f", pkg.getPriority()) + 
                              ", Deadline: " + pkg.getDeliveryDeadline() + ")");
            
            System.out.println("  Origen: " + pkg.getCurrentLocation().getName() + 
                              " (" + pkg.getCurrentLocation().getContinent() + ")");
            System.out.println("  Destino: " + pkg.getDestinationCity().getName() + 
                              " (" + pkg.getDestinationCity().getContinent() + ")");
            
            if (route.isEmpty()) {
                System.out.println("  Ruta: Ya está en el destino");
                continue;
            }
            
            System.out.println("  Ruta (" + route.size() + " vuelos):");
            double totalTime = 0;
            
            for (int j = 0; j < route.size(); j++) {
                Flight flight = route.get(j);
                totalTime += flight.getTransportTime();
                
                System.out.println("    " + (j+1) + ". " + 
                                  flight.getOriginAirport().getCity().getName() + " → " + 
                                  flight.getDestinationAirport().getCity().getName() + 
                                  " (" + String.format("%.1f", flight.getTransportTime()) + "h, " + 
                                  flight.getUsedCapacity() + "/" + flight.getMaxCapacity() + " paquetes)");
            }
            
            // Agregar tiempo de conexión
            if (route.size() > 1) {
                totalTime += (route.size() - 1) * 2.0; // 2 horas por conexión
            }
            
            System.out.println("  Tiempo total estimado: " + String.format("%.1f", totalTime) + "h");
            
            boolean onTime = isDeadlineRespected(pkg, route);
            System.out.println("  Entrega a tiempo: " + (onTime ? "SÍ" : "NO"));
        }
        
        if (routesToShow < sortedPackages.size()) {
            System.out.println("\n... y " + (sortedPackages.size() - routesToShow) + " paquetes más (use nivel de detalle 3 para ver todos)");
        }
        
        System.out.println("\n=================================================");
    }
    
    private String formatPercentage(int value, int total) {
        if (total == 0) return "0.0";
        return String.format("%.1f", (value * 100.0) / total);
    }
    
    /**
     * Inicializa el mapa de ocupación de almacenes.
     * Cada aeropuerto de destino inicia con 0 paquetes asignados.
     */
    private void initializeWarehouseOccupancy() {
        for (Airport airport : airports) {
            warehouseOccupancy.put(airport, 0);
        }
    }
    
    /**
     * Inicializa la matriz temporal de ocupación de almacenes.
     * Cada aeropuerto tiene un array de 1440 elementos (24h * 60min).
     */
    private void initializeTemporalWarehouseOccupancy() {
        final int MINUTES_PER_DAY = 24 * 60; // 1440 minutos
        for (Airport airport : airports) {
            temporalWarehouseOccupancy.put(airport, new int[MINUTES_PER_DAY]);
        }
    }
    
    /**
     * Verifica si un aeropuerto de destino puede aceptar un paquete adicional
     * sin exceder la capacidad de su almacén.
     * 
     * @param destinationAirport aeropuerto de destino
     * @param productCount cantidad de productos en el paquete
     * @return true si hay capacidad disponible, false si está lleno
     */
    private boolean hasWarehouseCapacity(Airport destinationAirport, int productCount) {
        if (destinationAirport.getWarehouse() == null) {
            System.out.println("Warning: Airport " + destinationAirport.getCodeIATA() + " has no warehouse");
            return false;
        }
        
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        return (currentOccupancy + productCount) <= destinationAirport.getWarehouse().getMaxCapacity();
    }
    
    /**
     * Incrementa la ocupación del almacén de destino cuando se asigna un paquete.
     * 
     * @param destinationAirport aeropuerto de destino
     * @param productCount cantidad de productos en el paquete
     */
    private void incrementWarehouseOccupancy(Airport destinationAirport, int productCount) {
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        warehouseOccupancy.put(destinationAirport, currentOccupancy + productCount);
    }
    
    /**
     * Decrementa la ocupación del almacén de destino cuando se libera un paquete.
     * 
     * @param destinationAirport aeropuerto de destino
     * @param productCount cantidad de productos en el paquete
     */
    private void decrementWarehouseOccupancy(Airport destinationAirport, int productCount) {
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        if (currentOccupancy >= productCount) {
            warehouseOccupancy.put(destinationAirport, currentOccupancy - productCount);
        }
    }
    
    /**
     * Valida temporalmente si una solución respeta las capacidades de almacenes
     * durante todo el día, simulando el flujo de paquetes minuto a minuto.
     * 
     * @param solutionMap mapa de paquetes y sus rutas asignadas
     * @return true si no hay violaciones de capacidad, false si las hay
     */
    public boolean isTemporalSolutionValid(HashMap<Package, ArrayList<Flight>> solutionMap) {
        // Reinicializar matriz temporal
        initializeTemporalWarehouseOccupancy();
        
        // Simular el flujo de cada paquete
        for (Map.Entry<Package, ArrayList<Flight>> entry : solutionMap.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            if (!simulatePackageFlow(pkg, route)) {
                return false; // Se encontró una violación de capacidad
            }
        }
        
        return true; // No hay violaciones de capacidad
    }
    
    /**
     * Simula el flujo temporal de un paquete a través de su ruta asignada.
     * 
     * @param pkg paquete a simular
     * @param route ruta asignada al paquete
     * @return true si no viola capacidades, false si las viola
     */
    private boolean simulatePackageFlow(Package pkg, ArrayList<Flight> route) {
        if (route == null || route.isEmpty()) {
            // El paquete ya está en destino, cliente tiene 2 horas para recoger
            Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
            int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
            return addTemporalOccupancy(destinationAirport, 0, Constants.CUSTOMER_PICKUP_MAX_HOURS * 60, productCount); // 2 horas para pickup
        }
        
        int currentMinute = getPackageStartTime(pkg); // Momento cuando el paquete inicia su viaje
        Airport currentAirport = getAirportByCity(pkg.getCurrentLocation());
        int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
        
        for (int i = 0; i < route.size(); i++) {
            Flight flight = route.get(i);
            Airport departureAirport = flight.getOriginAirport();
            Airport arrivalAirport = flight.getDestinationAirport();
            
            // FASE 1: El paquete está en el aeropuerto de origen esperando el vuelo
            // Asumimos que llega 2 horas antes del vuelo para procesamiento
            int waitingTime = 120; // 2 horas de espera antes del vuelo
            if (!addTemporalOccupancy(departureAirport, currentMinute, waitingTime, productCount)) {
                System.out.println("Capacity violation at " + departureAirport.getCity().getName() + 
                                  " at minute " + currentMinute + " (waiting phase) for package " + pkg.getId());
                return false;
            }
            
            // FASE 2: El vuelo despega - productos dejan de ocupar origen
            int flightStartMinute = currentMinute + waitingTime;
            int flightDuration = (int)(flight.getTransportTime() * 60);
            
            // FASE 3: El vuelo llega - productos ocupan destino
            int arrivalMinute = flightStartMinute + flightDuration;
            
            // FASE 4: Productos permanecen en destino (tiempo de conexión si hay más vuelos)
            int stayDuration;
            if (i < route.size() - 1) {
                stayDuration = 120; // 2 horas de conexión hasta el siguiente vuelo
            } else {
                // Es el destino final - cliente tiene máximo 2 horas para recoger
                stayDuration = Constants.CUSTOMER_PICKUP_MAX_HOURS * 60; // 2 horas para pickup del cliente
            }
            
            if (stayDuration > 0 && !addTemporalOccupancy(arrivalAirport, arrivalMinute, stayDuration, productCount)) {
                System.out.println("Capacity violation at " + arrivalAirport.getCity().getName() + 
                                  " at minute " + arrivalMinute + " (arrival phase) for package " + pkg.getId());
                return false;
            }
            
            // Actualizar tiempo para el siguiente vuelo
            currentMinute = arrivalMinute;
            if (i < route.size() - 1) {
                currentMinute += 120; // Tiempo de conexión
            }
            currentAirport = arrivalAirport;
        }
        
        // La ocupación del destino final ya se maneja en el bucle anterior
        
        return true;
    }
    
    /**
     * Agrega ocupación temporal a un aeropuerto durante un período de tiempo.
     * 
     * @param airport aeropuerto donde agregar ocupación
     * @param startMinute minuto de inicio (0-1439)
     * @param durationMinutes duración en minutos
     * @param productCount número de productos a agregar
     * @return true si no excede capacidad, false si la excede
     */
    private boolean addTemporalOccupancy(Airport airport, int startMinute, int durationMinutes, int productCount) {
        if (airport == null || airport.getWarehouse() == null) {
            return false;
        }
        
        int[] occupancyArray = temporalWarehouseOccupancy.get(airport);
        int maxCapacity = airport.getWarehouse().getMaxCapacity();
        
        // Verificar y agregar ocupación para cada minuto del período
        for (int minute = startMinute; minute < Math.min(startMinute + durationMinutes, 1440); minute++) {
            occupancyArray[minute] += productCount;
            if (occupancyArray[minute] > maxCapacity) {
                return false; // Violación de capacidad
            }
        }
        
        return true;
    }
    
    /**
     * Obtiene el minuto de inicio del día cuando un paquete comienza su viaje.
     * Basado en la hora de pedido del paquete.
     * 
     * @param pkg paquete
     * @return minuto del día (0-1439)
     */
    private int getPackageStartTime(Package pkg) {
        // Usar la hora de pedido + un offset basado en el ID para distribuir los paquetes
        int baseTime = pkg.getOrderDate().getHour() * 60 + pkg.getOrderDate().getMinute();
        // Agregar un offset basado en el ID del paquete para distribuir en el tiempo
        int offset = (pkg.getId() % 60); // Distribuir en hasta 60 minutos
        return (baseTime + offset) % 1440; // Asegurar que esté en el rango 0-1439
    }
    
    /**
     * Encuentra el minuto del día con mayor ocupación en un aeropuerto específico.
     * 
     * @param airport aeropuerto a analizar
     * @return array [minuto, ocupación_máxima]
     */
    private int[] findPeakOccupancy(Airport airport) {
        int[] occupancyArray = temporalWarehouseOccupancy.get(airport);
        int maxOccupancy = 0;
        int peakMinute = 0;
        
        for (int minute = 0; minute < 1440; minute++) {
            if (occupancyArray[minute] > maxOccupancy) {
                maxOccupancy = occupancyArray[minute];
                peakMinute = minute;
            }
        }
        
        return new int[]{peakMinute, maxOccupancy};
    }
}
