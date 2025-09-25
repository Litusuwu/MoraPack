package com.system.morapack.schemas.algorithm;

import com.system.morapack.schemas.Flight;
import com.system.morapack.schemas.Package;
import com.system.morapack.schemas.City;
import com.system.morapack.schemas.Airport;
import com.system.morapack.schemas.Continent;
import com.system.morapack.config.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Clase que implementa operadores de reparación para el algoritmo ALNS
 * (Adaptive Large Neighborhood Search) específicamente diseñados para el problema
 * de logística MoraPack.
 * 
 * Los operadores priorizan las entregas a tiempo y la eficiencia de rutas.
 */
public class ALNSRepair {
    
    private ArrayList<Airport> airports;
    private ArrayList<Flight> flights;
    private HashMap<Airport, Integer> warehouseOccupancy;
    private Random random;
    
    public ALNSRepair(ArrayList<Airport> airports, ArrayList<Flight> flights, 
                      HashMap<Airport, Integer> warehouseOccupancy) {
        this.airports = airports;
        this.flights = flights;
        this.warehouseOccupancy = warehouseOccupancy;
        this.random = new Random(System.currentTimeMillis());
    }
    
    /**
     * Constructor con semilla específica
     */
    public ALNSRepair(ArrayList<Airport> airports, ArrayList<Flight> flights, 
                      HashMap<Airport, Integer> warehouseOccupancy, long seed) {
        this.airports = airports;
        this.flights = flights;
        this.warehouseOccupancy = warehouseOccupancy;
        this.random = new Random(seed);
    }
    
    /**
     * Reparación Greedy: Inserta paquetes usando el enfoque greedy optimizado.
     * Prioriza paquetes por deadline y busca la mejor ruta disponible.
     */
    public RepairResult greedyRepair(
            HashMap<Package, ArrayList<Flight>> partialSolution,
            ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages) {
        
        HashMap<Package, ArrayList<Flight>> repairedSolution = new HashMap<>(partialSolution);
        ArrayList<Package> unassignedPackages = new ArrayList<>();
        
        // Ordenar paquetes destruidos por deadline (más urgente primero)
        ArrayList<Package> packagesToRepair = new ArrayList<>();
        for (Map.Entry<Package, ArrayList<Flight>> entry : destroyedPackages) {
            packagesToRepair.add(entry.getKey());
        }
        
        packagesToRepair.sort((p1, p2) -> p1.getDeliveryDeadline().compareTo(p2.getDeliveryDeadline()));
        
        int reinsertedCount = 0;
        
        // Intentar reinsertar cada paquete
        for (Package pkg : packagesToRepair) {
            Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
            
            // Get product count for this package
            int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
            
            // Verificar capacidad del almacén
            if (destinationAirport == null || !hasWarehouseCapacity(destinationAirport, productCount)) {
                unassignedPackages.add(pkg);
                continue;
            }
            
            // Buscar la mejor ruta
            ArrayList<Flight> bestRoute = findBestRoute(pkg);
            if (bestRoute != null && isRouteValid(pkg, bestRoute)) {
                repairedSolution.put(pkg, bestRoute);
                updateFlightCapacities(bestRoute, productCount);
                incrementWarehouseOccupancy(destinationAirport, productCount);
                reinsertedCount++;
            } else {
                unassignedPackages.add(pkg);
            }
        }
        
        System.out.println("Reparación Greedy: " + reinsertedCount + "/" + packagesToRepair.size() + 
                          " paquetes reinsertados");
        
        return new RepairResult(repairedSolution, unassignedPackages);
    }
    
