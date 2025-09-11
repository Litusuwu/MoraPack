package src.model.algorithm;

import src.model.Airport;
import src.model.City;
import src.model.Flight;
import src.model.Package;
import src.model.Solution;
import src.model.Warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Demostración del sistema MoraPack usando datos reales de archivos
 */
public class MoraPackRealDataDemo {
    private static final String AIRPORT_FILE_PATH = "data/airportInfo.txt";
    private static final String FLIGHT_FILE_PATH = "data/flights.txt";
    
    public static void main(String[] args) {
        System.out.println("=== SISTEMA MORAPACK CON DATOS REALES ===\n");
        
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
                            runRealTimeDemo(optimizer);
                            break;
                        case 2:
                            runWeeklyDemo(optimizer, scanner);
                            break;
                        case 3:
                            runCollapseDemo(optimizer);
                            break;
                        case 4:
                            printSystemInfo(optimizer);
                            break;
                        case 5:
                            showAirportDetails(optimizer, scanner);
                            break;
                        case 6:
                            showFlightConnections(optimizer, scanner);
                            break;
                        case 7:
                            exit = true;
                            System.out.println("¡Gracias por usar MoraPack!");
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
        System.out.println("\n=== MENÚ PRINCIPAL ===");
        System.out.println("1. Simulación en Tiempo Real");
        System.out.println("2. Simulación Semanal");
        System.out.println("3. Simulación hasta Colapso");
        System.out.println("4. Información del Sistema");
        System.out.println("5. Detalles de Aeropuertos");
        System.out.println("6. Conexiones de Vuelos");
        System.out.println("7. Salir");
    }
    
    private static void runRealTimeDemo(MoraPackOptimizer optimizer) {
        System.out.println("\n=== SIMULACIÓN EN TIEMPO REAL ===");
        System.out.println("Generando paquetes aleatorios para simulación en tiempo real...");
        
        // Generate some random packages for real-time simulation
        List<Package> incomingPackages = optimizer.generateRandomPackages(50);
        
        System.out.println("Procesando " + incomingPackages.size() + " paquetes...");
        
        long startTime = System.currentTimeMillis();
        Solution solution = optimizer.runRealTimeSimulation(incomingPackages);
        long endTime = System.currentTimeMillis();
        
        double executionTimeSeconds = (endTime - startTime) / 1000.0;
        
        if (solution != null) {
            printSolutionSummary(solution, executionTimeSeconds);
        } else {
            System.out.println("No se pudo generar una solución válida.");
        }
    }
    
