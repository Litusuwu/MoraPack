package src.model.algorithm;

import src.model.Flight;
import src.model.Package;
import src.model.Route;
import src.model.Solution;
import java.util.*;
import java.util.concurrent.*;

/**
 * Comparación paralela de algoritmos metaheurísticos usando múltiples hilos
 */
public class ParallelAlgorithmComparison {
    private final MoraPackOptimizer optimizer;
    private final ExecutorService executorService;
    
    public ParallelAlgorithmComparison(MoraPackOptimizer optimizer) {
        this.optimizer = optimizer;
        // Usar el número de procesadores disponibles
        int numThreads = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(numThreads);
        System.out.println("🚀 Usando " + numThreads + " hilos para paralelización");
    }
    
    /**
     * Ejecuta comparación paralela de algoritmos
     */
    public List<AlgorithmResult> compareAlgorithmsParallel(int packageCount) {
        List<Package> packages = optimizer.generateRandomPackages(packageCount);
        List<Future<AlgorithmResult>> futures = new ArrayList<>();
        List<AlgorithmResult> results = Collections.synchronizedList(new ArrayList<>());
        
        System.out.println("⚡ Ejecutando algoritmos en paralelo...\n");
        
        // Crear tareas para cada algoritmo
        futures.add(executorService.submit(() -> runAlgorithm("Greedy", AlgorithmType.GREEDY, packages)));
        futures.add(executorService.submit(() -> runAlgorithm("Tabu Search", AlgorithmType.TABU_SEARCH, packages)));
        futures.add(executorService.submit(() -> runAlgorithm("ALNS", AlgorithmType.ALNS, packages)));
        futures.add(executorService.submit(() -> runAlgorithm("Híbrido", AlgorithmType.HYBRID, packages)));
        
        // Recoger resultados
        for (Future<AlgorithmResult> future : futures) {
            try {
                AlgorithmResult result = future.get(30, TimeUnit.SECONDS); // Timeout de 30 segundos
                results.add(result);
                displayAlgorithmResult(result);
            } catch (TimeoutException e) {
                System.err.println("⚠️ Algoritmo excedió tiempo límite de 30 segundos");
            } catch (Exception e) {
                System.err.println("❌ Error ejecutando algoritmo: " + e.getMessage());
            }
        }
        
        return results;
    }
    
    /**
     * Ejecuta un algoritmo específico
     */
    private AlgorithmResult runAlgorithm(String name, AlgorithmType type, List<Package> packages) {
        long startTime = System.currentTimeMillis();
        
        try {
            System.out.println("🔄 Iniciando " + name + "...");
            
            // Crear una copia de los paquetes para cada algoritmo
            List<Package> packageCopy = new ArrayList<>(packages);
            
            Solution solution = optimizer.runRealTimeSimulation(packageCopy, type);
            
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            return new AlgorithmResult(name, type, solution, executionTime, packageCopy.size());
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            System.err.println("❌ Error en " + name + ": " + e.getMessage());
            return new AlgorithmResult(name, type, null, executionTime, packages.size());
        }
    }
    
    /**
     * Muestra el resultado de un algoritmo de forma detallada
     */
    private void displayAlgorithmResult(AlgorithmResult result) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 RESULTADO: " + result.algorithmName);
        System.out.println("=".repeat(60));
        System.out.println("⏱️  Tiempo de ejecución: " + String.format("%.2f", result.executionTime) + " segundos");
        System.out.println("📦 Paquetes procesados: " + result.packageCount);
        
        if (result.solution != null) {
            System.out.println("💰 Costo total: $" + String.format("%.2f", result.solution.getTotalCost()));
            System.out.println("⏰ Tiempo total: " + String.format("%.2f", result.solution.getTotalTime()) + " días");
            System.out.println("🎯 Fitness: " + String.format("%.4f", result.solution.getFitness()));
            System.out.println("📈 Rutas generadas: " + result.solution.getRoutes().size());
            
            // Mostrar detalles de rutas
            displayRouteDetails(result.solution);
            
            if (result.solution.isValid()) {
                System.out.println("✅ Solución VÁLIDA");
            } else {
                System.out.println("❌ Solución INVÁLIDA");
            }
        } else {
            System.out.println("❌ No se pudo generar solución");
        }
        
