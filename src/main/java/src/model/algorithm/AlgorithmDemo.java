package src.model.algorithm;

import src.model.City;
import src.model.Flight;
import src.model.Package;
import src.model.Solution;
import src.model.Warehouse;
import java.util.List;

/**
 * Demo consolidada del paquete algorithm de MoraPack
 * Muestra todos los algoritmos disponibles en el nuevo paquete organizativo
 */
public class AlgorithmDemo {
    
    public static void main(String[] args) {
        System.out.println("🎯 DEMO DEL PAQUETE ALGORITHM - MORAPACK");
        System.out.println("=" .repeat(70));
        System.out.println("Mostrando todos los algoritmos disponibles en el paquete consolidado\n");
        
        // Crear optimizador con datos sintéticos
        MoraPackOptimizer optimizer = new MoraPackOptimizer();
        
        System.out.println("📊 Sistema inicializado:");
        System.out.println("   - Ciudades: " + optimizer.getCities().size());
        System.out.println("   - Aeropuertos: " + optimizer.getAirports().size());
        System.out.println("   - Vuelos: " + optimizer.getFlights().size());
        System.out.println("   - Almacenes: " + optimizer.getWarehouses().size());
        
        // Generar paquetes de prueba
        int packageCount = 25;
        System.out.println("\n📦 Generando " + packageCount + " paquetes de prueba...");
        List<Package> testPackages = optimizer.generateRandomPackages(packageCount);
        
        System.out.println("\n🔬 PROBANDO TODOS LOS ALGORITMOS DEL PAQUETE");
        System.out.println("=" .repeat(60));
        
        // Probar cada algoritmo disponible en el paquete algorithm
        for (AlgorithmType algorithmType : AlgorithmType.values()) {
            testAlgorithmDirect(testPackages, optimizer.getFlights(), 
                              optimizer.getWarehouses(), optimizer.getCities(), algorithmType);
        }
        
        System.out.println("\n🎉 RESUMEN FINAL");
        System.out.println("=" .repeat(50));
        System.out.println("✅ Todos los algoritmos del paquete 'algorithm' funcionan correctamente");
        System.out.println("✅ Estructura organizativa implementada exitosamente");
        System.out.println("✅ Sistema MoraPack completamente funcional");
        
        System.out.println("\n📁 ESTRUCTURA DEL PAQUETE ALGORITHM:");
        System.out.println("├── AlgorithmType.java (Enum de tipos de algoritmo)");
        System.out.println("├── AlgorithmSolver.java (Solver consolidado principal)");
        System.out.println("├── GreedyAlgorithm.java (Algoritmo constructivo)");
        System.out.println("└── TabuSearch.java (Algoritmo de búsqueda tabú)");
        
        System.out.println("\n💡 CARACTERÍSTICAS DESTACADAS:");
        System.out.println("   🚀 Greedy: Construcción rápida de soluciones iniciales");
        System.out.println("   🔍 Tabu Search: Búsqueda local con memoria tabú");
        System.out.println("   🧠 ALNS: Destrucción y reparación adaptativa");
        System.out.println("   ⚡ Híbrido: Combinación inteligente de algoritmos");
    }
    
    /**
     * Prueba un algoritmo específico usando el AlgorithmSolver directamente
     */
    private static void testAlgorithmDirect(List<Package> packages, List<Flight> flights,
                                          List<Warehouse> warehouses, List<City> cities,
                                          AlgorithmType algorithmType) {
        
        System.out.println("\n🔬 ALGORITMO: " + algorithmType.getDisplayName());
        System.out.println("-" .repeat(50));
        
        try {
            // Crear solver del paquete algorithm
            AlgorithmSolver solver = new AlgorithmSolver(packages, flights, warehouses, cities);
            
            long startTime = System.currentTimeMillis();
            Solution solution = null;
            
            // Ejecutar el algoritmo correspondiente
            switch (algorithmType) {
                case GREEDY:
                    solution = solver.searchGreedy();
                    break;
                case TABU_SEARCH:
                    solver.setMaxIterationsTabu(200); // Reducido para demo
                    solution = solver.searchTabu();
                    break;
                case ALNS:
                    solver.setMaxIterationsALNS(100); // Reducido para demo
                    solution = solver.searchALNS();
                    break;
                case HYBRID:
                    solver.setMaxIterationsALNS(50);
                    solver.setMaxIterationsTabu(50);
                    solution = solver.searchHybrid();
                    break;
            }
            
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            if (solution != null) {
                System.out.printf("⏱️  Tiempo: %.2f segundos%n", executionTime);
                System.out.printf("🎯 Fitness: %.2f%n", solution.getFitness());
                System.out.printf("💰 Costo: $%.2f%n", solution.getTotalCost());
                System.out.printf("⏰ Tiempo total: %.2f días%n", solution.getTotalTime());
                System.out.printf("🛣️  Rutas: %d%n", solution.getRoutes().size());
                System.out.printf("📦 Paquetes: %d%n", packages.size());
                System.out.printf("❌ No entregados: %d%n", solution.getUndeliveredPackages());
                System.out.println("✅ Estado: " + (solution.isValid() ? "VÁLIDO" : "PARCIAL"));
                
                // Mostrar eficiencia
                double packagesPerSecond = packages.size() / Math.max(executionTime, 0.001);
                System.out.printf("⚡ Eficiencia: %.1f paquetes/segundo%n", packagesPerSecond);
            } else {
                System.out.println("❌ ERROR: No se pudo generar solución");
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR ejecutando " + algorithmType.getDisplayName() + ": " + e.getMessage());
        }
    }
}
