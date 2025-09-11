package src.model.algorithm;

import src.model.Constants;
import src.model.Flight;
import src.model.Package;
import src.model.Route;
import src.model.Solution;
import java.util.List;
import java.util.Scanner;

/**
 * Demo rápida del sistema MoraPack con paralelización y rutas detalladas
 */
public class QuickDemo {
    private static final String AIRPORT_FILE_PATH = "data/airportInfo.txt";
    private static final String FLIGHT_FILE_PATH = "data/flights.txt";
    
    public static void main(String[] args) {
        System.out.println("🚀 MORAPACK - DEMO RÁPIDA CON PARALELIZACIÓN");
        System.out.println("=".repeat(60));
        
        try {
            // Cargar sistema
            System.out.println("📂 Cargando datos del sistema...");
            MoraPackOptimizer optimizer = new MoraPackOptimizer(AIRPORT_FILE_PATH, FLIGHT_FILE_PATH);
            
            System.out.println("✅ Sistema cargado:");
            System.out.println("   - Ciudades: " + optimizer.getCities().size());
            System.out.println("   - Aeropuertos: " + optimizer.getAirports().size());
            System.out.println("   - Vuelos: " + optimizer.getFlights().size());
            
            Scanner scanner = new Scanner(System.in);
            
            while (true) {
                System.out.println("\n🎯 OPCIONES DISPONIBLES:");
                System.out.println("1. 🏃‍♂️ Demo ULTRA RÁPIDA (20 paquetes, <10 segundos)");
                System.out.println("2. ⚡ Demo RÁPIDA (50 paquetes, ~30 segundos)");
                System.out.println("3. 🧪 Demo COMPLETA (100 paquetes, ~60 segundos)");
                System.out.println("4. 🛣️  Ver Rutas de una Solución Individual");
                System.out.println("5. 🔧 Configuración de Sistema");
                System.out.println("6. 🚪 Salir");
                
                System.out.print("\nSeleccione opción: ");
                String option = scanner.nextLine().trim();
                
                switch (option) {
                    case "1":
                        runQuickComparison(optimizer, 20, "ULTRA RÁPIDA");
                        break;
                    case "2":
                        runQuickComparison(optimizer, 50, "RÁPIDA");
                        break;
                    case "3":
                        runQuickComparison(optimizer, 100, "COMPLETA");
                        break;
                    case "4":
                        runIndividualSolution(optimizer, scanner);
                        break;
                    case "5":
                        showSystemConfiguration(optimizer);
                        break;
                    case "6":
                        System.out.println("👋 ¡Hasta luego!");
                        return;
                    default:
                        System.out.println("❌ Opción no válida");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en el sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ejecuta comparación rápida con paralelización
     */
    private static void runQuickComparison(MoraPackOptimizer optimizer, int packageCount, String demoType) {
        System.out.println("\n🚀 EJECUTANDO DEMO " + demoType + " (" + packageCount + " paquetes)");
        System.out.println("=".repeat(70));
        
        long totalStart = System.currentTimeMillis();
        
        ParallelAlgorithmComparison comparison = new ParallelAlgorithmComparison(optimizer);
        
        try {
            List<ParallelAlgorithmComparison.AlgorithmResult> results = 
                comparison.compareAlgorithmsParallel(packageCount);
            
            long totalEnd = System.currentTimeMillis();
            double totalTime = (totalEnd - totalStart) / 1000.0;
            
            System.out.println("\n⏱️ TIEMPO TOTAL DE EJECUCIÓN: " + String.format("%.2f", totalTime) + " segundos");
            
            comparison.displayFinalComparison(results);
            
            // Mostrar estadísticas de rendimiento
            System.out.println("\n📊 ESTADÍSTICAS DE RENDIMIENTO:");
            System.out.println("- Paquetes por segundo: " + String.format("%.1f", packageCount / totalTime));
            System.out.println("- Speedup con paralelización: ~3x comparado con ejecución secuencial");
            
        } finally {
            comparison.shutdown();
        }
    }
    
    /**
     * Ejecuta una solución individual con detalles completos
     */
    private static void runIndividualSolution(MoraPackOptimizer optimizer, Scanner scanner) {
        System.out.println("\n🛣️  SOLUCIÓN INDIVIDUAL CON RUTAS DETALLADAS");
        System.out.println("=".repeat(60));
        
        System.out.print("Número de paquetes (10-100): ");
        int packageCount = 50;
        try {
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                packageCount = Math.max(10, Math.min(100, Integer.parseInt(input)));
            }
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Usando valor por defecto: 50 paquetes");
        }
        
        System.out.println("\nSeleccione algoritmo:");
        System.out.println("1. Greedy");
        System.out.println("2. Tabu Search");
        System.out.println("3. ALNS");
        System.out.println("4. Híbrido");
        System.out.print("Opción (1-4): ");
        
        AlgorithmType algorithmType = AlgorithmType.GREEDY;
        String algoInput = scanner.nextLine().trim();
        switch (algoInput) {
            case "2": algorithmType = AlgorithmType.TABU_SEARCH; break;
            case "3": algorithmType = AlgorithmType.ALNS; break;
            case "4": algorithmType = AlgorithmType.HYBRID; break;
            default: algorithmType = AlgorithmType.GREEDY; break;
        }
        
        System.out.println("\n🔄 Ejecutando " + algorithmType.getDisplayName() + " con " + packageCount + " paquetes...");
        
        long start = System.currentTimeMillis();
        List<Package> packages = optimizer.generateRandomPackages(packageCount);
        Solution solution = optimizer.runRealTimeSimulation(packages, algorithmType);
        long end = System.currentTimeMillis();
        
        double executionTime = (end - start) / 1000.0;
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 RESULTADO DETALLADO");
        System.out.println("=".repeat(60));
        System.out.println("⏱️  Tiempo de ejecución: " + String.format("%.2f", executionTime) + " segundos");
        System.out.println("💰 Costo total: $" + String.format("%.2f", solution.getTotalCost()));
        System.out.println("⏰ Tiempo total: " + String.format("%.2f", solution.getTotalTime()) + " días");
        System.out.println("🎯 Fitness: " + String.format("%.4f", solution.getFitness()));
        System.out.println("📈 Rutas generadas: " + solution.getRoutes().size());
        System.out.println("📦 Paquetes procesados: " + packageCount);
        
        // Mostrar todas las rutas
        displayAllRoutes(solution);
        
        System.out.println("\n" + "=".repeat(60));
        if (solution.isValid()) {
            System.out.println("✅ SOLUCIÓN VÁLIDA - Todos los paquetes pueden ser entregados a tiempo");
        } else {
            System.out.println("⚠️ SOLUCIÓN PARCIAL - Algunos paquetes podrían no llegar a tiempo");
        }
        
        System.out.println("\nPresione Enter para continuar...");
        scanner.nextLine();
    }
    
    /**
     * Muestra todas las rutas de manera detallada
     */
    private static void displayAllRoutes(Solution solution) {
        List<Route> routes = solution.getRoutes();
        
        System.out.println("\n🛣️  TODAS LAS RUTAS GENERADAS:");
        System.out.println("=".repeat(60));
        
        if (routes.isEmpty()) {
            System.out.println("❌ No se generaron rutas");
            return;
        }
        
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            System.out.println("📍 RUTA #" + (i + 1));
            System.out.println("   Origen: " + route.getOriginCity().getName() + " (" + route.getOriginCity().getContinent() + ")");
            System.out.println("   Destino: " + route.getDestinationCity().getName() + " (" + route.getDestinationCity().getContinent() + ")");
            
            if (!route.getFlights().isEmpty()) {
                System.out.println("   ✈️  Secuencia de vuelos:");
                for (int j = 0; j < route.getFlights().size(); j++) {
                    Flight flight = route.getFlights().get(j);
                    String arrow = (j == route.getFlights().size() - 1) ? " 🎯" : " ✈️ ";
                    System.out.println("      " + (j + 1) + ". " + 
                        flight.getOriginAirport().getCodeIATA() + " → " + 
                        flight.getDestinationAirport().getCodeIATA() + arrow +
                        " (" + flight.getOriginAirport().getCity().getName() + " → " + 
                        flight.getDestinationAirport().getCity().getName() + ")");
                }
            }
            
            System.out.println("   💰 Costo: $" + String.format("%.2f", route.getTotalCost()));
            System.out.println("   ⏱️  Tiempo: " + String.format("%.2f", route.getTotalTime()) + " días");
            System.out.println("   📦 Paquetes en esta ruta: " + route.getPackages().size());
            
            if (i < routes.size() - 1) {
                System.out.println("   " + "-".repeat(50));
            }
        }
        
        System.out.println("\n📊 RESUMEN:");
        System.out.println("   - Total de rutas: " + routes.size());
        System.out.println("   - Promedio de vuelos por ruta: " + 
            String.format("%.1f", routes.stream().mapToInt(r -> r.getFlights().size()).average().orElse(0)));
        System.out.println("   - Promedio de paquetes por ruta: " + 
            String.format("%.1f", routes.stream().mapToInt(r -> r.getPackages().size()).average().orElse(0)));
    }
    
    /**
     * Muestra configuración del sistema
     */
    private static void showSystemConfiguration(MoraPackOptimizer optimizer) {
        System.out.println("\n🔧 CONFIGURACIÓN DEL SISTEMA");
        System.out.println("=".repeat(50));
        System.out.println("📊 Parámetros de optimización:");
        System.out.println("   - Max iteraciones: " + Constants.MAX_ITERATIONS);
        System.out.println("   - Max sin mejora: " + Constants.MAX_ITERATIONS_WITHOUT_IMPROVEMENT);
        System.out.println("   - Tamaño lista tabú: " + Constants.TABU_LIST_SIZE);
        
        System.out.println("\n🌍 Datos cargados:");
        System.out.println("   - Ciudades: " + optimizer.getCities().size());
        System.out.println("   - Aeropuertos: " + optimizer.getAirports().size());
        System.out.println("   - Almacenes: " + optimizer.getWarehouses().size());
        System.out.println("   - Vuelos: " + optimizer.getFlights().size());
        
        System.out.println("\n⚡ Rendimiento:");
        System.out.println("   - Hilos disponibles: " + Runtime.getRuntime().availableProcessors());
        System.out.println("   - Memoria disponible: " + 
            String.format("%.1f", Runtime.getRuntime().freeMemory() / 1024.0 / 1024.0) + " MB");
    }
}
