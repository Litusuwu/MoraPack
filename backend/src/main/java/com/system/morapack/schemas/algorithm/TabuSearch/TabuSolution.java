package com.system.morapack.schemas.algorithm.TabuSearch;

import com.system.morapack.schemas.*;
import com.system.morapack.schemas.AirportSchema;
import com.system.morapack.config.Constants;
import com.system.morapack.schemas.OrderSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Representa una solución para el problema de asignación de paquetes
 * Incluye métodos para aplicar movimientos y evaluar soluciones
 */
public class TabuSolution {
    private HashMap<OrderSchema, ArrayList<FlightSchema>> solution;
    private ArrayList<OrderSchema> allOrderSchemas;
    private ArrayList<OrderSchema> unassignedOrderSchemas;
    private ArrayList<OrderSchema> originalOrderSchemas; // Paquetes originales antes de unitizar
    private ArrayList<AirportSchema> airportSchemas;
    private ArrayList<FlightSchema> flightSchemas;
    private Map<String, AirportSchema> cityToAirportMap;
    private HashMap<AirportSchema, Integer> warehouseOccupancy;
    private HashMap<AirportSchema, int[]> temporalWarehouseOccupancy; // Validación temporal minuto-a-minuto
    private LocalDateTime T0; // Ancla temporal para cálculos consistentes
    private int score;
    private boolean scoreOutdated;
    
    /**
     * Constructor que inicializa una solución vacía
     */
    public TabuSolution(ArrayList<OrderSchema> allOrderSchemas, ArrayList<AirportSchema> airportSchemas, ArrayList<FlightSchema> flightSchemas) {
        this.solution = new HashMap<>();
        this.originalOrderSchemas = new ArrayList<>(allOrderSchemas);
        this.airportSchemas = airportSchemas;
        this.flightSchemas = flightSchemas;
        this.cityToAirportMap = buildCityToAirportMap();
        this.warehouseOccupancy = new HashMap<>();
        this.temporalWarehouseOccupancy = new HashMap<>();
        this.scoreOutdated = true;
        
        // Aplicar unitización si está habilitada
        if (Constants.ENABLE_PRODUCT_UNITIZATION) {
            this.allOrderSchemas = expandPackagesToProductUnits(this.originalOrderSchemas);
            System.out.println("UNITIZACIÓN APLICADA: " + this.originalOrderSchemas.size() +
                             " paquetes originales → " + this.allOrderSchemas.size() + " unidades de producto");
        } else {
            this.allOrderSchemas = new ArrayList<>(allOrderSchemas);
            System.out.println("UNITIZACIÓN DESHABILITADA: Usando paquetes originales");
        }
        
        this.unassignedOrderSchemas = new ArrayList<>(this.allOrderSchemas);
        
        initializeWarehouseOccupancy();
        initializeTemporalWarehouseOccupancy();
        initializeT0();
    }
    
    /**
     * Constructor de copia para crear una nueva solución basada en otra
     */
    public TabuSolution(TabuSolution other) {
        this.solution = new HashMap<>();
        
        // Copiar solución
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : other.solution.entrySet()) {
            OrderSchema pkg = entry.getKey();
            ArrayList<FlightSchema> route = entry.getValue();
            this.solution.put(pkg, new ArrayList<>(route));
        }
        
        // Copiar otras estructuras
        this.allOrderSchemas = new ArrayList<>(other.allOrderSchemas);
        this.unassignedOrderSchemas = new ArrayList<>(other.unassignedOrderSchemas);
        this.originalOrderSchemas = new ArrayList<>(other.originalOrderSchemas);
        this.airportSchemas = other.airportSchemas;
        this.flightSchemas = other.flightSchemas;
        this.cityToAirportMap = other.cityToAirportMap;
        this.T0 = other.T0;
        
        // Copiar ocupación de almacenes
        this.warehouseOccupancy = new HashMap<>(other.warehouseOccupancy);
        
        // Copiar ocupación temporal de almacenes
        this.temporalWarehouseOccupancy = new HashMap<>();
        for (Map.Entry<AirportSchema, int[]> entry : other.temporalWarehouseOccupancy.entrySet()) {
            this.temporalWarehouseOccupancy.put(entry.getKey(), entry.getValue().clone());
        }
        
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
    private Map<String, AirportSchema> buildCityToAirportMap() {
        Map<String, AirportSchema> map = new HashMap<>();
        for (AirportSchema airportSchema : airportSchemas) {
            if (airportSchema.getCitySchema() != null && airportSchema.getCitySchema().getName() != null) {
                map.put(airportSchema.getCitySchema().getName().toLowerCase().trim(), airportSchema);
            }
        }
        return map;
    }
    