        System.out.println("=".repeat(60) + "\n");
    }
    
    /**
     * Muestra detalles de las rutas generadas
     */
    private void displayRouteDetails(Solution solution) {
        System.out.println("\n🛣️  DETALLES DE RUTAS:");
        System.out.println("-".repeat(50));
        
        List<Route> routes = solution.getRoutes();
        int routeNumber = 1;
        
        for (Route route : routes) {
            System.out.println("Ruta #" + routeNumber + ":");
            System.out.println("  📍 Origen: " + route.getOriginCity().getName() + " (" + route.getOriginCity().getContinent() + ")");
            System.out.println("  🎯 Destino: " + route.getDestinationCity().getName() + " (" + route.getDestinationCity().getContinent() + ")");
            
            if (!route.getFlights().isEmpty()) {
                System.out.println("  ✈️  Vuelos:");
                int flightNum = 1;
                for (Flight flight : route.getFlights()) {
                    System.out.println("    " + flightNum + ". " + 
                        flight.getOriginAirport().getCodeIATA() + " → " + 
                        flight.getDestinationAirport().getCodeIATA() + 
                        " (" + flight.getOriginAirport().getCity().getName() + " → " + 
                        flight.getDestinationAirport().getCity().getName() + ")");
                    flightNum++;
                }
            }
            
            System.out.println("  💰 Costo: $" + String.format("%.2f", route.getTotalCost()));
            System.out.println("  ⏱️  Tiempo: " + String.format("%.2f", route.getTotalTime()) + " días");
            System.out.println("  📦 Paquetes: " + route.getPackages().size());
            
            routeNumber++;
            if (routeNumber > 5) { // Limitar a mostrar solo las primeras 5 rutas
                int remaining = routes.size() - 5;
                if (remaining > 0) {
                    System.out.println("  ... y " + remaining + " rutas más");
                }
                break;
            }
        }
        System.out.println("-".repeat(50));
    }
    
    /**
     * Muestra comparación final de resultados
     */
    public void displayFinalComparison(List<AlgorithmResult> results) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🏆 COMPARACIÓN FINAL DE ALGORITMOS");
        System.out.println("=".repeat(80));
        
        // Ordenar por fitness (descendente - mejor primero)
        results.sort((a, b) -> {
            if (a.solution == null && b.solution == null) return 0;
            if (a.solution == null) return 1;
            if (b.solution == null) return -1;
            return Double.compare(b.solution.getFitness(), a.solution.getFitness());
        });
        
        System.out.printf("%-20s %-12s %-12s %-12s %-12s %-10s%n", 
                "Algoritmo", "Tiempo (s)", "Fitness", "Costo ($)", "Tiempo (días)", "Estado");
        System.out.println("-".repeat(80));
        
        for (AlgorithmResult result : results) {
            String fitness = result.solution != null ? String.format("%.4f", result.solution.getFitness()) : "N/A";
            String cost = result.solution != null ? String.format("%.2f", result.solution.getTotalCost()) : "N/A";
            String time = result.solution != null ? String.format("%.2f", result.solution.getTotalTime()) : "N/A";
            String status = result.solution != null && result.solution.isValid() ? "✅ Válido" : "❌ Error";
            
            System.out.printf("%-20s %-12.2f %-12s %-12s %-12s %-10s%n",
                    result.algorithmName, result.executionTime, fitness, cost, time, status);
        }
        
        System.out.println("=".repeat(80));
        
        // Mostrar ganador
        if (!results.isEmpty() && results.get(0).solution != null) {
            System.out.println("🥇 GANADOR: " + results.get(0).algorithmName + 
                    " (Fitness: " + String.format("%.4f", results.get(0).solution.getFitness()) + ")");
        }
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    
    /**
     * Clase para almacenar resultados de algoritmos
     */
    public static class AlgorithmResult {
        public final String algorithmName;
        public final AlgorithmType algorithmType;
        public final Solution solution;
        public final double executionTime;
        public final int packageCount;
        
        public AlgorithmResult(String algorithmName, AlgorithmType algorithmType, 
                             Solution solution, double executionTime, int packageCount) {
            this.algorithmName = algorithmName;
            this.algorithmType = algorithmType;
            this.solution = solution;
            this.executionTime = executionTime;
            this.packageCount = packageCount;
        }
    }
}