    /**
     * Reparación por Regret: Calcula el "arrepentimiento" de no insertar cada paquete
     * y prioriza aquellos con mayor diferencia entre mejor y segunda mejor opción.
     */
    public RepairResult regretRepair(
            HashMap<Package, ArrayList<Flight>> partialSolution,
            ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages,
            int regretLevel) {
        
        HashMap<Package, ArrayList<Flight>> repairedSolution = new HashMap<>(partialSolution);
        ArrayList<Package> unassignedPackages = new ArrayList<>();
        
        ArrayList<Package> remainingPackages = new ArrayList<>();
        for (Map.Entry<Package, ArrayList<Flight>> entry : destroyedPackages) {
            remainingPackages.add(entry.getKey());
        }
        
        int reinsertedCount = 0;
        
        // Mientras haya paquetes por insertar
        while (!remainingPackages.isEmpty()) {
            Package bestPackage = null;
            ArrayList<Flight> bestRoute = null;
            double maxRegret = Double.NEGATIVE_INFINITY;
            
            // Calcular regret para cada paquete restante
            for (Package pkg : remainingPackages) {
                Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
                int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
                
                if (destinationAirport == null || !hasWarehouseCapacity(destinationAirport, productCount)) {
                    continue;
                }
                
                // Encontrar las mejores rutas para este paquete
                ArrayList<RouteOption> routeOptions = findAllRouteOptions(pkg);
                
                if (routeOptions.isEmpty()) {
                    continue;
                }
                
                // Ordenar por margen de tiempo (mejor primero)
                routeOptions.sort((r1, r2) -> Double.compare(r2.timeMargin, r1.timeMargin));
                
                // Calcular regret
                double regret = 0;
                if (routeOptions.size() >= 2) {
                    // Regret = diferencia entre mejor y segunda mejor opción
                    regret = routeOptions.get(0).timeMargin - routeOptions.get(1).timeMargin;
                } else if (routeOptions.size() == 1) {
                    // Solo una opción: regret basado en urgencia
                    LocalDateTime now = LocalDateTime.now();
                    long hoursUntilDeadline = ChronoUnit.HOURS.between(now, pkg.getDeliveryDeadline());
                    regret = Math.max(0, 168 - hoursUntilDeadline); // Más regret para deadlines más cercanos
                }
                
                // Añadir factor de urgencia al regret
                LocalDateTime now = LocalDateTime.now();
                long hoursUntilDeadline = ChronoUnit.HOURS.between(now, pkg.getDeliveryDeadline());
                double urgencyFactor = Math.max(1, 72.0 / Math.max(1, hoursUntilDeadline));
                regret *= urgencyFactor;
                
                if (regret > maxRegret) {
                    maxRegret = regret;
                    bestPackage = pkg;
                    bestRoute = routeOptions.get(0).route;
                }
            }
            
            // Insertar el paquete con mayor regret
            if (bestPackage != null && bestRoute != null) {
                repairedSolution.put(bestPackage, bestRoute);
                int productCount = bestPackage.getProducts() != null ? bestPackage.getProducts().size() : 1;
                updateFlightCapacities(bestRoute, productCount);
                incrementWarehouseOccupancy(getAirportByCity(bestPackage.getDestinationCity()), productCount);
                remainingPackages.remove(bestPackage);
                reinsertedCount++;
            } else {
                // No se pudo insertar ningún paquete, agregar todos los restantes como no asignados
                unassignedPackages.addAll(remainingPackages);
                break;
            }
        }
        
        System.out.println("Reparación por Regret: " + reinsertedCount + "/" + destroyedPackages.size() + 
                          " paquetes reinsertados");
        
        return new RepairResult(repairedSolution, unassignedPackages);
    }
    