    /**
     * Inicializa la ocupación de almacenes
     */
    private void initializeWarehouseOccupancy() {
        for (AirportSchema airportSchema : airportSchemas) {
            warehouseOccupancy.put(airportSchema, 0);
        }
    }
    
    /**
     * Inicializa las estructuras de validación temporal minuto-a-minuto
     */
    private void initializeTemporalWarehouseOccupancy() {
        final int TOTAL_MINUTES = Constants.HORIZON_DAYS * 24 * 60; // 4 días = 5760 minutos
        for (AirportSchema airportSchema : airportSchemas) {
            temporalWarehouseOccupancy.put(airportSchema, new int[TOTAL_MINUTES]);
        }
    }
    
    /**
     * Inicializa la ancla temporal T0 para cálculos consistentes
     */
    private void initializeT0() {
        this.T0 = LocalDateTime.now().withSecond(0).withNano(0);
    }
    
    /**
     * UNITIZACIÓN: Expande paquetes en unidades de producto individuales
     * Permite dividir productos de un paquete entre múltiples vuelos
     */
    private ArrayList<OrderSchema> expandPackagesToProductUnits(ArrayList<OrderSchema> originalOrderSchemas) {
        ArrayList<OrderSchema> productUnits = new ArrayList<>();
        
        for (OrderSchema originalPkg : originalOrderSchemas) {
            // Validar origen desde sedes principales si está habilitado
            if (Constants.VALIDATE_HEADQUARTERS_ORIGIN && !isValidOrigin(originalPkg)) {
                System.out.println("ADVERTENCIA: Paquete " + originalPkg.getId() + " no se origina desde una sede principal de MoraPack");
                continue; // Saltar paquetes que no se originen desde sedes válidas
            }
            
            int productCount = (originalPkg.getProductSchemas() != null && !originalPkg.getProductSchemas().isEmpty())
                             ? originalPkg.getProductSchemas().size() : 1;
            
            // Crear una unidad por cada producto
            for (int i = 0; i < productCount; i++) {
                OrderSchema unit = createPackageUnit(originalPkg, i);
                productUnits.add(unit);
            }
        }
        
        return productUnits;
    }
    
    /**
     * Crea una unidad de producto individual a partir de un paquete original
     */
    private OrderSchema createPackageUnit(OrderSchema originalPkg, int productIndex) {
        OrderSchema unit = new OrderSchema();
        String unitIdString = originalPkg.getId() + "_UNIT_" + productIndex;
        unit.setId(unitIdString.hashCode()); // Convertir string a int usando hashCode
        unit.setCustomerSchema(originalPkg.getCustomerSchema());
        unit.setCurrentLocation(originalPkg.getCurrentLocation());
        unit.setDestinationCitySchema(originalPkg.getDestinationCitySchema());
        unit.setOrderDate(originalPkg.getOrderDate());
        unit.setDeliveryDeadline(originalPkg.getDeliveryDeadline());
        unit.setPriority(originalPkg.getPriority());
        
        // Crear lista con un solo producto
        ArrayList<ProductSchema> singleProductSchema = new ArrayList<>();
        if (originalPkg.getProductSchemas() != null && originalPkg.getProductSchemas().size() > productIndex) {
            singleProductSchema.add(originalPkg.getProductSchemas().get(productIndex));
        } else {
            // Crear producto genérico si no existe
            ProductSchema genericProductSchema = new ProductSchema();
            String productIdString = "MPE_" + originalPkg.getId() + "_" + productIndex;
            genericProductSchema.setId(productIdString.hashCode()); // Convertir string a int
            singleProductSchema.add(genericProductSchema);
        }
        unit.setProductSchemas(singleProductSchema);
        
        return unit;
    }
    
    /**
     * Valida que un paquete se origine desde una sede principal de MoraPack
     * (Lima, Bruselas o Baku según especificación)
     */
    private boolean isValidOrigin(OrderSchema pkg) {
        if (pkg.getCurrentLocation() == null || pkg.getCurrentLocation().getName() == null) {
            return false;
        }
        
        String originCity = pkg.getCurrentLocation().getName().toLowerCase().trim();
        
        return originCity.contains("lima") || 
               originCity.contains("brussels") || 
               originCity.contains("baku") ||
               originCity.contains("peru") ||
               originCity.contains("belgium") ||
               originCity.contains("azerbaijan");
    }
    
