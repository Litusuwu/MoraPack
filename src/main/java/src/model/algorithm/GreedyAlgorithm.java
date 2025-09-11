package src.model.algorithm;

import src.model.City;
import src.model.Flight;
import src.model.Package;
import src.model.Route;
import src.model.Solution;
import src.model.Warehouse;
import lombok.Getter;
import lombok.Setter;
import java.util.*;

/**
 * Implementación del algoritmo Greedy constructivo para MoraPack
 * Genera soluciones de forma rápida usando criterios de selección voraz
 */
@Getter
@Setter
public class GreedyAlgorithm {
    
    private List<Package> packages;
    private List<Flight> availableFlights;
    private List<Warehouse> warehouses;
    private List<City> cities;
    
    // Criterios de selección greedy
    private GreedyCriteria criteria = GreedyCriteria.BEST_COST;
    private boolean useRandomization = false;
    private double randomizationFactor = 0.0; // 0.0 = puramente greedy, 1.0 = completamente aleatorio
    
    public enum GreedyCriteria {
        BEST_COST("Menor Costo"),
        SHORTEST_TIME("Menor Tiempo"), 
        HIGHEST_CAPACITY("Mayor Capacidad"),
        NEAREST_WAREHOUSE("Almacén Más Cercano"),
        BALANCED("Balanceado");
        
        private final String description;
        
