package com.system.morapack.schemas.algorithm.TabuSearch;

import com.system.morapack.schemas.AirportSchema;
import com.system.morapack.schemas.FlightSchema;
import com.system.morapack.schemas.OrderSchema;
import com.system.morapack.schemas.algorithm.Input.InputAirports;
import com.system.morapack.schemas.algorithm.Input.InputData;
import com.system.morapack.schemas.algorithm.Input.InputProducts;
import com.system.morapack.config.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Implementación principal del algoritmo Tabu Search para el problema de asignación de paquetes
 */
public class TabuSearch {
    // Datos de entrada
    private ArrayList<AirportSchema> airportSchemas;
    private ArrayList<FlightSchema> flightSchemas;
    private ArrayList<OrderSchema> orderSchemas;
    private Map<String, AirportSchema> cityToAirportMap;
    
    // Parámetros del algoritmo
    private int maxIterations;
    private int maxNoImprovement;
    private int neighborhoodSize;
    private int tabuListSize;
    private long tabuTenure;
    private double intensificationThreshold;
    
    // Componentes del algoritmo
    private TabuList tabuList;
    private NeighborhoodGenerator neighborhoodGenerator;
    private Random random;
    private long startTime;
    
    // Variables de control
    private int iterationsSinceImprovement;
    private int diversificationCount;
    private int intensificationCount;
    private boolean inDiversificationMode;
    
    // Resultados
    private TabuSolution currentSolution;
    private TabuSolution bestSolution;
    private int bestScore;
    private int currentIteration;
    
    /**
     * Constructor con configuración por defecto
     */
    public TabuSearch(String airportsFilePath, String flightsFilePath, String productsFilePath) {
        this.random = new Random();
        
        // Cargar datos de entrada reutilizando las clases de Input
        loadInputData(airportsFilePath, flightsFilePath, productsFilePath);
        
        // Configurar parámetros por defecto
        this.maxIterations = 1000;
        this.maxNoImprovement = 100;
        this.neighborhoodSize = 100;
        this.tabuListSize = 50;
        this.tabuTenure = 10000; // 10 segundos
        this.intensificationThreshold = 0.05; // 5% de mejora
        
        // Inicializar componentes
        this.tabuList = new TabuList(tabuListSize, tabuTenure);
        this.neighborhoodGenerator = new NeighborhoodGenerator(flightSchemas, airportSchemas, cityToAirportMap, neighborhoodSize);
        
        // Inicializar variables de control
        this.iterationsSinceImprovement = 0;
        this.diversificationCount = 0;
        this.intensificationCount = 0;
        this.inDiversificationMode = false;
    }
    
    /**
     * Constructor con parámetros configurables
     */
    public TabuSearch(String airportsFilePath, String flightsFilePath, String productsFilePath,
                      int maxIterations, int maxNoImprovement, int neighborhoodSize,
                      int tabuListSize, long tabuTenure) {
        this(airportsFilePath, flightsFilePath, productsFilePath);
        this.maxIterations = maxIterations;
        this.maxNoImprovement = maxNoImprovement;
        this.neighborhoodSize = neighborhoodSize;
        this.tabuListSize = tabuListSize;
        this.tabuTenure = tabuTenure;
        
        // Reinicializar componentes con los nuevos parámetros
        this.tabuList = new TabuList(tabuListSize, tabuTenure);
        this.neighborhoodGenerator = new NeighborhoodGenerator(flightSchemas, airportSchemas, cityToAirportMap, neighborhoodSize);
    }
    