    /**
     * Valida que un vuelo respete los tiempos de transporte PACK
     * - Mismo continente: 0.5 días (12 horas)
     * - Diferente continente: 1 día (24 horas)
     */
    private boolean validatePACKTransportTimes(FlightSchema flightSchema) {
        if (flightSchema.getOriginAirportSchema() == null || flightSchema.getDestinationAirportSchema() == null ||
            flightSchema.getOriginAirportSchema().getCitySchema() == null || flightSchema.getDestinationAirportSchema().getCitySchema() == null) {
            return false;
        }
        
        boolean sameContinentFlight = flightSchema.getOriginAirportSchema().getCitySchema().getContinent() ==
                                     flightSchema.getDestinationAirportSchema().getCitySchema().getContinent();
        
        double expectedTime = sameContinentFlight ? 
                             Constants.SAME_CONTINENT_TRANSPORT_TIME * 24.0 : // 12 horas
                             Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME * 24.0; // 24 horas
        
        // Permitir variación del ±10% en los tiempos
        double tolerance = expectedTime * 0.1;
        double actualTime = flightSchema.getTransportTime();
        
        return Math.abs(actualTime - expectedTime) <= tolerance;
    }
    
    /**
     * Añade ocupación temporal al almacén de un aeropuerto
     * Implementa la regla de liberación después de 2 horas en destino
     */
    private boolean addTemporalOccupancy(AirportSchema airportSchema, int startMinute, int durationMinutes, int productCount) {
        if (airportSchema == null || airportSchema.getWarehouse() == null) {
            return false;
        }
        
        int[] occupancyArray = temporalWarehouseOccupancy.get(airportSchema);
        int maxCapacity = airportSchema.getWarehouse().getMaxCapacity();
        
        // Verificar y agregar ocupación para cada minuto del período
        final int TOTAL_MINUTES = Constants.HORIZON_DAYS * 24 * 60;
        int clampedStart = Math.max(0, Math.min(startMinute, TOTAL_MINUTES - 1));
        int clampedEnd = Math.max(0, Math.min(startMinute + durationMinutes, TOTAL_MINUTES));
        
        // Verificar capacidad disponible
        for (int minute = clampedStart; minute < clampedEnd; minute++) {
            if (occupancyArray[minute] + productCount > maxCapacity) {
                return false; // Violación de capacidad
            }
        }
        
        // Aplicar la ocupación
        for (int minute = clampedStart; minute < clampedEnd; minute++) {
            occupancyArray[minute] += productCount;
        }
        
        return true;
    }
    
    /**
     * Calcula el tiempo de inicio de un paquete en minutos desde T0
     */
    private int getPackageStartTime(OrderSchema pkg) {
        if (pkg.getOrderDate() == null || T0 == null) {
            return 0; // Tiempo por defecto
        }
        
        long minutesDifference = ChronoUnit.MINUTES.between(T0, pkg.getOrderDate());
        
        // Asegurar que el tiempo esté dentro del horizonte
        final int TOTAL_MINUTES = Constants.HORIZON_DAYS * 24 * 60;
        return Math.max(0, Math.min((int)minutesDifference, TOTAL_MINUTES - 1));
    }
    
