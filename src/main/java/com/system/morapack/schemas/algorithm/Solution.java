package com.system.morapack.schemas.algorithm;

import com.system.morapack.schemas.Airport;
import com.system.morapack.schemas.City;
import com.system.morapack.schemas.Flight;
import com.system.morapack.schemas.Package;
import com.system.morapack.config.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Solution {
    private HashMap<HashMap<Package, ArrayList<Flight>>, Integer> solution;
    private HashMap<ArrayList<HashMap<Package, ArrayList<Flight>>>, Integer> solutionSpace;
    private InputAirports inputAirports;
    private InputData inputData;
    private InputProducts inputProducts;
    private ArrayList<Airport> airports;
    private ArrayList<Flight> flights;
    private ArrayList<Package> packages;
    // Mapa para rastrear la ocupación de almacenes por destino
    private HashMap<Airport, Integer> warehouseOccupancy;
    // Matriz temporal para validar capacidad de almacenes por minuto [aeropuerto][minuto_del_dia]
    private HashMap<Airport, int[]> temporalWarehouseOccupancy;

    
    public Solution() {
        this.inputAirports = new InputAirports(Constants.AIRPORT_INFO_FILE_PATH);
        this.solution = new HashMap<>();
        this.solutionSpace = new HashMap<>();
        this.airports = inputAirports.readAirports();
        this.inputData = new InputData(Constants.FLIGHTS_FILE_PATH, this.airports);
        this.flights = inputData.readFlights();
        this.inputProducts = new InputProducts(Constants.PRODUCTS_FILE_PATH, this.airports);
        this.packages = inputProducts.readProducts();
        this.warehouseOccupancy = new HashMap<>();
        this.temporalWarehouseOccupancy = new HashMap<>();
        // Inicializar ocupación de almacenes
        initializeWarehouseOccupancy();
        initializeTemporalWarehouseOccupancy();
    }

    public void solve() {
        // 1. Inicialización
        System.out.println("Iniciando solución");
        System.out.println("Lectura de aeropuertos");
        System.out.println("Aeropuertos leídos: " + this.airports.size());
        System.out.println("Lectura de vuelos");
        System.out.println("Vuelos leídos: " + this.flights.size());
        System.out.println("Lectura de productos");
        System.out.println("Productos leídos: " + this.packages.size());
        // 2. Generar una solución inicial s_actual
        this.generateInitialSolution();
        // Validar solución generada
        System.out.println("Validando solución...");
        boolean isValid = this.isSolutionValid();
        System.out.println("Solución válida: " + (isValid ? "SÍ" : "NO"));
        
        // Mostrar descripción de la solución inicial
        this.printSolutionDescription(3);
        // 3. Establecer s_mejor = s_actual
        // 4. Inicializar los pesos w(op_d, op_r) para todos los pares de operadores de destrucción (d) y reparación (r)
        // 5. Inicializar los puntajes p(op_d, op_r) = 0
        // 6. Establecer la temperatura inicial T
        // 7. Bucle principal
        // 8. Mientras (criterio de parada no se cumpla):
        // 9. Seleccionar operadores
        // 10. Seleccionar un par de operadores (op_d, op_r) basado en sus pesos w
        // 11. Generar una nueva solución
        // 12. s_temporal = s_actual
        // 13. Destruir(s_temporal) usando op_d
        // 14. Reparar(s_temporal) usando op_r
        // 15. Evaluar y aceptar la nueva solución
        // 16. Si (costo(s_temporal) < costo(s_actual)):
        // 17. s_actual = s_temporal
        // 18. Actualizar la mejor solución encontrada
        // 19. Si (costo(s_actual) < costo(s_mejor)):
        // 20. s_mejor = s_actual
        // 21. Asignar puntaje por encontrar una nueva mejor solución global
        // 22. p(op_d, op_r) += sigma1
        // 23. Sino:
        // 24. Asignar puntaje por encontrar una solución mejor que la actual
        // 25. p(op_d, op_r) += sigma2
        // 26. Sino:
        // 27. Criterio de aceptación tipo "Simulated Annealing"
        // 28. Si (exp((costo(s_actual) - costo(s_temporal)) / T) > random(0, 1)):
        // 29. s_actual = s_temporal
        // 30. No se asigna puntaje por aceptar una peor solución
        // 31. Actualizar pesos de los operadores al final de un segmento
        // 32. Si (iteración % número_de_iteraciones_por_segmento == 0):
        // 33. Para cada par de operadores (op_d, op_r):
        // 34. w(op_d, op_r) = (1 - lambda) * w(op_d, op_r) + lambda * (p(op_d, op_r) / usos(op_d, op_r))
        // 35. Reiniciar puntajes p(op_d, op_r) = 0
        // 36. Reiniciar contador de usos(op_d, op_r) = 0
        // 37. Enfriar la temperatura
        // 38. T = T * factor_de_enfriamiento
        // 39. Devolver s_mejor
        // 40. Fin del bucle principal
        // 41. Fin del algoritmo
    }
    
    public void generateInitialSolution() {
        System.out.println("Generating initial solution using greedy approach...");
        
        // Crear estructura de solución temporal
        HashMap<Package, ArrayList<Flight>> currentSolution = new HashMap<>();
        
        // Ordenar paquetes por prioridad (deadline más cercano primero)
        ArrayList<Package> sortedPackages = new ArrayList<>(packages);
        sortedPackages.sort((p1, p2) -> p1.getDeliveryDeadline().compareTo(p2.getDeliveryDeadline()));
        
        int assignedPackages = 0;
        
        for (Package pkg : sortedPackages) {
            // Obtener el aeropuerto de destino
            Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
            
            // Verificar capacidad del almacén de destino
            if (destinationAirport == null || !hasWarehouseCapacity(destinationAirport)) {
                System.out.println("Package " + pkg.getId() + " cannot be assigned - warehouse at " + 
                                 pkg.getDestinationCity().getName() + " is full or unavailable");
                continue;
            }
            
            ArrayList<Flight> route = findBestRoute(pkg);
            if (route != null && isRouteValid(pkg, route)) {
                currentSolution.put(pkg, route);
                assignedPackages++;
                
                // Actualizar capacidades de los vuelos
                updateFlightCapacities(route, 1); // +1 paquete por vuelo
                
                // Incrementar ocupación del almacén de destino
                incrementWarehouseOccupancy(destinationAirport);
            }
        }
        
        // Calcular el peso/costo de esta solución
        int solutionWeight = calculateSolutionWeight(currentSolution);
        
        // Almacenar la solución con su peso
        solution.put(currentSolution, solutionWeight);
        
        System.out.println("Initial solution generated: " + assignedPackages + "/" + packages.size() + " packages assigned");
        System.out.println("Solution weight: " + solutionWeight);
    }
    
    private ArrayList<Flight> findBestRoute(Package pkg) {
        City origin = pkg.getCurrentLocation();
        City destination = pkg.getDestinationCity();
        
        // Si ya está en la ciudad destino, no necesita vuelos
        if (origin.equals(destination)) {
            return new ArrayList<>();
        }
        
        // Buscar ruta directa primero
        ArrayList<Flight> directRoute = findDirectRoute(origin, destination);
        if (directRoute != null) {
            return directRoute;
        }
        
        // Buscar ruta con una escala
        ArrayList<Flight> oneStopRoute = findOneStopRoute(origin, destination);
        if (oneStopRoute != null) {
            return oneStopRoute;
        }
        
        // Buscar ruta con dos escalas (máximo)
        ArrayList<Flight> twoStopRoute = findTwoStopRoute(origin, destination);
        return twoStopRoute;
    }
    
    private ArrayList<Flight> findDirectRoute(City origin, City destination) {
        Airport originAirport = getAirportByCity(origin);
        Airport destinationAirport = getAirportByCity(destination);
        
        if (originAirport == null || destinationAirport == null) {
            return null;
        }
        
        // Buscar vuelo directo
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
        
        if (originAirport == null || destinationAirport == null) {
            return null;
        }
        
        // Buscar escala intermedia
        for (Airport intermediateAirport : airports) {
            if (intermediateAirport.equals(originAirport) || intermediateAirport.equals(destinationAirport)) {
                continue;
            }
            
            // Buscar vuelo de origen a intermedio
            Flight firstFlight = null;
            for (Flight flight : flights) {
                if (flight.getOriginAirport().equals(originAirport) && 
                    flight.getDestinationAirport().equals(intermediateAirport) &&
                    flight.getUsedCapacity() < flight.getMaxCapacity()) {
                    firstFlight = flight;
                    break;
                }
            }
            
            if (firstFlight == null) continue;
            
            // Buscar vuelo de intermedio a destino
            Flight secondFlight = null;
            for (Flight flight : flights) {
                if (flight.getOriginAirport().equals(intermediateAirport) && 
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
        
        if (originAirport == null || destinationAirport == null) {
            return null;
        }
        
        // Buscar ruta con dos escalas intermedias
        for (Airport firstIntermediate : airports) {
            if (firstIntermediate.equals(originAirport) || firstIntermediate.equals(destinationAirport)) {
                continue;
            }
            
            for (Airport secondIntermediate : airports) {
                if (secondIntermediate.equals(originAirport) || 
                    secondIntermediate.equals(destinationAirport) ||
                    secondIntermediate.equals(firstIntermediate)) {
                    continue;
                }
                
                // Buscar primer vuelo: origen -> primera escala
                Flight firstFlight = null;
                for (Flight flight : flights) {
                    if (flight.getOriginAirport().equals(originAirport) && 
                        flight.getDestinationAirport().equals(firstIntermediate) &&
                        flight.getUsedCapacity() < flight.getMaxCapacity()) {
                        firstFlight = flight;
                        break;
                    }
                }
                
                if (firstFlight == null) continue;
                
                // Buscar segundo vuelo: primera escala -> segunda escala
                Flight secondFlight = null;
                for (Flight flight : flights) {
                    if (flight.getOriginAirport().equals(firstIntermediate) && 
                        flight.getDestinationAirport().equals(secondIntermediate) &&
                        flight.getUsedCapacity() < flight.getMaxCapacity()) {
                        secondFlight = flight;
                        break;
                    }
                }
                
                if (secondFlight == null) continue;
                
                // Buscar tercer vuelo: segunda escala -> destino
                Flight thirdFlight = null;
                for (Flight flight : flights) {
                    if (flight.getOriginAirport().equals(secondIntermediate) && 
                        flight.getDestinationAirport().equals(destinationAirport) &&
                        flight.getUsedCapacity() < flight.getMaxCapacity()) {
                        thirdFlight = flight;
                        break;
                    }
                }
                
                if (thirdFlight != null) {
                    ArrayList<Flight> route = new ArrayList<>();
                    route.add(firstFlight);
                    route.add(secondFlight);
                    route.add(thirdFlight);
                    
                    // Verificar que la ruta total no exceda límites de tiempo
                    double totalTime = firstFlight.getTransportTime() + 
                                      secondFlight.getTransportTime() + 
                                      thirdFlight.getTransportTime();
                    
                    // Agregar penalización por múltiples escalas (tiempo de conexión)
                    totalTime += 2.0; // 2 horas adicionales por cada escala
                    
                    // Si la ruta es demasiado larga, continuar buscando
                    if (totalTime > Constants.DIFFERENT_CONTINENT_MAX_DELIVERY_TIME * 24) {
                        continue;
                    }
                    
                    return route;
                }
            }
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
        
        // Verificar que la ruta sea continua
        for (int i = 0; i < route.size() - 1; i++) {
            if (!route.get(i).getDestinationAirport().equals(route.get(i + 1).getOriginAirport())) {
                return false;
            }
        }
        
        // Verificar restricciones de tiempo
        return isDeadlineRespected(pkg, route);
    }
    
    private boolean isDeadlineRespected(Package pkg, ArrayList<Flight> route) {
        double totalTime = 0;
        
        for (Flight flight : route) {
            totalTime += flight.getTransportTime();
        }
        
        // Agregar tiempo de traslado según continente
        City origin = pkg.getCurrentLocation();
        City destination = pkg.getDestinationCity();
        
        boolean sameContinentRoute = origin.getContinent() == destination.getContinent();
        
        if (sameContinentRoute) {
            totalTime += Constants.SAME_CONTINENT_TRANSPORT_TIME;
        } else {
            totalTime += Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME;
        }
        
        // Convertir tiempo límite a horas para comparar
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDeadline = ChronoUnit.HOURS.between(now, pkg.getDeliveryDeadline());
        
        return totalTime <= hoursUntilDeadline;
    }
    
    private void updateFlightCapacities(ArrayList<Flight> route, int packageCount) {
        for (Flight flight : route) {
            flight.setUsedCapacity(flight.getUsedCapacity() + packageCount);
        }
    }
    
    private int calculateSolutionWeight(HashMap<Package, ArrayList<Flight>> solutionMap) {
        // El peso de la solución puede representar múltiples factores:
        // 1. Número total de paquetes asignados (maximizar)
        // 2. Tiempo total de entrega (minimizar)
        // 3. Utilización de capacidad de vuelos (maximizar)
        // 4. Cumplimiento de deadlines (maximizar)
        
        int totalPackages = solutionMap.size();
        double totalDeliveryTime = 0;
        int onTimeDeliveries = 0;
        double totalCapacityUtilization = 0;
        int totalFlightsUsed = 0;
        
        // Calcular métricas
        for (Map.Entry<Package, ArrayList<Flight>> entry : solutionMap.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            // Tiempo total de la ruta
            double routeTime = 0;
            for (Flight flight : route) {
                routeTime += flight.getTransportTime();
                totalCapacityUtilization += (double) flight.getUsedCapacity() / flight.getMaxCapacity();
                totalFlightsUsed++;
            }
            totalDeliveryTime += routeTime;
            
            // Verificar si llega a tiempo
            if (isDeadlineRespected(pkg, route)) {
                onTimeDeliveries++;
            }
        }
        
        // Fórmula de peso que combina múltiples objetivos
        // Maximizar paquetes asignados y entregas a tiempo, minimizar tiempo promedio
        double avgDeliveryTime = totalPackages > 0 ? totalDeliveryTime / totalPackages : 0;
        double avgCapacityUtilization = totalFlightsUsed > 0 ? totalCapacityUtilization / totalFlightsUsed : 0;
        double onTimeRate = totalPackages > 0 ? (double) onTimeDeliveries / totalPackages : 0;
        
        // Peso final (los factores pueden ajustarse según prioridades)
        int weight = (int) (
            totalPackages * 1000 +           // Más paquetes asignados = mejor
            onTimeRate * 1000 +             // Más entregas a tiempo = mejor
            avgCapacityUtilization * 500 -    // Mayor utilización = mejor
            avgDeliveryTime * 400            // Menos tiempo promedio = mejor
        );
        
        return weight;
    }

    public boolean isSolutionValid() {
        if (solution.isEmpty()) {
            return false;
        }
        
        // Obtener la solución actual
        HashMap<Package, ArrayList<Flight>> currentSolution = solution.keySet().iterator().next();
        
        // Verificar que todos los paquetes asignados tengan rutas válidas
        for (Map.Entry<Package, ArrayList<Flight>> entry : currentSolution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            if (!isRouteValid(pkg, route)) {
                return false;
            }
        }
        
        // Validación temporal de capacidades de almacenes
        if (!isTemporalSolutionValid(currentSolution)) {
            System.out.println("Solution violates temporal warehouse capacity constraints");
            return false;
        }
        
        return true;
    }
    
    public boolean isSolutionCapacityValid() {
        if (solution.isEmpty()) {
            return false;
        }
        
        // Crear mapa de uso de capacidad por vuelo
        HashMap<Flight, Integer> flightUsage = new HashMap<>();
        
        // Obtener la solución actual
        HashMap<Package, ArrayList<Flight>> currentSolution = solution.keySet().iterator().next();
        
        // Contar cuántos paquetes usan cada vuelo
        for (ArrayList<Flight> route : currentSolution.values()) {
            for (Flight flight : route) {
                flightUsage.put(flight, flightUsage.getOrDefault(flight, 0) + 1);
            }
        }
        
        // Verificar que ningún vuelo exceda su capacidad
        for (Map.Entry<Flight, Integer> entry : flightUsage.entrySet()) {
            if (entry.getValue() > entry.getKey().getMaxCapacity()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Imprime una descripción detallada de la solución actual.
     * Muestra estadísticas generales y las rutas asignadas a cada paquete.
     * 
     * @param detailLevel nivel de detalle (1: resumen, 2: rutas principales, 3: todas las rutas)
     */
    public void printSolutionDescription(int detailLevel) {
        if (solution.isEmpty()) {
            System.out.println("No hay solución disponible para mostrar.");
            return;
        }
        
        // Obtener la solución actual y su peso
        HashMap<Package, ArrayList<Flight>> currentSolution = solution.keySet().iterator().next();
        int solutionWeight = solution.get(currentSolution);
        
        // Estadísticas generales
        System.out.println("\n========== DESCRIPCIÓN DE LA SOLUCIÓN ==========");
        System.out.println("Peso de la solución: " + solutionWeight);
        System.out.println("Paquetes asignados: " + currentSolution.size() + "/" + packages.size());
        
        // Calcular estadísticas adicionales
        int directRoutes = 0;
        int oneStopRoutes = 0;
        int twoStopRoutes = 0;
        int sameContinentRoutes = 0;
        int differentContinentRoutes = 0;
        int onTimeDeliveries = 0;
        
        for (Map.Entry<Package, ArrayList<Flight>> entry : currentSolution.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            // Contar tipos de rutas
            if (route.size() == 1) directRoutes++;
            else if (route.size() == 2) oneStopRoutes++;
            else if (route.size() == 3) twoStopRoutes++;
            
            // Contar rutas por continente
            if (pkg.getCurrentLocation().getContinent() == pkg.getDestinationCity().getContinent()) {
                sameContinentRoutes++;
            } else {
                differentContinentRoutes++;
            }
            
            // Contar entregas a tiempo
            if (isDeadlineRespected(pkg, route)) {
                onTimeDeliveries++;
            }
        }
        
        // Mostrar estadísticas detalladas
        System.out.println("\n----- Estadísticas de Rutas -----");
        System.out.println("Rutas directas: " + directRoutes + " (" + formatPercentage(directRoutes, currentSolution.size()) + "%)");
        System.out.println("Rutas con 1 escala: " + oneStopRoutes + " (" + formatPercentage(oneStopRoutes, currentSolution.size()) + "%)");
        System.out.println("Rutas con 2 escalas: " + twoStopRoutes + " (" + formatPercentage(twoStopRoutes, currentSolution.size()) + "%)");
        System.out.println("Rutas en mismo continente: " + sameContinentRoutes + " (" + formatPercentage(sameContinentRoutes, currentSolution.size()) + "%)");
        System.out.println("Rutas entre continentes: " + differentContinentRoutes + " (" + formatPercentage(differentContinentRoutes, currentSolution.size()) + "%)");
        System.out.println("Entregas a tiempo: " + onTimeDeliveries + " (" + formatPercentage(onTimeDeliveries, currentSolution.size()) + "%)");
        
        // Mostrar estadísticas de ocupación de almacenes
        System.out.println("\n----- Ocupación de Almacenes -----");
        int totalWarehouseCapacity = 0;
        int totalWarehouseOccupancy = 0;
        int warehousesAtCapacity = 0;
        
        for (Map.Entry<Airport, Integer> entry : warehouseOccupancy.entrySet()) {
            Airport airport = entry.getKey();
            int occupancy = entry.getValue();
            
            if (airport.getWarehouse() != null) {
                int maxCapacity = airport.getWarehouse().getMaxCapacity();
                totalWarehouseCapacity += maxCapacity;
                totalWarehouseOccupancy += occupancy;
                
                if (occupancy >= maxCapacity) {
                    warehousesAtCapacity++;
                }
                
                // Mostrar almacenes con alta ocupación (>80%)
                double occupancyPercentage = (occupancy * 100.0) / maxCapacity;
                if (occupancyPercentage > 80.0) {
                    System.out.println("  " + airport.getCity().getName() + ": " + occupancy + "/" + maxCapacity + 
                                      " (" + String.format("%.1f", occupancyPercentage) + "%)");
                }
            }
        }
        
        double avgOccupancyPercentage = totalWarehouseCapacity > 0 ? 
            (totalWarehouseOccupancy * 100.0) / totalWarehouseCapacity : 0.0;
        
        System.out.println("Ocupación promedio de almacenes: " + String.format("%.1f", avgOccupancyPercentage) + "%");
        System.out.println("Almacenes llenos: " + warehousesAtCapacity + "/" + airports.size());
        
        // Mostrar información de picos temporales si la validación temporal está disponible
        if (temporalWarehouseOccupancy != null && !temporalWarehouseOccupancy.isEmpty()) {
            System.out.println("\n----- Picos de Ocupación Temporal -----");
            for (Airport airport : airports) {
                if (airport.getWarehouse() != null) {
                    int[] peakInfo = findPeakOccupancy(airport);
                    int peakMinute = peakInfo[0];
                    int maxOccupancy = peakInfo[1];
                    
                    if (maxOccupancy > 0) {
                        int peakHour = peakMinute / 60;
                        int peakMin = peakMinute % 60;
                        double peakPercentage = (maxOccupancy * 100.0) / airport.getWarehouse().getMaxCapacity();
                        
                        if (peakPercentage > 50.0) { // Mostrar solo aeropuertos con picos significativos
                            System.out.println("  " + airport.getCity().getName() + 
                                              " - Pico: " + maxOccupancy + "/" + airport.getWarehouse().getMaxCapacity() + 
                                              " (" + String.format("%.1f", peakPercentage) + "%) a las " + 
                                              String.format("%02d:%02d", peakHour, peakMin));
                        }
                    }
                }
            }
        }
        
        // Si el nivel de detalle es bajo, terminar aquí
        if (detailLevel < 2) {
            return;
        }
        
        // Mostrar rutas por prioridad
        System.out.println("\n----- Rutas por Prioridad -----");
        
        // Ordenar paquetes por prioridad
        List<Package> sortedPackages = new ArrayList<>(currentSolution.keySet());
        sortedPackages.sort((p1, p2) -> {
            // Primero por prioridad (mayor a menor)
            int priorityCompare = Double.compare(p2.getPriority(), p1.getPriority());
            if (priorityCompare != 0) return priorityCompare;
            
            // Luego por deadline (más cercano primero)
            return p1.getDeliveryDeadline().compareTo(p2.getDeliveryDeadline());
        });
        
        // Mostrar rutas de alta prioridad o todas según el nivel de detalle
        int routesToShow = detailLevel == 2 ? Math.min(10, sortedPackages.size()) : sortedPackages.size();
        
        for (int i = 0; i < routesToShow; i++) {
            Package pkg = sortedPackages.get(i);
            ArrayList<Flight> route = currentSolution.get(pkg);
            
            System.out.println("\nPaquete #" + pkg.getId() + 
                              " (Prioridad: " + String.format("%.2f", pkg.getPriority()) + 
                              ", Deadline: " + pkg.getDeliveryDeadline() + ")");
            
            System.out.println("  Origen: " + pkg.getCurrentLocation().getName() + 
                              " (" + pkg.getCurrentLocation().getContinent() + ")");
            System.out.println("  Destino: " + pkg.getDestinationCity().getName() + 
                              " (" + pkg.getDestinationCity().getContinent() + ")");
            
            if (route.isEmpty()) {
                System.out.println("  Ruta: Ya está en el destino");
                continue;
            }
            
            System.out.println("  Ruta (" + route.size() + " vuelos):");
            double totalTime = 0;
            
            for (int j = 0; j < route.size(); j++) {
                Flight flight = route.get(j);
                totalTime += flight.getTransportTime();
                
                System.out.println("    " + (j+1) + ". " + 
                                  flight.getOriginAirport().getCity().getName() + " → " + 
                                  flight.getDestinationAirport().getCity().getName() + 
                                  " (" + String.format("%.1f", flight.getTransportTime()) + "h, " + 
                                  flight.getUsedCapacity() + "/" + flight.getMaxCapacity() + " paquetes)");
            }
            
            // Agregar tiempo de conexión
            if (route.size() > 1) {
                totalTime += (route.size() - 1) * 2.0; // 2 horas por conexión
            }
            
            System.out.println("  Tiempo total estimado: " + String.format("%.1f", totalTime) + "h");
            
            boolean onTime = isDeadlineRespected(pkg, route);
            System.out.println("  Entrega a tiempo: " + (onTime ? "SÍ" : "NO"));
        }
        
        if (routesToShow < sortedPackages.size()) {
            System.out.println("\n... y " + (sortedPackages.size() - routesToShow) + " paquetes más (use nivel de detalle 3 para ver todos)");
        }
        
        System.out.println("\n=================================================");
    }
    
    private String formatPercentage(int value, int total) {
        if (total == 0) return "0.0";
        return String.format("%.1f", (value * 100.0) / total);
    }
    
    /**
     * Inicializa el mapa de ocupación de almacenes.
     * Cada aeropuerto de destino inicia con 0 paquetes asignados.
     */
    private void initializeWarehouseOccupancy() {
        for (Airport airport : airports) {
            warehouseOccupancy.put(airport, 0);
        }
    }
    
    /**
     * Inicializa la matriz temporal de ocupación de almacenes.
     * Cada aeropuerto tiene un array de 1440 elementos (24h * 60min).
     */
    private void initializeTemporalWarehouseOccupancy() {
        final int MINUTES_PER_DAY = 24 * 60; // 1440 minutos
        for (Airport airport : airports) {
            temporalWarehouseOccupancy.put(airport, new int[MINUTES_PER_DAY]);
        }
    }
    
    /**
     * Verifica si un aeropuerto de destino puede aceptar un paquete adicional
     * sin exceder la capacidad de su almacén.
     * 
     * @param destinationAirport aeropuerto de destino
     * @return true si hay capacidad disponible, false si está lleno
     */
    private boolean hasWarehouseCapacity(Airport destinationAirport) {
        if (destinationAirport.getWarehouse() == null) {
            System.out.println("Warning: Airport " + destinationAirport.getCodeIATA() + " has no warehouse");
            return false;
        }
        
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        return currentOccupancy < destinationAirport.getWarehouse().getMaxCapacity();
    }
    
    /**
     * Incrementa la ocupación del almacén de destino cuando se asigna un paquete.
     * 
     * @param destinationAirport aeropuerto de destino
     */
    private void incrementWarehouseOccupancy(Airport destinationAirport) {
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        warehouseOccupancy.put(destinationAirport, currentOccupancy + 1);
    }
    
    /**
     * Decrementa la ocupación del almacén de destino cuando se libera un paquete.
     * 
     * @param destinationAirport aeropuerto de destino
     */
    private void decrementWarehouseOccupancy(Airport destinationAirport) {
        int currentOccupancy = warehouseOccupancy.getOrDefault(destinationAirport, 0);
        if (currentOccupancy > 0) {
            warehouseOccupancy.put(destinationAirport, currentOccupancy - 1);
        }
    }
    
    /**
     * Valida temporalmente si una solución respeta las capacidades de almacenes
     * durante todo el día, simulando el flujo de paquetes minuto a minuto.
     * 
     * @param solutionMap mapa de paquetes y sus rutas asignadas
     * @return true si no hay violaciones de capacidad, false si las hay
     */
    public boolean isTemporalSolutionValid(HashMap<Package, ArrayList<Flight>> solutionMap) {
        // Reinicializar matriz temporal
        initializeTemporalWarehouseOccupancy();
        
        // Simular el flujo de cada paquete
        for (Map.Entry<Package, ArrayList<Flight>> entry : solutionMap.entrySet()) {
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            if (!simulatePackageFlow(pkg, route)) {
                return false; // Se encontró una violación de capacidad
            }
        }
        
        return true; // No hay violaciones de capacidad
    }
    
    /**
     * Simula el flujo temporal de un paquete a través de su ruta asignada.
     * 
     * @param pkg paquete a simular
     * @param route ruta asignada al paquete
     * @return true si no viola capacidades, false si las viola
     */
    private boolean simulatePackageFlow(Package pkg, ArrayList<Flight> route) {
        if (route == null || route.isEmpty()) {
            // El paquete ya está en destino, solo verificar capacidad final
            Airport destinationAirport = getAirportByCity(pkg.getDestinationCity());
            return addTemporalOccupancy(destinationAirport, 0, 1440); // Todo el día
        }
        
        int currentMinute = getPackageStartTime(pkg); // Momento cuando el paquete inicia su viaje
        Airport currentAirport = getAirportByCity(pkg.getCurrentLocation());
        
        for (int i = 0; i < route.size(); i++) {
            Flight flight = route.get(i);
            Airport departureAirport = flight.getOriginAirport();
            Airport arrivalAirport = flight.getDestinationAirport();
            
            // El paquete permanece en el aeropuerto de origen hasta el vuelo
            if (!addTemporalOccupancy(departureAirport, currentMinute, 
                                    (int)(flight.getTransportTime() * 60))) {
                System.out.println("Capacity violation at " + departureAirport.getCity().getName() + 
                                  " at minute " + currentMinute + " for package " + pkg.getId());
                return false;
            }
            
            // Actualizar tiempo: vuelo + tiempo de conexión si no es el último vuelo
            currentMinute += (int)(flight.getTransportTime() * 60);
            if (i < route.size() - 1) {
                currentMinute += 120; // 2 horas de conexión
            }
            currentAirport = arrivalAirport;
        }
        
        // El paquete permanece en el aeropuerto de destino final
        Airport finalDestination = currentAirport;
        int remainingMinutes = 1440 - currentMinute; // Resto del día
        if (remainingMinutes > 0) {
            return addTemporalOccupancy(finalDestination, currentMinute, remainingMinutes);
        }
        
        return true;
    }
    
    /**
     * Agrega ocupación temporal a un aeropuerto durante un período de tiempo.
     * 
     * @param airport aeropuerto donde agregar ocupación
     * @param startMinute minuto de inicio (0-1439)
     * @param durationMinutes duración en minutos
     * @return true si no excede capacidad, false si la excede
     */
    private boolean addTemporalOccupancy(Airport airport, int startMinute, int durationMinutes) {
        if (airport == null || airport.getWarehouse() == null) {
            return false;
        }
        
        int[] occupancyArray = temporalWarehouseOccupancy.get(airport);
        int maxCapacity = airport.getWarehouse().getMaxCapacity();
        
        // Verificar y agregar ocupación para cada minuto del período
        for (int minute = startMinute; minute < Math.min(startMinute + durationMinutes, 1440); minute++) {
            occupancyArray[minute]++;
            if (occupancyArray[minute] > maxCapacity) {
                return false; // Violación de capacidad
            }
        }
        
        return true;
    }
    
    /**
     * Obtiene el minuto de inicio del día cuando un paquete comienza su viaje.
     * Basado en la hora de pedido del paquete.
     * 
     * @param pkg paquete
     * @return minuto del día (0-1439)
     */
    private int getPackageStartTime(Package pkg) {
        // Usar la hora de pedido para determinar cuándo inicia el viaje
        return pkg.getOrderDate().getHour() * 60 + pkg.getOrderDate().getMinute();
    }
    
    /**
     * Encuentra el minuto del día con mayor ocupación en un aeropuerto específico.
     * 
     * @param airport aeropuerto a analizar
     * @return array [minuto, ocupación_máxima]
     */
    private int[] findPeakOccupancy(Airport airport) {
        int[] occupancyArray = temporalWarehouseOccupancy.get(airport);
        int maxOccupancy = 0;
        int peakMinute = 0;
        
        for (int minute = 0; minute < 1440; minute++) {
            if (occupancyArray[minute] > maxOccupancy) {
                maxOccupancy = occupancyArray[minute];
                peakMinute = minute;
            }
        }
        
        return new int[]{peakMinute, maxOccupancy};
    }
}
