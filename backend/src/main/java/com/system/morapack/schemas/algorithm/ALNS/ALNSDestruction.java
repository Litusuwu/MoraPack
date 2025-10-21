package com.system.morapack.schemas.algorithm.ALNS;

import com.system.morapack.schemas.FlightSchema;
import com.system.morapack.schemas.OrderSchema;
import com.system.morapack.config.Constants;
import com.system.morapack.schemas.Continent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.time.temporal.ChronoUnit;

/**
 * Clase que implementa operadores de destrucción para el algoritmo ALNS
 * (Adaptive Large Neighborhood Search) específicamente diseñados para el problema
 * de logística MoraPack.
 * 
 * Los operadores están diseñados para preservar la prioridad de entregas a tiempo.
 */
public class ALNSDestruction {
    
    private Random random;
    
    public ALNSDestruction() {
        this.random = new Random(System.currentTimeMillis());
    }
    
    /**
     * Constructor con semilla específica para pruebas deterministas
     */
    public ALNSDestruction(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * CORRECCIÓN: Helpers para cálculos precisos de slack y productos
     */
    private static double routeHours(ArrayList<FlightSchema> route) {
        if (route == null || route.isEmpty()) return Double.POSITIVE_INFINITY;
        double h = 0;
        for (FlightSchema f : route) h += f.getTransportTime();
        if (route.size() > 1) h += (route.size() - 1) * 2.0; // conexiones de 2h
        return h;
    }
    
    private static int productsOf(OrderSchema pkg) {
        return (pkg.getProductSchemas() != null && !pkg.getProductSchemas().isEmpty()) ? pkg.getProductSchemas().size() : 1;
    }
    
    /**
     * CORRECCIÓN: Slack real - horas disponibles desde orderDate menos horas de la ruta actual
     * REFINAMIENTO: Clampar slack negativo por deadlines raros (deadline < orderDate)
     */
    private static double slackHours(OrderSchema pkg, ArrayList<FlightSchema> route) {
        long budget = ChronoUnit.HOURS.between(pkg.getOrderDate(), pkg.getDeliveryDeadline());
        // REFINAMIENTO: Clampar budget negativo a 0 para evitar data mala
        if (budget < 0) {
            budget = 0; // Deadline en el pasado o mal configurado
        }
        double slack = budget - routeHours(route);
        return Math.max(slack, 0.0); // REFINAMIENTO: Clampar slack final a >= 0
    }
    
    /**
     * REFINAMIENTO: Verificar si un paquete ya está en destino (ruta null/empty)
     * Estos paquetes no liberan capacidad de vuelo
     */
    private static boolean isAlreadyAtDestination(ArrayList<FlightSchema> route) {
        return route == null || route.isEmpty();
    }
    
    /**
     * CORRECCIÓN: Destrucción aleatoria mejorada - sesgo por mayor slack y más productos
     */
    public DestructionResult randomDestroy(
            HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution,
            double destructionRate,
            int minDestroy,
            int maxDestroy) {
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> partialSolution = new HashMap<>(currentSolution);
        
        if (currentSolution.isEmpty()) {
            return new DestructionResult(partialSolution, new ArrayList<>());
        }
        
        // CORRECCIÓN: Construir lista con score = w1*slack + w2*productos
        class Cand { 
            OrderSchema pkg;
            double score; 
        }
        
        ArrayList<Cand> cands = new ArrayList<>();
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> e : currentSolution.entrySet()) {
            OrderSchema p = e.getKey();
            ArrayList<FlightSchema> r = e.getValue();
            double slack = slackHours(p, r);
            int prods = productsOf(p);
            
            // REFINAMIENTO: Penalizar fuertemente paquetes ya en destino (no liberan capacidad de vuelo)
            if (isAlreadyAtDestination(r)) {
                slack = slack * 0.1; // Reducir significativamente su prioridad
            }
            
            double score = 1.0 * slack + 0.2 * prods; // pesos: slack y productos
            
            Cand c = new Cand();
            c.pkg = p;
            c.score = score;
            cands.add(c);
        }
        
        // CORRECCIÓN: Ordenar por score desc y destruir los top-k con diversidad
        cands.sort((a, b) -> Double.compare(b.score, a.score));
        
        int numToDestroy = Math.min(
            Math.max((int)(currentSolution.size() * destructionRate), minDestroy),
            Math.min(maxDestroy, currentSolution.size())
        );
        
        ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyed = new ArrayList<>();
        int taken = 0, i = 0;
        
        while (taken < numToDestroy && i < cands.size()) {
            // CORRECCIÓN: 10% probabilidad de saltar para diversidad
            if (random.nextDouble() < 0.10 && i + 1 < cands.size()) {
                i++;
            }
            
            OrderSchema sel = cands.get(i).pkg;
            ArrayList<FlightSchema> route = currentSolution.get(sel);
            if (route == null) route = new ArrayList<>(); // Protección contra nulos
            
            destroyed.add(new java.util.AbstractMap.SimpleEntry<>(sel, new ArrayList<>(route)));
            partialSolution.remove(sel);
            taken++; 
            i++;
        }
        
        return new DestructionResult(partialSolution, destroyed);
    }
    