    /**
     * Reparación por tiempo: Prioriza paquetes con deadlines más cercanos.
     */
    public RepairResult timeBasedRepair(
            HashMap<Package, ArrayList<Flight>> partialSolution,
            ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages) {
        
        HashMap<Package, ArrayList<Flight>> repairedSolution = new HashMap<>(partialSolution);
        ArrayList<Package> unassignedPackages = new ArrayList<>();
        
        // Extraer paquetes y ordenar por urgencia (deadline más cercano primero)
        ArrayList<Package> packagesToRepair = new ArrayList<>();
        for (Map.Entry<Package, ArrayList<Flight>> entry : destroyedPackages) {
            packagesToRepair.add(entry.getKey());
        }
        
        packagesToRepair.sort((p1, p2) -> {
            LocalDateTime now = LocalDateTime.now();
            long p1Hours = ChronoUnit.HOURS.between(now, p1.getDeliveryDeadline());
            long p2Hours = ChronoUnit.HOURS.between(now, p2.getDeliveryDeadline());
            return Long.compare(p1Hours, p2Hours);
        });
        
        int reinsertedCount = 0;
        
        // Insertar paquetes en orden de urgencia
        for (Package pkg : packagesToRepair) {
            Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
            int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
            
            if (destinationAirport == null || !hasWarehouseCapacity(destinationAirport, productCount)) {
                unassignedPackages.add(pkg);
                continue;
            }
            
            // Buscar ruta con mayor margen de tiempo
            ArrayList<Flight> bestRoute = findRouteWithMaxMargin(pkg);
            if (bestRoute != null && isRouteValid(pkg, bestRoute)) {
                repairedSolution.put(pkg, bestRoute);
                updateFlightCapacities(bestRoute, productCount);
                incrementWarehouseOccupancy(destinationAirport, productCount);
                reinsertedCount++;
            } else {
                unassignedPackages.add(pkg);
            }
        }
        
        System.out.println("Reparación por tiempo: " + reinsertedCount + "/" + packagesToRepair.size() + 
                          " paquetes reinsertados");
        
        return new RepairResult(repairedSolution, unassignedPackages);
    }
    
    /**
     * Reparación por capacidad: Prioriza rutas con mayor capacidad disponible.
     */
    public RepairResult capacityBasedRepair(
            HashMap<Package, ArrayList<Flight>> partialSolution,
            ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages) {
        
        HashMap<Package, ArrayList<Flight>> repairedSolution = new HashMap<>(partialSolution);
        ArrayList<Package> unassignedPackages = new ArrayList<>();
        
        ArrayList<Package> packagesToRepair = new ArrayList<>();
        for (Map.Entry<Package, ArrayList<Flight>> entry : destroyedPackages) {
            packagesToRepair.add(entry.getKey());
        }
        
        // Ordenar por deadline como criterio secundario
        packagesToRepair.sort((p1, p2) -> p1.getDeliveryDeadline().compareTo(p2.getDeliveryDeadline()));
        
        int reinsertedCount = 0;
        
        for (Package pkg : packagesToRepair) {
            Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
            int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
            
            if (destinationAirport == null || !hasWarehouseCapacity(destinationAirport, productCount)) {
                unassignedPackages.add(pkg);
                continue;
            }
            
            // Buscar ruta con mayor capacidad disponible
            ArrayList<Flight> bestRoute = findRouteWithMaxCapacity(pkg);
            if (bestRoute != null && isRouteValid(pkg, bestRoute)) {
                repairedSolution.put(pkg, bestRoute);
                updateFlightCapacities(bestRoute, productCount);
                incrementWarehouseOccupancy(destinationAirport, productCount);
                reinsertedCount++;
            } else {
                unassignedPackages.add(pkg);
            }
        }
        
        System.out.println("Reparación por capacidad: " + reinsertedCount + "/" + packagesToRepair.size() + 
                          " paquetes reinsertados");
        
        return new RepairResult(repairedSolution, unassignedPackages);
    }
    
    // ================= MÉTODOS AUXILIARES =================
    
