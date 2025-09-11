package src.model.algorithm;

import src.model.Customer;
import src.model.Package;
import src.model.Route;
import src.model.Solution;
import java.util.*;

/**
 * Demostración y comparación de algoritmos metaheurísticos para MoraPack
 * Compara Tabu Search, GRASP y el enfoque híbrido
 */
public class AlgorithmComparisonDemo {
    private static final String AIRPORT_FILE_PATH = "data/airportInfo.txt";
    private static final String FLIGHT_FILE_PATH = "data/flights.txt";
    
    public static void main(String[] args) {
        System.out.println("=== COMPARACIÓN DE ALGORITMOS METAHEURÍSTICOS MORAPACK ===\n");
        
        Scanner scanner = new Scanner(System.in);
        
        try {
            // Crear el optimizador con datos reales
            System.out.println("Cargando datos reales desde archivos...");
            MoraPackOptimizer optimizer = new MoraPackOptimizer(AIRPORT_FILE_PATH, FLIGHT_FILE_PATH);
            
            System.out.println("\n=== DATOS CARGADOS EXITOSAMENTE ===");
            printSystemInfo(optimizer);
            
            boolean exit = false;
            while (!exit) {
                printMenu();
                System.out.print("Seleccione una opción: ");
                
                try {
                    int choice = Integer.parseInt(scanner.nextLine());
                    
                    switch (choice) {
                        case 1:
                            runSingleAlgorithmTest(optimizer, scanner);
                            break;
                        case 2:
                            runAlgorithmComparison(optimizer, scanner);
                            break;
                        case 3:
                            runWeeklyComparison(optimizer, scanner);
                            break;
                        case 4:
                            runParameterTuning(optimizer, scanner);
                            break;
                        case 5:
                            printSystemInfo(optimizer);
                            break;
                        case 6:
                            exit = true;
                            System.out.println("¡Gracias por usar el sistema de comparación de algoritmos!");
                            break;
                        default:
                            System.out.println("Opción no válida. Intente nuevamente.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Por favor ingrese un número válido.");
                }
                
                if (!exit) {
                    System.out.println("\nPresione Enter para continuar...");
                    scanner.nextLine();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error durante la ejecución: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    private static void printMenu() {
        System.out.println("\n=== MENÚ DE COMPARACIÓN DE ALGORITMOS ===");
        System.out.println("1. Prueba de Algoritmo Individual");
        System.out.println("2. Comparación Rápida (Tiempo Real)");
        System.out.println("3. Comparación Extensiva (Simulación Semanal)");
        System.out.println("4. Ajuste de Parámetros GRASP");
        System.out.println("5. Información del Sistema");
        System.out.println("6. Salir");
    }
    
    private static void runSingleAlgorithmTest(MoraPackOptimizer optimizer, Scanner scanner) {
        System.out.println("\n=== PRUEBA DE ALGORITMO INDIVIDUAL ===");
        
        AlgorithmType selectedAlgorithm = selectAlgorithm(scanner);
        if (selectedAlgorithm == null) return;
        
        System.out.print("Número de paquetes a procesar (10-100): ");
        try {
            int packageCount = Integer.parseInt(scanner.nextLine());
            if (packageCount < 10 || packageCount > 100) {
                System.out.println("Número de paquetes debe estar entre 10 y 100.");
                return;
            }
            
            System.out.println("Ejecutando " + selectedAlgorithm.getDisplayName() + 
                             " con " + packageCount + " paquetes...");
            
            List<Package> testPackages = optimizer.generateRandomPackages(packageCount);
            
            long startTime = System.currentTimeMillis();
            Solution solution = optimizer.runRealTimeSimulation(testPackages, selectedAlgorithm);
            long endTime = System.currentTimeMillis();
            
            double executionTime = (endTime - startTime) / 1000.0;
            
            if (solution != null) {
                printDetailedResults(selectedAlgorithm, solution, executionTime);
            } else {
                System.out.println("❌ No se pudo generar una solución.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Por favor ingrese un número válido.");
        }
    }
    
    private static void runAlgorithmComparison(MoraPackOptimizer optimizer, Scanner scanner) {
        System.out.println("\n=== COMPARACIÓN RÁPIDA DE ALGORITMOS ===");
        
        System.out.print("Número de paquetes para comparar (20-200): ");
        try {
            int packageCount = Integer.parseInt(scanner.nextLine());
            if (packageCount < 20 || packageCount > 200) {
                System.out.println("Número de paquetes debe estar entre 20 y 200.");
                return;
            }
            
            // Generar paquetes de prueba
            List<Package> testPackages = optimizer.generateRandomPackages(packageCount);
            
            System.out.println("Comparando algoritmos con " + packageCount + " paquetes...\n");
            
            List<ComparisonResult> results = new ArrayList<>();
            
            // Probar cada algoritmo
            for (AlgorithmType algorithm : AlgorithmType.values()) {
                System.out.println("Ejecutando " + algorithm.getDisplayName() + "...");
                
                // Crear copia de paquetes para cada algoritmo
                List<Package> packagesCopy = createPackagesCopy(testPackages, optimizer);
                
                long startTime = System.currentTimeMillis();
                Solution solution = optimizer.runRealTimeSimulation(packagesCopy, algorithm);
                long endTime = System.currentTimeMillis();
                
                double executionTime = (endTime - startTime) / 1000.0;
                
                results.add(new ComparisonResult(algorithm, solution, executionTime));
            }
            
            // Mostrar resultados comparativos
            printComparisonResults(results);
            
        } catch (NumberFormatException e) {
            System.out.println("Por favor ingrese un número válido.");
        }
    }
    
    private static void runWeeklyComparison(MoraPackOptimizer optimizer, Scanner scanner) {
        System.out.println("\n=== COMPARACIÓN EXTENSIVA (SIMULACIÓN SEMANAL) ===");
        System.out.println("⚠️ Advertencia: Esta prueba puede tomar 30-90 minutos por algoritmo.");
        
        System.out.print("¿Continuar? (s/n): ");
        if (!scanner.nextLine().toLowerCase().startsWith("s")) {
            return;
        }
        
        System.out.print("Número de paquetes (500-2000): ");
        try {
            int packageCount = Integer.parseInt(scanner.nextLine());
            if (packageCount < 500 || packageCount > 2000) {
                System.out.println("Número de paquetes debe estar entre 500 y 2000.");
                return;
            }
            
            System.out.println("Iniciando comparación extensiva con " + packageCount + " paquetes...\n");
            
            List<ComparisonResult> results = new ArrayList<>();
            
            for (AlgorithmType algorithm : AlgorithmType.values()) {
                System.out.println("=== Ejecutando " + algorithm.getDisplayName() + " ===");
                System.out.println("Iniciado a las: " + new Date());
                
                long startTime = System.currentTimeMillis();
                Solution solution = optimizer.runWeeklySimulation(packageCount, algorithm);
                long endTime = System.currentTimeMillis();
                
                double executionTime = (endTime - startTime) / 1000.0;
                results.add(new ComparisonResult(algorithm, solution, executionTime));
                
                System.out.printf("Completado en %.2f segundos (%.2f minutos)%n%n", 
                                executionTime, executionTime / 60.0);
            }
            
            printComparisonResults(results);
            
        } catch (NumberFormatException e) {
            System.out.println("Por favor ingrese un número válido.");
        }
    }
    
    private static void runParameterTuning(MoraPackOptimizer optimizer, Scanner scanner) {
        System.out.println("\n=== AJUSTE DE PARÁMETROS GRASP ===");
        
        System.out.print("Número de paquetes para pruebas (50-150): ");
        try {
            int packageCount = Integer.parseInt(scanner.nextLine());
            if (packageCount < 50 || packageCount > 150) {
                System.out.println("Número de paquetes debe estar entre 50 y 150.");
                return;
            }
            
            // Probar diferentes valores de alpha
            double[] alphaValues = {0.1, 0.3, 0.5, 0.7, 0.9};
            
            System.out.println("Probando diferentes valores de α (aleatoriedad) en GRASP...\n");
            
            List<Package> testPackages = optimizer.generateRandomPackages(packageCount);
            
            System.out.println("α\tFitness\tTiempo(s)\tRutas\tNo entregados");
            System.out.println("=" .repeat(55));
            
            for (double alpha : alphaValues) {
                List<Package> packagesCopy = createPackagesCopy(testPackages, optimizer);
                
                // Use ALNS for parameter tuning
                src.model.algorithm.AlgorithmSolver solver = new src.model.algorithm.AlgorithmSolver(packagesCopy, 
                    optimizer.getFlights(), optimizer.getWarehouses(), optimizer.getCities());
                solver.setMaxIterationsALNS(100);
                solver.setDestructionRate(alpha); // Use alpha as destruction rate
                
                long startTime = System.currentTimeMillis();
                Solution solution = solver.searchALNS();
                long endTime = System.currentTimeMillis();
                
                double executionTime = (endTime - startTime) / 1000.0;
                
                if (solution != null) {
                    System.out.printf("%.1f\t%.2f\t%.2f\t\t%d\t%d%n", 
                                    alpha, solution.getFitness(), executionTime, 
                                    solution.getRoutes().size(), solution.getUndeliveredPackages());
                } else {
                    System.out.printf("%.1f\t--\t%.2f\t\t--\t--%n", alpha, executionTime);
                }
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Por favor ingrese un número válido.");
        }
    }
    
    private static AlgorithmType selectAlgorithm(Scanner scanner) {
        System.out.println("\nSeleccione algoritmo:");
        AlgorithmType[] algorithms = AlgorithmType.values();
        for (int i = 0; i < algorithms.length; i++) {
            System.out.printf("%d. %s%n", i + 1, algorithms[i].getDisplayName());
        }
        System.out.print("Opción: ");
        
        try {
            int choice = Integer.parseInt(scanner.nextLine());
            if (choice >= 1 && choice <= algorithms.length) {
                return algorithms[choice - 1];
            } else {
                System.out.println("Opción no válida.");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Por favor ingrese un número válido.");
            return null;
        }
    }
    
    private static List<Package> createPackagesCopy(List<Package> original, MoraPackOptimizer optimizer) {
        List<Package> copy = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            Package originalPkg = original.get(i);
            Customer customer = optimizer.generateRandomCustomer();
            customer.assignOriginWarehouse(optimizer.getWarehouses());
            
            Package newPkg = new Package(i + 1, customer, originalPkg.getDestinationCity(), 
                                       originalPkg.getOrderDate());
            newPkg.setPriority(originalPkg.getPriority());
            copy.add(newPkg);
        }
        return copy;
    }
    
    private static void printDetailedResults(AlgorithmType algorithm, Solution solution, double executionTime) {
        System.out.println("\n=== RESULTADOS DETALLADOS ===");
        System.out.println("Algoritmo: " + algorithm.getDisplayName());
        System.out.printf("Tiempo de ejecución: %.2f segundos%n", executionTime);
        System.out.printf("Fitness: %.2f%n", solution.getFitness());
        System.out.printf("Costo total: $%.2f%n", solution.getTotalCost());
        System.out.printf("Tiempo total: %.2f días%n", solution.getTotalTime());
        System.out.printf("Rutas generadas: %d%n", solution.getRoutes().size());
        System.out.printf("Paquetes no entregados: %d%n", solution.getUndeliveredPackages());
        System.out.printf("Solución válida: %s%n", solution.isValid() ? "✅ Sí" : "❌ No");
    }
    
    private static void printComparisonResults(List<ComparisonResult> results) {
        System.out.println("\n=== RESULTADOS DE COMPARACIÓN ===");
        System.out.println();
        System.out.printf("%-30s %-10s %-12s %-8s %-8s %-10s%n", 
                         "Algoritmo", "Fitness", "Tiempo(s)", "Rutas", "No Ent.", "Estado");
        System.out.println("=" .repeat(80));
        
        // Ordenar por fitness (descendente)
        results.sort((a, b) -> {
            if (a.solution == null && b.solution == null) return 0;
            if (a.solution == null) return 1;
            if (b.solution == null) return -1;
            return Double.compare(b.solution.getFitness(), a.solution.getFitness());
        });
        
        for (ComparisonResult result : results) {
            if (result.solution != null) {
                System.out.printf("%-30s %-10.2f %-12.2f %-8d %-8d %-10s%n",
                                result.algorithm.getDisplayName(),
                                result.solution.getFitness(),
                                result.executionTime,
                                result.solution.getRoutes().size(),
                                result.solution.getUndeliveredPackages(),
                                result.solution.isValid() ? "✅ Válido" : "❌ Inválido");
            } else {
                System.out.printf("%-30s %-10s %-12.2f %-8s %-8s %-10s%n",
                                result.algorithm.getDisplayName(),
                                "FALLO", result.executionTime, "--", "--", "❌ Error");
            }
        }
        
        // Mostrar ganador
        if (!results.isEmpty() && results.get(0).solution != null) {
            System.out.printf("%n🏆 GANADOR: %s (Fitness: %.2f)%n", 
                             results.get(0).algorithm.getDisplayName(), 
                             results.get(0).solution.getFitness());
        }
        
        // Estadísticas adicionales
        System.out.println("\n=== ESTADÍSTICAS ===");
        double avgFitness = results.stream()
                .filter(r -> r.solution != null)
                .mapToDouble(r -> r.solution.getFitness())
                .average().orElse(0.0);
        double avgTime = results.stream()
                .mapToDouble(r -> r.executionTime)
                .average().orElse(0.0);
        
        System.out.printf("Fitness promedio: %.2f%n", avgFitness);
        System.out.printf("Tiempo promedio: %.2f segundos%n", avgTime);
        
        long successfulRuns = results.stream().filter(r -> r.solution != null).count();
        System.out.printf("Ejecuciones exitosas: %d/%d%n", successfulRuns, results.size());
    }
    
    private static void printSystemInfo(MoraPackOptimizer optimizer) {
        System.out.println("Ciudades: " + optimizer.getCities().size());
        System.out.println("Aeropuertos: " + optimizer.getAirports().size());
        System.out.println("Almacenes: " + optimizer.getWarehouses().size());
        System.out.println("Vuelos: " + optimizer.getFlights().size());
    }
    
    private static class ComparisonResult {
        AlgorithmType algorithm;
        Solution solution;
        double executionTime;
        
        ComparisonResult(AlgorithmType algorithm, Solution solution, double executionTime) {
            this.algorithm = algorithm;
            this.solution = solution;
            this.executionTime = executionTime;
        }
    }
}