    /**
     * Destrucción por zona geográfica: elimina paquetes de un continente específico.
     * Útil para liberar capacidad en rutas intercontinentales.
     */
    public DestructionResult geographicDestroy(
            HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution,
            double destructionRate,
            int minDestroy,
            int maxDestroy) {
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> partialSolution = new HashMap<>(currentSolution);
        
        if (currentSolution.isEmpty()) {
            return new DestructionResult(partialSolution, new ArrayList<>());
        }
        
        // Contar paquetes por continente (origen y destino)
        Map<Continent, ArrayList<OrderSchema>> packagesByOriginContinent = new HashMap<>();
        Map<Continent, ArrayList<OrderSchema>> packagesByDestinationContinent = new HashMap<>();
        
        for (OrderSchema pkg : currentSolution.keySet()) {
            Continent originContinent = pkg.getCurrentLocation().getContinent();
            Continent destinationContinent = pkg.getDestinationCitySchema().getContinent();
            
            packagesByOriginContinent.computeIfAbsent(originContinent, k -> new ArrayList<>()).add(pkg);
            packagesByDestinationContinent.computeIfAbsent(destinationContinent, k -> new ArrayList<>()).add(pkg);
        }
        
        // Seleccionar continente con más paquetes intercontinentales
        Continent selectedContinent = null;
        int maxIntercontinentalPackages = 0;
        
        for (Continent continent : Continent.values()) {
            ArrayList<OrderSchema> originOrderSchemas = packagesByOriginContinent.getOrDefault(continent, new ArrayList<>());
            int intercontinentalCount = 0;
            
            for (OrderSchema pkg : originOrderSchemas) {
                if (pkg.getCurrentLocation().getContinent() != pkg.getDestinationCitySchema().getContinent()) {
                    intercontinentalCount++;
                }
            }
            
            if (intercontinentalCount > maxIntercontinentalPackages) {
                maxIntercontinentalPackages = intercontinentalCount;
                selectedContinent = continent;
            }
        }
        
        if (selectedContinent == null) {
            return randomDestroy(currentSolution, destructionRate, minDestroy, maxDestroy);
        }
        
        // Encontrar paquetes del continente seleccionado
        ArrayList<OrderSchema> candidateOrderSchemas = new ArrayList<>();
        for (OrderSchema pkg : currentSolution.keySet()) {
            if (pkg.getCurrentLocation().getContinent() == selectedContinent ||
                pkg.getDestinationCitySchema().getContinent() == selectedContinent) {
                candidateOrderSchemas.add(pkg);
            }
        }
        
        if (candidateOrderSchemas.size() < minDestroy) {
            return randomDestroy(currentSolution, destructionRate, minDestroy, maxDestroy);
        }
        
        int numToDestroy = Math.min(
            Math.max((int)(candidateOrderSchemas.size() * destructionRate), minDestroy),
            Math.min(maxDestroy, candidateOrderSchemas.size())
        );
        
        ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyedPackages = new ArrayList<>();
        
        // REFINAMIENTO: Precomputar slack y productos para evitar recalcular en comparator
        class CandidateInfo {
            OrderSchema pkg;
            boolean intercontinental;
            double slack;
            int products;
            boolean atDestination;
        }
        
        ArrayList<CandidateInfo> candidates = new ArrayList<>();
        for (OrderSchema pkg : candidateOrderSchemas) {
            CandidateInfo info = new CandidateInfo();
            info.pkg = pkg;
            info.intercontinental = pkg.getCurrentLocation().getContinent() != pkg.getDestinationCitySchema().getContinent();
            
            ArrayList<FlightSchema> route = currentSolution.get(pkg);
            info.slack = slackHours(pkg, route);
            info.products = productsOf(pkg);
            info.atDestination = isAlreadyAtDestination(route);
            
            candidates.add(info);
        }
        
        // Priorizar: 1) intercontinental, 2) NO en destino, 3) mayor slack, 4) más productos
        candidates.sort((c1, c2) -> {
            // Primero: intercontinental vs continental
            if (c1.intercontinental != c2.intercontinental) {
                return Boolean.compare(c2.intercontinental, c1.intercontinental);
            }
            
            // REFINAMIENTO: Segundo: evitar paquetes ya en destino
            if (c1.atDestination != c2.atDestination) {
                return Boolean.compare(c1.atDestination, c2.atDestination);
            }
            
            // Tercero: mayor slack
            int slackComparison = Double.compare(c2.slack, c1.slack);
            if (slackComparison != 0) {
                return slackComparison;
            }
            
            // REFINAMIENTO: Cuarto: tie-break por más productos (liberar más capacidad)
            return Integer.compare(c2.products, c1.products);
        });
        
        // Extraer los packages ordenados
        candidateOrderSchemas.clear();
        for (CandidateInfo info : candidates) {
            candidateOrderSchemas.add(info.pkg);
        }
        
        // Seleccionar paquetes con sesgo hacia los intercontinentales
        for (int i = 0; i < numToDestroy; i++) {
            OrderSchema selectedOrderSchema = candidateOrderSchemas.get(i);
            // REFINAMIENTO: Consistencia - usar siempre currentSolution como fuente
            ArrayList<FlightSchema> route = currentSolution.get(selectedOrderSchema);
            if (route == null) route = new ArrayList<>(); // Protección contra nulos
            
            destroyedPackages.add(new java.util.AbstractMap.SimpleEntry<>(
                    selectedOrderSchema,
                new ArrayList<>(route)
            ));
            
            partialSolution.remove(selectedOrderSchema);
        }
        if (Constants.VERBOSE_LOGGING) {
          System.out.println("Destrucción geográfica: " + numToDestroy + " paquetes eliminados del continente " + selectedContinent);
        }
        return new DestructionResult(partialSolution, destroyedPackages);
    }
    