    private ArrayList<RouteOption> findAllRouteOptions(Package pkg) {
        ArrayList<RouteOption> options = new ArrayList<>();
        City origin = pkg.getCurrentLocation();
        City destination = pkg.getDestinationCity();
        
        if (origin.equals(destination)) {
            options.add(new RouteOption(new ArrayList<>(), Double.MAX_VALUE));
            return options;
        }
        
        // Buscar ruta directa
        ArrayList<Flight> directRoute = findDirectRoute(origin, destination);
        if (directRoute != null && isRouteValid(pkg, directRoute)) {
            double margin = calculateRouteTimeMargin(pkg, directRoute);
            options.add(new RouteOption(directRoute, margin));
        }
        
        // Buscar rutas con una escala
        ArrayList<Flight> oneStopRoute = findOneStopRoute(origin, destination);
        if (oneStopRoute != null && isRouteValid(pkg, oneStopRoute)) {
            double margin = calculateRouteTimeMargin(pkg, oneStopRoute);
            options.add(new RouteOption(oneStopRoute, margin));
        }
        
        // Buscar rutas con dos escalas
        ArrayList<Flight> twoStopRoute = findTwoStopRoute(origin, destination);
        if (twoStopRoute != null && isRouteValid(pkg, twoStopRoute)) {
            double margin = calculateRouteTimeMargin(pkg, twoStopRoute);
            options.add(new RouteOption(twoStopRoute, margin));
        }
        
        return options;
    }
    
    private ArrayList<Flight> findBestRoute(Package pkg) {
        ArrayList<RouteOption> options = findAllRouteOptions(pkg);
        if (options.isEmpty()) return null;
        
        // Seleccionar la ruta con mayor margen de tiempo
        options.sort((r1, r2) -> Double.compare(r2.timeMargin, r1.timeMargin));
        return options.get(0).route;
    }
    
    private ArrayList<Flight> findRouteWithMaxMargin(Package pkg) {
        return findBestRoute(pkg); // Ya implementado arriba
    }
    
    private ArrayList<Flight> findRouteWithMaxCapacity(Package pkg) {
        ArrayList<RouteOption> options = findAllRouteOptions(pkg);
        if (options.isEmpty()) return null;
        
        // Calcular capacidad disponible para cada ruta
        ArrayList<RouteCapacityOption> capacityOptions = new ArrayList<>();
        for (RouteOption option : options) {
            double totalCapacity = 0;
            double usedCapacity = 0;
            
            for (Flight flight : option.route) {
                totalCapacity += flight.getMaxCapacity();
                usedCapacity += flight.getUsedCapacity();
            }
            
            double availableCapacityRatio = (totalCapacity - usedCapacity) / Math.max(1, totalCapacity);
            capacityOptions.add(new RouteCapacityOption(option.route, availableCapacityRatio, option.timeMargin));
        }
        
        // Ordenar por capacidad disponible, pero considerando también el margen de tiempo
        capacityOptions.sort((r1, r2) -> {
            // Priorizar rutas con capacidad disponible, pero no sacrificar entregas a tiempo
            if (r1.timeMargin <= 0 && r2.timeMargin > 0) return 1;
            if (r2.timeMargin <= 0 && r1.timeMargin > 0) return -1;
            
            return Double.compare(r2.availableCapacity, r1.availableCapacity);
        });
        
        return capacityOptions.get(0).route;
    }
    
    private double calculateRouteTimeMargin(Package pkg, ArrayList<Flight> route) {
        double totalTime = 0;
        
        for (Flight flight : route) {
            totalTime += flight.getTransportTime();
        }
        
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0; // Conexiones
        }
        
        boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == pkg.getDestinationCity().getContinent();
        if (sameContinentRoute) {
            totalTime += Constants.SAME_CONTINENT_TRANSPORT_TIME;
        } else {
            totalTime += Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDeadline = ChronoUnit.HOURS.between(now, pkg.getDeliveryDeadline());
        double margin = hoursUntilDeadline - totalTime;
        
        return Math.max(margin * 10, 1.0);
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
        
        // Verificar continuidad de ruta
        City currentLocation = pkg.getCurrentLocation();
        for (Flight flight : route) {
            Airport currentAirport = getAirportByCity(currentLocation);
            if (!flight.getOriginAirport().equals(currentAirport)) {
                return false;
            }
            currentLocation = getCityByAirport(flight.getDestinationAirport());
        }
        
        if (!currentLocation.equals(pkg.getDestinationCity())) {
            return false;
        }
        
        // Verificar deadline
        return isDeadlineRespected(pkg, route);
    }
    
