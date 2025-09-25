package com.system.morapack.schemas.algorithm.TabuSearch;

import com.system.morapack.schemas.Airport;
import com.system.morapack.schemas.Flight;
import com.system.morapack.schemas.Package;
import com.system.morapack.config.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.time.temporal.ChronoUnit;

/**
 * Genera movimientos vecinos para explorar el espacio de soluciones
 */
public class NeighborhoodGenerator {
    private final ArrayList<Flight> flights;
    private final ArrayList<Airport> airports;
    private final Map<String, Airport> cityToAirportMap;
    private final Random random;
    private final int maxNeighborhoodSize;
    
    public NeighborhoodGenerator(ArrayList<Flight> flights, ArrayList<Airport> airports, Map<String, Airport> cityToAirportMap, int maxNeighborhoodSize) {
        this.flights = flights;
        this.airports = airports;
        this.cityToAirportMap = cityToAirportMap;
        this.random = new Random();
        this.maxNeighborhoodSize = maxNeighborhoodSize;
    }
    
    /**
     * Genera movimientos de inserción para paquetes no asignados
     */
    public List<TabuMove> generateInsertMoves(HashMap<Package, ArrayList<Flight>> currentSolution, List<Package> unassignedPackages, int maxMoves) {
        List<TabuMove> moves = new ArrayList<>();
        
        // Limitar el número de paquetes a considerar para inserción
        List<Package> candidatePackages = new ArrayList<>(unassignedPackages);
        if (candidatePackages.size() > maxMoves) {
            Collections.shuffle(candidatePackages, random);
            candidatePackages = candidatePackages.subList(0, maxMoves);
        }
        
        for (Package pkg : candidatePackages) {
            // Buscar una ruta viable para este paquete
            ArrayList<ArrayList<Flight>> candidateRoutes = findViableRoutes(pkg, currentSolution);
            
            // Si encontramos rutas, crear movimientos de inserción
            for (ArrayList<Flight> route : candidateRoutes) {
                moves.add(new TabuMove(TabuMove.MoveType.INSERT, pkg, route));
                
                // Limitar el número total de movimientos
                if (moves.size() >= maxNeighborhoodSize) {
                    return moves;
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Genera movimientos de eliminación para paquetes ya asignados
     */
    public List<TabuMove> generateRemoveMoves(HashMap<Package, ArrayList<Flight>> currentSolution, int maxMoves) {
        List<TabuMove> moves = new ArrayList<>();
        
        // Limitar el número de paquetes a considerar para eliminación
        List<Package> candidatePackages = new ArrayList<>(currentSolution.keySet());
        if (candidatePackages.size() > maxMoves) {
            Collections.shuffle(candidatePackages, random);
            candidatePackages = candidatePackages.subList(0, maxMoves);
        }
        
        for (Package pkg : candidatePackages) {
            // Crear un movimiento de eliminación
            ArrayList<Flight> route = currentSolution.get(pkg);
            moves.add(new TabuMove(TabuMove.MoveType.REMOVE, pkg, route));
            
            // Limitar el número total de movimientos
            if (moves.size() >= maxNeighborhoodSize) {
                return moves;
            }
        }
        
        return moves;
    }
    
    /**
     * Genera movimientos de reasignación para paquetes ya asignados
     */
    public List<TabuMove> generateReassignMoves(HashMap<Package, ArrayList<Flight>> currentSolution, int maxMoves) {
        List<TabuMove> moves = new ArrayList<>();
        
        // Limitar el número de paquetes a considerar para reasignación
        List<Package> candidatePackages = new ArrayList<>(currentSolution.keySet());
        if (candidatePackages.size() > maxMoves) {
            Collections.shuffle(candidatePackages, random);
            candidatePackages = candidatePackages.subList(0, maxMoves);
        }
        
        for (Package pkg : candidatePackages) {
            ArrayList<Flight> currentRoute = currentSolution.get(pkg);
            
            // Buscar rutas alternativas
            ArrayList<ArrayList<Flight>> alternativeRoutes = findAlternativeRoutes(pkg, currentRoute, currentSolution);
            
            for (ArrayList<Flight> newRoute : alternativeRoutes) {
                // Crear un movimiento de reasignación
                moves.add(new TabuMove(TabuMove.MoveType.REASSIGN, pkg, currentRoute, newRoute));
                
                // Limitar el número total de movimientos
                if (moves.size() >= maxNeighborhoodSize) {
                    return moves;
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Genera movimientos de intercambio entre dos paquetes
     */
    public List<TabuMove> generateSwapMoves(HashMap<Package, ArrayList<Flight>> currentSolution, int maxMoves) {
        List<TabuMove> moves = new ArrayList<>();
        
        List<Package> assignedPackages = new ArrayList<>(currentSolution.keySet());
        if (assignedPackages.size() < 2) {
            return moves; // No hay suficientes paquetes para intercambiar
        }
        
        // Limitar el número de intercambios a considerar
        int totalPossibleSwaps = assignedPackages.size() * (assignedPackages.size() - 1) / 2;
        int swapsToCheck = Math.min(maxMoves, totalPossibleSwaps);
        
        // Generar pares aleatorios de paquetes para intercambiar
        for (int i = 0; i < swapsToCheck; i++) {
            // Seleccionar dos paquetes aleatorios diferentes
            int idx1 = random.nextInt(assignedPackages.size());
            int idx2;
            do {
                idx2 = random.nextInt(assignedPackages.size());
            } while (idx1 == idx2);
            
            Package pkg1 = assignedPackages.get(idx1);
            Package pkg2 = assignedPackages.get(idx2);
            
            // Verificar si las rutas son intercambiables
            ArrayList<Flight> route1 = currentSolution.get(pkg1);
            ArrayList<Flight> route2 = currentSolution.get(pkg2);
            
            if (isRouteValidForPackage(pkg2, route1) && isRouteValidForPackage(pkg1, route2)) {
                // Crear un movimiento de intercambio
                moves.add(new TabuMove(TabuMove.MoveType.SWAP, pkg1, route1, pkg2, route2));
                
                // Limitar el número total de movimientos
                if (moves.size() >= maxNeighborhoodSize) {
                    return moves;
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Genera un conjunto diversificado de movimientos vecinos
     */
    public List<TabuMove> generateNeighborhood(TabuSolution solution) {
        List<TabuMove> neighborhood = new ArrayList<>();
        
        // Obtener la solución actual y paquetes no asignados
        HashMap<Package, ArrayList<Flight>> currentSolution = solution.getSolution();
        List<Package> unassignedPackages = solution.getUnassignedPackages();
        
        // Limitar el número de movimientos a generar por tipo
        int solutionSize = currentSolution.size();
        int unassignedSize = unassignedPackages.size();
        
        // Calcular proporción de cada tipo de movimiento
        int maxInserts = Math.min(30, unassignedSize);
        int maxRemoves = Math.min(20, solutionSize);
        int maxReassigns = Math.min(40, solutionSize);
        int maxSwaps = Math.min(30, solutionSize);
        
        // Generar cada tipo de movimiento
        List<TabuMove> insertMoves = generateInsertMoves(currentSolution, unassignedPackages, maxInserts);
        List<TabuMove> removeMoves = generateRemoveMoves(currentSolution, maxRemoves);
        List<TabuMove> reassignMoves = generateReassignMoves(currentSolution, maxReassigns);
        List<TabuMove> swapMoves = generateSwapMoves(currentSolution, maxSwaps);
        
        // Combinar todos los movimientos
        neighborhood.addAll(insertMoves);
        neighborhood.addAll(removeMoves);
        neighborhood.addAll(reassignMoves);
        neighborhood.addAll(swapMoves);
        
        // Mezclar para diversificación
        Collections.shuffle(neighborhood, random);
        
        // Limitar tamaño final
        if (neighborhood.size() > maxNeighborhoodSize) {
            neighborhood = neighborhood.subList(0, maxNeighborhoodSize);
        }
        
        return neighborhood;
    }
    
    /**
     * Encuentra rutas viables para un paquete no asignado
     */
    private ArrayList<ArrayList<Flight>> findViableRoutes(Package pkg, HashMap<Package, ArrayList<Flight>> currentSolution) {
        ArrayList<ArrayList<Flight>> viableRoutes = new ArrayList<>();
        
        // 1. Intentar rutas directas
        ArrayList<Flight> directRoute = findDirectRoute(pkg);
        if (directRoute != null && isRouteValidWithSolution(pkg, directRoute, currentSolution)) {
            viableRoutes.add(directRoute);
        }
        
        // 2. Intentar rutas con una escala
        ArrayList<ArrayList<Flight>> oneStopRoutes = findOneStopRoutes(pkg);
        for (ArrayList<Flight> route : oneStopRoutes) {
            if (isRouteValidWithSolution(pkg, route, currentSolution)) {
                viableRoutes.add(route);
                
                // Limitar el número de rutas a considerar
                if (viableRoutes.size() >= 5) {
                    return viableRoutes;
                }
            }
        }
        
        // 3. Intentar rutas con dos escalas (si es necesario)
        if (viableRoutes.isEmpty()) {
            ArrayList<ArrayList<Flight>> twoStopRoutes = findTwoStopRoutes(pkg);
            for (ArrayList<Flight> route : twoStopRoutes) {
                if (isRouteValidWithSolution(pkg, route, currentSolution)) {
                    viableRoutes.add(route);
                    
                    // Limitar el número de rutas a considerar
                    if (viableRoutes.size() >= 3) {
                        return viableRoutes;
                    }
                }
            }
        }
        
        return viableRoutes;
    }
    
    /**
     * Encuentra rutas alternativas para un paquete ya asignado
     */
    private ArrayList<ArrayList<Flight>> findAlternativeRoutes(Package pkg, ArrayList<Flight> currentRoute, HashMap<Package, ArrayList<Flight>> currentSolution) {
        ArrayList<ArrayList<Flight>> alternativeRoutes = new ArrayList<>();
        
        // Generar rutas alternativas
        ArrayList<ArrayList<Flight>> allPossibleRoutes = new ArrayList<>();
        
        // Intentar rutas directas
        ArrayList<Flight> directRoute = findDirectRoute(pkg);
        if (directRoute != null && !directRoute.equals(currentRoute)) {
            allPossibleRoutes.add(directRoute);
        }
        
        // Intentar rutas con una escala
        ArrayList<ArrayList<Flight>> oneStopRoutes = findOneStopRoutes(pkg);
        for (ArrayList<Flight> route : oneStopRoutes) {
            if (!route.equals(currentRoute)) {
                allPossibleRoutes.add(route);
            }
        }
        
        // Intentar rutas con dos escalas (limitado)
        if (allPossibleRoutes.size() < 3) {
            ArrayList<ArrayList<Flight>> twoStopRoutes = findTwoStopRoutes(pkg);
            for (ArrayList<Flight> route : twoStopRoutes) {
                if (!route.equals(currentRoute)) {
                    allPossibleRoutes.add(route);
                    
                    if (allPossibleRoutes.size() >= 5) {
                        break;
                    }
                }
            }
        }
        
        // Verificar validez de las rutas alternativas
        for (ArrayList<Flight> route : allPossibleRoutes) {
            // Crear una copia de la solución actual con el paquete reasignado para validar
            HashMap<Package, ArrayList<Flight>> tempSolution = new HashMap<>(currentSolution);
            tempSolution.put(pkg, route);
            
            if (isRouteValidWithSolution(pkg, route, tempSolution)) {
                alternativeRoutes.add(route);
                
                // Limitar el número de rutas alternativas
                if (alternativeRoutes.size() >= 3) {
                    break;
                }
            }
        }
        
        return alternativeRoutes;
    }
    
    /**
     * Encuentra una ruta directa para un paquete
     */
    private ArrayList<Flight> findDirectRoute(Package pkg) {
        Airport originAirport = getAirportByCity(pkg.getCurrentLocation().getName());
        Airport destAirport = getAirportByCity(pkg.getDestinationCity().getName());
        
        if (originAirport == null || destAirport == null) {
            return null;
        }
        
        // Buscar un vuelo directo
        for (Flight flight : flights) {
            if (flight.getOriginAirport().equals(originAirport) && 
                flight.getDestinationAirport().equals(destAirport)) {
                
                ArrayList<Flight> route = new ArrayList<>();
                route.add(flight);
                return route;
            }
        }
        
        return null;
    }
    
    /**
     * Encuentra rutas con una escala para un paquete
     */
    private ArrayList<ArrayList<Flight>> findOneStopRoutes(Package pkg) {
        ArrayList<ArrayList<Flight>> routes = new ArrayList<>();
        Airport originAirport = getAirportByCity(pkg.getCurrentLocation().getName());
        Airport destAirport = getAirportByCity(pkg.getDestinationCity().getName());
        
        if (originAirport == null || destAirport == null) {
            return routes;
        }
        
        // Buscar intermedios potenciales
        List<Airport> intermediates = new ArrayList<>(airports);
        Collections.shuffle(intermediates, random);
        
        // Limitar búsqueda a un número razonable de intermedios
        int maxIntermediates = Math.min(10, intermediates.size());
        
        for (int i = 0; i < maxIntermediates; i++) {
            Airport intermediate = intermediates.get(i);
            
            // Evitar usar origen o destino como intermedio
            if (intermediate.equals(originAirport) || intermediate.equals(destAirport)) {
                continue;
            }
            
            Flight firstLeg = null;
            Flight secondLeg = null;
            
            // Buscar primer tramo
            for (Flight flight : flights) {
                if (flight.getOriginAirport().equals(originAirport) && 
                    flight.getDestinationAirport().equals(intermediate)) {
                    
                    firstLeg = flight;
                    break;
                }
            }
            
            // Si encontramos primer tramo, buscar segundo tramo
            if (firstLeg != null) {
                for (Flight flight : flights) {
                    if (flight.getOriginAirport().equals(intermediate) && 
                        flight.getDestinationAirport().equals(destAirport)) {
                        
                        secondLeg = flight;
                        break;
                    }
                }
                
                // Si encontramos ambos tramos, añadir la ruta
                if (secondLeg != null) {
                    ArrayList<Flight> route = new ArrayList<>();
                    route.add(firstLeg);
                    route.add(secondLeg);
                    routes.add(route);
                    
                    // Limitar el número de rutas
                    if (routes.size() >= 3) {
                        return routes;
                    }
                }
            }
        }
        
        return routes;
    }
    
    /**
     * Encuentra rutas con dos escalas para un paquete
     */
    private ArrayList<ArrayList<Flight>> findTwoStopRoutes(Package pkg) {
        ArrayList<ArrayList<Flight>> routes = new ArrayList<>();
        Airport originAirport = getAirportByCity(pkg.getCurrentLocation().getName());
        Airport destAirport = getAirportByCity(pkg.getDestinationCity().getName());
        
        if (originAirport == null || destAirport == null) {
            return routes;
        }
        
        // Buscar intermedios potenciales
        List<Airport> intermediates = new ArrayList<>(airports);
        Collections.shuffle(intermediates, random);
        
        // Limitar búsqueda a un número razonable de intermedios
        int maxFirstIntermediates = Math.min(5, intermediates.size());
        
        for (int i = 0; i < maxFirstIntermediates; i++) {
            Airport firstIntermediate = intermediates.get(i);
            
            // Evitar usar origen o destino como primer intermedio
            if (firstIntermediate.equals(originAirport) || firstIntermediate.equals(destAirport)) {
                continue;
            }
            
            Flight firstLeg = null;
            
            // Buscar primer tramo
            for (Flight flight : flights) {
                if (flight.getOriginAirport().equals(originAirport) && 
                    flight.getDestinationAirport().equals(firstIntermediate)) {
                    
                    firstLeg = flight;
                    break;
                }
            }
            
            // Si encontramos primer tramo, buscar segundos intermedios
            if (firstLeg != null) {
                int maxSecondIntermediates = Math.min(3, intermediates.size());
                
                for (int j = 0; j < maxSecondIntermediates; j++) {
                    // Asegurar que no repetimos intermedios
                    if (i == j) continue;
                    
                    Airport secondIntermediate = intermediates.get(j);
                    
                    // Evitar usar origen, destino o primer intermedio como segundo intermedio
                    if (secondIntermediate.equals(originAirport) || 
                        secondIntermediate.equals(destAirport) ||
                        secondIntermediate.equals(firstIntermediate)) {
                        continue;
                    }
                    
                    Flight secondLeg = null;
                    
                    // Buscar segundo tramo
                    for (Flight flight : flights) {
                        if (flight.getOriginAirport().equals(firstIntermediate) && 
                            flight.getDestinationAirport().equals(secondIntermediate)) {
                            
                            secondLeg = flight;
                            break;
                        }
                    }
                    
                    // Si encontramos segundo tramo, buscar tercer tramo
                    if (secondLeg != null) {
                        Flight thirdLeg = null;
                        
                        for (Flight flight : flights) {
                            if (flight.getOriginAirport().equals(secondIntermediate) && 
                                flight.getDestinationAirport().equals(destAirport)) {
                                
                                thirdLeg = flight;
                                break;
                            }
                        }
                        
                        // Si encontramos los tres tramos, añadir la ruta
                        if (thirdLeg != null) {
                            ArrayList<Flight> route = new ArrayList<>();
                            route.add(firstLeg);
                            route.add(secondLeg);
                            route.add(thirdLeg);
                            routes.add(route);
                            
                            // Limitar el número de rutas
                            if (routes.size() >= 2) {
                                return routes;
                            }
                        }
                    }
                }
            }
        }
        
        return routes;
    }
    
    /**
     * Verifica si una ruta es válida para un paquete
     */
    private boolean isRouteValidForPackage(Package pkg, ArrayList<Flight> route) {
        if (route == null || route.isEmpty()) {
            // Si no hay ruta, solo es válido si ya está en destino
            return pkg.getCurrentLocation().getName().equals(pkg.getDestinationCity().getName());
        }
        
        // Verificar origen correcto
        Airport originAirport = getAirportByCity(pkg.getCurrentLocation().getName());
        if (originAirport == null || !route.get(0).getOriginAirport().equals(originAirport)) {
            return false;
        }
        
        // Verificar destino correcto
        Airport destAirport = getAirportByCity(pkg.getDestinationCity().getName());
        if (destAirport == null || !route.get(route.size() - 1).getDestinationAirport().equals(destAirport)) {
            return false;
        }
        
        // Verificar continuidad de la ruta
        for (int i = 0; i < route.size() - 1; i++) {
            if (!route.get(i).getDestinationAirport().equals(route.get(i + 1).getOriginAirport())) {
                return false;
            }
        }
        
        // Verificar tiempo de entrega
        double totalTime = 0;
        for (Flight flight : route) {
            totalTime += flight.getTransportTime();
        }
        
        // Agregar tiempo de conexión entre vuelos
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0; // 2 horas por conexión
        }
        
        // Verificar cumplimiento de deadlines según continente (promesas MoraPack)
        boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == 
                                    pkg.getDestinationCity().getContinent();
        double maxHours = sameContinentRoute ? 
                         Constants.SAME_CONTINENT_MAX_DELIVERY_TIME * 24.0 : 
                         Constants.DIFFERENT_CONTINENT_MAX_DELIVERY_TIME * 24.0;
        
        if (totalTime > maxHours) {
            return false; // Excede promesas MoraPack
        }
        
        // Verificar deadline específico del cliente
        if (pkg.getOrderDate() != null && pkg.getDeliveryDeadline() != null) {
            long hoursUntilDeadline = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
            return totalTime <= hoursUntilDeadline;
        }
        
        return true;
    }
    
    /**
     * Verifica si una ruta es válida en el contexto de la solución actual
     */
    private boolean isRouteValidWithSolution(Package pkg, ArrayList<Flight> route, HashMap<Package, ArrayList<Flight>> solution) {
        if (!isRouteValidForPackage(pkg, route)) {
            return false;
        }
        
        // Verificar capacidad de vuelos
        int productCount = pkg.getProducts() != null ? pkg.getProducts().size() : 1;
        
        // Simular asignación para verificar capacidades
        HashMap<Flight, Integer> flightUsage = new HashMap<>();
        
        // Inicializar con el uso actual
        for (Map.Entry<Package, ArrayList<Flight>> entry : solution.entrySet()) {
            Package existingPkg = entry.getKey();
            ArrayList<Flight> existingRoute = entry.getValue();
            
            // Saltarse el paquete actual si está siendo reasignado
            if (existingPkg.equals(pkg)) {
                continue;
            }
            
            int existingProductCount = existingPkg.getProducts() != null ? 
                                     existingPkg.getProducts().size() : 1;
            
            for (Flight flight : existingRoute) {
                flightUsage.put(flight, flightUsage.getOrDefault(flight, 0) + existingProductCount);
            }
        }
        
        // Verificar si la nueva ruta respeta las capacidades
        for (Flight flight : route) {
            int newUsage = flightUsage.getOrDefault(flight, 0) + productCount;
            if (newUsage > flight.getMaxCapacity()) {
                return false;
            }
        }
        
        // También deberíamos verificar capacidades de almacenes, pero lo simplificamos para este ejemplo
        
        return true;
    }
    
    /**
     * Obtiene el aeropuerto por el nombre de la ciudad
     */
    private Airport getAirportByCity(String cityName) {
        if (cityName == null) return null;
        return cityToAirportMap.get(cityName.toLowerCase().trim());
    }
}