    /**
     * Carga los datos de entrada utilizando las clases existentes
     */
    private void loadInputData(String airportsFilePath, String flightsFilePath, String productsFilePath) {
        System.out.println("Cargando datos para Tabu Search...");
        
        // Cargar aeropuertos
        InputAirports inputAirports = new InputAirports(airportsFilePath);
        this.airportSchemas = inputAirports.readAirports();
        System.out.println("Aeropuertos cargados: " + airportSchemas.size());
        
        // Cargar vuelos
        InputData inputData = new InputData(flightsFilePath, airportSchemas);
        this.flightSchemas = inputData.readFlights();
        System.out.println("Vuelos cargados: " + flightSchemas.size());
        
        // Cargar productos/paquetes
        InputProducts inputProducts = new InputProducts(productsFilePath, airportSchemas);
        this.orderSchemas = inputProducts.readProducts();
        System.out.println("Paquetes cargados: " + orderSchemas.size());
        
        // Construir mapa de ciudades a aeropuertos
        buildCityToAirportMap();
    }
    
    /**
     * Construye un mapa de nombres de ciudad a objetos AirportSchema
     */
    private void buildCityToAirportMap() {
        this.cityToAirportMap = new HashMap<>();
        for (AirportSchema airportSchema : airportSchemas) {
            if (airportSchema.getCitySchema() != null && airportSchema.getCitySchema().getName() != null) {
                cityToAirportMap.put(airportSchema.getCitySchema().getName().toLowerCase().trim(), airportSchema);
            }
        }
    }
    
    /**
     * Ejecuta el algoritmo Tabu Search
     */
    public TabuSolution solve() {
        System.out.println("\n=== INICIANDO ALGORITMO TABU SEARCH ===");
        startTime = System.currentTimeMillis();
        
        // Inicializar solución actual y mejor solución
        currentSolution = generateInitialSolution();
        bestSolution = new TabuSolution(currentSolution);
        bestScore = bestSolution.getScore();
        
        System.out.println("Solución inicial generada con " + 
                          currentSolution.getAssignedPackagesCount() + " paquetes asignados");
        System.out.println("Peso inicial: " + bestScore);
        
        // Bucle principal del algoritmo
        currentIteration = 0;
        iterationsSinceImprovement = 0;
        
        while (currentIteration < maxIterations && iterationsSinceImprovement < maxNoImprovement) {
            if (currentIteration % 10 == 0 || Constants.VERBOSE_LOGGING) {
                System.out.println("Tabu Search Iteración " + currentIteration + "/" + maxIterations + 
                                 " | Sin mejora: " + iterationsSinceImprovement);
            }
            
            // Generar vecindario
            List<TabuMove> neighborhood = neighborhoodGenerator.generateNeighborhood(currentSolution);
            
            if (neighborhood.isEmpty()) {
                System.out.println("No se pudieron generar movimientos vecinos, aplicando diversificación.");
                applyDiversification();
                currentIteration++;
                continue;
            }
            
            // Evaluar movimientos y seleccionar el mejor no tabú o que cumpla el criterio de aspiración
            TabuMove bestMove = null;
            TabuSolution bestNeighbor = null;
            int bestNeighborScore = Integer.MIN_VALUE;
            
            for (TabuMove move : neighborhood) {
                // Crear una solución temporal aplicando el movimiento
                TabuSolution tempSolution = new TabuSolution(currentSolution);
                if (tempSolution.applyMove(move)) {
                    int tempScore = tempSolution.getScore();
                    
                    // Verificar si es un movimiento tabú y si cumple el criterio de aspiración
                    boolean isTabu = tabuList.contains(move);
                    boolean satisfiesAspiration = tempScore > bestScore; // Criterio de aspiración
                    
                    if (!isTabu || satisfiesAspiration) {
                        if (tempScore > bestNeighborScore) {
                            bestNeighborScore = tempScore;
                            bestNeighbor = tempSolution;
                            bestMove = move;
                        }
                    }
                }
            }
            
            // Si no encontramos un movimiento válido, aplicar diversificación
            if (bestMove == null) {
                System.out.println("No se encontró un movimiento válido, aplicando diversificación.");
                applyDiversification();
                currentIteration++;
                continue;
            }
            
            // Aplicar el mejor movimiento
            currentSolution = bestNeighbor;
            int currentScore = currentSolution.getScore();
            
            // Añadir el movimiento inverso a la lista tabú
            tabuList.add(bestMove.getInverseMove());
            
            // Verificar si hay mejora
            if (currentScore > bestScore) {
                // Calcular porcentaje de mejora
                double improvementRatio = (double)(currentScore - bestScore) / Math.max(bestScore, 1);
                
                // Actualizar mejor solución
                bestSolution = new TabuSolution(currentSolution);
                bestScore = currentScore;
                iterationsSinceImprovement = 0;
                
                // Mostrar progreso
                System.out.println("Iteración " + currentIteration + ": ¡Nueva mejor solución! " +
                                 "Peso: " + bestScore + 
                                 " (mejora: " + String.format("%.2f%%", improvementRatio * 100) + ")" +
                                 " | No asignados: " + bestSolution.getUnassignedPackagesCount());
                
                // Aplicar intensificación si la mejora es significativa
                if (improvementRatio > intensificationThreshold) {
                    applyIntensification();
                }
            } else {
                iterationsSinceImprovement++;
                
                // Aplicar diversificación si llevamos muchas iteraciones sin mejora
                if (iterationsSinceImprovement >= maxNoImprovement / 2 && !inDiversificationMode) {
                    applyDiversification();
                }
            }
            
            currentIteration++;
        }
        
        // Mostrar estadísticas finales
        long endTime = System.currentTimeMillis();
        long totalTime = (endTime - startTime) / 1000;
        
        System.out.println("\nTabu Search completado:");
        System.out.println("  Tiempo total: " + totalTime + " segundos");
        System.out.println("  Iteraciones: " + currentIteration);
        System.out.println("  Peso final: " + bestScore);
        System.out.println("  Paquetes asignados: " + bestSolution.getAssignedPackagesCount() + "/" + orderSchemas.size());
        System.out.println("  Diversificaciones: " + diversificationCount);
        System.out.println("  Intensificaciones: " + intensificationCount);
        
        return bestSolution;
    }
    