    private boolean isDeadlineRespected(Package pkg, ArrayList<Flight> route) {
        double totalTime = 0;
        
        for (Flight flight : route) {
            totalTime += flight.getTransportTime();
        }
        
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0;
        }
        
        City origin = pkg.getCurrentLocation();
        City destination = pkg.getDestinationCity();
        boolean sameContinentRoute = origin.getContinent() == destination.getContinent();
        
        if (sameContinentRoute) {
            totalTime += Constants.SAME_CONTINENT_TRANSPORT_TIME;
        } else {
            totalTime += Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME;
        }
        
        // Factor de seguridad
        if (random != null) {
            int complexityFactor = route.size() + (sameContinentRoute ? 0 : 2);
            double safetyMargin = 0.01 * (1 + random.nextInt(complexityFactor * 3));
            totalTime = totalTime * (1.0 + safetyMargin);
        }
        
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDeadline = ChronoUnit.HOURS.between(now, pkg.getDeliveryDeadline());
        
        return totalTime <= hoursUntilDeadline;
    }
    
    // Métodos de búsqueda de rutas (simplificados, podrían referenciar a Solution.java)
    private ArrayList<Flight> findDirectRoute(City origin, City destination) {
        Airport originAirport = getAirportByCity(origin);
        Airport destinationAirport = getAirportByCity(destination);
        
        if (originAirport == null || destinationAirport == null) return null;
        
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
        
        if (originAirport == null || destinationAirport == null) return null;
        
        // Crear lista de aeropuertos intermedios y barajarla
        ArrayList<Airport> intermediates = new ArrayList<>();
        for (Airport airport : airports) {
            if (!airport.equals(originAirport) && !airport.equals(destinationAirport)) {
                intermediates.add(airport);
            }
        }
        Collections.shuffle(intermediates, random);
        
        for (Airport intermediate : intermediates) {
            Flight firstFlight = null;
            Flight secondFlight = null;
            
            // Buscar primer segmento
            for (Flight flight : flights) {
                if (flight.getOriginAirport().equals(originAirport) && 
                    flight.getDestinationAirport().equals(intermediate) &&
                    flight.getUsedCapacity() < flight.getMaxCapacity()) {
                    firstFlight = flight;
                    break;
                }
            }
            
            if (firstFlight == null) continue;
            
            // Buscar segundo segmento
            for (Flight flight : flights) {
                if (flight.getOriginAirport().equals(intermediate) && 
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
        
        if (originAirport == null || destinationAirport == null) return null;
        
        // Simplificado: buscar solo algunas combinaciones aleatorias para eficiencia
        ArrayList<Airport> candidates = new ArrayList<>();
        for (Airport airport : airports) {
            if (!airport.equals(originAirport) && !airport.equals(destinationAirport)) {
                candidates.add(airport);
            }
        }
        
        if (candidates.size() < 2) return null;
        
        Collections.shuffle(candidates, random);
        int maxTries = Math.min(10, candidates.size() - 1);
        
        for (int i = 0; i < maxTries; i++) {
            Airport first = candidates.get(i);
            for (int j = i + 1; j < Math.min(i + 5, candidates.size()); j++) {
                Airport second = candidates.get(j);
                
                ArrayList<Flight> route = tryTwoStopRoute(originAirport, first, second, destinationAirport);
                if (route != null) return route;
                
                // También probar en orden inverso
                route = tryTwoStopRoute(originAirport, second, first, destinationAirport);
                if (route != null) return route;
            }
        }
        
        return null;
    }
    
    private ArrayList<Flight> tryTwoStopRoute(Airport origin, Airport first, Airport second, Airport destination) {
        Flight flight1 = null, flight2 = null, flight3 = null;
        
        // Buscar vuelo 1: origin -> first
        for (Flight flight : flights) {
            if (flight.getOriginAirport().equals(origin) && 
                flight.getDestinationAirport().equals(first) &&
                flight.getUsedCapacity() < flight.getMaxCapacity()) {
                flight1 = flight;
                break;
            }
        }
        
        if (flight1 == null) return null;
        
        // Buscar vuelo 2: first -> second
        for (Flight flight : flights) {
            if (flight.getOriginAirport().equals(first) && 
                flight.getDestinationAirport().equals(second) &&
                flight.getUsedCapacity() < flight.getMaxCapacity()) {
                flight2 = flight;
                break;
            }
        }
        
        if (flight2 == null) return null;
        
        // Buscar vuelo 3: second -> destination
        for (Flight flight : flights) {
            if (flight.getOriginAirport().equals(second) && 
                flight.getDestinationAirport().equals(destination) &&
                flight.getUsedCapacity() < flight.getMaxCapacity()) {
                flight3 = flight;
                break;
            }
        }
        
        if (flight3 != null) {
            ArrayList<Flight> route = new ArrayList<>();
            route.add(flight1);
            route.add(flight2);
            route.add(flight3);
            return route;
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
    
    private City getCityByAirport(Airport airport) {
        return airport.getCity();
    }
    
    private boolean hasWarehouseCapacity(Airport destinationAirport, int productCount) {
        if (destinationAirport.getWarehouse() == null) {
            return false;
        }
        
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        return (currentOccupancy + productCount) <= destinationAirport.getWarehouse().getMaxCapacity();
    }
    
    private void updateFlightCapacities(ArrayList<Flight> route, int productCount) {
        for (Flight flight : route) {
            flight.setUsedCapacity(flight.getUsedCapacity() + productCount);
        }
    }
    
    private void incrementWarehouseOccupancy(Airport airport, int productCount) {
        int current = warehouseOccupancy.getOrDefault(airport, 0);
        warehouseOccupancy.put(airport, current + productCount);
    }
    
    // ================= CLASES AUXILIARES =================
    
    private static class RouteOption {
        ArrayList<Flight> route;
        double timeMargin;
        
        RouteOption(ArrayList<Flight> route, double timeMargin) {
            this.route = route;
            this.timeMargin = timeMargin;
        }
    }
    
    private static class RouteCapacityOption {
        ArrayList<Flight> route;
        double availableCapacity;
        double timeMargin;
        
        RouteCapacityOption(ArrayList<Flight> route, double availableCapacity, double timeMargin) {
            this.route = route;
            this.availableCapacity = availableCapacity;
            this.timeMargin = timeMargin;
        }
    }
    
    /**
     * Clase para encapsular el resultado de una operación de reparación
     */
    public static class RepairResult {
        private HashMap<Package, ArrayList<Flight>> repairedSolution;
        private ArrayList<Package> unassignedPackages;
        
        public RepairResult(HashMap<Package, ArrayList<Flight>> repairedSolution,
                           ArrayList<Package> unassignedPackages) {
            this.repairedSolution = repairedSolution;
            this.unassignedPackages = unassignedPackages;
        }
        
        public HashMap<Package, ArrayList<Flight>> getRepairedSolution() {
            return repairedSolution;
        }
        
        public ArrayList<Package> getUnassignedPackages() {
            return unassignedPackages;
        }
        
        public int getNumRepairedPackages() {
            return repairedSolution.size();
        }
        
        public boolean isSuccess() {
            return !repairedSolution.isEmpty() || unassignedPackages.isEmpty();
        }
        
        public int getNumUnassignedPackages() {
            return unassignedPackages.size();
        }
    }
}