    /**
     * Destrucción por tiempo: elimina paquetes con deadlines en un rango específico.
     * Útil para rebalancear la carga temporal.
     */
    public DestructionResult timeBasedDestroy(
            HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution,
            double destructionRate,
            int minDestroy,
            int maxDestroy) {
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> partialSolution = new HashMap<>(currentSolution);
        
        if (currentSolution.isEmpty()) {
            return new DestructionResult(partialSolution, new ArrayList<>());
        }
        
        // CORRECCIÓN: Agrupar por slack real, no por "horas a deadline"
        ArrayList<OrderSchema> lowSlack = new ArrayList<>();    // slack ≤ 8 h (no tocar si es posible)
        ArrayList<OrderSchema> midSlack = new ArrayList<>();    // 8–32 h
        ArrayList<OrderSchema> highSlack = new ArrayList<>();   // > 32 h
        ArrayList<OrderSchema> atDestination = new ArrayList<>(); // REFINAMIENTO: Separar paquetes ya en destino
        
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> e : currentSolution.entrySet()) {
            OrderSchema pkg = e.getKey();
            ArrayList<FlightSchema> route = e.getValue();
            
            // REFINAMIENTO: Separar paquetes ya en destino (fallback only)
            if (isAlreadyAtDestination(route)) {
                atDestination.add(pkg);
                continue;
            }
            
            double s = slackHours(pkg, route);
            if (s <= 8) {
                lowSlack.add(pkg);
            } else if (s <= 32) {
                midSlack.add(pkg);
            } else {
                highSlack.add(pkg);
            }
        }
        
