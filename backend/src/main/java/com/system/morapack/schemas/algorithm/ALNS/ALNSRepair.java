package com.system.morapack.schemas.algorithm.ALNS;

import com.system.morapack.schemas.AirportSchema;
import com.system.morapack.schemas.CitySchema;
import com.system.morapack.schemas.FlightSchema;
import com.system.morapack.schemas.OrderSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import com.system.morapack.config.Constants;

/**
 * Clase que implementa operadores de reparación para el algoritmo ALNS
 * (Adaptive Large Neighborhood Search) específicamente diseñados para el problema
 * de logística MoraPack.
 * 
 * Los operadores priorizan las entregas a tiempo y la eficiencia de rutas.
 */
public class ALNSRepair {
    
    private ArrayList<AirportSchema> airportSchemas;
    private ArrayList<FlightSchema> flightSchemas;
    private HashMap<AirportSchema, Integer> warehouseOccupancy;
    private Random random;
    
    public ALNSRepair(ArrayList<AirportSchema> airportSchemas, ArrayList<FlightSchema> flightSchemas,
                      HashMap<AirportSchema, Integer> warehouseOccupancy) {
        this.airportSchemas = airportSchemas;
        this.flightSchemas = flightSchemas;
        this.warehouseOccupancy = warehouseOccupancy;
        this.random = new Random(System.currentTimeMillis());
    }
    
    /**
     * Constructor con semilla específica
     */
    public ALNSRepair(ArrayList<AirportSchema> airportSchemas, ArrayList<FlightSchema> flightSchemas,
                      HashMap<AirportSchema, Integer> warehouseOccupancy, long seed) {
        this.airportSchemas = airportSchemas;
        this.flightSchemas = flightSchemas;
        this.warehouseOccupancy = warehouseOccupancy;
        this.random = new Random(seed);
    }
    
    /**
     * Reparación Greedy Mejorada: Inserta paquetes usando enfoque optimizado para MoraPack.
     * Prioriza paquetes por deadline y eficiencia de ruta específica para el negocio.
     */
    public RepairResult greedyRepair(
            HashMap<OrderSchema, ArrayList<FlightSchema>> partialSolution,
            ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyedPackages) {
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> repairedSolution = new HashMap<>(partialSolution);
        ArrayList<OrderSchema> unassignedOrderSchemas = new ArrayList<>();
        
        // Ordenamiento inteligente específico para MoraPack
        ArrayList<OrderSchema> packagesToRepair = new ArrayList<>();
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : destroyedPackages) {
            packagesToRepair.add(entry.getKey());
        }
        
        packagesToRepair.sort((p1, p2) -> {
            // 1. Priorizar por urgencia (tiempo restante vs promesa MoraPack)
            double urgency1 = calculatePackageUrgency(p1);
            double urgency2 = calculatePackageUrgency(p2);
            int urgencyComparison = Double.compare(urgency2, urgency1); // Mayor urgencia primero
            if (urgencyComparison != 0) return urgencyComparison;
            
            // 2. Priorizar paquetes con más productos (mayor valor de negocio)
            int products1 = p1.getProductSchemas() != null ? p1.getProductSchemas().size() : 1;
            int products2 = p2.getProductSchemas() != null ? p2.getProductSchemas().size() : 1;
            int productComparison = Integer.compare(products2, products1);
            if (productComparison != 0) return productComparison;
            
            // 3. Tie-break por deadline absoluto
            LocalDateTime d1 = p1.getDeliveryDeadline();
            LocalDateTime d2 = p2.getDeliveryDeadline();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1; // nulls last
            if (d2 == null) return -1; // nulls last
            return d1.compareTo(d2);
        });
        
        int reinsertedCount = 0;
        
