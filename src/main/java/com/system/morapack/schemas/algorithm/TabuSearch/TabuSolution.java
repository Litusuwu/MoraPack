package com.system.morapack.schemas.algorithm.TabuSearch;

import com.system.morapack.schemas.Airport;
import com.system.morapack.schemas.City;
import com.system.morapack.schemas.Flight;
import com.system.morapack.schemas.Package;
import com.system.morapack.schemas.Continent;
import com.system.morapack.config.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Representa una solución para el problema de asignación de paquetes
 * Incluye métodos para aplicar movimientos y evaluar soluciones
 */
public class TabuSolution {
    private HashMap<Package, ArrayList<Flight>> solution;
    private ArrayList<Package> allPackages;
    private ArrayList<Package> unassignedPackages;
    private ArrayList<Airport> airports;
    private ArrayList<Flight> flights;
    private Map<String, Airport> cityToAirportMap;
    private HashMap<Airport, Integer> warehouseOccupancy;
    private int score;
    private boolean scoreOutdated;
    
    /**
     * Constructor que inicializa una solución vacía
     */
    public TabuSolution(ArrayList<Package> allPackages, ArrayList<Airport> airports, ArrayList<Flight> flights) {
        this.solution = new HashMap<>();
        this.allPackages = new ArrayList<>(allPackages);
        this.unassignedPackages = new ArrayList<>(allPackages);
        this.airports = airports;
        this.flights = flights;
        this.cityToAirportMap = buildCityToAirportMap();
        this.warehouseOccupancy = new HashMap<>();
        this.scoreOutdated = true;
        
        initializeWarehouseOccupancy();
    }
    
    /**
     * Constructor de copia para crear una nueva solución basada en otra
     */
    public TabuSolution(TabuSolution other) {
        this.solution = new HashMap<>();
        
        // Copiar solución
        for (Map.Entry<Package, ArrayList<Flight>> entry : other.solution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            this.solution.put(pkg, new ArrayList<>(route));
        }
        
        // Copiar otras estructuras
        this.allPackages = new ArrayList<>(other.allPackages);
        this.unassignedPackages = new ArrayList<>(other.unassignedPackages);
        this.airports = other.airports;
        this.flights = other.flights;
        this.cityToAirportMap = other.cityToAirportMap;
        
        // Copiar ocupación de almacenes
        this.warehouseOccupancy = new HashMap<>(other.warehouseOccupancy);
        
        // Copiar score si está actualizado
        if (!other.scoreOutdated) {
            this.score = other.score;
            this.scoreOutdated = false;
        } else {
            this.scoreOutdated = true;
        }
    }
    
    /**
     * Crea un mapa de nombres de ciudad a aeropuertos
     */
    private Map<String, Airport> buildCityToAirportMap() {
        Map<String, Airport> map = new HashMap<>();
        for (Airport airport : airports) {
            if (airport.getCity() != null && airport.getCity().getName() != null) {
                map.put(airport.getCity().getName().toLowerCase().trim(), airport);
            }
        }
        return map;
    }
    
    /**
     * Inicializa la ocupación de almacenes
     */
    private void initializeWarehouseOccupancy() {
        for (Airport airport : airports) {
            warehouseOccupancy.put(airport, 0);
        }
    }
    