    /**
     * Genera una solución inicial para el algoritmo Tabu Search
     */
    private TabuSolution generateInitialSolution() {
        System.out.println("=== GENERANDO SOLUCIÓN INICIAL PARA TABU SEARCH ===");
        
        // Crear una solución vacía - la unitización se aplica internamente en TabuSolution
        TabuSolution solution = new TabuSolution(orderSchemas, airportSchemas, flightSchemas);
        
        // Decidir entre greedy o aleatoria basado en el parámetro
        if (Constants.USE_GREEDY_INITIAL_SOLUTION) {
            generateGreedyInitialSolution(solution);
        } else {
            generateRandomInitialSolution(solution);
        }
        
        return solution;
    }
    
    /**
     * Genera una solución inicial greedy
     */
    private void generateGreedyInitialSolution(TabuSolution solution) {
        System.out.println("Generando solución inicial greedy...");
        
        // Obtener paquetes y ordenar por deadline
        ArrayList<OrderSchema> sortedOrderSchemas = new ArrayList<>(orderSchemas);
        Collections.sort(sortedOrderSchemas, (p1, p2) -> {
            if (p1.getDeliveryDeadline() == null) return 1;
            if (p2.getDeliveryDeadline() == null) return -1;
            return p1.getDeliveryDeadline().compareTo(p2.getDeliveryDeadline());
        });
        
        int assignedCount = 0;
        
        // Intentar asignar cada paquete
        for (OrderSchema pkg : sortedOrderSchemas) {
            // Generar posibles rutas para este paquete
            List<TabuMove> insertMoves = neighborhoodGenerator.generateInsertMoves(
                solution.getSolution(), Collections.singletonList(pkg), 5);
            
            // Si encontramos movimientos, aplicar el mejor
            if (!insertMoves.isEmpty()) {
                // Evaluar cada movimiento
                TabuMove bestMove = null;
                int bestScore = Integer.MIN_VALUE;
                
                for (TabuMove move : insertMoves) {
                    TabuSolution tempSolution = new TabuSolution(solution);
                    if (tempSolution.applyMove(move)) {
                        int score = tempSolution.getScore();
                        if (score > bestScore) {
                            bestScore = score;
                            bestMove = move;
                        }
                    }
                }
                
                // Aplicar el mejor movimiento
                if (bestMove != null && solution.applyMove(bestMove)) {
                    assignedCount++;
                }
            }
        }
        
        System.out.println("Solución greedy generada: " + assignedCount + "/" + orderSchemas.size() + " paquetes asignados");
    }
    