        // Intentar reinsertar cada paquete
        for (OrderSchema pkg : packagesToRepair) {
            AirportSchema destinationAirportSchema = getAirportByCity(pkg.getDestinationCitySchema());
            
            // Get product count for this package
            int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
            
            // Verificar capacidad del almacén
            if (destinationAirportSchema == null || !hasWarehouseCapacity(destinationAirportSchema, productCount)) {
                unassignedOrderSchemas.add(pkg);
                continue;
            }
            
            // Buscar la mejor ruta
            ArrayList<FlightSchema> bestRoute = findBestRoute(pkg);
            if (bestRoute != null && isRouteValid(pkg, bestRoute, Math.max(1, productCount))) {
                repairedSolution.put(pkg, bestRoute);
                updateFlightCapacities(bestRoute, productCount);
                incrementWarehouseOccupancy(destinationAirportSchema, productCount);
                reinsertedCount++;
            } else {
                unassignedOrderSchemas.add(pkg);
            }
        }
        if (Constants.VERBOSE_LOGGING) {
          System.out.println("Reparación Greedy: " + reinsertedCount + "/" + packagesToRepair.size() + " paquetes reinsertados");
        }
        
        return new RepairResult(repairedSolution, unassignedOrderSchemas);
    }
    
    /**
     * Reparación por Regret: Calcula el "arrepentimiento" de no insertar cada paquete
     * y prioriza aquellos con mayor diferencia entre mejor y segunda mejor opción.
     */
    public RepairResult regretRepair(
            HashMap<OrderSchema, ArrayList<FlightSchema>> partialSolution,
            ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyedPackages,
            int regretLevel) {
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> repairedSolution = new HashMap<>(partialSolution);
        ArrayList<OrderSchema> unassignedOrderSchemas = new ArrayList<>();
        
        ArrayList<OrderSchema> remainingOrderSchemas = new ArrayList<>();
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : destroyedPackages) {
            remainingOrderSchemas.add(entry.getKey());
        }
        
        int reinsertedCount = 0;
        
        // Mientras haya paquetes por insertar
            while (!remainingOrderSchemas.isEmpty()) {
            OrderSchema bestOrderSchema = null;
            ArrayList<FlightSchema> bestRoute = null;
            double maxRegret = Double.NEGATIVE_INFINITY;
            
            // Calcular regret para cada paquete restante
            for (OrderSchema pkg : remainingOrderSchemas) {
                AirportSchema destinationAirportSchema = getAirportByCity(pkg.getDestinationCitySchema());
                int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
                
                if (destinationAirportSchema == null || !hasWarehouseCapacity(destinationAirportSchema, productCount)) {
                    continue;
                }
                
                // Encontrar las mejores rutas para este paquete
                ArrayList<RouteOption> routeOptions = findAllRouteOptions(pkg);
                
                if (routeOptions.isEmpty()) {
                    continue;
                }
                
                // Ordenar por margen de tiempo (mejor primero)
                routeOptions.sort((r1, r2) -> Double.compare(r2.timeMargin, r1.timeMargin));
                
                // Calcular regret-k real
                double regret = 0;
                int k = Math.max(2, regretLevel);
                int limit = Math.min(k, routeOptions.size());
                if (limit >= 2) {
                    double bestMargin = routeOptions.get(0).timeMargin;
                    for (int i = 1; i < limit; i++) {
                        regret += (bestMargin - routeOptions.get(i).timeMargin);
                    }
                } else if (routeOptions.size() == 1) {
                    // Solo una opción: usar urgencia basada en orderDate→deadline
                    if (pkg.getOrderDate() != null && pkg.getDeliveryDeadline() != null) {
                        long hoursUntilDeadline = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
                        regret = Math.max(0, 72 - Math.min(72, hoursUntilDeadline));
                    } else {
                        regret = 0;
                    }
                }
                
                // Añadir factor de urgencia al regret
                LocalDateTime now = LocalDateTime.now();
                long hoursUntilDeadline = ChronoUnit.HOURS.between(now, pkg.getDeliveryDeadline());
                double urgencyFactor = Math.max(1, 72.0 / Math.max(1, hoursUntilDeadline));
                regret *= urgencyFactor;
                
                if (regret > maxRegret) {
                    maxRegret = regret;
                    bestOrderSchema = pkg;
                    bestRoute = routeOptions.get(0).route;
                }
            }
            
            // Insertar el paquete con mayor regret
            if (bestOrderSchema != null && bestRoute != null && isRouteValid(bestOrderSchema, bestRoute, Math.max(1, bestOrderSchema.getProductSchemas() != null ? bestOrderSchema.getProductSchemas().size() : 1))) {
                repairedSolution.put(bestOrderSchema, bestRoute);
                int productCount = bestOrderSchema.getProductSchemas() != null ? bestOrderSchema.getProductSchemas().size() : 1;
                updateFlightCapacities(bestRoute, productCount);
                incrementWarehouseOccupancy(getAirportByCity(bestOrderSchema.getDestinationCitySchema()), productCount);
                remainingOrderSchemas.remove(bestOrderSchema);
                reinsertedCount++;
            } else {
                // No se pudo insertar ningún paquete, agregar todos los restantes como no asignados
                unassignedOrderSchemas.addAll(remainingOrderSchemas);
                break;
            }
        }
        if (Constants.VERBOSE_LOGGING){
          System.out.println("Reparación por Regret: " + reinsertedCount + "/" + destroyedPackages.size() + " paquetes reinsertados");
        }
        return new RepairResult(repairedSolution, unassignedOrderSchemas);
    }
    
    /**
     * Reparación por tiempo: Prioriza paquetes con deadlines más cercanos.
     */
    public RepairResult timeBasedRepair(
            HashMap<OrderSchema, ArrayList<FlightSchema>> partialSolution,
            ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyedPackages) {
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> repairedSolution = new HashMap<>(partialSolution);
        ArrayList<OrderSchema> unassignedOrderSchemas = new ArrayList<>();
        
        // Extraer paquetes y ordenar por urgencia (deadline más cercano primero)
        ArrayList<OrderSchema> packagesToRepair = new ArrayList<>();
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : destroyedPackages) {
            packagesToRepair.add(entry.getKey());
        }
        
        packagesToRepair.sort((p1, p2) -> {
            // Ordenar por presupuesto real desde orderDate (nulls last)
            if (p1.getOrderDate() == null || p1.getDeliveryDeadline() == null) return 1;
            if (p2.getOrderDate() == null || p2.getDeliveryDeadline() == null) return -1;
            long p1Hours = ChronoUnit.HOURS.between(p1.getOrderDate(), p1.getDeliveryDeadline());
            long p2Hours = ChronoUnit.HOURS.between(p2.getOrderDate(), p2.getDeliveryDeadline());
            return Long.compare(p1Hours, p2Hours);
        });
        
        int reinsertedCount = 0;
        
        // Insertar paquetes en orden de urgencia
        for (OrderSchema pkg : packagesToRepair) {
            AirportSchema destinationAirportSchema = getAirportByCity(pkg.getDestinationCitySchema());
            int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
            
            if (destinationAirportSchema == null || !hasWarehouseCapacity(destinationAirportSchema, productCount)) {
                unassignedOrderSchemas.add(pkg);
                continue;
            }
            
            // Buscar ruta con mayor margen de tiempo
            ArrayList<FlightSchema> bestRoute = findRouteWithMaxMargin(pkg);
            if (bestRoute != null && isRouteValid(pkg, bestRoute, Math.max(1, productCount))) {
                repairedSolution.put(pkg, bestRoute);
                updateFlightCapacities(bestRoute, productCount);
                incrementWarehouseOccupancy(destinationAirportSchema, productCount);
                reinsertedCount++;
            } else {
                unassignedOrderSchemas.add(pkg);
            }
        }
        if (Constants.VERBOSE_LOGGING) {
          System.out.println("Reparación por tiempo: " + reinsertedCount + "/" + packagesToRepair.size() + " paquetes reinsertados");
        }
        return new RepairResult(repairedSolution, unassignedOrderSchemas);
    }
    
    /**
     * Reparación por capacidad: Prioriza rutas con mayor capacidad disponible.
     */
    public RepairResult capacityBasedRepair(
            HashMap<OrderSchema, ArrayList<FlightSchema>> partialSolution,
            ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyedPackages) {
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> repairedSolution = new HashMap<>(partialSolution);
        ArrayList<OrderSchema> unassignedOrderSchemas = new ArrayList<>();
        
        ArrayList<OrderSchema> packagesToRepair = new ArrayList<>();
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : destroyedPackages) {
            packagesToRepair.add(entry.getKey());
        }
        
        // Ordenar por deadline como criterio secundario
        packagesToRepair.sort((p1, p2) -> {
            LocalDateTime d1 = p1.getDeliveryDeadline();
            LocalDateTime d2 = p2.getDeliveryDeadline();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d1.compareTo(d2);
        });
        
        int reinsertedCount = 0;
        
        for (OrderSchema pkg : packagesToRepair) {
            AirportSchema destinationAirportSchema = getAirportByCity(pkg.getDestinationCitySchema());
            int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
            
            if (destinationAirportSchema == null || !hasWarehouseCapacity(destinationAirportSchema, productCount)) {
                unassignedOrderSchemas.add(pkg);
                continue;
            }
            
            // Buscar ruta con mayor capacidad disponible
            ArrayList<FlightSchema> bestRoute = findRouteWithMaxCapacity(pkg);
            if (bestRoute != null && isRouteValid(pkg, bestRoute, Math.max(1, productCount))) {
                repairedSolution.put(pkg, bestRoute);
                updateFlightCapacities(bestRoute, productCount);
                incrementWarehouseOccupancy(destinationAirportSchema, productCount);
                reinsertedCount++;
            } else {
                unassignedOrderSchemas.add(pkg);
            }
        }
        if (Constants.VERBOSE_LOGGING) {
          System.out.println("Reparación por capacidad: " + reinsertedCount + "/" + packagesToRepair.size() + " paquetes reinsertados");
        }
        return new RepairResult(repairedSolution, unassignedOrderSchemas);
    }
    
    // ================= MÉTODOS AUXILIARES =================
    
    /**
     * Calcula la urgencia de un paquete específico para MoraPack
     * Considera promesas de entrega según continentes y tiempo restante
     */
    private double calculatePackageUrgency(OrderSchema pkg) {
        if (pkg.getOrderDate() == null || pkg.getDeliveryDeadline() == null) {
            return 0.0; // Sin información de tiempo, baja prioridad
        }
        
        // Calcular tiempo disponible desde orden hasta deadline
        long totalAvailableHours = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        
        // Determinar promesa MoraPack según continentes
        boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == 
                                    pkg.getDestinationCitySchema().getContinent();
        long moraPackPromiseHours = sameContinentRoute ? 48 : 72; // 2 días intra / 3 días inter
        
        // Calcular factor de urgencia
        double urgencyFactor;
        if (totalAvailableHours <= moraPackPromiseHours * 0.5) {
            // Muy urgente: menos de la mitad del tiempo de promesa disponible
            urgencyFactor = 10.0;
        } else if (totalAvailableHours <= moraPackPromiseHours * 0.75) {
            // Urgente: menos del 75% del tiempo de promesa disponible
            urgencyFactor = 5.0;
        } else if (totalAvailableHours <= moraPackPromiseHours) {
            // Moderadamente urgente: dentro del tiempo de promesa
            urgencyFactor = 3.0;
        } else if (totalAvailableHours <= moraPackPromiseHours * 1.5) {
            // Tiempo holgado: 50% más tiempo que la promesa
            urgencyFactor = 1.0;
        } else {
            // Mucho tiempo disponible
            urgencyFactor = 0.5;
        }
        
        // Ajustar por prioridad del paquete (si está disponible)
        if (pkg.getPriority() > 0) {
            urgencyFactor *= (1.0 + pkg.getPriority() / 10.0); // Boost por prioridad
        }
        
        // Penalizar si excede promesa MoraPack
        if (totalAvailableHours > moraPackPromiseHours * 1.2) {
            urgencyFactor *= 0.8; // Reducir prioridad para paquetes con demasiado tiempo
        }
        
        return urgencyFactor;
    }
    
    private ArrayList<RouteOption> findAllRouteOptions(OrderSchema pkg) {
        ArrayList<RouteOption> options = new ArrayList<>();
        CitySchema origin = pkg.getCurrentLocation();
        CitySchema destination = pkg.getDestinationCitySchema();
        
        if (origin.equals(destination)) {
            options.add(new RouteOption(new ArrayList<>(), Double.MAX_VALUE));
            return options;
        }
        
        // Buscar ruta directa
        ArrayList<FlightSchema> directRoute = findDirectRoute(origin, destination);
        if (directRoute != null && isRouteValid(pkg, directRoute)) {
            double margin = calculateRouteTimeMargin(pkg, directRoute);
            options.add(new RouteOption(directRoute, margin));
        }
        
        // Buscar rutas con una escala
        ArrayList<FlightSchema> oneStopRoute = findOneStopRoute(origin, destination);
        if (oneStopRoute != null && isRouteValid(pkg, oneStopRoute)) {
            double margin = calculateRouteTimeMargin(pkg, oneStopRoute);
            options.add(new RouteOption(oneStopRoute, margin));
        }
        
        // Buscar rutas con dos escalas
        ArrayList<FlightSchema> twoStopRoute = findTwoStopRoute(origin, destination);
        if (twoStopRoute != null && isRouteValid(pkg, twoStopRoute)) {
            double margin = calculateRouteTimeMargin(pkg, twoStopRoute);
            options.add(new RouteOption(twoStopRoute, margin));
        }
        
        return options;
    }
    
    private ArrayList<FlightSchema> findBestRoute(OrderSchema pkg) {
        ArrayList<RouteOption> options = findAllRouteOptions(pkg);
        if (options.isEmpty()) return null;
        
        // Seleccionar la ruta con mayor margen de tiempo
        options.sort((r1, r2) -> Double.compare(r2.timeMargin, r1.timeMargin));
        return options.get(0).route;
    }
    
    private ArrayList<FlightSchema> findRouteWithMaxMargin(OrderSchema pkg) {
        return findBestRoute(pkg); // Ya implementado arriba
    }
    
    private ArrayList<FlightSchema> findRouteWithMaxCapacity(OrderSchema pkg) {
        ArrayList<RouteOption> options = findAllRouteOptions(pkg);
        if (options.isEmpty()) return null;
        
        // Calcular capacidad disponible para cada ruta
        ArrayList<RouteCapacityOption> capacityOptions = new ArrayList<>();
        for (RouteOption option : options) {
            double totalCapacity = 0;
            double usedCapacity = 0;
            
            for (FlightSchema flightSchema : option.route) {
                totalCapacity += flightSchema.getMaxCapacity();
                usedCapacity += flightSchema.getUsedCapacity();
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
    
    private double calculateRouteTimeMargin(OrderSchema pkg, ArrayList<FlightSchema> route) {
        if (pkg == null || route == null) return 1.0;
        if (pkg.getOrderDate() == null || pkg.getDeliveryDeadline() == null) return 1.0;
        double totalTime = 0;
        for (FlightSchema flightSchema : route) {
            totalTime += flightSchema.getTransportTime();
        }
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0; // 2h por conexión
        }
        long availableHours = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        double margin = availableHours - totalTime;
        return Math.max(margin, 0.0) + 1.0;
    }
    
    private boolean isRouteValid(OrderSchema pkg, ArrayList<FlightSchema> route) {
        int qty = (pkg.getProductSchemas()!=null && !pkg.getProductSchemas().isEmpty()) ? pkg.getProductSchemas().size() : 1;
        return isRouteValid(pkg, route, Math.max(1, qty));
    }

    private boolean fitsRouteCapacity(ArrayList<FlightSchema> route, int qty) {
        if (route == null) return false;
        for (FlightSchema f : route) {
            if (f.getUsedCapacity() + qty > f.getMaxCapacity()) return false;
        }
        return true;
    }

    private boolean isRouteValid(OrderSchema pkg, ArrayList<FlightSchema> route, int qty) {
        if (pkg == null || route == null) return false;
        if (route.isEmpty()) {
            return pkg.getCurrentLocation() != null && pkg.getDestinationCitySchema() != null &&
                   normalizeCityName(pkg.getCurrentLocation()).equals(normalizeCityName(pkg.getDestinationCitySchema()));
        }
        // Capacidad por qty
        if (!fitsRouteCapacity(route, qty)) return false;
        // Origen correcto
        AirportSchema expectedOrigin = getAirportByCity(pkg.getCurrentLocation());
        if (expectedOrigin == null || !route.get(0).getOriginAirportSchema().equals(expectedOrigin)) return false;
        // Continuidad
        for (int i = 0; i < route.size() - 1; i++) {
            if (!route.get(i).getDestinationAirportSchema().equals(route.get(i + 1).getOriginAirportSchema())) return false;
        }
        // Destino correcto
        AirportSchema expectedDestination = getAirportByCity(pkg.getDestinationCitySchema());
        if (expectedDestination == null || !route.get(route.size() - 1).getDestinationAirportSchema().equals(expectedDestination)) return false;
        // Deadline
        return isDeadlineRespected(pkg, route);
    }
    
    /**
     * CORRECCIÓN: Aplicar las mismas correcciones que SolutionSchema.java
     */
    private boolean isDeadlineRespected(OrderSchema pkg, ArrayList<FlightSchema> route) {
        double totalTime = 0;
        
        for (FlightSchema flightSchema : route) {
            totalTime += flightSchema.getTransportTime();
        }
        
        // Añadir penalización por conexiones
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * 2.0;
        }
        
        CitySchema origin = pkg.getCurrentLocation();
        CitySchema destination = pkg.getDestinationCitySchema();
        boolean sameContinentRoute = origin.getContinent() == destination.getContinent();
        long moraPackPromiseHours = sameContinentRoute ? 48 : 72; // 2 días intra / 3 días inter
        
        if (totalTime > moraPackPromiseHours) {
            return false; // Excede promesa MoraPack
        }
        
        // Factor de seguridad
        if (random != null) {
            int complexityFactor = route.size() + (sameContinentRoute ? 0 : 2);
            double safetyMargin = 0.01 * (1 + random.nextInt(complexityFactor * 3));
            totalTime = totalTime * (1.0 + safetyMargin);
        }
        
        long hoursUntilDeadline = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        
        return totalTime <= hoursUntilDeadline;
    }
    
    private ArrayList<FlightSchema> findDirectRoute(CitySchema origin, CitySchema destination) {
        AirportSchema originAirportSchema = getAirportByCity(origin);
        AirportSchema destinationAirportSchema = getAirportByCity(destination);
        
        if (originAirportSchema == null || destinationAirportSchema == null) return null;
        
        for (FlightSchema flightSchema : flightSchemas) {
            if (flightSchema.getOriginAirportSchema().equals(originAirportSchema) &&
                flightSchema.getDestinationAirportSchema().equals(destinationAirportSchema) &&
                flightSchema.getUsedCapacity() < flightSchema.getMaxCapacity()) {
                
                ArrayList<FlightSchema> route = new ArrayList<>();
                route.add(flightSchema);
                return route;
            }
        }
        return null;
    }
    
    private ArrayList<FlightSchema> findOneStopRoute(CitySchema origin, CitySchema destination) {
        AirportSchema originAirportSchema = getAirportByCity(origin);
        AirportSchema destinationAirportSchema = getAirportByCity(destination);
        
        if (originAirportSchema == null || destinationAirportSchema == null) return null;
        
        // Crear lista de aeropuertos intermedios y barajarla
        ArrayList<AirportSchema> intermediates = new ArrayList<>();
        for (AirportSchema airportSchema : airportSchemas) {
            if (!airportSchema.equals(originAirportSchema) && !airportSchema.equals(destinationAirportSchema)) {
                intermediates.add(airportSchema);
            }
        }
        Collections.shuffle(intermediates, random);
        
        for (AirportSchema intermediate : intermediates) {
            FlightSchema firstFlightSchema = null;
            FlightSchema secondFlightSchema = null;
            
            // Buscar primer segmento
            for (FlightSchema flightSchema : flightSchemas) {
                if (flightSchema.getOriginAirportSchema().equals(originAirportSchema) &&
                    flightSchema.getDestinationAirportSchema().equals(intermediate) &&
                    flightSchema.getUsedCapacity() < flightSchema.getMaxCapacity()) {
                    firstFlightSchema = flightSchema;
                    break;
                }
            }
            
            if (firstFlightSchema == null) continue;
            
            // Buscar segundo segmento
            for (FlightSchema flightSchema : flightSchemas) {
                if (flightSchema.getOriginAirportSchema().equals(intermediate) &&
                    flightSchema.getDestinationAirportSchema().equals(destinationAirportSchema) &&
                    flightSchema.getUsedCapacity() < flightSchema.getMaxCapacity()) {
                    secondFlightSchema = flightSchema;
                    break;
                }
            }
            
            if (secondFlightSchema != null) {
                ArrayList<FlightSchema> route = new ArrayList<>();
                route.add(firstFlightSchema);
                route.add(secondFlightSchema);
                return route;
            }
        }
        
        return null;
    }
    
    private ArrayList<FlightSchema> findTwoStopRoute(CitySchema origin, CitySchema destination) {
        AirportSchema originAirportSchema = getAirportByCity(origin);
        AirportSchema destinationAirportSchema = getAirportByCity(destination);
        
        if (originAirportSchema == null || destinationAirportSchema == null) return null;
        
        ArrayList<AirportSchema> candidates = new ArrayList<>();
        for (AirportSchema airportSchema : airportSchemas) {
            if (!airportSchema.equals(originAirportSchema) && !airportSchema.equals(destinationAirportSchema)) {
                candidates.add(airportSchema);
            }
        }
        
        if (candidates.size() < 2) return null;
        
        Collections.shuffle(candidates, random);
        int maxTries = Math.min(10, candidates.size() - 1);
        
        for (int i = 0; i < maxTries; i++) {
            AirportSchema first = candidates.get(i);
            for (int j = i + 1; j < Math.min(i + 5, candidates.size()); j++) {
                AirportSchema second = candidates.get(j);
                
                ArrayList<FlightSchema> route = tryTwoStopRoute(originAirportSchema, first, second, destinationAirportSchema);
                if (route != null) return route;
                
                // También probar en orden inverso
                route = tryTwoStopRoute(originAirportSchema, second, first, destinationAirportSchema);
                if (route != null) return route;
            }
        }
        
        return null;
    }
    
    private ArrayList<FlightSchema> tryTwoStopRoute(AirportSchema origin, AirportSchema first, AirportSchema second, AirportSchema destination) {
        FlightSchema flightSchema1 = null, flightSchema2 = null, flightSchema3 = null;
        
        // Buscar vuelo 1: origin -> first
        for (FlightSchema flightSchema : flightSchemas) {
            if (flightSchema.getOriginAirportSchema().equals(origin) &&
                flightSchema.getDestinationAirportSchema().equals(first) &&
                flightSchema.getUsedCapacity() < flightSchema.getMaxCapacity()) {
                flightSchema1 = flightSchema;
                break;
            }
        }
        
        if (flightSchema1 == null) return null;
        
        // Buscar vuelo 2: first -> second
        for (FlightSchema flightSchema : flightSchemas) {
            if (flightSchema.getOriginAirportSchema().equals(first) &&
                flightSchema.getDestinationAirportSchema().equals(second) &&
                flightSchema.getUsedCapacity() < flightSchema.getMaxCapacity()) {
                flightSchema2 = flightSchema;
                break;
            }
        }
        
        if (flightSchema2 == null) return null;
        
        // Buscar vuelo 3: second -> destination
        for (FlightSchema flightSchema : flightSchemas) {
            if (flightSchema.getOriginAirportSchema().equals(second) &&
                flightSchema.getDestinationAirportSchema().equals(destination) &&
                flightSchema.getUsedCapacity() < flightSchema.getMaxCapacity()) {
                flightSchema3 = flightSchema;
                break;
            }
        }
        
        if (flightSchema3 != null) {
            ArrayList<FlightSchema> route = new ArrayList<>();
            route.add(flightSchema1);
            route.add(flightSchema2);
            route.add(flightSchema3);
            return route;
        }
        
        return null;
    }
    
    private AirportSchema getAirportByCity(CitySchema citySchema) {
        if (citySchema == null || citySchema.getName() == null) return null;
        String target = normalizeCityName(citySchema);
        for (AirportSchema airportSchema : airportSchemas) {
            if (airportSchema.getCitySchema() != null && airportSchema.getCitySchema().getName() != null) {
                String key = normalizeCityName(airportSchema.getCitySchema());
                if (key.equals(target)) return airportSchema;
            }
        }
        return null;
    }

    private String normalizeCityName(CitySchema citySchema) {
        return citySchema.getName().trim().toLowerCase();
    }
    
    private boolean hasWarehouseCapacity(AirportSchema destinationAirportSchema, int productCount) {
        if (destinationAirportSchema.getWarehouse() == null) {
            return false;
        }
        
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirportSchema, 0);
        return (currentOccupancy + productCount) <= destinationAirportSchema.getWarehouse().getMaxCapacity();
    }
    
    private void updateFlightCapacities(ArrayList<FlightSchema> route, int productCount) {
        for (FlightSchema flightSchema : route) {
            flightSchema.setUsedCapacity(flightSchema.getUsedCapacity() + productCount);
        }
    }
    
    private void incrementWarehouseOccupancy(AirportSchema airportSchema, int productCount) {
        int current = warehouseOccupancy.getOrDefault(airportSchema, 0);
        warehouseOccupancy.put(airportSchema, current + productCount);
    }
    
    // ================= CLASES AUXILIARES =================
    
    private static class RouteOption {
        ArrayList<FlightSchema> route;
        double timeMargin;
        
        RouteOption(ArrayList<FlightSchema> route, double timeMargin) {
            this.route = route;
            this.timeMargin = timeMargin;
        }
    }
    
    private static class RouteCapacityOption {
        ArrayList<FlightSchema> route;
        double availableCapacity;
        double timeMargin;
        
        RouteCapacityOption(ArrayList<FlightSchema> route, double availableCapacity, double timeMargin) {
            this.route = route;
            this.availableCapacity = availableCapacity;
            this.timeMargin = timeMargin;
        }
    }
    
    /**
     * Clase para encapsular el resultado de una operación de reparación
     */
    public static class RepairResult {
        private HashMap<OrderSchema, ArrayList<FlightSchema>> repairedSolution;
        private ArrayList<OrderSchema> unassignedOrderSchemas;
        
        public RepairResult(HashMap<OrderSchema, ArrayList<FlightSchema>> repairedSolution,
                           ArrayList<OrderSchema> unassignedOrderSchemas) {
            this.repairedSolution = repairedSolution;
            this.unassignedOrderSchemas = unassignedOrderSchemas;
        }
        
        public HashMap<OrderSchema, ArrayList<FlightSchema>> getRepairedSolution() {
            return repairedSolution;
        }
        
        public ArrayList<OrderSchema> getUnassignedPackages() {
            return unassignedOrderSchemas;
        }
        
        public int getNumRepairedPackages() {
            return repairedSolution.size();
        }
        
        public boolean isSuccess() {
            return !repairedSolution.isEmpty() || unassignedOrderSchemas.isEmpty();
        }
        
        public int getNumUnassignedPackages() {
            return unassignedOrderSchemas.size();
        }
    }
}