    /**
     * Valida el flujo temporal completo de un paquete respetando capacidades minuto-a-minuto
     */
    private boolean validateSinglePackageTemporalFlow(OrderSchema pkg, ArrayList<FlightSchema> route) {
        if (pkg == null) {
            return false;
        }
        
        if (route == null || route.isEmpty()) {
            // El paquete ya está en destino, cliente tiene 2 horas para recoger
            AirportSchema destinationAirportSchema = getAirportByCity(pkg.getDestinationCitySchema().getName());
            int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
            int startMinute = getPackageStartTime(pkg);
            return addTemporalOccupancy(destinationAirportSchema, startMinute, Constants.CUSTOMER_PICKUP_MAX_HOURS * 60, productCount);
        }
        
        int currentMinute = getPackageStartTime(pkg);
        int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
        
        for (int i = 0; i < route.size(); i++) {
            FlightSchema flightSchema = route.get(i);
            AirportSchema departureAirportSchema = flightSchema.getOriginAirportSchema();
            AirportSchema arrivalAirportSchema = flightSchema.getDestinationAirportSchema();
            
            // Validar tiempos de transporte PACK
            if (!validatePACKTransportTimes(flightSchema)) {
                System.out.println("ADVERTENCIA: Vuelo " + flightSchema.getId() + " no respeta tiempos PACK");
            }
            
            // FASE 1: El paquete está en el aeropuerto de origen esperando el vuelo
            int waitingTime = Constants.PRE_FLIGHT_PROCESSING_MINUTES;
            if (!addTemporalOccupancy(departureAirportSchema, currentMinute, waitingTime, productCount)) {
                return false;
            }
            
            // FASE 2: Vuelo en progreso (el paquete no ocupa almacén durante el vuelo)
            int flightDurationMinutes = (int)(flightSchema.getTransportTime() * 60);
            currentMinute += waitingTime + flightDurationMinutes;
            
            // FASE 3: El paquete llega al aeropuerto de destino
            int arrivalMinute = currentMinute;
            int stayDuration;
            if (i < route.size() - 1) {
                stayDuration = Constants.CONNECTION_TIME_MINUTES; // 2 horas de conexión
            } else {
                // Es el destino final - cliente tiene máximo 2 horas para recoger
                stayDuration = Constants.CUSTOMER_PICKUP_MAX_HOURS * 60;
            }
            
            if (stayDuration > 0 && !addTemporalOccupancy(arrivalAirportSchema, arrivalMinute, stayDuration, productCount)) {
                return false;
            }
            
            // Actualizar tiempo para el siguiente vuelo
            currentMinute = arrivalMinute;
            if (i < route.size() - 1) {
                currentMinute += Constants.CONNECTION_TIME_MINUTES;
            }
        }
        
        return true;
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
    private boolean insertPackage(OrderSchema pkg, ArrayList<FlightSchema> route) {
        if (pkg == null || !unassignedOrderSchemas.contains(pkg)) {
            return false;
        }
        
        // Validar origen desde sedes principales si está habilitado
        if (Constants.VALIDATE_HEADQUARTERS_ORIGIN && !isValidOrigin(pkg)) {
            return false;
        }
        
        // Verificar capacidad de vuelos
        int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
        if (!checkFlightCapacity(route, productCount)) {
            return false;
        }
        
        // Verificar capacidad de almacén en destino (validación básica)
        AirportSchema destinationAirportSchema = getAirportByCity(pkg.getDestinationCitySchema().getName());
        if (destinationAirportSchema == null || !checkWarehouseCapacity(destinationAirportSchema, productCount)) {
            return false;
        }
        
        // Validar flujo temporal completo (validación avanzada minuto-a-minuto)
        if (!validateSinglePackageTemporalFlow(pkg, route)) {
            return false;
        }
        
        // Validar deadlines y promesas MoraPack
        if (!isDeadlineRespected(pkg, route)) {
            return false;
        }
        
        // Insertar paquete
        solution.put(pkg, new ArrayList<>(route));
        unassignedOrderSchemas.remove(pkg);
        
        // Actualizar capacidades básicas
        updateFlightCapacities(route, productCount);
        updateWarehouseOccupancy(destinationAirportSchema, productCount);
        
        // Marcar score como desactualizado
        scoreOutdated = true;
        
        return true;
    }
    
    /**
     * Elimina un paquete de la solución
     */
    private boolean removePackage(OrderSchema pkg) {
        if (pkg == null || !solution.containsKey(pkg)) {
            return false;
        }
        
        // Obtener ruta actual
        ArrayList<FlightSchema> route = solution.get(pkg);
        int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
        
        // Eliminar paquete
        solution.remove(pkg);
        unassignedOrderSchemas.add(pkg);
        
        // Actualizar capacidades
        updateFlightCapacities(route, -productCount);
        
        AirportSchema destinationAirportSchema = getAirportByCity(pkg.getDestinationCitySchema().getName());
        if (destinationAirportSchema != null) {
            updateWarehouseOccupancy(destinationAirportSchema, -productCount);
        }
        
        // Marcar score como desactualizado
        scoreOutdated = true;
        
        return true;
    }
    
    /**
     * Reasigna un paquete a una nueva ruta
     */
    private boolean reassignPackage(OrderSchema pkg, ArrayList<FlightSchema> newRoute) {
        if (pkg == null || !solution.containsKey(pkg)) {
            return false;
        }
        
        // Obtener ruta actual
        ArrayList<FlightSchema> oldRoute = solution.get(pkg);
        int productCount = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
        
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
    private boolean swapPackages(OrderSchema pkg1, OrderSchema pkg2) {
        if (pkg1 == null || pkg2 == null || 
            !solution.containsKey(pkg1) || !solution.containsKey(pkg2)) {
            return false;
        }
        
        // Obtener rutas actuales
        ArrayList<FlightSchema> route1 = solution.get(pkg1);
        ArrayList<FlightSchema> route2 = solution.get(pkg2);
        
        int productCount1 = pkg1.getProductSchemas() != null ? pkg1.getProductSchemas().size() : 1;
        int productCount2 = pkg2.getProductSchemas() != null ? pkg2.getProductSchemas().size() : 1;
        
        // Verificar capacidad de vuelos después del intercambio
        ArrayList<FlightSchema> tempRoute1 = new ArrayList<>(route1);
        ArrayList<FlightSchema> tempRoute2 = new ArrayList<>(route2);
        
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
    private boolean checkFlightCapacity(ArrayList<FlightSchema> route, int productCount) {
        if (route == null || route.isEmpty()) {
            return true; // No hay vuelos, por lo que no hay restricción de capacidad
        }
        
        for (FlightSchema flightSchema : route) {
            int currentUsage = flightSchema.getUsedCapacity();
            if (currentUsage + productCount > flightSchema.getMaxCapacity()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Verifica si hay capacidad suficiente en el almacén
     */
    private boolean checkWarehouseCapacity(AirportSchema airportSchema, int productCount) {
        if (airportSchema == null || airportSchema.getWarehouse() == null) {
            return false;
        }
        
        int currentOccupancy = warehouseOccupancy.getOrDefault(airportSchema, 0);
        return (currentOccupancy + productCount) <= airportSchema.getWarehouse().getMaxCapacity();
    }
    
    /**
     * Actualiza la capacidad utilizada en los vuelos
     */
    private void updateFlightCapacities(ArrayList<FlightSchema> route, int productCount) {
        if (route == null || route.isEmpty()) {
            return;
        }
        
        for (FlightSchema flightSchema : route) {
            flightSchema.setUsedCapacity(flightSchema.getUsedCapacity() + productCount);
        }
    }
    
    /**
     * Actualiza la ocupación de almacén
     */
    private void updateWarehouseOccupancy(AirportSchema airportSchema, int productCount) {
        if (airportSchema == null) {
            return;
        }
        
        int currentOccupancy = warehouseOccupancy.getOrDefault(airportSchema, 0);
        warehouseOccupancy.put(airportSchema, currentOccupancy + productCount);
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
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : solution.entrySet()) {
            OrderSchema pkg = entry.getKey();
            ArrayList<FlightSchema> route = entry.getValue();
            
            // Contar productos
            int packageProducts = pkg.getProductSchemas() != null ? pkg.getProductSchemas().size() : 1;
            totalProducts += packageProducts;
            
            // Tiempo total de la ruta
            double routeTime = 0;
            for (FlightSchema flightSchema : route) {
                routeTime += flightSchema.getTransportTime();
                totalCapacityUtilization += (double) flightSchema.getUsedCapacity() / flightSchema.getMaxCapacity();
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
     * Incluye validación de promesas MoraPack y deadlines del cliente
     */
    private boolean isDeadlineRespected(OrderSchema pkg, ArrayList<FlightSchema> route) {
        if (pkg == null || pkg.getOrderDate() == null || pkg.getDeliveryDeadline() == null) {
            return false;
        }
        
        double totalTime = 0;
        
        // Calcular tiempo total de la ruta
        for (FlightSchema flightSchema : route) {
            totalTime += flightSchema.getTransportTime();
            
            // Validar que cada vuelo respete los tiempos PACK
            if (!validatePACKTransportTimes(flightSchema)) {
                return false; // Vuelo no válido según estándares PACK
            }
        }
        
        // Añadir tiempo de conexión entre vuelos
        if (route.size() > 1) {
            totalTime += (route.size() - 1) * (Constants.CONNECTION_TIME_MINUTES / 60.0); // 2 horas por conexión
        }
        
        // 1. Verificar promesa MoraPack según continentes
        if (!validateMoraPackDeliveryPromise(pkg, totalTime)) {
            return false; // Excede promesas MoraPack
        }
        
        // 2. Verificar deadline específico del cliente
        long hoursUntilDeadline = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        
        // 3. Aplicar factor de seguridad para asegurar entregas a tiempo
        double safetyMargin = calculateSafetyMargin(pkg, route);
        totalTime = totalTime * (1.0 + safetyMargin);
        
        return totalTime <= hoursUntilDeadline;
    }
    
    /**
     * Valida las promesas de entrega de MoraPack según continentes
     * - Mismo continente: máximo 2 días (48 horas)
     * - Diferentes continentes: máximo 3 días (72 horas)
     */
    private boolean validateMoraPackDeliveryPromise(OrderSchema pkg, double totalTimeHours) {
        CitySchema origin = pkg.getCurrentLocation();
        CitySchema destination = pkg.getDestinationCitySchema();
        
        if (origin == null || destination == null) {
            return false;
        }
        
        boolean sameContinentRoute = origin.getContinent() == destination.getContinent();
        long moraPackPromiseHours = sameContinentRoute ? 
                                   (long)(Constants.SAME_CONTINENT_MAX_DELIVERY_TIME * 24) : 
                                   (long)(Constants.DIFFERENT_CONTINENT_MAX_DELIVERY_TIME * 24);
        
        if (totalTimeHours > moraPackPromiseHours) {
            return false; // Excede promesa MoraPack
        }
        
        // También verificar contra el deadline del cliente
        long hoursUntilDeadline = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        
        return totalTimeHours <= hoursUntilDeadline;
    }
    
    /**
     * Calcula un margen de seguridad basado en la complejidad de la ruta
     */
    private double calculateSafetyMargin(OrderSchema pkg, ArrayList<FlightSchema> route) {
        CitySchema origin = pkg.getCurrentLocation();
        CitySchema destination = pkg.getDestinationCitySchema();
        boolean sameContinentRoute = (origin != null && destination != null) &&
                                    origin.getContinent() == destination.getContinent();
        
        // Factor base de complejidad
        int complexityFactor = route.size() + (sameContinentRoute ? 0 : 2);
        
        // Margen del 1-5% dependiendo de la complejidad
        return 0.01 * (1 + (complexityFactor * 2));
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
        
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : solution.entrySet()) {
            OrderSchema pkg = entry.getKey();
            ArrayList<FlightSchema> route = entry.getValue();
            
            boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == 
                                        pkg.getDestinationCitySchema().getContinent();
            
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
        
        for (Map.Entry<AirportSchema, Integer> entry : warehouseOccupancy.entrySet()) {
            AirportSchema airportSchema = entry.getKey();
            int occupancy = entry.getValue();
            
            if (airportSchema.getWarehouse() != null && airportSchema.getWarehouse().getMaxCapacity() > 0) {
                double utilization = (double) occupancy / airportSchema.getWarehouse().getMaxCapacity();
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
        
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : solution.entrySet()) {
            OrderSchema pkg = entry.getKey();
            ArrayList<FlightSchema> route = entry.getValue();
            
            if (route.isEmpty()) continue;
            
            // Penalizar rutas con más escalas de las necesarias
            boolean sameContinentRoute = pkg.getCurrentLocation().getContinent() == 
                                        pkg.getDestinationCitySchema().getContinent();
            
            int expectedMaxStops = sameContinentRoute ? 1 : 2; // 1 para mismo continente, 2 para diferentes
            
            if (route.size() > expectedMaxStops) {
                totalComplexity += (route.size() - expectedMaxStops) * 2.0; // Penalización por escala extra
            }
            
            // Penalizar vuelos con baja utilización en rutas largas
            if (route.size() > 1) {
                for (FlightSchema flightSchema : route) {
                    double utilization = (double) flightSchema.getUsedCapacity() / flightSchema.getMaxCapacity();
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
    private AirportSchema getAirportByCity(String cityName) {
        if (cityName == null) return null;
        return cityToAirportMap.get(cityName.toLowerCase().trim());
    }
    
    /**
     * Getters y setters
     */
    public HashMap<OrderSchema, ArrayList<FlightSchema>> getSolution() {
        return solution;
    }
    
    public ArrayList<OrderSchema> getUnassignedPackages() {
        return unassignedOrderSchemas;
    }
    
    public int getAssignedPackagesCount() {
        return solution.size();
    }
    
    public int getUnassignedPackagesCount() {
        return unassignedOrderSchemas.size();
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