    /**
     * Genera una solución inicial aleatoria
     */
    private void generateRandomInitialSolution(TabuSolution solution) {
        System.out.println("Generando solución inicial aleatoria...");
        System.out.println("Probabilidad de asignación: " + (Constants.RANDOM_ASSIGNMENT_PROBABILITY * 100) + "%");
        
        // Barajar paquetes para orden aleatorio
        ArrayList<OrderSchema> shuffledOrderSchemas = new ArrayList<>(orderSchemas);
        Collections.shuffle(shuffledOrderSchemas, random);
        
        int assignedCount = 0;
        
        for (OrderSchema pkg : shuffledOrderSchemas) {
            // Asignación aleatoria basada en probabilidad
            if (random.nextDouble() < Constants.RANDOM_ASSIGNMENT_PROBABILITY) {
                // Generar posibles rutas para este paquete
                List<TabuMove> insertMoves = neighborhoodGenerator.generateInsertMoves(
                    solution.getSolution(), Collections.singletonList(pkg), 3);
                
                // Si encontramos movimientos, aplicar uno aleatorio
                if (!insertMoves.isEmpty()) {
                    TabuMove randomMove = insertMoves.get(random.nextInt(insertMoves.size()));
                    if (solution.applyMove(randomMove)) {
                        assignedCount++;
                    }
                }
            }
        }
        
        System.out.println("Solución aleatoria generada: " + assignedCount + "/" + orderSchemas.size() + " paquetes asignados");
    }
    
    /**
     * Implementa la estrategia de intensificación
     * Enfoca la búsqueda en áreas prometedoras del espacio de soluciones
     */
    private void applyIntensification() {
        if (Constants.VERBOSE_LOGGING) {
            System.out.println("Aplicando intensificación...");
        }
        
        // Enfocar en mejorar los paquetes ya asignados
        HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolutionMap = currentSolution.getSolution();
        ArrayList<OrderSchema> assignedOrderSchemas = new ArrayList<>(currentSolutionMap.keySet());
        
        // Ordenar paquetes por prioridad o algún otro criterio relevante
        Collections.sort(assignedOrderSchemas, (p1, p2) ->
            Double.compare(p2.getPriority(), p1.getPriority()));
        
        // Intentar reasignar paquetes de alta prioridad a mejores rutas
        int maxPackagesToIntensify = Math.min(50, assignedOrderSchemas.size());
        
        for (int i = 0; i < maxPackagesToIntensify; i++) {
            // Procesar paquetes de alta prioridad
            
            // Generar movimientos de reasignación
            List<TabuMove> reassignMoves = neighborhoodGenerator.generateReassignMoves(
                currentSolution.getSolution(), 1);
            
            // Evaluar y aplicar el mejor movimiento no tabú
            TabuMove bestMove = null;
            TabuSolution bestNeighbor = null;
            int bestScore = currentSolution.getScore();
            
            for (TabuMove move : reassignMoves) {
                if (!tabuList.contains(move)) {
                    TabuSolution tempSolution = new TabuSolution(currentSolution);
                    if (tempSolution.applyMove(move)) {
                        int score = tempSolution.getScore();
                        if (score > bestScore) {
                            bestScore = score;
                            bestNeighbor = tempSolution;
                            bestMove = move;
                        }
                    }
                }
            }
            
            // Aplicar el mejor movimiento
            if (bestMove != null) {
                currentSolution = bestNeighbor;
                tabuList.add(bestMove.getInverseMove());
            }
        }
        
        inDiversificationMode = false;
    }
    
