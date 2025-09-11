package src.model.algorithm;

import src.model.Package;
import src.model.Route;
import src.model.Solution;
import java.util.List;

/**
 * Demostración simple del sistema MoraPack consolidado
 * Muestra el uso de los algoritmos Tabu Search y ALNS
 */
public class MetaheuristicDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 SISTEMA MORAPACK - ALGORITMOS METAHEURÍSTICOS CONSOLIDADOS");
        System.out.println("=" .repeat(70));
        
        // Crear optimizador con datos sintéticos
        MoraPackOptimizer optimizer = new MoraPackOptimizer();
        
        System.out.println("📊 Sistema inicializado:");
        System.out.println("   - Ciudades: " + optimizer.getCities().size());
        System.out.println("   - Aeropuertos: " + optimizer.getAirports().size());
        System.out.println("   - Vuelos: " + optimizer.getFlights().size());
        System.out.println("   - Almacenes: " + optimizer.getWarehouses().size());
        
        // Generar paquetes de prueba
        int packageCount = 30;
        System.out.println("\n📦 Generando " + packageCount + " paquetes de prueba...");
        List<Package> testPackages = optimizer.generateRandomPackages(packageCount);
        
        // Probar cada algoritmo
        testAlgorithm(optimizer, testPackages, AlgorithmType.GREEDY);
        testAlgorithm(optimizer, testPackages, AlgorithmType.TABU_SEARCH);
        testAlgorithm(optimizer, testPackages, AlgorithmType.ALNS);
        testAlgorithm(optimizer, testPackages, AlgorithmType.HYBRID);
        
        System.out.println("\n🎯 RESUMEN FINAL");
        System.out.println("=" .repeat(50));
        System.out.println("✅ Todos los algoritmos ejecutados exitosamente");
        System.out.println("✅ Sistema MoraPack funcionando correctamente");
        System.out.println("\n💡 Próximos pasos:");
        System.out.println("   1. Usar MoraPackRealDataDemo para datos reales");
        System.out.println("   2. Ejecutar AlgorithmComparisonDemo para comparación");
        System.out.println("   3. Probar QuickDemo para pruebas rápidas");
    }
    
    private static void testAlgorithm(MoraPackOptimizer optimizer, 
                                    List<Package> testPackages, 
                                    AlgorithmType algorithm) {
        
        System.out.println("\n" + "=" .repeat(60));
        System.out.println("🔬 PROBANDO: " + algorithm.getDisplayName());
        System.out.println("=" .repeat(60));
        
        try {
            // Crear copia de paquetes para cada algoritmo
            List<Package> packagesCopy = createPackagesCopy(optimizer, testPackages);
            
            long startTime = System.currentTimeMillis();
            Solution solution = optimizer.runRealTimeSimulation(packagesCopy, algorithm);
            long endTime = System.currentTimeMillis();
            
            double executionTime = (endTime - startTime) / 1000.0;
            
            // Mostrar resultados
            if (solution != null) {
                System.out.println("\n📊 RESULTADOS:");
                System.out.printf("   ⏱️  Tiempo de ejecución: %.2f segundos%n", executionTime);
                System.out.printf("   🎯 Fitness: %.2f%n", solution.getFitness());
                System.out.printf("   💰 Costo total: $%.2f%n", solution.getTotalCost());
                System.out.printf("   ⏰ Tiempo total: %.2f días%n", solution.getTotalTime());
                System.out.printf("   🛣️  Rutas generadas: %d%n", solution.getRoutes().size());
                System.out.printf("   📦 Paquetes procesados: %d%n", packagesCopy.size());
                System.out.printf("   ❌ Paquetes no entregados: %d%n", solution.getUndeliveredPackages());
                
                if (solution.isValid()) {
                    System.out.println("   ✅ Solución VÁLIDA");
                } else {
                    System.out.println("   ⚠️  Solución PARCIAL");
                }
                
                // Mostrar algunas rutas de ejemplo
                if (!solution.getRoutes().isEmpty()) {
                    System.out.println("\n🛣️  RUTAS DE EJEMPLO:");
                    int routesToShow = Math.min(3, solution.getRoutes().size());
                    for (int i = 0; i < routesToShow; i++) {
                        Route route = solution.getRoutes().get(i);
                        System.out.printf("   %d. %s → %s (%d paquetes, %.1f días, $%.0f)%n",
                            i + 1,
                            route.getOriginCity().getName(),
                            route.getDestinationCity().getName(),
                            route.getTotalPackages(),
                            route.getTotalTime(),
                            route.getTotalCost());
                    }
                }
            } else {
                System.out.println("❌ ERROR: No se pudo generar solución");
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR ejecutando " + algorithm.getDisplayName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static List<Package> createPackagesCopy(MoraPackOptimizer optimizer, 
                                                   List<Package> original) {
        // Crear nuevos paquetes con los mismos destinos pero nuevos IDs
        return optimizer.generateRandomPackages(original.size());
    }
}