        GreedyCriteria(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public GreedyAlgorithm(List<Package> packages, List<Flight> availableFlights, 
                          List<Warehouse> warehouses, List<City> cities) {
        this.packages = packages;
        this.availableFlights = availableFlights;
        this.warehouses = warehouses;
        this.cities = cities;
    }
    
    /**
     * Ejecuta el algoritmo Greedy y genera una solución
     */
    public Solution solve() {
        System.out.println("🔍 Iniciando Algoritmo Greedy (" + criteria.getDescription() + ")...");
        
        Solution solution = new Solution();
        
        // Agrupar paquetes por destino para eficiencia
        Map<City, List<Package>> packagesByDestination = groupPackagesByDestination();
        
        // Procesar cada destino de forma greedy
        for (Map.Entry<City, List<Package>> entry : packagesByDestination.entrySet()) {
            City destination = entry.getKey();
            List<Package> destPackages = entry.getValue();
            
            // Encontrar la mejor ruta para este destino usando criterio greedy
            Route bestRoute = findBestRouteGreedy(destination, destPackages.size());
            
            if (bestRoute != null && bestRoute.isValidRoute()) {
                // Asignar paquetes a la ruta
                for (Package pkg : destPackages) {
                    solution.assignPackageToRoute(pkg, bestRoute);
                }
                solution.addRoute(bestRoute);
            }
        }
        
        System.out.println("✅ Algoritmo Greedy completado");
        return solution;
    }
    
    /**
     * Agrupa paquetes por ciudad de destino
     */
    private Map<City, List<Package>> groupPackagesByDestination() {
        Map<City, List<Package>> grouped = new HashMap<>();
        
        for (Package pkg : packages) {
            grouped.computeIfAbsent(pkg.getDestinationCity(), k -> new ArrayList<>()).add(pkg);
        }
        
        return grouped;
    }
    
    /**
     * Encuentra la mejor ruta usando criterio greedy
     */
    private Route findBestRouteGreedy(City destination, int packageCount) {
        List<RouteCandidate> candidates = generateRouteCandidates(destination, packageCount);
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        // Ordenar candidatos según criterio greedy
        candidates.sort(this::compareRouteCandidates);
        
        // Seleccionar el mejor candidato (con posible aleatorización)
        RouteCandidate selected = selectCandidate(candidates);
        
        return selected.route;
    }
    
    /**
     * Genera candidatos de ruta para un destino
     */
    private List<RouteCandidate> generateRouteCandidates(City destination, int packageCount) {
        List<RouteCandidate> candidates = new ArrayList<>();
        
        // Probar desde cada almacén principal
        for (Warehouse warehouse : warehouses) {
            if (warehouse.isMainWarehouse()) {
                City origin = warehouse.getAirport().getCity();
                
                // Ruta directa
                Route directRoute = tryDirectRoute(origin, destination, packageCount);
                if (directRoute != null) {
                    double score = calculateRouteScore(directRoute);
                    candidates.add(new RouteCandidate(directRoute, score));
                }
                
                // Rutas con una conexión
                List<Route> connectionRoutes = tryConnectionRoutes(origin, destination, packageCount);
                for (Route route : connectionRoutes) {
                    double score = calculateRouteScore(route);
                    candidates.add(new RouteCandidate(route, score));
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * Intenta crear una ruta directa
     */
    private Route tryDirectRoute(City origin, City destination, int packageCount) {
        Flight directFlight = findBestFlight(origin, destination, packageCount);
        
        if (directFlight != null) {
            Route route = new Route(origin, destination);
            route.addFlight(directFlight);
            
            if (route.isValidRoute()) {
                return route;
            }
        }
        
        return null;
    }
    
    /**
     * Intenta crear rutas con una conexión
     */
    private List<Route> tryConnectionRoutes(City origin, City destination, int packageCount) {
        List<Route> routes = new ArrayList<>();
        
        // Probar cada ciudad como conexión intermedia
        for (City intermediate : cities) {
            if (!intermediate.equals(origin) && !intermediate.equals(destination)) {
                
                Flight firstFlight = findBestFlight(origin, intermediate, packageCount);
                Flight secondFlight = findBestFlight(intermediate, destination, packageCount);
                
                if (firstFlight != null && secondFlight != null) {
                    Route route = new Route(origin, destination);
                    route.addFlight(firstFlight);
                    route.addFlight(secondFlight);
                    
                    if (route.isValidRoute()) {
                        routes.add(route);
                    }
                }
            }
        }
        
        return routes;
    }
    
    /**
     * Encuentra el mejor vuelo según criterio greedy
     */
    private Flight findBestFlight(City origin, City destination, int packageCount) {
        return availableFlights.stream()
                .filter(f -> f.getOriginAirport().getCity().equals(origin) &&
                           f.getDestinationAirport().getCity().equals(destination) &&
                           f.canAccommodatePackages(packageCount))
                .min(this::compareFlights)
                .orElse(null);
    }
    
    /**
     * Calcula score de una ruta según el criterio greedy
     */
    private double calculateRouteScore(Route route) {
        switch (criteria) {
            case BEST_COST:
                return route.getTotalCost();
                
            case SHORTEST_TIME:
                return route.getTotalTime();
                
            case HIGHEST_CAPACITY:
                return -getTotalCapacity(route); // Negativo para orden ascendente
                
            case NEAREST_WAREHOUSE:
                return calculateDistanceScore(route);
                
            case BALANCED:
                return route.getTotalCost() * 0.5 + route.getTotalTime() * 100; // Balance costo/tiempo
                
            default:
                return route.getTotalCost();
        }
    }
    
    /**
     * Compara dos candidatos de ruta
     */
    private int compareRouteCandidates(RouteCandidate a, RouteCandidate b) {
        return Double.compare(a.score, b.score);
    }
    
    /**
     * Compara dos vuelos según criterio
     */
    private int compareFlights(Flight a, Flight b) {
        switch (criteria) {
            case BEST_COST:
                return Double.compare(a.getCost(), b.getCost());
                
            case SHORTEST_TIME:
                return Double.compare(a.getTransportTime(), b.getTransportTime());
                
            case HIGHEST_CAPACITY:
                return Integer.compare(b.getMaxCapacity(), a.getMaxCapacity()); // Descendente
                
            default:
                return Double.compare(a.getCost(), b.getCost());
        }
    }
    
    /**
     * Selecciona un candidato (con posible aleatorización)
     */
    private RouteCandidate selectCandidate(List<RouteCandidate> candidates) {
        if (!useRandomization || randomizationFactor <= 0.0) {
            return candidates.get(0); // El mejor
        }
        
        if (randomizationFactor >= 1.0) {
            return candidates.get(new Random().nextInt(candidates.size())); // Aleatorio
        }
        
        // Selección semi-aleatoria: elegir entre los mejores
        int candidatePoolSize = Math.max(1, (int)(candidates.size() * (1.0 - randomizationFactor)));
        return candidates.get(new Random().nextInt(candidatePoolSize));
    }
    
    /**
     * Obtiene la capacidad total de una ruta
     */
    private int getTotalCapacity(Route route) {
        return route.getFlights().stream()
                .mapToInt(Flight::getMaxCapacity)
                .min()
                .orElse(0);
    }
    
    /**
     * Calcula un score de distancia (simplificado)
     */
    private double calculateDistanceScore(Route route) {
        // Score basado en número de vuelos (más vuelos = más "distancia")
        return route.getFlights().size();
    }
    
    /**
     * Clase interna para candidatos de ruta
     */
    private static class RouteCandidate {
        Route route;
        double score;
        
        RouteCandidate(Route route, double score) {
            this.route = route;
            this.score = score;
        }
    }
    
    /**
     * Configuración rápida para diferentes tipos de greedy
     */
    public void setGreedyType(GreedyCriteria criteria) {
        this.criteria = criteria;
    }
    
    public void enableRandomization(double factor) {
        this.useRandomization = true;
        this.randomizationFactor = Math.max(0.0, Math.min(1.0, factor));
    }
    
    public void disableRandomization() {
        this.useRandomization = false;
        this.randomizationFactor = 0.0;
    }
}