    /**
     * Implementa la estrategia de diversificación
     * Explora nuevas áreas del espacio de soluciones
     */
    private void applyDiversification() {
        System.out.println("\n🔍 ACTIVANDO DIVERSIFICACIÓN EN TABU SEARCH 🔍");
        System.out.println("Iteración " + currentIteration + ": " + iterationsSinceImprovement + 
                         " iteraciones sin mejora");
        
        // Dependiendo del número de diversificaciones, elegir diferentes estrategias
        int strategyIndex = diversificationCount % 3;
        
        switch (strategyIndex) {
            case 0:
                // Estrategia 1: Liberar un porcentaje de paquetes asignados
                diversifyByRemoving();
                break;
                
            case 1:
                // Estrategia 2: Reasignar un porcentaje de paquetes
                diversifyByReassigning();
                break;
                
            case 2:
                // Estrategia 3: Intercambiar rutas entre paquetes
                diversifyBySwapping();
                break;
        }
        
        // Actualizar variables de control
        diversificationCount++;
        iterationsSinceImprovement = 0;
        inDiversificationMode = true;
        
        // Limpiar la lista tabú para permitir exploración fresca
        tabuList.clear();
        
        System.out.println("Diversificación #" + diversificationCount + " completada");
        System.out.println("=== FIN DIVERSIFICACIÓN ===\n");
    }
    
    /**
     * Diversifica la solución liberando paquetes asignados
     */
    private void diversifyByRemoving() {
        System.out.println("Estrategia: ELIMINACIÓN DE PAQUETES");
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolutionMap = currentSolution.getSolution();
        ArrayList<OrderSchema> assignedOrderSchemas = new ArrayList<>(currentSolutionMap.keySet());
        
        // Determinar cuántos paquetes liberar
        int packagesToRemove = (int)(assignedOrderSchemas.size() * 0.3); // 30% de los paquetes
        packagesToRemove = Math.min(packagesToRemove, 500); // Máximo 500 paquetes
        packagesToRemove = Math.max(packagesToRemove, 50);  // Mínimo 50 paquetes
        
        // Barajar la lista para aleatorización
        Collections.shuffle(assignedOrderSchemas, random);
        
        // Eliminar paquetes
        int removedCount = 0;
        for (int i = 0; i < Math.min(packagesToRemove, assignedOrderSchemas.size()); i++) {
            OrderSchema pkg = assignedOrderSchemas.get(i);
            TabuMove removeMove = new TabuMove(TabuMove.MoveType.REMOVE, pkg, currentSolutionMap.get(pkg));
            
            if (currentSolution.applyMove(removeMove)) {
                removedCount++;
            }
        }
        
        System.out.println("Eliminados " + removedCount + "/" + assignedOrderSchemas.size() + " paquetes");
    }
    
    /**
     * Diversifica la solución reasignando paquetes a nuevas rutas
     */
    private void diversifyByReassigning() {
        System.out.println("Estrategia: REASIGNACIÓN DE RUTAS");
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolutionMap = currentSolution.getSolution();
        ArrayList<OrderSchema> assignedOrderSchemas = new ArrayList<>(currentSolutionMap.keySet());
        
        // Determinar cuántos paquetes reasignar
        int packagesToReassign = (int)(assignedOrderSchemas.size() * 0.4); // 40% de los paquetes
        packagesToReassign = Math.min(packagesToReassign, 400); // Máximo 400 paquetes
        packagesToReassign = Math.max(packagesToReassign, 50);  // Mínimo 50 paquetes
        
        // Barajar la lista para aleatorización
        Collections.shuffle(assignedOrderSchemas, random);
        
        // Reasignar paquetes
        int reassignedCount = 0;
        for (int i = 0; i < Math.min(packagesToReassign, assignedOrderSchemas.size()); i++) {
            // Generar rutas alternativas para paquetes seleccionados
            List<TabuMove> reassignMoves = neighborhoodGenerator.generateReassignMoves(
                currentSolution.getSolution(), 1);
            
            if (!reassignMoves.isEmpty()) {
                // Seleccionar un movimiento aleatorio
                TabuMove randomMove = reassignMoves.get(random.nextInt(reassignMoves.size()));
                
                if (currentSolution.applyMove(randomMove)) {
                    reassignedCount++;
                }
            }
        }
        
        System.out.println("Reasignados " + reassignedCount + "/" + assignedOrderSchemas.size() + " paquetes");
    }
    