    private static void runWeeklyDemo(MoraPackOptimizer optimizer, Scanner scanner) {
        System.out.println("\n=== SIMULACIÓN SEMANAL ===");
        System.out.print("Ingrese el número de paquetes a procesar (recomendado: 1000-5000): ");
        
        try {
            int packageCount = Integer.parseInt(scanner.nextLine());
            if (packageCount <= 0) {
                System.out.println("El número de paquetes debe ser mayor a 0.");
                return;
            }
            
            System.out.println("Ejecutando simulación semanal con " + packageCount + " paquetes...");
            System.out.println("Esto puede tomar varios minutos. Por favor espere...");
            
            long startTime = System.currentTimeMillis();
            Solution solution = optimizer.runWeeklySimulation(packageCount);
            long endTime = System.currentTimeMillis();
            
            double executionTimeSeconds = (endTime - startTime) / 1000.0;
            double executionTimeMinutes = executionTimeSeconds / 60.0;
            
            if (solution != null) {
                System.out.println("\n=== RESULTADOS DE SIMULACIÓN SEMANAL ===");
                System.out.printf("Tiempo de ejecución: %.2f segundos (%.2f minutos)%n", 
                                executionTimeSeconds, executionTimeMinutes);
                printSolutionSummary(solution, executionTimeSeconds);
            } else {
                System.out.println("No se pudo generar una solución válida para la simulación semanal.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Por favor ingrese un número válido.");
        }
    }
    
    private static void runCollapseDemo(MoraPackOptimizer optimizer) {
        System.out.println("\n=== SIMULACIÓN HASTA COLAPSO ===");
        System.out.println("Ejecutando simulación incremental hasta encontrar el punto de colapso...");
        System.out.println("Esto puede tomar varios minutos. Por favor espere...");
        
        long startTime = System.currentTimeMillis();
        MoraPackOptimizer.SimulationResult result = optimizer.runCollapseSimulation();
        long endTime = System.currentTimeMillis();
        
        double executionTimeSeconds = (endTime - startTime) / 1000.0;
        double executionTimeMinutes = executionTimeSeconds / 60.0;
        
        System.out.printf("\nSimulación completada en %.2f segundos (%.2f minutos)%n", 
                         executionTimeSeconds, executionTimeMinutes);
        
        if (result != null && !result.getPackageLoads().isEmpty()) {
            System.out.println("\n=== RESULTADOS DE SIMULACIÓN DE COLAPSO ===");
            
            List<Integer> loads = result.getPackageLoads();
            List<Double> fitness = result.getFitnessValues();
            List<Integer> undelivered = result.getUndeliveredCounts();
            List<Boolean> collapsed = result.getCollapseFlags();
            
            System.out.println("Puntos de datos recolectados: " + loads.size());
            
            // Find collapse point
            int collapsePoint = -1;
            for (int i = 0; i < collapsed.size(); i++) {
                if (collapsed.get(i)) {
                    collapsePoint = i;
                    break;
                }
            }
            
            if (collapsePoint >= 0) {
                System.out.printf("¡COLAPSO DETECTADO en el punto %d!%n", collapsePoint + 1);
                System.out.printf("- Carga de paquetes en colapso: %d%n", loads.get(collapsePoint));
                System.out.printf("- Paquetes no entregados: %d%n", undelivered.get(collapsePoint));
                System.out.printf("- Fitness en colapso: %.2f%n", fitness.get(collapsePoint));
            } else {
                System.out.println("No se detectó colapso del sistema en el rango probado.");
                System.out.printf("Máxima carga probada: %d paquetes%n", loads.get(loads.size() - 1));
            }
            
            // Print summary statistics
            System.out.println("\n=== ESTADÍSTICAS ===");
            System.out.printf("Rango de cargas probadas: %d - %d paquetes%n", 
                             loads.get(0), loads.get(loads.size() - 1));
            
            double avgFitness = fitness.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            System.out.printf("Fitness promedio: %.2f%n", avgFitness);
            
        } else {
            System.out.println("No se pudieron obtener resultados de la simulación de colapso.");
        }
    }
    
    private static void printSystemInfo(MoraPackOptimizer optimizer) {
        System.out.println("\n=== INFORMACIÓN DEL SISTEMA ===");
        System.out.println("Ciudades:");
        for (City city : optimizer.getCities()) {
            System.out.printf("  - %s (%s)%n", city.getName(), city.getContinent());
        }
        
        System.out.println("\nAeropuertos principales:");
        for (Airport airport : optimizer.getAirports()) {
            if (airport.getWarehouse() != null && airport.getWarehouse().isMainWarehouse()) {
                System.out.printf("  - %s (%s) - %s%n", 
                    airport.getCodeIATA(), 
                    airport.getCity().getName(),
                    airport.getCity().getContinent());
            }
        }
        
        System.out.println("\nAlmacenes:");
        int mainWarehouses = 0;
        int totalWarehouses = optimizer.getWarehouses().size();
        for (Warehouse warehouse : optimizer.getWarehouses()) {
            if (warehouse.isMainWarehouse()) {
                mainWarehouses++;
                System.out.printf("  - [PRINCIPAL] %s - Capacidad: %d%n", 
                    warehouse.getName(), warehouse.getMaxCapacity());
            }
        }
        System.out.printf("Total: %d almacenes (%d principales, %d secundarios)%n", 
                         totalWarehouses, mainWarehouses, totalWarehouses - mainWarehouses);
        
        System.out.printf("\nVuelos totales: %d%n", optimizer.getFlights().size());
        
        // Count flights by continent combination
        int sameContinent = 0;
        int differentContinent = 0;
        for (Flight flight : optimizer.getFlights()) {
            if (flight.getOriginAirport().getCity().getContinent() == 
                flight.getDestinationAirport().getCity().getContinent()) {
                sameContinent++;
            } else {
                differentContinent++;
            }
        }
        System.out.printf("  - Mismo continente: %d%n", sameContinent);
        System.out.printf("  - Diferentes continentes: %d%n", differentContinent);
    }
    
    private static void showAirportDetails(MoraPackOptimizer optimizer, Scanner scanner) {
        System.out.println("\n=== DETALLES DE AEROPUERTOS ===");
        System.out.println("Aeropuertos disponibles:");
        
        List<Airport> airports = optimizer.getAirports();
        for (int i = 0; i < Math.min(airports.size(), 10); i++) {
            Airport airport = airports.get(i);
            System.out.printf("%d. %s - %s, %s%n", 
                i + 1, airport.getCodeIATA(), airport.getCity().getName(), 
                airport.getCity().getContinent());
        }
        
        if (airports.size() > 10) {
            System.out.printf("... y %d aeropuertos más.%n", airports.size() - 10);
        }
        
        System.out.print("Ingrese el número del aeropuerto para ver detalles (0 para volver): ");
        try {
            int choice = Integer.parseInt(scanner.nextLine());
            if (choice > 0 && choice <= airports.size()) {
                Airport airport = airports.get(choice - 1);
                System.out.printf("\n=== DETALLES DE %s ===", airport.getCodeIATA());
                System.out.printf("Ciudad: %s%n", airport.getCity().getName());
                System.out.printf("Continente: %s%n", airport.getCity().getContinent());
                System.out.printf("Alias: %s%n", airport.getAlias());
                System.out.printf("Zona horaria UTC: %+d%n", airport.getTimezoneUTC());
                System.out.printf("Capacidad máxima: %.0f%n", airport.getMaxCapacity());
                System.out.printf("Capacidad usada: %.0f%n", airport.getUsedCapacity());
                System.out.printf("Capacidad disponible: %.0f%n", airport.getAvailableCapacity());
                
                if (airport.getWarehouse() != null) {
                    Warehouse warehouse = airport.getWarehouse();
                    System.out.printf("Almacén: %s%s%n", warehouse.getName(), 
                        warehouse.isMainWarehouse() ? " (PRINCIPAL)" : "");
                    System.out.printf("Capacidad del almacén: %d%n", warehouse.getMaxCapacity());
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Número no válido.");
        }
    }
    
    private static void showFlightConnections(MoraPackOptimizer optimizer, Scanner scanner) {
        System.out.println("\n=== CONEXIONES DE VUELOS ===");
        System.out.print("Ingrese código IATA del aeropuerto de origen: ");
        String originCode = scanner.nextLine().toUpperCase().trim();
        
        List<Flight> connections = optimizer.getFlights().stream()
                .filter(flight -> flight.getOriginAirport().getCodeIATA().equals(originCode))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        if (connections.isEmpty()) {
            System.out.println("No se encontraron vuelos desde " + originCode);
            return;
        }
        
        System.out.printf("\nVuelos desde %s:%n", originCode);
        for (Flight flight : connections) {
            Airport dest = flight.getDestinationAirport();
            System.out.printf("  -> %s (%s, %s) - Capacidad: %d, Tiempo: %.1f días%n",
                dest.getCodeIATA(), dest.getCity().getName(), 
                dest.getCity().getContinent(), flight.getMaxCapacity(), 
                flight.getTransportTime());
        }
    }
    
    private static void printSolutionSummary(Solution solution, double executionTimeSeconds) {
        System.out.println("\n=== RESUMEN DE LA SOLUCIÓN ===");
        System.out.printf("Tiempo de ejecución: %.2f segundos%n", executionTimeSeconds);
        System.out.printf("Rutas generadas: %d%n", solution.getRoutes().size());
        System.out.printf("Costo total: $%.2f%n", solution.getTotalCost());
        System.out.printf("Tiempo total: %.2f días%n", solution.getTotalTime());
        System.out.printf("Paquetes no entregados: %d%n", solution.getUndeliveredPackages());
        System.out.printf("Fitness de la solución: %.2f%n", solution.getFitness());
        
        if (solution.isValid()) {
            System.out.println("✅ La solución es válida");
        } else {
            System.out.println("❌ La solución tiene problemas de validez");
        }
    }
}