    /**
     * Aplica un movimiento a la solución
     */
    public boolean applyMove(TabuMove move) {
        try {
            switch (move.getMoveType()) {
                case INSERT:
                    return insertPackage(move.getPrimaryPackage(), move.getPrimaryRoute());
                case REMOVE:
                    return removePackage(move.getPrimaryPackage());
                case REASSIGN:
                    return reassignPackage(move.getPrimaryPackage(), move.getSecondaryRoute());
                case SWAP:
                    return swapPackages(move.getPrimaryPackage(), move.getSecondaryPackage());
                default:
                    return false;
            }
        } catch (Exception e) {
            System.err.println("Error al aplicar movimiento: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Inserta un paquete en la solución
     */
    private boolean insertPackage(Package pkg, ArrayList<Flight> route) {
        if (pkg == null || !unassignedPackages.contains(pkg)) {
            return false;
        }
        
        // Verificar capacidad de vuelos
        int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
        if (!checkFlightCapacity(route, productCount)) {
            return false;
        }
        
        // Verificar capacidad de almacén en destino
        Airport destinationAirport = getAirportByCity(pkg.getDestinationCity().getName());
        if (destinationAirport == null || !checkWarehouseCapacity(destinationAirport, productCount)) {
            return false;
        }
        
        // Insertar paquete
        solution.put(pkg, new ArrayList<>(route));
        unassignedPackages.remove(pkg);
        
        // Actualizar capacidades
        updateFlightCapacities(route, productCount);
        updateWarehouseOccupancy(destinationAirport, productCount);
        
        // Marcar score como desactualizado
        scoreOutdated = true;
        
        return true;
    }
    
    /**
     * Elimina un paquete de la solución
     */
    private boolean removePackage(Package pkg) {
        if (pkg == null || !solution.containsKey(pkg)) {
            return false;
        }
        
        // Obtener ruta actual
        ArrayList<Flight> route = solution.get(pkg);
        int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
        
        // Eliminar paquete
        solution.remove(pkg);
        unassignedPackages.add(pkg);
        
        // Actualizar capacidades
        updateFlightCapacities(route, -productCount);
        
        Airport destinationAirport = getAirportByCity(pkg.getDestinationCity().getName());
        if (destinationAirport != null) {
            updateWarehouseOccupancy(destinationAirport, -productCount);
        }
        
        // Marcar score como desactualizado
        scoreOutdated = true;
        
        return true;
    }
    
    /**
     * Reasigna un paquete a una nueva ruta
     */
    private boolean reassignPackage(Package pkg, ArrayList<Flight> newRoute) {
        if (pkg == null || !solution.containsKey(pkg)) {
            return false;
        }
        
        // Obtener ruta actual
        ArrayList<Flight> oldRoute = solution.get(pkg);
        int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
        
        // Verificar capacidad de vuelos en la nueva ruta
        if (!checkFlightCapacity(newRoute, productCount)) {
            return false;
        }
        
        // Eliminar capacidad utilizada por la ruta anterior
        updateFlightCapacities(oldRoute, -productCount);
        
        // Asignar nueva ruta
        solution.put(pkg, new ArrayList<>(newRoute));
        
        // Actualizar capacidades de vuelos en la nueva ruta
        updateFlightCapacities(newRoute, productCount);
        
        // No es necesario actualizar almacenes si el destino es el mismo
        
        // Marcar score como desactualizado
        scoreOutdated = true;
        
        return true;
    }
    
    /**
     * Intercambia rutas entre dos paquetes
     */
    private boolean swapPackages(Package pkg1, Package pkg2) {
        if (pkg1 == null || pkg2 == null || 
            !solution.containsKey(pkg1) || !solution.containsKey(pkg2)) {
            return false;
        }
        
        // Obtener rutas actuales
        ArrayList<Flight> route1 = solution.get(pkg1);
        ArrayList<Flight> route2 = solution.get(pkg2);
        
        int productCount1 = pkg1.getProducts() != null ? pkg1.getProducts().size() : 1;
        int productCount2 = pkg2.getProducts() != null ? pkg2.getProducts().size() : 1;
        
        // Verificar capacidad de vuelos después del intercambio
        ArrayList<Flight> tempRoute1 = new ArrayList<>(route1);
        ArrayList<Flight> tempRoute2 = new ArrayList<>(route2);
        
        // Liberar capacidad actual
        updateFlightCapacities(route1, -productCount1);
        updateFlightCapacities(route2, -productCount2);
        
        // Verificar capacidad para la nueva asignación
        boolean canAssignPkg2ToRoute1 = checkFlightCapacity(tempRoute1, productCount2);
        boolean canAssignPkg1ToRoute2 = checkFlightCapacity(tempRoute2, productCount1);
        
        if (!canAssignPkg2ToRoute1 || !canAssignPkg1ToRoute2) {
            // Restaurar capacidades
            updateFlightCapacities(route1, productCount1);
            updateFlightCapacities(route2, productCount2);
            return false;
        }
        
        // Realizar intercambio
        solution.put(pkg1, tempRoute2);
        solution.put(pkg2, tempRoute1);
        
        // Actualizar capacidades
        updateFlightCapacities(tempRoute1, productCount2);
        updateFlightCapacities(tempRoute2, productCount1);
        
        // Marcar score como desactualizado
        scoreOutdated = true;
        
        return true;
    }
    
    /**
     * Verifica si hay capacidad suficiente en los vuelos
     */
    private boolean checkFlightCapacity(ArrayList<Flight> route, int productCount) {
        if (route == null || route.isEmpty()) {
            return true; // No hay vuelos, por lo que no hay restricción de capacidad
        }
        
        for (Flight flight : route) {
            int currentUsage = flight.getUsedCapacity();
            if (currentUsage + productCount > flight.getMaxCapacity()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Verifica si hay capacidad suficiente en el almacén
     */
    private boolean checkWarehouseCapacity(Airport airport, int productCount) {
        if (airport == null || airport.getWarehouse() == null) {
            return false;
        }
        
        int currentOccupancy = warehouseOccupancy.getOrDefault(airport, 0);
        return (currentOccupancy + productCount) <= airport.getWarehouse().getMaxCapacity();
    }
    
    /**
     * Actualiza la capacidad utilizada en los vuelos
     */
    private void updateFlightCapacities(ArrayList<Flight> route, int productCount) {
        if (route == null || route.isEmpty()) {
            return;
        }
        
        for (Flight flight : route) {
            flight.setUsedCapacity(flight.getUsedCapacity() + productCount);
        }
    }
    
    /**
     * Actualiza la ocupación de almacén
     */
    private void updateWarehouseOccupancy(Airport airport, int productCount) {
        if (airport == null) {
            return;
        }
        
        int currentOccupancy = warehouseOccupancy.getOrDefault(airport, 0);
        warehouseOccupancy.put(airport, currentOccupancy + productCount);
    }
    
    /**
     * Evalúa la calidad de la solución
     */
    public int evaluate() {
        if (!scoreOutdated) {
            return score;
        }
        
        // El peso de la solución considera múltiples factores:
        // 1. Número total de paquetes asignados (maximizar)
        // 2. Número total de productos transportados (maximizar)
        // 3. Cumplimiento de deadlines (maximizar)
        // 4. Tiempo total de entrega (minimizar)
        // 5. Utilización de capacidad de vuelos (maximizar)
        // 6. Margen de seguridad antes de deadline (maximizar)
        
        int totalPackages = solution.size();
        int totalProducts = 0;
        double totalDeliveryTime = 0;
        int onTimeDeliveries = 0;
        double totalCapacityUtilization = 0;
        int totalFlightsUsed = 0;
        double totalDeliveryMargin = 0;
        
        // Calcular métricas
        for (Map.Entry<Package, ArrayList<Flight>> entry : solution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            // Contar productos
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
                long availableHours = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
                double marginHours = availableHours - routeTime;
                totalDeliveryMargin += marginHours;
            }
        }
        
        // Calcular métricas promedio
        double avgDeliveryTime = totalPackages > 0 ? totalDeliveryTime / totalPackages : 0;
        double avgCapacityUtilization = totalFlightsUsed > 0 ? totalCapacityUtilization / totalFlightsUsed : 0;
        double onTimeRate = totalPackages > 0 ? (double) onTimeDeliveries / totalPackages : 0;
        double avgDeliveryMargin = onTimeDeliveries > 0 ? totalDeliveryMargin / onTimeDeliveries : 0;
        
        // Componentes adicionales
        double continentalEfficiency = calculateContinentalEfficiency();
        double warehouseUtilization = calculateWarehouseUtilization();
        
        // Peso final optimizado para priorizar cantidad de paquetes asignados
        score = (int) (
            // PRIORIDAD ABSOLUTA: Cantidad de paquetes y productos (MAXIMIZAR)
            totalPackages * 100000 +                 // 100,000 puntos por paquete
            totalProducts * 10000 +                  // 10,000 puntos por producto
            
            // FACTOR CALIDAD: On-time como multiplicador
            onTimeRate * 5000 +                      // 5,000 puntos máximo por calidad on-time
            
            // EFICIENCIA OPERATIVA (secundaria)
            Math.min(avgDeliveryMargin * 50, 1000) + // Margen de seguridad reducido
            continentalEfficiency * 500 +            // Eficiencia continental
            avgCapacityUtilization * 200 +           // Utilización de vuelos
            warehouseUtilization * 100 +             // Utilización de almacenes
            
            // PENALIZACIONES MENORES
            - avgDeliveryTime * 20 -                 // Penalización tiempo reducida
            - calculateRoutingComplexity() * 50      // Penalización complejidad reducida
        );
        
        // PENALIZACIÓN: Solo si on-time rate es muy bajo (<80%)
        if (onTimeRate < 0.8) {
            score = (int)(score * 0.5); // Penalización 50% solo si muy malo
        }
        
        // BONUS por alta calidad (≥95% on-time)
        if (onTimeRate >= 0.95 && totalPackages > 10) {
            score = (int)(score * 1.1); // 10% bonus por alta calidad
        }
        
        // BONUS por volumen alto (>1000 paquetes asignados)
        if (totalPackages > 1000) {
            score = (int)(score * 1.15); // 15% bonus por alto volumen
        }
        
        scoreOutdated = false;
        return score;
    }
    
    /**
     * Verifica si se respeta el deadline de un paquete con una ruta
     */
    private boolean isDeadlineRespected(Package pkg, ArrayList<Flight> route) {
        if (pkg == null || pkg.getOrderDate() == null || pkg.getDeliveryDeadline() == null) {
            return false;
        }
        
        double totalTime = 0;
        
        // Calcular tiempo total de la ruta
        for (Flight flight : route) {
            totalTime += flight.getTransportTime();
        }
        
        // Añadir penalización por conexiones (2 horas por conexión)
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0;
        }
        
        // Verificar promesa MoraPack según continentes
        City origin = pkg.getCurrentLocation();
        City destination = pkg.getDestinationCity();
        
        boolean sameContinentRoute = origin.getContinent() == destination.getContinent();
        long moraPackPromiseHours = sameContinentRoute ? 48 : 72; // 2 días intra / 3 días inter
        
        if (totalTime > moraPackPromiseHours) {
            return false; // Excede promesa MoraPack
        }
        
        // Verificar deadline específico del cliente
        long hoursUntilDeadline = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        
        return totalTime <= hoursUntilDeadline;
    }
    
    /**
     * Calcula la eficiencia de rutas continentales vs intercontinentales
     */
    private double calculateContinentalEfficiency() {
        if (solution.isEmpty()) return 0.0;
        
        int sameContinentDirect = 0;
        int sameContinentOneStop = 0;
        int differentContinentDirect = 0;
        int differentContinentOneStop = 0;
        int inefficientRoutes = 0;
        
        for (Map.Entry<Package, ArrayList<Flight>> entry : solution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == 
                                        pkg.getDestinationCity().getContinent();
            
            if (route.isEmpty()) continue;
            
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
    private double calculateRoutingComplexity() {
        if (solution.isEmpty()) return 0.0;
        
        double totalComplexity = 0.0;
        
        for (Map.Entry<Package, ArrayList<Flight>> entry : solution.entrySet()) {
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
    
    /**
     * Obtiene el aeropuerto por el nombre de la ciudad
     */
    private Airport getAirportByCity(String cityName) {
        if (cityName == null) return null;
        return cityToAirportMap.get(cityName.toLowerCase().trim());
    }
    
    /**
     * Getters y setters
     */
    public HashMap<Package, ArrayList<Flight>> getSolution() {
        return solution;
    }
    
    public ArrayList<Package> getUnassignedPackages() {
        return unassignedPackages;
    }
    
    public int getAssignedPackagesCount() {
        return solution.size();
    }
    
    public int getUnassignedPackagesCount() {
        return unassignedPackages.size();
    }
    
    public int getScore() {
        if (scoreOutdated) {
            return evaluate();
        }
        return score;
    }
    
    @Override
    public String toString() {
        return "TabuSolution{" +
               "assigned=" + getAssignedPackagesCount() +
               ", unassigned=" + getUnassignedPackagesCount() +
               ", score=" + getScore() +
               '}';
    }
}