    /**
     * Diversifica la solución intercambiando rutas entre paquetes
     */
    private void diversifyBySwapping() {
        System.out.println("Estrategia: INTERCAMBIO DE RUTAS");
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolutionMap = currentSolution.getSolution();
        ArrayList<OrderSchema> assignedOrderSchemas = new ArrayList<>(currentSolutionMap.keySet());
        
        if (assignedOrderSchemas.size() < 2) {
            System.out.println("No hay suficientes paquetes para intercambiar, cambiando a estrategia de eliminación.");
            diversifyByRemoving();
            return;
        }
        
        // Determinar cuántos intercambios realizar
        int swapsToPerform = (int)(assignedOrderSchemas.size() * 0.25); // 25% de los paquetes
        swapsToPerform = Math.min(swapsToPerform, 300); // Máximo 300 intercambios
        swapsToPerform = Math.max(swapsToPerform, 40);  // Mínimo 40 intercambios
        
        // Realizar intercambios
        int swapsPerformed = 0;
        for (int i = 0; i < swapsToPerform; i++) {
            // Seleccionar dos paquetes aleatorios diferentes
            int idx1 = random.nextInt(assignedOrderSchemas.size());
            int idx2;
            do {
                idx2 = random.nextInt(assignedOrderSchemas.size());
            } while (idx1 == idx2);
            
            OrderSchema pkg1 = assignedOrderSchemas.get(idx1);
            OrderSchema pkg2 = assignedOrderSchemas.get(idx2);
            
            ArrayList<FlightSchema> route1 = currentSolutionMap.get(pkg1);
            ArrayList<FlightSchema> route2 = currentSolutionMap.get(pkg2);
            
            // Crear movimiento de intercambio
            TabuMove swapMove = new TabuMove(TabuMove.MoveType.SWAP, pkg1, route1, pkg2, route2);
            
            if (currentSolution.applyMove(swapMove)) {
                swapsPerformed++;
            }
            
            // Actualizar la solución para el siguiente intercambio
            currentSolutionMap = currentSolution.getSolution();
        }
        
        System.out.println("Intercambios realizados: " + swapsPerformed);
    }
    
    /**
     * Imprime una descripción detallada de la solución
     */
    public void printSolutionDescription(TabuSolution solution, int detailLevel) {
        // Aquí podrías reutilizar el código de descripción de solución de ALNS
        // o implementar tu propia versión para Tabu Search
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> solutionMap = solution.getSolution();
        int assignedPackages = solutionMap.size();
        int unassignedPackages = solution.getUnassignedPackagesCount();
        int score = solution.getScore();
        
        System.out.println("\n========== DESCRIPCIÓN DE LA SOLUCIÓN TABU SEARCH ==========");
        System.out.println("Peso de la solución: " + score);
        System.out.println("Paquetes asignados: " + assignedPackages + "/" + orderSchemas.size());
        System.out.println("Paquetes no asignados: " + unassignedPackages + "/" + orderSchemas.size() +
                         " (" + String.format("%.1f", (unassignedPackages * 100.0 / orderSchemas.size())) + "%)");
        
        // Más estadísticas detalladas según el nivel de detalle...
        // (podrías expandir esto según sea necesario)
    }
}