        // REFINAMIENTO: Elige grupo prioritariamente, usando atDestination como último recurso
        ArrayList<OrderSchema> selectedGroup;
        String groupName;
        
        if (!highSlack.isEmpty()) {
            selectedGroup = highSlack;
            groupName = "alto slack";
        } else if (!midSlack.isEmpty()) {
            selectedGroup = midSlack;
            groupName = "slack medio";
        } else if (!lowSlack.isEmpty()) {
            selectedGroup = lowSlack;
            groupName = "bajo slack";
        } else {
            selectedGroup = atDestination;
            groupName = "ya en destino (fallback)";
        }
        
        if (selectedGroup.size() < minDestroy) {
            return randomDestroy(currentSolution, destructionRate, minDestroy, maxDestroy);
        }
        
        // REFINAMIENTO: Ordenar por productos desc para tie-break (más productos = más capacidad liberada)
        if (selectedGroup != atDestination) { // Solo si no son paquetes en destino
            selectedGroup.sort((p1, p2) -> {
                int p1Products = productsOf(p1);
                int p2Products = productsOf(p2);
                return Integer.compare(p2Products, p1Products); // Más productos primero
            });
        }
        
        // Barajar parcialmente para diversidad (mantener bias hacia más productos en el top)
        if (selectedGroup.size() > 10) {
            // Solo barajar los últimos elementos, mantener los primeros (más productos) intactos
            Collections.shuffle(selectedGroup.subList(5, selectedGroup.size()), random);
        } else {
            Collections.shuffle(selectedGroup, random);
        }
        
