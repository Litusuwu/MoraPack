package com.system.morapack.schemas.algorithm.TabuSearch;

import com.system.morapack.schemas.AirportSchema;
import com.system.morapack.schemas.FlightSchema;
import com.system.morapack.schemas.OrderSchema;
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
    private final ArrayList<FlightSchema> flightSchemas;
    private final ArrayList<AirportSchema> airportSchemas;
    private final Map<String, AirportSchema> cityToAirportMap;
    private final Random random;
    private final int maxNeighborhoodSize;
    
    public NeighborhoodGenerator(ArrayList<FlightSchema> flightSchemas, ArrayList<AirportSchema> airportSchemas, Map<String, AirportSchema> cityToAirportMap, int maxNeighborhoodSize) {
        this.flightSchemas = flightSchemas;
        this.airportSchemas = airportSchemas;
        this.cityToAirportMap = cityToAirportMap;
        this.random = new Random();
        this.maxNeighborhoodSize = maxNeighborhoodSize;
    }
    
    /**
     * Genera movimientos de inserción para paquetes no asignados
     */
    public List<TabuMove> generateInsertMoves(HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution, List<OrderSchema> unassignedOrderSchemas, int maxMoves) {
        List<TabuMove> moves = new ArrayList<>();
        
        // Limitar el número de paquetes a considerar para inserción
        List<OrderSchema> candidateOrderSchemas = new ArrayList<>(unassignedOrderSchemas);
        if (candidateOrderSchemas.size() > maxMoves) {
            Collections.shuffle(candidateOrderSchemas, random);
            candidateOrderSchemas = candidateOrderSchemas.subList(0, maxMoves);
        }
        
        for (OrderSchema pkg : candidateOrderSchemas) {
            // Buscar una ruta viable para este paquete
            ArrayList<ArrayList<FlightSchema>> candidateRoutes = findViableRoutes(pkg, currentSolution);
            
            // Si encontramos rutas, crear movimientos de inserción
            for (ArrayList<FlightSchema> route : candidateRoutes) {
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
    public List<TabuMove> generateRemoveMoves(HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution, int maxMoves) {
        List<TabuMove> moves = new ArrayList<>();
        
        // Limitar el número de paquetes a considerar para eliminación
        List<OrderSchema> candidateOrderSchemas = new ArrayList<>(currentSolution.keySet());
        if (candidateOrderSchemas.size() > maxMoves) {
            Collections.shuffle(candidateOrderSchemas, random);
            candidateOrderSchemas = candidateOrderSchemas.subList(0, maxMoves);
        }
        
        for (OrderSchema pkg : candidateOrderSchemas) {
            // Crear un movimiento de eliminación
            ArrayList<FlightSchema> route = currentSolution.get(pkg);
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
    public List<TabuMove> generateReassignMoves(HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution, int maxMoves) {
        List<TabuMove> moves = new ArrayList<>();
        
        // Limitar el número de paquetes a considerar para reasignación
        List<OrderSchema> candidateOrderSchemas = new ArrayList<>(currentSolution.keySet());
        if (candidateOrderSchemas.size() > maxMoves) {
            Collections.shuffle(candidateOrderSchemas, random);
            candidateOrderSchemas = candidateOrderSchemas.subList(0, maxMoves);
        }
        
        for (OrderSchema pkg : candidateOrderSchemas) {
            ArrayList<FlightSchema> currentRoute = currentSolution.get(pkg);
            
            // Buscar rutas alternativas
            ArrayList<ArrayList<FlightSchema>> alternativeRoutes = findAlternativeRoutes(pkg, currentRoute, currentSolution);
            
            for (ArrayList<FlightSchema> newRoute : alternativeRoutes) {
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
    public List<TabuMove> generateSwapMoves(HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution, int maxMoves) {
        List<TabuMove> moves = new ArrayList<>();
        
        List<OrderSchema> assignedOrderSchemas = new ArrayList<>(currentSolution.keySet());
        if (assignedOrderSchemas.size() < 2) {
            return moves; // No hay suficientes paquetes para intercambiar
        }
        
        // Limitar el número de intercambios a considerar
        int totalPossibleSwaps = assignedOrderSchemas.size() * (assignedOrderSchemas.size() - 1) / 2;
        int swapsToCheck = Math.min(maxMoves, totalPossibleSwaps);
        
        // Generar pares aleatorios de paquetes para intercambiar
        for (int i = 0; i < swapsToCheck; i++) {
            // Seleccionar dos paquetes aleatorios diferentes
            int idx1 = random.nextInt(assignedOrderSchemas.size());
            int idx2;
            do {
                idx2 = random.nextInt(assignedOrderSchemas.size());
            } while (idx1 == idx2);
            
            OrderSchema pkg1 = assignedOrderSchemas.get(idx1);
            OrderSchema pkg2 = assignedOrderSchemas.get(idx2);
            
            // Verificar si las rutas son intercambiables
            ArrayList<FlightSchema> route1 = currentSolution.get(pkg1);
            ArrayList<FlightSchema> route2 = currentSolution.get(pkg2);
            
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
        HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution = solution.getSolution();
        List<OrderSchema> unassignedOrderSchemas = solution.getUnassignedPackages();
        
        // Limitar el número de movimientos a generar por tipo
        int solutionSize = currentSolution.size();
        int unassignedSize = unassignedOrderSchemas.size();
        
        // Calcular proporción de cada tipo de movimiento
        int maxInserts = Math.min(30, unassignedSize);
        int maxRemoves = Math.min(20, solutionSize);
        int maxReassigns = Math.min(40, solutionSize);
        int maxSwaps = Math.min(30, solutionSize);
        
        // Generar cada tipo de movimiento
        List<TabuMove> insertMoves = generateInsertMoves(currentSolution, unassignedOrderSchemas, maxInserts);
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
    private ArrayList<ArrayList<FlightSchema>> findViableRoutes(OrderSchema pkg, HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution) {
        ArrayList<ArrayList<FlightSchema>> viableRoutes = new ArrayList<>();
        
        // 1. Intentar rutas directas
        ArrayList<FlightSchema> directRoute = findDirectRoute(pkg);
        if (directRoute != null && isRouteValidWithSolution(pkg, directRoute, currentSolution)) {
            viableRoutes.add(directRoute);
        }
        
        // 2. Intentar rutas con una escala
        ArrayList<ArrayList<FlightSchema>> oneStopRoutes = findOneStopRoutes(pkg);
        for (ArrayList<FlightSchema> route : oneStopRoutes) {
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
            ArrayList<ArrayList<FlightSchema>> twoStopRoutes = findTwoStopRoutes(pkg);
            for (ArrayList<FlightSchema> route : twoStopRoutes) {
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
    private ArrayList<ArrayList<FlightSchema>> findAlternativeRoutes(OrderSchema pkg, ArrayList<FlightSchema> currentRoute, HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution) {
        ArrayList<ArrayList<FlightSchema>> alternativeRoutes = new ArrayList<>();
        
        // Generar rutas alternativas
        ArrayList<ArrayList<FlightSchema>> allPossibleRoutes = new ArrayList<>();
        
        // Intentar rutas directas
        ArrayList<FlightSchema> directRoute = findDirectRoute(pkg);
        if (directRoute != null && !directRoute.equals(currentRoute)) {
            allPossibleRoutes.add(directRoute);
        }
        
        // Intentar rutas con una escala
        ArrayList<ArrayList<FlightSchema>> oneStopRoutes = findOneStopRoutes(pkg);
        for (ArrayList<FlightSchema> route : oneStopRoutes) {
            if (!route.equals(currentRoute)) {
                allPossibleRoutes.add(route);
            }
        }
        
        // Intentar rutas con dos escalas (limitado)
        if (allPossibleRoutes.size() < 3) {
            ArrayList<ArrayList<FlightSchema>> twoStopRoutes = findTwoStopRoutes(pkg);
            for (ArrayList<FlightSchema> route : twoStopRoutes) {
                if (!route.equals(currentRoute)) {
                    allPossibleRoutes.add(route);
                    
                    if (allPossibleRoutes.size() >= 5) {
                        break;
                    }
                }
            }
        }
        
        // Verificar validez de las rutas alternativas
        for (ArrayList<FlightSchema> route : allPossibleRoutes) {
            // Crear una copia de la solución actual con el paquete reasignado para validar
            HashMap<OrderSchema, ArrayList<FlightSchema>> tempSolution = new HashMap<>(currentSolution);
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
    private ArrayList<FlightSchema> findDirectRoute(OrderSchema pkg) {
        AirportSchema originAirportSchema = getAirportByCity(pkg.getCurrentLocation().getName());
        AirportSchema destAirportSchema = getAirportByCity(pkg.getDestinationCitySchema().getName());
        
        if (originAirportSchema == null || destAirportSchema == null) {
            return null;
        }
        
        // Buscar un vuelo directo
        for (FlightSchema flightSchema : flightSchemas) {
            if (flightSchema.getOriginAirportSchema().equals(originAirportSchema) &&
                flightSchema.getDestinationAirportSchema().equals(destAirportSchema)) {
                
                ArrayList<FlightSchema> route = new ArrayList<>();
                route.add(flightSchema);
                return route;
            }
        }
        
        return null;
    }
    
    /**
     * Encuentra rutas con una escala para un paquete
     */
    private ArrayList<ArrayList<FlightSchema>> findOneStopRoutes(OrderSchema pkg) {
        ArrayList<ArrayList<FlightSchema>> routes = new ArrayList<>();
        AirportSchema originAirportSchema = getAirportByCity(pkg.getCurrentLocation().getName());
        AirportSchema destAirportSchema = getAirportByCity(pkg.getDestinationCitySchema().getName());
        
        if (originAirportSchema == null || destAirportSchema == null) {
            return routes;
        }
        
        // Buscar intermedios potenciales
        List<AirportSchema> intermediates = new ArrayList<>(airportSchemas);
        Collections.shuffle(intermediates, random);
        
        // Limitar búsqueda a un número razonable de intermedios
        int maxIntermediates = Math.min(10, intermediates.size());
        
        for (int i = 0; i < maxIntermediates; i++) {
            AirportSchema intermediate = intermediates.get(i);
            
            // Evitar usar origen o destino como intermedio
            if (intermediate.equals(originAirportSchema) || intermediate.equals(destAirportSchema)) {
                continue;
            }
            
            FlightSchema firstLeg = null;
            FlightSchema secondLeg = null;
            
            // Buscar primer tramo
            for (FlightSchema flightSchema : flightSchemas) {
                if (flightSchema.getOriginAirportSchema().equals(originAirportSchema) &&
                    flightSchema.getDestinationAirportSchema().equals(intermediate)) {
                    
                    firstLeg = flightSchema;
                    break;
                }
            }
            
            // Si encontramos primer tramo, buscar segundo tramo
            if (firstLeg != null) {
                for (FlightSchema flightSchema : flightSchemas) {
                    if (flightSchema.getOriginAirportSchema().equals(intermediate) &&
                        flightSchema.getDestinationAirportSchema().equals(destAirportSchema)) {
                        
                        secondLeg = flightSchema;
                        break;
                    }
                }
                
                // Si encontramos ambos tramos, añadir la ruta
                if (secondLeg != null) {
                    ArrayList<FlightSchema> route = new ArrayList<>();
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
    private ArrayList<ArrayList<FlightSchema>> findTwoStopRoutes(OrderSchema pkg) {
        ArrayList<ArrayList<FlightSchema>> routes = new ArrayList<>();
        AirportSchema originAirportSchema = getAirportByCity(pkg.getCurrentLocation().getName());
        AirportSchema destAirportSchema = getAirportByCity(pkg.getDestinationCitySchema().getName());
        
        if (originAirportSchema == null || destAirportSchema == null) {
            return routes;
        }
        
        // Buscar intermedios potenciales
        List<AirportSchema> intermediates = new ArrayList<>(airportSchemas);
        Collections.shuffle(intermediates, random);
        
        // Limitar búsqueda a un número razonable de intermedios
        int maxFirstIntermediates = Math.min(5, intermediates.size());
        
        for (int i = 0; i < maxFirstIntermediates; i++) {
            AirportSchema firstIntermediate = intermediates.get(i);
            
            // Evitar usar origen o destino como primer intermedio
            if (firstIntermediate.equals(originAirportSchema) || firstIntermediate.equals(destAirportSchema)) {
                continue;
            }
            
            FlightSchema firstLeg = null;
            
            // Buscar primer tramo
            for (FlightSchema flightSchema : flightSchemas) {
                if (flightSchema.getOriginAirportSchema().equals(originAirportSchema) &&
                    flightSchema.getDestinationAirportSchema().equals(firstIntermediate)) {
                    
                    firstLeg = flightSchema;
                    break;
                }
            }
            
            // Si encontramos primer tramo, buscar segundos intermedios
            if (firstLeg != null) {
                int maxSecondIntermediates = Math.min(3, intermediates.size());
                
                for (int j = 0; j < maxSecondIntermediates; j++) {
                    // Asegurar que no repetimos intermedios
                    if (i == j) continue;
                    
                    AirportSchema secondIntermediate = intermediates.get(j);
                    
                    // Evitar usar origen, destino o primer intermedio como segundo intermedio
                    if (secondIntermediate.equals(originAirportSchema) ||
                        secondIntermediate.equals(destAirportSchema) ||
                        secondIntermediate.equals(firstIntermediate)) {
                        continue;
                    }
                    
                    FlightSchema secondLeg = null;
                    
                    // Buscar segundo tramo
                    for (FlightSchema flightSchema : flightSchemas) {
                        if (flightSchema.getOriginAirportSchema().equals(firstIntermediate) &&
                            flightSchema.getDestinationAirportSchema().equals(secondIntermediate)) {
                            
                            secondLeg = flightSchema;
                            break;
                        }
                    }
                    
                    // Si encontramos segundo tramo, buscar tercer tramo
                    if (secondLeg != null) {
                        FlightSchema thirdLeg = null;
                        
                        for (FlightSchema flightSchema : flightSchemas) {
                            if (flightSchema.getOriginAirportSchema().equals(secondIntermediate) &&
                                flightSchema.getDestinationAirportSchema().equals(destAirportSchema)) {
                                
                                thirdLeg = flightSchema;
                                break;
                            }
                        }
                        
                        // Si encontramos los tres tramos, añadir la ruta
                        if (thirdLeg != null) {
                            ArrayList<FlightSchema> route = new ArrayList<>();
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
    private boolean isRouteValidForPackage(OrderSchema pkg, ArrayList<FlightSchema> route) {
        if (route == null || route.isEmpty()) {
            // Si no hay ruta, solo es válido si ya está en destino
            return pkg.getCurrentLocation().getName().equals(pkg.getDestinationCitySchema().getName());
        }
        
        // Verificar origen correcto
        AirportSchema originAirportSchema = getAirportByCity(pkg.getCurrentLocation().getName());
        if (originAirportSchema == null || !route.get(0).getOriginAirportSchema().equals(originAirportSchema)) {
            return false;
        }
        
        // Verificar destino correcto
        AirportSchema destAirportSchema = getAirportByCity(pkg.getDestinationCitySchema().getName());
        if (destAirportSchema == null || !route.get(route.size() - 1).getDestinationAirportSchema().equals(destAirportSchema)) {
            return false;
        }
        
        // Verificar continuidad de la ruta
        for (int i = 0; i < route.size() - 1; i++) {
            if (!route.get(i).getDestinationAirportSchema().equals(route.get(i + 1).getOriginAirportSchema())) {
                return false;
            }
        }
        
        // Verificar tiempo de entrega
        double totalTime = 0;
        for (FlightSchema flightSchema : route) {
            totalTime += flightSchema.getTransportTime();
        }
        
        // Agregar tiempo de conexión entre vuelos
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0; // 2 horas por conexión
        }
        
        // Verificar cumplimiento de deadlines según continente (promesas MoraPack)
        boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == 
                                    pkg.getDestinationCitySchema().getContinent();
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
    private boolean isRouteValidWithSolution(OrderSchema pkg, ArrayList<FlightSchema> route, HashMap<OrderSchema, ArrayList<FlightSchema>> solution) {
        if (!isRouteValidForPackage(pkg, route)) {
            return false;
        }
        
        // Verificar capacidad de vuelos
        int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
        
        // Simular asignación para verificar capacidades
        HashMap<FlightSchema, Integer> flightUsage = new HashMap<>();
        
        // Inicializar con el uso actual
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : solution.entrySet()) {
            OrderSchema existingPkg = entry.getKey();
            ArrayList<FlightSchema> existingRoute = entry.getValue();
            
            // Saltarse el paquete actual si está siendo reasignado
            if (existingPkg.equals(pkg)) {
                continue;
            }
            
            int existingProductCount = existingPkg.getProductSchemas() != null ?
                                     existingPkg.getProductSchemas().size() : 1;
            
            for (FlightSchema flightSchema : existingRoute) {
                flightUsage.put(flightSchema, flightUsage.getOrDefault(flightSchema, 0) + existingProductCount);
            }
        }
        
        // Verificar si la nueva ruta respeta las capacidades
        for (FlightSchema flightSchema : route) {
            int newUsage = flightUsage.getOrDefault(flightSchema, 0) + productCount;
            if (newUsage > flightSchema.getMaxCapacity()) {
                return false;
            }
        }
        
        // También deberíamos verificar capacidades de almacenes, pero lo simplificamos para este ejemplo
        
        return true;
    }
    
    /**
     * Obtiene el aeropuerto por el nombre de la ciudad
     */
    private AirportSchema getAirportByCity(String cityName) {
        if (cityName == null) return null;
        return cityToAirportMap.get(cityName.toLowerCase().trim());
    }
}
