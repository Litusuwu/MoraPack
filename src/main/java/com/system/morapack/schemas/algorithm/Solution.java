package com.system.morapack.schemas.algorithm;

import com.system.morapack.schemas.Airport;
import com.system.morapack.schemas.City;
import com.system.morapack.schemas.Flight;
import com.system.morapack.schemas.Package;
import com.system.morapack.schemas.Product;
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
    private InputAirports inputAirports;
    private InputData inputData;
    private InputProducts inputProducts;
    
    // CHANGED: Cache robusta City→Airport por nombre (evita problemas de equals)
    private Map<String, Airport> cityNameToAirportCache;
    private ArrayList<Airport> airports;
    private ArrayList<Flight> flights;
    private ArrayList<Package> packages;
    
    // PATCH: Unitización - flag y datos
    private static final boolean ENABLE_PRODUCT_UNITIZATION = true; // Flag para activar/desactivar
    private ArrayList<Package> originalPackages; // Packages originales antes de unitizar
    
    // NEW: Ancla temporal T0 para cálculos consistentes
    private LocalDateTime T0;
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
    
    // Mecanismos de diversificación e intensificación
    private int stagnationCounter;
    private int diversificationThreshold;
    private boolean diversificationMode;
    private int lastImprovementIteration;
    private double diversificationFactor;
    
    // Pool de paquetes no asignados para expansión ALNS
    private ArrayList<Package> unassignedPool;

    
    public Solution() {
        this.inputAirports = new InputAirports(Constants.AIRPORT_INFO_FILE_PATH);
        this.solution = new HashMap<>();
        this.airports = inputAirports.readAirports();
        this.inputData = new InputData(Constants.FLIGHTS_FILE_PATH, this.airports);
        this.flights = inputData.readFlights();
        this.inputProducts = new InputProducts(Constants.PRODUCTS_FILE_PATH, this.airports);
        this.originalPackages = inputProducts.readProducts();
        
        // PATCH: Aplicar unitización si está habilitada
        if (ENABLE_PRODUCT_UNITIZATION) {
            this.packages = expandPackagesToProductUnits(this.originalPackages);
            System.out.println("UNITIZACIÓN APLICADA: " + this.originalPackages.size() + 
                             " paquetes originales → " + this.packages.size() + " unidades de producto");
        } else {
            this.packages = new ArrayList<>(this.originalPackages);
            System.out.println("UNITIZACIÓN DESHABILITADA: Usando paquetes originales");
        }
        
        this.warehouseOccupancy = new HashMap<>();
        this.temporalWarehouseOccupancy = new HashMap<>();
        
        // CHANGED: Inicializar cache robusta y T0
        initializeCityToAirportCache();
        initializeT0();
        
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
        
        // Parámetros del algoritmo optimizados para MoraPack
        this.temperature = 100.0;         // Temperatura inicial más baja
        this.coolingRate = 0.98;          // Enfriamiento más rápido
        this.maxIterations = 1000;        // Muchas más iteraciones
        this.segmentSize = 25;            // Segmentos más pequeños para mejor adaptación
        
        // Inicializar mecanismos de diversificación
        this.stagnationCounter = 0;
        this.diversificationThreshold = 100; // Cambiar a diversificación después de 100 iteraciones sin mejora
        this.diversificationMode = false;
        this.lastImprovementIteration = 0;
        this.diversificationFactor = 1.0;
        
        // Inicializar pool de paquetes no asignados
        this.unassignedPool = new ArrayList<>();
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
        
        // 3.5. Inicializar pool de paquetes no asignados para ALNS
        initializeUnassignedPool();
        
        // 4. Ejecutar algoritmo ALNS
        System.out.println("\n=== INICIANDO ALGORITMO ALNS ===");
        runALNSAlgorithm();
        
        // 5. Mostrar resultado final
        System.out.println("\n=== RESULTADO FINAL ALNS ===");
        this.printSolutionDescription(2);
    }
    
    /**
     * Inicializa el pool de paquetes no asignados para expansión ALNS
     */
    private void initializeUnassignedPool() {
        unassignedPool.clear();
        
        // Obtener la solución actual
        HashMap<Package, ArrayList<Flight>> currentSolution = solution.keySet().iterator().next();
        
        // Agregar todos los paquetes no asignados al pool
        for (Package pkg : packages) {
            if (!currentSolution.containsKey(pkg)) {
                unassignedPool.add(pkg);
            }
        }
        
        if (Constants.VERBOSE_LOGGING) {
            System.out.println("Pool de no asignados inicializado: " + unassignedPool.size() + " paquetes disponibles para expansión ALNS");
        }
    }
    
    /**
     * Expande la lista de paquetes a reparar con algunos del pool no asignado
     * para permitir que ALNS explore la asignación de nuevos paquetes
     */
    private ArrayList<Map.Entry<Package, ArrayList<Flight>>> expandWithUnassignedPackages(
            ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages, int maxToAdd) {
        
        if (unassignedPool.isEmpty() || maxToAdd <= 0) {
            return destroyedPackages;
        }
        
        ArrayList<Map.Entry<Package, ArrayList<Flight>>> expandedList = new ArrayList<>(destroyedPackages);
        
        // Determinar probabilidad de expansión según pool no asignado
        double poolRatio = (double) unassignedPool.size() / packages.size();
        double expansionProbability;
        
        if (poolRatio > 0.5) {
            // Si >50% no asignados: expansión MUY AGRESIVA
            expansionProbability = diversificationMode ? 0.9 : 0.7; // 90%/70%
        } else if (poolRatio > 0.3) {
            // Si >30% no asignados: expansión AGRESIVA  
            expansionProbability = diversificationMode ? 0.7 : 0.5; // 70%/50%
        } else if (poolRatio > 0.1) {
            // Si >10% no asignados: expansión MODERADA
            expansionProbability = diversificationMode ? 0.5 : 0.3; // 50%/30%
        } else {
            // Si <10% no asignados: expansión CONSERVADORA
            expansionProbability = diversificationMode ? 0.3 : 0.1; // 30%/10%
        }
        
        if (random.nextDouble() < expansionProbability) {
            // Ordenar pool no asignado por urgencia (más urgentes primero)
            ArrayList<Package> sortedUnassigned = new ArrayList<>(unassignedPool);
            sortedUnassigned.sort((p1, p2) -> {
                // Ordenar por deadline (más urgente primero)
                LocalDateTime d1 = p1.getDeliveryDeadline();
                LocalDateTime d2 = p2.getDeliveryDeadline();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d1.compareTo(d2);
            });
            
            // Determinar cantidad a agregar según pool no asignado
            int dynamicMaxToAdd;
            if (poolRatio > 0.5) {
                dynamicMaxToAdd = Math.min(200, unassignedPool.size()); // Hasta 200 si >50% no asignados
            } else if (poolRatio > 0.3) {
                dynamicMaxToAdd = Math.min(100, unassignedPool.size()); // Hasta 100 si >30% no asignados
            } else {
                dynamicMaxToAdd = Math.min(50, unassignedPool.size());  // Hasta 50 si pocos no asignados
            }
            
            int toAdd = Math.min(dynamicMaxToAdd, sortedUnassigned.size());
            
            for (int i = 0; i < toAdd; i++) {
                Package pkg = sortedUnassigned.get(i);
                // Crear entrada con ruta vacía (será determinada por reparación)
                expandedList.add(new java.util.AbstractMap.SimpleEntry<>(pkg, new ArrayList<>()));
            }
            
            if (Constants.VERBOSE_LOGGING) {
                System.out.println("Expansión ALNS: Agregando " + toAdd + " paquetes no asignados para exploración" +
                                 " (Pool: " + unassignedPool.size() + "/" + packages.size() + 
                                 " = " + String.format("%.1f%%", poolRatio * 100) + 
                                 ", Prob: " + String.format("%.0f%%", expansionProbability * 100) + ")");
            }
        }
        
        return expandedList;
    }
    
    /**
     * Actualiza el pool de paquetes no asignados basado en la solución actual
     */
    private void updateUnassignedPool(HashMap<Package, ArrayList<Flight>> currentSolution) {
        unassignedPool.clear();
        
        // Agregar todos los paquetes no asignados al pool
        for (Package pkg : packages) {
            if (!currentSolution.containsKey(pkg)) {
                unassignedPool.add(pkg);
            }
        }
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
            // Log de iteración solo si es verboso o es múltiplo del intervalo
            if (Constants.VERBOSE_LOGGING || iteration % Constants.LOG_ITERATION_INTERVAL == 0) {
                System.out.println("ALNS Iteración " + iteration + "/" + maxIterations);
            }
            
            // Seleccionar operadores basado en pesos
            int[] selectedOps = selectOperators();
            int destructionOp = selectedOps[0];
            int repairOp = selectedOps[1];
            
            // Log de operadores solo en modo verboso
            if (Constants.VERBOSE_LOGGING) {
                System.out.println("  Operadores seleccionados: Destrucción=" + destructionOp + ", Reparación=" + repairOp);
            }
            
            // Crear copia de la solución actual
            HashMap<Package, ArrayList<Flight>> tempSolution = new HashMap<>(currentSolution);
            
            // PATCH: Crear snapshots completos antes de modificar
            Map<Flight, Integer> capacitySnapshot = snapshotCapacities();
            Map<Airport, Integer> warehouseSnapshot = snapshotWarehouses();
            
            // Aplicar operador de destrucción
            if (Constants.VERBOSE_LOGGING) {
                System.out.println("  Aplicando operador de destrucción...");
            }
            long startTime = System.currentTimeMillis();
            ALNSDestruction.DestructionResult destructionResult = applyDestructionOperator(
                tempSolution, destructionOp);
            long endTime = System.currentTimeMillis();
            
            if (Constants.VERBOSE_LOGGING) {
                System.out.println("  Operador de destrucción completado en " + (endTime - startTime) + "ms");
            }
            
            if (destructionResult == null || destructionResult.getDestroyedPackages().isEmpty()) {
                if (Constants.VERBOSE_LOGGING) {
                    System.out.println("  No se pudo destruir nada, continuando...");
                }
                continue; // No se pudo destruir nada
            }
            
            if (Constants.VERBOSE_LOGGING) {
                System.out.println("  Paquetes destruidos: " + destructionResult.getDestroyedPackages().size());
            }
            
            // PATCH: Usar solución parcial de destrucción y reconstruir estado
            tempSolution = new HashMap<>(destructionResult.getPartialSolution());
            rebuildCapacitiesFromSolution(tempSolution);
            rebuildWarehousesFromSolution(tempSolution);
            
            // NUEVO: Expandir con paquetes no asignados para exploración
            ArrayList<Map.Entry<Package, ArrayList<Flight>>> expandedPackages = 
                expandWithUnassignedPackages(destructionResult.getDestroyedPackages(), 100);
            
            // Aplicar operador de reparación con lista expandida
            ALNSRepair.RepairResult repairResult = applyRepairOperator(
                tempSolution, repairOp, expandedPackages);
            
            if (repairResult == null || !repairResult.isSuccess()) {
                // PATCH: Restaurar snapshots si falla la reparación
                restoreCapacities(capacitySnapshot);
                restoreWarehouses(warehouseSnapshot);
                continue; // No se pudo reparar
            }
            
            // PATCH: Usar solución reparada y reconstruir estado
            tempSolution = new HashMap<>(repairResult.getRepairedSolution());
            rebuildCapacitiesFromSolution(tempSolution);
            rebuildWarehousesFromSolution(tempSolution);
            
            // Evaluar nueva solución
            int tempWeight = calculateSolutionWeight(tempSolution);
            
            // Actualizar contador de uso
            operatorUsage[destructionOp][repairOp]++;
            
            // Criterio de aceptación mejorado con múltiples niveles de recompensa
            boolean accepted = false;
            double improvementRatio = 0.0;
            
            if (tempWeight > currentWeight) {
                improvementRatio = (double)(tempWeight - currentWeight) / Math.max(currentWeight, 1);
                currentSolution = tempSolution;
                currentWeight = tempWeight;
                accepted = true;

                if (tempWeight > bestWeight) {
                    // Nueva mejor solución global
                    bestWeight = tempWeight;
                    bestSolution.clear();
                    bestSolution.put(new HashMap<>(currentSolution), currentWeight);
                    operatorScores[destructionOp][repairOp] += 300; // Recompensa máxima
                    improvements++;
                    noImprovementCount = 0;
                    lastImprovementIteration = iteration;
                    stagnationCounter = 0;
                    diversificationMode = false; // Volver a intensificación después de mejora
                    updateUnassignedPool(currentSolution); // Actualizar pool de no asignados
                    // Siempre mostrar mejoras en la mejor solución global
                    System.out.println("Iteración " + iteration + ": ¡Nueva mejor solución! Peso: " + bestWeight + 
                                     " (mejora: " + String.format("%.2f%%", improvementRatio * 100) + ")" +
                                     " | No asignados: " + unassignedPool.size());
                } else if (improvementRatio > 0.05) {
                    // Mejora significativa (>5%)
                    operatorScores[destructionOp][repairOp] += 100;
                    noImprovementCount = Math.max(0, noImprovementCount - 5);
                    updateUnassignedPool(currentSolution); // Actualizar pool de no asignados
                } else if (improvementRatio > 0.01) {
                    // Mejora moderada (1-5%)
                    operatorScores[destructionOp][repairOp] += 50;
                    noImprovementCount = Math.max(0, noImprovementCount - 2);
                    updateUnassignedPool(currentSolution); // Actualizar pool de no asignados
                } else {
                    // Mejora pequeña (<1%)
                    operatorScores[destructionOp][repairOp] += 25;
                    updateUnassignedPool(currentSolution); // Actualizar pool de no asignados
                }
            } else {
                // Simulated Annealing con ajuste por calidad de la solución
                double delta = tempWeight - currentWeight;
                double adjustedTemp = temperature * (1.0 + 0.1 * Math.random()); // Pequeña variación
                double probability = Math.exp(delta / adjustedTemp);
                
                if (random.nextDouble() < probability) {
                    currentSolution = tempSolution;
                    currentWeight = tempWeight;
                    accepted = true;
                    updateUnassignedPool(currentSolution); // Actualizar pool de no asignados
                    operatorScores[destructionOp][repairOp] += 15; // Recompensa menor por exploración
                    noImprovementCount++;
                } else {
                    operatorScores[destructionOp][repairOp] += 5; // Recompensa mínima por intentar
                    noImprovementCount++;
                }
            }
            
            // PATCH: Restaurar snapshots si no se acepta la solución
            if (!accepted) {
                restoreCapacities(capacitySnapshot);
                restoreWarehouses(warehouseSnapshot);
                noImprovementCount++;
            }
            // NOTA: Si se acepta, tempSolution ya tiene el estado correcto reconstruido
            
            // Manejar diversificación vs intensificación
            stagnationCounter = iteration - lastImprovementIteration;
            if (stagnationCounter > diversificationThreshold && !diversificationMode) {
                diversificationMode = true;
                diversificationFactor = 1.5; // Aumentar destrucción
                temperature *= 2.0; // Aumentar temperatura para más exploración
                if (Constants.VERBOSE_LOGGING) {
                    System.out.println("Iteración " + iteration + ": Activando modo DIVERSIFICACIÓN");
                }
            } else if (diversificationMode && stagnationCounter <= diversificationThreshold / 2) {
                diversificationMode = false;
                diversificationFactor = 1.0; // Destrucción normal
                if (Constants.VERBOSE_LOGGING) {
                    System.out.println("Iteración " + iteration + ": Volviendo a modo INTENSIFICACIÓN");
                }
            }
            
            // Actualizar pesos cada segmentSize iteraciones
            if ((iteration + 1) % segmentSize == 0) {
                updateOperatorWeights();
                temperature *= coolingRate;
                
                // Reportar estado
                if (iteration % 100 == 0) {
                    System.out.println("Iteración " + iteration + 
                                     " | Mejor peso: " + bestWeight + 
                                     " | Temperatura: " + String.format("%.2f", temperature) +
                                     " | Modo: " + (diversificationMode ? "DIVERSIFICACIÓN" : "INTENSIFICACIÓN"));
                }
            }
            
            // Parada temprana inteligente
            if (stagnationCounter > 300) { // Más iteraciones antes de parar
                if (Constants.VERBOSE_LOGGING) {
                    System.out.println("Parada temprana en iteración " + iteration + 
                                     " (sin mejoras por " + stagnationCounter + " iteraciones)");
                }
                break;
            }
        }
        
        // Actualizar la solución final
        solution.clear();
        solution.putAll(bestSolution);
        
        // Siempre mostrar resumen final
        System.out.println("ALNS completado:");
        System.out.println("  Mejoras encontradas: " + improvements);
        System.out.println("  Peso final: " + bestWeight);
        if (Constants.VERBOSE_LOGGING) {
            System.out.println("  Temperatura final: " + temperature);
        }
    }
    
    /**
     * Selecciona operadores de destrucción y reparación basado en sus pesos
     */
    private int[] selectOperators() {
        try {
            if (Constants.VERBOSE_LOGGING) {
                System.out.println("    Seleccionando operadores...");
            }
            
            // Selección por ruleta basada en pesos
            double totalWeight = 0.0;
            for (int i = 0; i < operatorWeights.length; i++) {
                for (int j = 0; j < operatorWeights[i].length; j++) {
                    totalWeight += operatorWeights[i][j];
                }
            }
            
            if (Constants.VERBOSE_LOGGING) {
                System.out.println("    Peso total: " + totalWeight);
            }
            double randomValue = random.nextDouble() * totalWeight;
            double cumulativeWeight = 0.0;
            
            for (int i = 0; i < operatorWeights.length; i++) {
                for (int j = 0; j < operatorWeights[i].length; j++) {
                    cumulativeWeight += operatorWeights[i][j];
                    if (randomValue <= cumulativeWeight) {
                        if (Constants.VERBOSE_LOGGING) {
                            System.out.println("    Operadores seleccionados: " + i + ", " + j);
                        }
                        return new int[]{i, j};
                    }
                }
            }
            
            // Fallback: seleccionar el primero
            if (Constants.VERBOSE_LOGGING) {
                System.out.println("    Usando fallback: 0, 0");
            }
            return new int[]{0, 0};
        } catch (Exception e) {
            // Siempre mostrar errores críticos
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
            // Ajustar ratio de destrucción según modo de diversificación
            double adjustedRatio = Constants.DESTRUCTION_RATIO * diversificationFactor;
            int adjustedMin = (int)(Constants.DESTRUCTION_MIN_PACKAGES * diversificationFactor);
            int adjustedMax = (int)(Constants.DESTRUCTION_MAX_PACKAGES * diversificationFactor);
            
            switch (operatorIndex) {
                case 0: // Random Destroy
                    if (Constants.VERBOSE_LOGGING) {
                        System.out.println("    Ejecutando randomDestroy... (ratio: " + String.format("%.2f", adjustedRatio) + ")");
                    }
                    return destructionOperators.randomDestroy(solution, adjustedRatio, adjustedMin, adjustedMax);
                case 1: // Geographic Destroy
                    if (Constants.VERBOSE_LOGGING) {
                        System.out.println("    Ejecutando geographicDestroy... (ratio: " + String.format("%.2f", adjustedRatio) + ")");
                    }
                    return destructionOperators.geographicDestroy(solution, adjustedRatio, adjustedMin, adjustedMax);
                case 2: // Time Based Destroy
                    if (Constants.VERBOSE_LOGGING) {
                        System.out.println("    Ejecutando timeBasedDestroy... (ratio: " + String.format("%.2f", adjustedRatio) + ")");
                    }
                    return destructionOperators.timeBasedDestroy(solution, adjustedRatio, adjustedMin, adjustedMax);
                case 3: // Congested Route Destroy - OPTIMIZADO
                    if (Constants.VERBOSE_LOGGING) {
                        System.out.println("    Ejecutando congestedRouteDestroy... (ratio: " + String.format("%.2f", adjustedRatio) + ")");
                    }
                    return destructionOperators.congestedRouteDestroy(solution, adjustedRatio, adjustedMin, adjustedMax);
                default:
                    if (Constants.VERBOSE_LOGGING) {
                        System.out.println("    Ejecutando randomDestroy (default)... (ratio: " + String.format("%.2f", adjustedRatio) + ")");
                    }
                    return destructionOperators.randomDestroy(solution, adjustedRatio, adjustedMin, adjustedMax);
            }
        } catch (Exception e) {
            // Siempre mostrar errores críticos
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
    
    /**
     * CORRECCIÓN: Crear snapshot de capacidades de vuelos
     */
    private Map<Flight, Integer> snapshotCapacities() {
        Map<Flight, Integer> snapshot = new HashMap<>();
        for (Flight f : flights) {
            snapshot.put(f, f.getUsedCapacity());
        }
        return snapshot;
    }
    
    /**
     * CORRECCIÓN: Restaurar capacidades desde snapshot
     */
    private void restoreCapacities(Map<Flight, Integer> snapshot) {
        for (Flight f : flights) {
            f.setUsedCapacity(snapshot.getOrDefault(f, 0));
        }
    }
    
    /**
     * CORRECCIÓN: Reconstruir capacidades limpiamente desde una solución
     */
    private void rebuildCapacitiesFromSolution(HashMap<Package, ArrayList<Flight>> solution) {
        // Primero resetear todas las capacidades
        for (Flight f : flights) {
            f.setUsedCapacity(0);
        }
        
        // Luego reconstruir desde la solución
        for (Map.Entry<Package, ArrayList<Flight>> entry : solution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
            
            for (Flight f : route) {
                f.setUsedCapacity(f.getUsedCapacity() + productCount);
            }
        }
    }
    
    /**
     * PATCH: Snapshot/restore completo de almacenes para ALNS
     */
    private Map<Airport, Integer> snapshotWarehouses() {
        Map<Airport, Integer> snapshot = new HashMap<>();
        for (Airport airport : airports) {
            snapshot.put(airport, warehouseOccupancy.getOrDefault(airport, 0));
        }
        return snapshot;
    }
    
    /**
     * PATCH: Restaurar ocupación de almacenes desde snapshot
     */
    private void restoreWarehouses(Map<Airport, Integer> snapshot) {
        warehouseOccupancy.clear();
        warehouseOccupancy.putAll(snapshot);
    }
    
    /**
     * PATCH: Reconstruir almacenes limpiamente desde una solución
     */
    private void rebuildWarehousesFromSolution(HashMap<Package, ArrayList<Flight>> solution) {
        // Resetear todas las ocupaciones
        initializeWarehouseOccupancy();
        
        // Reconstruir desde la solución
        for (Map.Entry<Package, ArrayList<Flight>> entry : solution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
            
            if (route == null || route.isEmpty()) {
                // Paquete ya en destino final
                Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
                if (destinationAirport != null) {
                    incrementWarehouseOccupancy(destinationAirport, productCount);
                }
            } else {
                // Paquete en ruta - ocupa almacén de destino del último vuelo
                Flight lastFlight = route.get(route.size() - 1);
                incrementWarehouseOccupancy(lastFlight.getDestinationAirport(), productCount);
            }
        }
    }
    
    /**
     * PATCH: Helper para validar capacidad por cantidad de productos
     * @param route ruta de vuelos a validar
     * @param qty cantidad de productos que se quieren asignar
     * @return true si todos los vuelos de la ruta pueden acomodar qty productos adicionales
     */
    private boolean fitsCapacity(ArrayList<Flight> route, int qty) {
        if (route == null || route.isEmpty()) return true;
        
        for (Flight flight : route) {
            if (flight.getUsedCapacity() + qty > flight.getMaxCapacity()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * CHANGED: Cache robusta City→Airport por nombre de ciudad
     * Evita problemas de equals/hashCode con objetos City
     */
    private void initializeCityToAirportCache() {
        cityNameToAirportCache = new HashMap<>();
        for (Airport airport : airports) {
            if (airport.getCity() != null && airport.getCity().getName() != null) {
                String cityKey = airport.getCity().getName().toLowerCase().trim();
                cityNameToAirportCache.put(cityKey, airport);
            }
        }
        System.out.println("Cache inicializada: " + cityNameToAirportCache.size() + " ciudades");
    }
    
    /**
     * NEW: Inicializar T0 como mínimo orderDate o now si vacío
     */
    private void initializeT0() {
        T0 = LocalDateTime.now(); // Default fallback
        
        if (packages != null && !packages.isEmpty()) {
            LocalDateTime minOrderDate = packages.stream()
                .filter(pkg -> pkg.getOrderDate() != null)
                .map(Package::getOrderDate)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            T0 = minOrderDate;
        }
        
        System.out.println("T0 inicializado: " + T0);
    }
    
    /**
     * PATCH: Unitización - expandir paquetes a unidades de producto
     * 
     * Estrategia: cada paquete con N productos se convierte en N "package units"
     * independientes, cada uno con 1 producto, permitiendo que viajen en vuelos diferentes.
     * 
     * Para desactivar: cambiar ENABLE_PRODUCT_UNITIZATION = false
     * 
     * @param originalPackages lista de paquetes originales
     * @return lista de unidades de producto (1 producto = 1 package unit)
     */
    private ArrayList<Package> expandPackagesToProductUnits(ArrayList<Package> originalPackages) {
        ArrayList<Package> productUnits = new ArrayList<>();
        
        for (Package originalPkg : originalPackages) {
            int productCount = (originalPkg.getProducts() != null && !originalPkg.getProducts().isEmpty()) 
                             ? originalPkg.getProducts().size() : 1;
            
            // Crear una unidad por cada producto
            for (int i = 0; i < productCount; i++) {
                Package unit = createPackageUnit(originalPkg, i);
                productUnits.add(unit);
            }
        }
        
        return productUnits;
    }
    
    /**
     * PATCH: Crear una unidad de paquete (1 producto) a partir del paquete original
     * 
     * @param originalPkg paquete original
     * @param unitIndex índice de la unidad (0, 1, 2, ...)
     * @return nueva unidad de paquete con ID derivado y 1 producto
     */
    private Package createPackageUnit(Package originalPkg, int unitIndex) {
        Package unit = new Package();
        
        // PATCH: ID derivado usando hash para compatibilidad con int
        String unitIdString = originalPkg.getId() + "#" + unitIndex;
        unit.setId(unitIdString.hashCode());
        
        // Copiar todos los metadatos del paquete original
        unit.setCustomer(originalPkg.getCustomer());
        unit.setDestinationCity(originalPkg.getDestinationCity());
        unit.setOrderDate(originalPkg.getOrderDate());
        unit.setDeliveryDeadline(originalPkg.getDeliveryDeadline());
        unit.setStatus(originalPkg.getStatus());
        unit.setCurrentLocation(originalPkg.getCurrentLocation());
        unit.setPriority(originalPkg.getPriority());
        unit.setAssignedRoute(originalPkg.getAssignedRoute());
        
        // CRÍTICO: Crear lista con exactamente 1 producto
        ArrayList<Product> singleProduct = new ArrayList<>();
        if (originalPkg.getProducts() != null && unitIndex < originalPkg.getProducts().size()) {
            // Copiar el producto específico del paquete original
            Product originalProduct = originalPkg.getProducts().get(unitIndex);
            Product productCopy = new Product();
            productCopy.setId(originalProduct.getId());
            productCopy.setAssignedFlight(originalProduct.getAssignedFlight());
            productCopy.setStatus(originalProduct.getStatus());
            singleProduct.add(productCopy);
        } else {
            // Crear un producto genérico si no existe
            Product genericProduct = new Product();
            String productIdString = originalPkg.getId() + "_P" + unitIndex;
            genericProduct.setId(productIdString.hashCode());
            singleProduct.add(genericProduct);
        }
        
        unit.setProducts(singleProduct);
        
        return unit;
    }
    
    /**
     * CHANGED: getAirportByCity usando cache robusta por nombre
     * Eliminada dependencia de equals/hashCode de objetos City
     */
    private Airport getAirportByCity(City city) {
        if (city == null || city.getName() == null) return null;
        String cityKey = city.getName().toLowerCase().trim();
        return cityNameToAirportCache.get(cityKey);
    }
    
    /**
     * PATCH: Implementar findDirectRoute (método crítico)
     */
    private ArrayList<Flight> findDirectRoute(City origin, City destination) {
        Airport originAirport = getAirportByCity(origin);
        Airport destAirport = getAirportByCity(destination);
        
        if (originAirport == null || destAirport == null) return null;
        
        // Buscar vuelo directo entre aeropuertos
        for (Flight flight : flights) {
            if (flight.getOriginAirport().equals(originAirport) && 
                flight.getDestinationAirport().equals(destAirport) &&
                flight.getUsedCapacity() < flight.getMaxCapacity()) {
                ArrayList<Flight> route = new ArrayList<>();
                route.add(flight);
                return route;
            }
        }
        
        return null; // No hay vuelo directo
    }
    
    /**
     * PATCH: Implementar isRouteValid (método crítico)
     */
    private boolean isRouteValid(Package pkg, ArrayList<Flight> route) {
        if (pkg == null || route == null || route.isEmpty()) return false;
        
        // Cantidad de productos (qty)
        int qty = (pkg.getProducts() != null && !pkg.getProducts().isEmpty()) ? pkg.getProducts().size() : 1;
        
        // 1) Validar capacidad de todos los vuelos en la ruta para qty
        if (!fitsCapacity(route, qty)) return false;
        
        // 2) Origen correcto
        Airport expectedOrigin = getAirportByCity(pkg.getCurrentLocation());
        if (expectedOrigin == null || !route.get(0).getOriginAirport().equals(expectedOrigin)) return false;
        
        // 3) Continuidad de la ruta
        for (int i = 0; i < route.size() - 1; i++) {
            if (!route.get(i).getDestinationAirport().equals(route.get(i + 1).getOriginAirport())) {
                return false;
            }
        }
        
        // 4) Destino correcto y deadline respetado
        Airport expectedDestination = getAirportByCity(pkg.getDestinationCity());
        if (expectedDestination == null || 
            !route.get(route.size() - 1).getDestinationAirport().equals(expectedDestination)) {
            return false;
        }
        
        // 5) Deadline respetado
        return isDeadlineRespected(pkg, route);
    }
    
    /**
     * PATCH: Implementar canAssignWithSpaceOptimization (método crítico)
     */
    private boolean canAssignWithSpaceOptimization(Package pkg, ArrayList<Flight> route,
                                                 HashMap<Package, ArrayList<Flight>> currentSolution) {
        // Validación simplificada de capacidad de almacén final
        Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
        if (destinationAirport == null) return false;
        
        int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        int maxCapacity = destinationAirport.getWarehouse().getMaxCapacity();
        
        return (currentOccupancy + productCount) <= maxCapacity;
    }
    
    /**
     * PATCH: Implementar updateFlightCapacities (método crítico)
     */
    private void updateFlightCapacities(ArrayList<Flight> route, int productCount) {
        for (Flight flight : route) {
            flight.setUsedCapacity(flight.getUsedCapacity() + productCount);
        }
    }
    
    /**
     * PATCH: Implementar incrementWarehouseOccupancy (método crítico)
     */
    private void incrementWarehouseOccupancy(Airport airport, int productCount) {
        int currentOccupancy = warehouseOccupancy.getOrDefault(airport, 0);
        warehouseOccupancy.put(airport, currentOccupancy + productCount);
    }
    
    /**
     * NEW: getPackageStartTime corregido con ancla T0 y clamp
     */
    private int getPackageStartTime(Package pkg) {
        if (pkg == null || pkg.getOrderDate() == null || T0 == null) {
            return 0;
        }
        
        long minutesFromT0 = ChronoUnit.MINUTES.between(T0, pkg.getOrderDate());
        int offset = Math.floorMod(pkg.getId(), 60); // Offset por ID
        int startMinute = (int) (minutesFromT0 + offset);
        
        // Clamp a rango válido [0, TOTAL_MINUTES-1]
        final int TOTAL_MINUTES = HORIZON_DAYS * 24 * 60;
        return Math.max(0, Math.min(startMinute, TOTAL_MINUTES - 1));
    }
    
    /**
     * PATCH: Implementar findBestRouteWithTimeWindows (método crítico)
     */
    private ArrayList<Flight> findBestRouteWithTimeWindows(Package pkg, HashMap<Package, ArrayList<Flight>> currentSolution) {
        // Primero intentar con el método original
        ArrayList<Flight> originalRoute = findBestRoute(pkg);
        
        // Si no funciona, intentar con diferentes horarios de salida
        if (originalRoute == null || !canAssignWithSpaceOptimization(pkg, originalRoute, currentSolution)) {
            return findRouteWithDelayedDeparture(pkg, currentSolution);
        }
        
        return originalRoute;
    }
    
    /**
     * CHANGED: calculateRouteTimeMargin unificado sin doble conteo
     * Solo transportTime + 2h conexiones, margen vs orderDate-deadline
     */
    private double calculateRouteTimeMargin(Package pkg, ArrayList<Flight> route) {
        if (pkg == null || route == null) return 1.0;
        if (pkg.getOrderDate() == null || pkg.getDeliveryDeadline() == null) return 1.0;
        
        // Tiempo total de la ruta
        double totalTime = 0.0;
        for (Flight flight : route) {
            totalTime += flight.getTransportTime();
        }
        
        // Añadir 2 horas por conexión
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0;
        }
        
        // Margen disponible
        long availableHours = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        double margin = availableHours - totalTime;
        
        return Math.max(margin, 0.0) + 1.0; // +1 para evitar margen 0
    }
    
    public void generateInitialSolution() {
        // NEW: Usar flag de Constants para decidir tipo de solución inicial
        if (Constants.USE_GREEDY_INITIAL_SOLUTION) {
            generateGreedyInitialSolution();
        } else {
            generateRandomInitialSolution();
        }
    }
    
    /**
     * NEW: Generar solución inicial completamente aleatoria para probar ALNS
     */
    private void generateRandomInitialSolution() {
        System.out.println("=== GENERANDO SOLUCIÓN INICIAL ALEATORIA ===");
        System.out.println("Probabilidad de asignación: " + (Constants.RANDOM_ASSIGNMENT_PROBABILITY * 100) + "%");
        
        HashMap<Package, ArrayList<Flight>> currentSolution = new HashMap<>();
        int assignedPackages = 0;
        
        // Barajar paquetes para orden aleatorio
        ArrayList<Package> shuffledPackages = new ArrayList<>(packages);
        Collections.shuffle(shuffledPackages, random);
        
        for (Package pkg : shuffledPackages) {
            // Asignación aleatoria basada en probabilidad
            if (random.nextDouble() < Constants.RANDOM_ASSIGNMENT_PROBABILITY) {
                ArrayList<Flight> randomRoute = generateRandomRoute(pkg);
                
                if (randomRoute != null && !randomRoute.isEmpty()) {
                    int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
                    
                    // Validación básica de capacidad
                    if (fitsCapacity(randomRoute, productCount)) {
                        Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
                        if (destinationAirport != null && 
                            canAssignWithSpaceOptimization(pkg, randomRoute, currentSolution)) {
                            
                            currentSolution.put(pkg, randomRoute);
                            updateFlightCapacities(randomRoute, productCount);
                            incrementWarehouseOccupancy(destinationAirport, productCount);
                            assignedPackages++;
                        }
                    }
                }
            }
        }
        
        // Calcular el peso/costo de esta solución
        int solutionWeight = calculateSolutionWeight(currentSolution);
        
        // Almacenar la solución con su peso
        solution.put(currentSolution, solutionWeight);
        
        System.out.println("Random initial solution generated: " + assignedPackages + "/" + packages.size() + " packages assigned");
        System.out.println("Solution weight: " + solutionWeight);
    }
    
    /**
     * NEW: Generar una ruta completamente aleatoria para testing
     */
    private ArrayList<Flight> generateRandomRoute(Package pkg) {
        City origin = pkg.getCurrentLocation();
        City destination = pkg.getDestinationCity();
        
        if (origin == null || destination == null) return null;
        
        Airport originAirport = getAirportByCity(origin);
        Airport destAirport = getAirportByCity(destination);
        
        if (originAirport == null || destAirport == null) return null;
        
        // Intentar encontrar cualquier ruta válida (directo prioritario)
        ArrayList<Flight> directRoute = findDirectRoute(origin, destination);
        if (directRoute != null && !directRoute.isEmpty()) {
            return directRoute;
        }
        
        // Si no hay directo, intentar ruta con 1 escala aleatoria
        ArrayList<Airport> shuffledAirports = new ArrayList<>(airports);
        Collections.shuffle(shuffledAirports, random);
        
        for (int i = 0; i < Math.min(5, shuffledAirports.size()); i++) { // Máximo 5 intentos
            Airport intermediate = shuffledAirports.get(i);
            if (intermediate.equals(originAirport) || intermediate.equals(destAirport)) continue;
            
            ArrayList<Flight> leg1 = findDirectRoute(origin, intermediate.getCity());
            ArrayList<Flight> leg2 = findDirectRoute(intermediate.getCity(), destination);
            
            if (leg1 != null && leg2 != null && !leg1.isEmpty() && !leg2.isEmpty()) {
                ArrayList<Flight> route = new ArrayList<>();
                route.addAll(leg1);
                route.addAll(leg2);
                return route;
            }
        }
        
        return null; // No se pudo generar ruta
    }
    
    /**
     * RENAMED: Método greedy original (antes generateInitialSolution)
     */
    private void generateGreedyInitialSolution() {
        System.out.println("=== GENERANDO SOLUCIÓN INICIAL GREEDY ===");
        
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
    
    
    
    private boolean isDeadlineRespected(Package pkg, ArrayList<Flight> route) {
        if (pkg == null || pkg.getOrderDate() == null || pkg.getDeliveryDeadline() == null) {
            return false;
        }
        double totalTime = 0;
        
        // CORRECCIÓN: Solo usar transportTime de vuelos (que ya respeta la política PACK)
        for (Flight flight : route) {
            totalTime += flight.getTransportTime();
        }
        
        // Añadir penalización por conexiones (2 horas por conexión)
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0;
        }
        
        // CORRECCIÓN: Eliminar doble conteo - transportTime ya incluye política de continentes
        // No agregar tiempo adicional por continente porque ya está en flight.getTransportTime()
        
        // CORRECCIÓN: Usar validación robusta de promesas MoraPack
        if (!validateMoraPackDeliveryPromise(pkg, totalTime)) {
            return false; // Excede promesas MoraPack
        }
        
        // Factor de seguridad aleatorio (1-10%) para asegurar entregas a tiempo
        // Más margen de seguridad para rutas complejas o intercontinentales
        double safetyMargin = 0.0;
        if (random != null) { // Verificar que random esté inicializado
            // CORRECCIÓN: Recalcular sameContinentRoute para el factor de complejidad
            City origin = pkg.getCurrentLocation();
            City destination = pkg.getDestinationCity();
            boolean sameContinentRoute = (origin != null && destination != null) && 
                                        origin.getContinent() == destination.getContinent();
            
            int complexityFactor = route.size() + (sameContinentRoute ? 0 : 2);
            safetyMargin = 0.01 * (1 + random.nextInt(complexityFactor * 3));
            totalTime = totalTime * (1.0 + safetyMargin); // Aumentar tiempo estimado para ser conservadores
        }
        
        // CORRECCIÓN: Calcular tiempo límite desde orderDate, no desde "now"
        long hoursUntilDeadline = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        
        return totalTime <= hoursUntilDeadline;
    }
    
    /**
     * CORRECCIÓN: Validación explícita y robusta de promesas de entrega MoraPack
     * 
     * Promesas MoraPack:
     * - Mismo continente: máximo 2 días (48 horas)
     * - Diferentes continentes: máximo 3 días (72 horas)
     * 
     * También verifica que el tiempo estimado no exceda el deadline del cliente
     */
    private boolean validateMoraPackDeliveryPromise(Package pkg, double totalTimeHours) {
        // 1. Verificar promesa MoraPack según continentes
        City origin = pkg.getCurrentLocation();
        City destination = pkg.getDestinationCity();
        
        if (origin == null || destination == null) {
            System.err.println("Error: origen o destino nulo para paquete " + pkg.getId());
            return false;
        }
        
        boolean sameContinentRoute = origin.getContinent() == destination.getContinent();
        long moraPackPromiseHours = sameContinentRoute ? 48 : 72; // 2 días intra / 3 días inter
        
        // Verificar promesa MoraPack
        if (totalTimeHours > moraPackPromiseHours) {
            if (DEBUG_MODE) {
                System.out.println("VIOLACIÓN PROMESA MORAPACK - Paquete " + pkg.getId() + 
                    ": " + totalTimeHours + "h > " + moraPackPromiseHours + "h (" + 
                    (sameContinentRoute ? "mismo continente" : "diferentes continentes") + ")");
            }
            return false;
        }
        
        // 2. Verificar deadline específico del cliente
        long hoursUntilDeadline = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        
        if (totalTimeHours > hoursUntilDeadline) {
            if (DEBUG_MODE) {
                System.out.println("VIOLACIÓN DEADLINE CLIENTE - Paquete " + pkg.getId() + 
                    ": " + totalTimeHours + "h > " + hoursUntilDeadline + "h disponibles");
            }
            return false;
        }
        
        // 3. Verificar que el origen sea efectivamente una sede MoraPack
        if (!isMoraPackHeadquarters(origin)) {
            if (DEBUG_MODE) {
                System.out.println("ADVERTENCIA - Paquete " + pkg.getId() + 
                    " no origina desde sede MoraPack: " + origin.getName());
            }
            // No bloquear, pero advertir
        }
        
        return true; // Cumple todas las promesas
    }
    
    /**
     * CORRECCIÓN: Verificar si una ciudad es sede principal de MoraPack
     */
    private boolean isMoraPackHeadquarters(City city) {
        if (city == null || city.getName() == null) return false;
        
        String cityName = city.getName().toLowerCase();
        return cityName.contains("lima") || 
               cityName.contains("bruselas") || cityName.contains("brussels") ||
               cityName.contains("baku");
    }
    
    private static final boolean DEBUG_MODE = false; // Cambiar a true para debug detallado
    
    
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
        
        // Fórmula de peso optimizada para MoraPack
        double avgDeliveryTime = totalPackages > 0 ? totalDeliveryTime / totalPackages : 0;
        double avgCapacityUtilization = totalFlightsUsed > 0 ? totalCapacityUtilization / totalFlightsUsed : 0;
        double onTimeRate = totalPackages > 0 ? (double) onTimeDeliveries / totalPackages : 0;
        double avgDeliveryMargin = onTimeDeliveries > 0 ? totalDeliveryMargin / onTimeDeliveries : 0;
        
        // Componentes específicos de MoraPack
        double continentalEfficiency = calculateContinentalEfficiency(solutionMap);
        double warehouseUtilization = calculateWarehouseUtilization();
        
        // Peso final REBALANCEADO - PRIORIDAD: MÁS PAQUETES ASIGNADOS
        int weight = (int) (
            // PRIORIDAD ABSOLUTA: Cantidad de paquetes y productos (MAXIMIZAR)
            totalPackages * 100000 +                // 100,000 puntos por paquete (DOMINANTE)
            totalProducts * 10000 +                 // 10,000 puntos por producto (MUY ALTO)
            
            // FACTOR CALIDAD: On-time como multiplicador, no aditivo
            onTimeRate * 5000 +                     // 5,000 puntos máximo por calidad on-time
            
            // EFICIENCIA OPERATIVA (secundaria)
            Math.min(avgDeliveryMargin * 50, 1000) + // Margen de seguridad reducido
            continentalEfficiency * 500 +           // Eficiencia continental
            avgCapacityUtilization * 200 +          // Utilización de vuelos
            warehouseUtilization * 100 +            // Utilización de almacenes
            
            // PENALIZACIONES MENORES
            - avgDeliveryTime * 20 -                // Penalización tiempo reducida
            - calculateRoutingComplexity(solutionMap) * 50 // Penalización complejidad reducida
        );
        
        // NUEVA PENALIZACIÓN: Solo si on-time rate es muy bajo (< 80%)
        if (onTimeRate < 0.8) {
            weight = (int)(weight * 0.5); // Penalización 50% solo si muy malo
        }
        
        // BONUS MODERADO por alta calidad (≥95% on-time)
        if (onTimeRate >= 0.95 && totalPackages > 10) {
            weight = (int)(weight * 1.1); // 10% bonus moderado por alta calidad
        }
        
        // BONUS EXTRA por volumen alto (>1000 paquetes asignados)
        if (totalPackages > 1000) {
            weight = (int)(weight * 1.15); // 15% bonus por alto volumen
        }
        
        return weight;
    }
    
    /**
     * Calcula la eficiencia de rutas continentales vs intercontinentales
     * Premia rutas directas y penaliza complejidad innecesaria
     */
    private double calculateContinentalEfficiency(HashMap<Package, ArrayList<Flight>> solutionMap) {
        if (solutionMap.isEmpty()) return 0.0;
        
        int sameContinentDirect = 0;
        int sameContinentOneStop = 0;
        int differentContinentDirect = 0;
        int differentContinentOneStop = 0;
        int inefficientRoutes = 0;
        
        for (Map.Entry<Package, ArrayList<Flight>> entry : solutionMap.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == 
                                        pkg.getDestinationCity().getContinent();
            
            if (route.isEmpty()) continue; // Ya en destino
            
            if (sameContinentRoute) {
                if (route.size() == 1) sameContinentDirect++;
                else if (route.size() == 2) sameContinentOneStop++;
                else inefficientRoutes++;
            } else {
                if (route.size() == 1) differentContinentDirect++;
                else if (route.size() <= 2) differentContinentOneStop++;
                else inefficientRoutes++;
            }
        }
        
        // Puntuación basada en eficiencia esperada para MoraPack
        double efficiency = sameContinentDirect * 1.0 +        // Ideal para mismo continente
                           sameContinentOneStop * 0.8 +         // Aceptable para mismo continente
                           differentContinentDirect * 1.2 +     // Excelente para diferentes continentes
                           differentContinentOneStop * 1.0 +    // Bueno para diferentes continentes
                           inefficientRoutes * (-0.5);         // Penalizar rutas ineficientes
        
        return efficiency;
    }
    
    /**
     * Calcula la utilización promedio de almacenes
     */
    private double calculateWarehouseUtilization() {
        if (warehouseOccupancy.isEmpty()) return 0.0;
        
        double totalUtilization = 0.0;
        int validWarehouses = 0;
        
        for (Map.Entry<Airport, Integer> entry : warehouseOccupancy.entrySet()) {
            Airport airport = entry.getKey();
            int occupancy = entry.getValue();
            
            if (airport.getWarehouse() != null && airport.getWarehouse().getMaxCapacity() > 0) {
                double utilization = (double) occupancy / airport.getWarehouse().getMaxCapacity();
                totalUtilization += utilization;
                validWarehouses++;
            }
        }
        
        return validWarehouses > 0 ? totalUtilization / validWarehouses : 0.0;
    }
    
    /**
     * Calcula la complejidad de enrutamiento - penaliza rutas excesivamente complejas
     */
    private double calculateRoutingComplexity(HashMap<Package, ArrayList<Flight>> solutionMap) {
        if (solutionMap.isEmpty()) return 0.0;
        
        double totalComplexity = 0.0;
        
        for (Map.Entry<Package, ArrayList<Flight>> entry : solutionMap.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            if (route.isEmpty()) continue;
            
            // Penalizar rutas con más escalas de las necesarias
            boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == 
                                        pkg.getDestinationCity().getContinent();
            
            int expectedMaxStops = sameContinentRoute ? 1 : 2; // 1 para mismo continente, 2 para diferentes
            
            if (route.size() > expectedMaxStops) {
                totalComplexity += (route.size() - expectedMaxStops) * 2.0; // Penalización por escala extra
            }
            
            // Penalizar vuelos con baja utilización en rutas largas
            if (route.size() > 1) {
                for (Flight flight : route) {
                    double utilization = (double) flight.getUsedCapacity() / flight.getMaxCapacity();
                    if (utilization < 0.3) { // Vuelos con menos del 30% de utilización
                        totalComplexity += 1.0;
                    }
                }
            }
        }
        
        return totalComplexity;
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
        if (solution.isEmpty()) return false;

        // Tomamos la solución actual
        HashMap<Package, ArrayList<Flight>> currentSolution = solution.keySet().iterator().next();

        // Uso de capacidad por vuelo en términos de "productos"
        Map<Flight, Integer> flightUsage = new HashMap<>();

        for (Map.Entry<Package, ArrayList<Flight>> entry : currentSolution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();

            // Contar productos del paquete (mínimo 1)
            int products = (pkg.getProducts() != null && !pkg.getProducts().isEmpty())
                    ? pkg.getProducts().size()
                    : 1;

            // Sumar esos productos a cada vuelo de la ruta
            for (Flight f : route) {
                flightUsage.merge(f, products, Integer::sum);
            }
        }

        // Verificar que ningún vuelo exceda su capacidad máxima
        for (Map.Entry<Flight, Integer> e : flightUsage.entrySet()) {
            Flight f = e.getKey();
            int used = e.getValue(); // productos cargados en ese vuelo según la solución
            if (used > f.getMaxCapacity()) {
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
    /**
     * CORRECCIÓN: Horizonte temporal extendido a 4 días (cubre 3 días de promesas + holgura)
     */
    private static final int HORIZON_DAYS = 4;
    
    private void initializeTemporalWarehouseOccupancy() {
        final int TOTAL_MINUTES = HORIZON_DAYS * 24 * 60; // 5760 minutos (4 días)
        for (Airport airport : airports) {
            temporalWarehouseOccupancy.put(airport, new int[TOTAL_MINUTES]);
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
            int startMinute = getPackageStartTime(pkg);
            return addTemporalOccupancy(destinationAirport, startMinute, Constants.CUSTOMER_PICKUP_MAX_HOURS * 60, productCount); // 2 horas para pickup
        }
        
        int currentMinute = getPackageStartTime(pkg); // Momento cuando el paquete inicia su viaje
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
        
        // CORRECCIÓN: Verificar y agregar ocupación para cada minuto del período (4 días)
        final int TOTAL_MINUTES = HORIZON_DAYS * 24 * 60;
        int clampedStart = Math.max(0, Math.min(startMinute, TOTAL_MINUTES - 1));
        int clampedEnd = Math.max(0, Math.min(startMinute + durationMinutes, TOTAL_MINUTES));
        for (int minute = clampedStart; minute < clampedEnd; minute++) {
            occupancyArray[minute] += productCount;
            if (occupancyArray[minute] > maxCapacity) {
                return false; // Violación de capacidad
            }
        }
        
        return true;
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
        
        final int TOTAL_MINUTES = HORIZON_DAYS * 24 * 60;
        for (int minute = 0; minute < TOTAL_MINUTES; minute++) {
            if (occupancyArray[minute] > maxOccupancy) {
                maxOccupancy = occupancyArray[minute];
                peakMinute = minute;
            }
        }
        
        return new int[]{peakMinute, maxOccupancy};
    }
}