        int numToDestroy = Math.min(
            Math.max((int)(selectedGroup.size() * destructionRate), minDestroy),
            Math.min(maxDestroy, selectedGroup.size())
        );
        
        ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyed = new ArrayList<>();
        for (int i = 0; i < numToDestroy; i++) {
            OrderSchema sel = selectedGroup.get(i);
            ArrayList<FlightSchema> route = currentSolution.get(sel);
            if (route == null) route = new ArrayList<>(); // Protección contra nulos
            
            destroyed.add(new java.util.AbstractMap.SimpleEntry<>(sel, new ArrayList<>(route)));
            partialSolution.remove(sel);
        }
        if (Constants.VERBOSE_LOGGING) {
          System.out.println("Destrucción temporal por slack: " + numToDestroy + " paquetes del grupo " + groupName);
        }
        return new DestructionResult(partialSolution, destroyed);
    }
    
    /**
     * CORRECCIÓN COMPLETA: Destrucción de rutas congestionadas con scoring por
     * vuelo crítico + productos - urgencia
     */
    public DestructionResult congestedRouteDestroy(
            HashMap<OrderSchema, ArrayList<FlightSchema>> currentSolution,
            double destructionRate,
            int minDestroy,
            int maxDestroy) {
        
        HashMap<OrderSchema, ArrayList<FlightSchema>> partial = new HashMap<>(currentSolution);
        if (currentSolution.isEmpty()) {
            return new DestructionResult(partial, new ArrayList<>());
        }
        
        // CORRECCIÓN: Parámetros de scoring mejorados
        final double UTIL_THRESHOLD = 0.85;   // umbral de "crítico"
        final double W_UTIL = 1.0;            // peso de congestión
        final double W_PRODS = 0.25;          // peso por productos
        final double W_SLACK_PENALTY = 0.5;   // penaliza baja holgura
        
        // CORRECCIÓN: Score por paquete basado en congestión crítica + productos - urgencia
        class Cand { 
            OrderSchema pkg;
            double score; 
        }
        
        ArrayList<Cand> cands = new ArrayList<>();
        
        for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> e : currentSolution.entrySet()) {
            OrderSchema p = e.getKey();
            ArrayList<FlightSchema> r = e.getValue();
            if (r == null || r.isEmpty()) continue;
            
            int prods = productsOf(p);
            
            // CORRECCIÓN: Congestión acumulada en vuelos por encima del umbral
            double cong = 0.0;
            for (FlightSchema f : r) {
                double util = (f.getMaxCapacity() > 0) ? 
                    ((double) f.getUsedCapacity() / f.getMaxCapacity()) : 0.0;
                if (util > UTIL_THRESHOLD) {
                    cong += (util - UTIL_THRESHOLD);
                }
            }
            
            // CORRECCIÓN: Penalizar quitar paquetes con poca holgura
            double slack = slackHours(p, r);
            double slackPenalty = (slack <= 8) ? (8 - Math.max(slack, 0)) : 0.0;
            
            double score = W_UTIL * cong + W_PRODS * prods - W_SLACK_PENALTY * slackPenalty;
            if (score > 0) {
                Cand c = new Cand();
                c.pkg = p;
                c.score = score;
                cands.add(c);
            }
        }
        
        if (cands.size() < minDestroy) {
            return randomDestroy(currentSolution, destructionRate, minDestroy, maxDestroy);
        }
        
        // CORRECCIÓN: Ordenar por score desc (más alivio esperado primero)
        cands.sort((a, b) -> Double.compare(b.score, a.score));
        
        int numToDestroy = Math.min(
            Math.max((int)(cands.size() * destructionRate), minDestroy),
            Math.min(maxDestroy, cands.size())
        );
        
        ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyed = new ArrayList<>();
        for (int i = 0; i < numToDestroy; i++) {
            OrderSchema sel = cands.get(i).pkg;
            // REFINAMIENTO: Consistencia - usar currentSolution como fuente
            ArrayList<FlightSchema> route = currentSolution.get(sel);
            if (route == null) route = new ArrayList<>(); // Protección contra nulos
            
            destroyed.add(new java.util.AbstractMap.SimpleEntry<>(sel, new ArrayList<>(route)));
            partial.remove(sel);
        }
        if (Constants.VERBOSE_LOGGING){
          System.out.println("Destrucción por congestión (mejorada): " + numToDestroy + " paquetes");
        }
        return new DestructionResult(partial, destroyed);
    }
    
    /**
     * Clase para encapsular el resultado de una operación de destrucción
     */
    public static class DestructionResult {
        private HashMap<OrderSchema, ArrayList<FlightSchema>> partialSolution;
        private ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyedPackages;
        
        public DestructionResult(
                HashMap<OrderSchema, ArrayList<FlightSchema>> partialSolution,
                ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> destroyedPackages) {
            this.partialSolution = partialSolution;
            this.destroyedPackages = destroyedPackages;
        }
        
        public HashMap<OrderSchema, ArrayList<FlightSchema>> getPartialSolution() {
            return partialSolution;
        }
        
        public ArrayList<Map.Entry<OrderSchema, ArrayList<FlightSchema>>> getDestroyedPackages() {
            return destroyedPackages;
        }
        
        public int getNumDestroyedPackages() {
            return destroyedPackages.size();
        }
    }
}
