package com.system.morapack.schemas.algorithm;

import com.system.morapack.schemas.Flight;
import com.system.morapack.schemas.Package;
import com.system.morapack.schemas.City;
import com.system.morapack.schemas.Continent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
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
     * Destrucción aleatoria: elimina un número aleatorio de paquetes de la solución.
     * Prioritiza la eliminación de paquetes con mayor margen de tiempo.
     */
    public DestructionResult randomDestroy(
            HashMap<Package, ArrayList<Flight>> currentSolution,
            double destructionRate,
            int minDestroy,
            int maxDestroy) {
        
        HashMap<Package, ArrayList<Flight>> partialSolution = new HashMap<>(currentSolution);
        ArrayList<Package> packages = new ArrayList<>(currentSolution.keySet());
        
        int numPackages = packages.size();
        int numToDestroy = Math.min(
            Math.max((int)(numPackages * destructionRate), minDestroy),
            Math.min(maxDestroy, numPackages)
        );
        
        ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages = new ArrayList<>();
        
        // Ordenar paquetes por margen de tiempo (mayor margen primero para destruir)
        packages.sort((p1, p2) -> {
            LocalDateTime now = LocalDateTime.now();
            long p1Margin = ChronoUnit.HOURS.between(now, p1.getDeliveryDeadline());
            long p2Margin = ChronoUnit.HOURS.between(now, p2.getDeliveryDeadline());
            return Long.compare(p2Margin, p1Margin); // Mayor margen primero
        });
        
        // Usar selección probabilística: mayor probabilidad para paquetes con más margen
        for (int i = 0; i < numToDestroy && !packages.isEmpty(); i++) {
            int selectedIndex;
            if (packages.size() > 10) {
                // Seleccionar con sesgo hacia los primeros (más margen)
                double rand = random.nextDouble();
                if (rand < 0.6) {
                    selectedIndex = random.nextInt(Math.min(packages.size() / 3, 10));
                } else if (rand < 0.9) {
                    selectedIndex = random.nextInt(Math.min(packages.size() / 2, packages.size()));
                } else {
                    selectedIndex = random.nextInt(packages.size());
                }
            } else {
                selectedIndex = random.nextInt(packages.size());
            }
            
            Package selectedPackage = packages.get(selectedIndex);
            
            destroyedPackages.add(Map.entry(
                selectedPackage, 
                new ArrayList<>(partialSolution.get(selectedPackage))
            ));
            
            partialSolution.remove(selectedPackage);
            packages.remove(selectedIndex);
        }
        
        return new DestructionResult(partialSolution, destroyedPackages);
    }
    
    /**
     * Destrucción por zona geográfica: elimina paquetes de un continente específico.
     * Útil para liberar capacidad en rutas intercontinentales.
     */
    public DestructionResult geographicDestroy(
            HashMap<Package, ArrayList<Flight>> currentSolution,
            double destructionRate,
            int minDestroy,
            int maxDestroy) {
        
        HashMap<Package, ArrayList<Flight>> partialSolution = new HashMap<>(currentSolution);
        
        if (currentSolution.isEmpty()) {
            return new DestructionResult(partialSolution, new ArrayList<>());
        }
        
        // Contar paquetes por continente (origen y destino)
        Map<Continent, ArrayList<Package>> packagesByOriginContinent = new HashMap<>();
        Map<Continent, ArrayList<Package>> packagesByDestinationContinent = new HashMap<>();
        
        for (Package pkg : currentSolution.keySet()) {
            Continent originContinent = pkg.getCurrentLocation().getContinent();
            Continent destinationContinent = pkg.getDestinationCity().getContinent();
            
            packagesByOriginContinent.computeIfAbsent(originContinent, k -> new ArrayList<>()).add(pkg);
            packagesByDestinationContinent.computeIfAbsent(destinationContinent, k -> new ArrayList<>()).add(pkg);
        }
        
        // Seleccionar continente con más paquetes intercontinentales
        Continent selectedContinent = null;
        int maxIntercontinentalPackages = 0;
        
        for (Continent continent : Continent.values()) {
            ArrayList<Package> originPackages = packagesByOriginContinent.getOrDefault(continent, new ArrayList<>());
            int intercontinentalCount = 0;
            
            for (Package pkg : originPackages) {
                if (pkg.getCurrentLocation().getContinent() != pkg.getDestinationCity().getContinent()) {
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
        ArrayList<Package> candidatePackages = new ArrayList<>();
        for (Package pkg : currentSolution.keySet()) {
            if (pkg.getCurrentLocation().getContinent() == selectedContinent ||
                pkg.getDestinationCity().getContinent() == selectedContinent) {
                candidatePackages.add(pkg);
            }
        }
        
        if (candidatePackages.size() < minDestroy) {
            return randomDestroy(currentSolution, destructionRate, minDestroy, maxDestroy);
        }
        
        int numToDestroy = Math.min(
            Math.max((int)(candidatePackages.size() * destructionRate), minDestroy),
            Math.min(maxDestroy, candidatePackages.size())
        );
        
        ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages = new ArrayList<>();
        
        // Priorizar paquetes intercontinentales y con mayor margen de tiempo
        candidatePackages.sort((p1, p2) -> {
            // Primero: intercontinental vs continental
            boolean p1Intercontinental = p1.getCurrentLocation().getContinent() != p1.getDestinationCity().getContinent();
            boolean p2Intercontinental = p2.getCurrentLocation().getContinent() != p2.getDestinationCity().getContinent();
            
            if (p1Intercontinental != p2Intercontinental) {
                return Boolean.compare(p2Intercontinental, p1Intercontinental);
            }
            
            // Segundo: mayor margen de tiempo
            LocalDateTime now = LocalDateTime.now();
            long p1Margin = ChronoUnit.HOURS.between(now, p1.getDeliveryDeadline());
            long p2Margin = ChronoUnit.HOURS.between(now, p2.getDeliveryDeadline());
            return Long.compare(p2Margin, p1Margin);
        });
        
        // Seleccionar paquetes con sesgo hacia los intercontinentales
        for (int i = 0; i < numToDestroy; i++) {
            Package selectedPackage = candidatePackages.get(i);
            
            destroyedPackages.add(Map.entry(
                selectedPackage, 
                new ArrayList<>(partialSolution.get(selectedPackage))
            ));
            
            partialSolution.remove(selectedPackage);
        }
        
        System.out.println("Destrucción geográfica: " + numToDestroy + 
                          " paquetes eliminados del continente " + selectedContinent);
        
        return new DestructionResult(partialSolution, destroyedPackages);
    }
    
    /**
     * Destrucción por tiempo: elimina paquetes con deadlines en un rango específico.
     * Útil para rebalancear la carga temporal.
     */
    public DestructionResult timeBasedDestroy(
            HashMap<Package, ArrayList<Flight>> currentSolution,
            double destructionRate,
            int minDestroy,
            int maxDestroy) {
        
        HashMap<Package, ArrayList<Flight>> partialSolution = new HashMap<>(currentSolution);
        
        if (currentSolution.isEmpty()) {
            return new DestructionResult(partialSolution, new ArrayList<>());
        }
        
        // Agrupar paquetes por rango de deadline
        LocalDateTime now = LocalDateTime.now();
        ArrayList<Package> shortTermPackages = new ArrayList<>(); // <= 24 horas
        ArrayList<Package> mediumTermPackages = new ArrayList<>(); // 24-96 horas
        ArrayList<Package> longTermPackages = new ArrayList<>(); // > 96 horas
        
        for (Package pkg : currentSolution.keySet()) {
            long hoursUntilDeadline = ChronoUnit.HOURS.between(now, pkg.getDeliveryDeadline());
            
            if (hoursUntilDeadline <= 24) {
                shortTermPackages.add(pkg);
            } else if (hoursUntilDeadline <= 96) {
                mediumTermPackages.add(pkg);
            } else {
                longTermPackages.add(pkg);
            }
        }
        
        // Seleccionar grupo con más paquetes (excluyendo short-term para preservar entregas urgentes)
        ArrayList<Package> selectedGroup;
        String groupName;
        
        if (longTermPackages.size() >= mediumTermPackages.size() && !longTermPackages.isEmpty()) {
            selectedGroup = longTermPackages;
            groupName = "largo plazo";
        } else if (!mediumTermPackages.isEmpty()) {
            selectedGroup = mediumTermPackages;
            groupName = "mediano plazo";
        } else if (!shortTermPackages.isEmpty()) {
            // Solo usar short-term como último recurso
            selectedGroup = shortTermPackages;
            groupName = "corto plazo";
        } else {
            return new DestructionResult(partialSolution, new ArrayList<>());
        }
        
        if (selectedGroup.size() < minDestroy) {
            return randomDestroy(currentSolution, destructionRate, minDestroy, maxDestroy);
        }
        
        int numToDestroy = Math.min(
            Math.max((int)(selectedGroup.size() * destructionRate), minDestroy),
            Math.min(maxDestroy, selectedGroup.size())
        );
        
        ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages = new ArrayList<>();
        
        // Barajar para selección aleatoria dentro del grupo
        Collections.shuffle(selectedGroup, random);
        
        for (int i = 0; i < numToDestroy; i++) {
            Package selectedPackage = selectedGroup.get(i);
            
            destroyedPackages.add(Map.entry(
                selectedPackage, 
                new ArrayList<>(partialSolution.get(selectedPackage))
            ));
            
            partialSolution.remove(selectedPackage);
        }
        
        System.out.println("Destrucción temporal: " + numToDestroy + 
                          " paquetes eliminados del grupo " + groupName);
        
        return new DestructionResult(partialSolution, destroyedPackages);
    }
    
    /**
     * Destrucción de rutas congestionadas: elimina paquetes que usan vuelos 
     * con alta utilización de capacidad.
     * OPTIMIZADO: Evita cálculos costosos y bucles innecesarios.
     */
    public DestructionResult congestedRouteDestroy(
            HashMap<Package, ArrayList<Flight>> currentSolution,
            double destructionRate,
            int minDestroy,
            int maxDestroy) {
        
        System.out.println("      Iniciando congestedRouteDestroy...");
        HashMap<Package, ArrayList<Flight>> partialSolution = new HashMap<>(currentSolution);
        
        if (currentSolution.isEmpty()) {
            System.out.println("      Solución vacía, retornando...");
            return new DestructionResult(partialSolution, new ArrayList<>());
        }
        
        // OPTIMIZACIÓN: Calcular tiempo una sola vez fuera del sort
        System.out.println("      Calculando tiempo...");
        LocalDateTime now = LocalDateTime.now();
        
        // OPTIMIZACIÓN: Versión simplificada para debugging
        System.out.println("      Analizando " + currentSolution.size() + " paquetes...");
        ArrayList<Package> congestedPackages = new ArrayList<>();
        
        // SIMPLIFICACIÓN: Solo tomar los primeros 10 paquetes para evitar bucles largos
        int maxPackagesToAnalyze = Math.min(currentSolution.size(), 10);
        int count = 0;
        
        for (Map.Entry<Package, ArrayList<Flight>> entry : currentSolution.entrySet()) {
            if (count >= maxPackagesToAnalyze) break;
            
            Package pkg = entry.getKey();
            ArrayList<Flight> route = entry.getValue();
            
            if (route.isEmpty()) continue; // OPTIMIZACIÓN: Evitar división por cero
            
            // SIMPLIFICACIÓN: Solo verificar si hay al menos un vuelo con alta utilización
            boolean isCongested = false;
            for (Flight flight : route) {
                double utilization = (double) flight.getUsedCapacity() / flight.getMaxCapacity();
                if (utilization > 0.7) {
                    isCongested = true;
                    break; // OPTIMIZACIÓN: Salir del bucle interno tan pronto como encontremos uno
                }
            }
            
            if (isCongested) {
                congestedPackages.add(pkg);
            }
            
            count++;
        }
        
        System.out.println("      Paquetes congestionados encontrados: " + congestedPackages.size());
        
        // OPTIMIZACIÓN: Si no hay suficientes paquetes congestionados, usar random destroy
        if (congestedPackages.size() < minDestroy) {
            System.out.println("      Pocos paquetes congestionados, usando random destroy...");
            return randomDestroy(currentSolution, destructionRate, minDestroy, maxDestroy);
        }
        
        // OPTIMIZACIÓN: Ordenar usando tiempo pre-calculado y límite de paquetes
        System.out.println("      Ordenando paquetes...");
        int maxPackagesToSort = Math.min(congestedPackages.size(), 100); // Limitar sorting a 100 paquetes
        congestedPackages.sort((p1, p2) -> {
            // OPTIMIZACIÓN: Usar tiempo ya calculado
            long p1Margin = ChronoUnit.HOURS.between(now, p1.getDeliveryDeadline());
            long p2Margin = ChronoUnit.HOURS.between(now, p2.getDeliveryDeadline());
            return Long.compare(p2Margin, p1Margin);
        });
        System.out.println("      Ordenamiento completado");
        
        // OPTIMIZACIÓN: Limitar el número de paquetes a procesar
        if (congestedPackages.size() > maxPackagesToSort) {
            congestedPackages = new ArrayList<>(congestedPackages.subList(0, maxPackagesToSort));
        }
        
        int numToDestroy = Math.min(
            Math.max((int)(congestedPackages.size() * destructionRate), minDestroy),
            Math.min(maxDestroy, congestedPackages.size())
        );
        
        ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages = new ArrayList<>();
        
        for (int i = 0; i < numToDestroy; i++) {
            Package selectedPackage = congestedPackages.get(i);
            
            destroyedPackages.add(Map.entry(
                selectedPackage, 
                new ArrayList<>(partialSolution.get(selectedPackage))
            ));
            
            partialSolution.remove(selectedPackage);
        }
        
        System.out.println("Destrucción por congestión: " + numToDestroy + 
                          " paquetes eliminados de rutas congestionadas");
        
        return new DestructionResult(partialSolution, destroyedPackages);
    }
    
    /**
     * Clase para encapsular el resultado de una operación de destrucción
     */
    public static class DestructionResult {
        private HashMap<Package, ArrayList<Flight>> partialSolution;
        private ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages;
        
        public DestructionResult(
                HashMap<Package, ArrayList<Flight>> partialSolution,
                ArrayList<Map.Entry<Package, ArrayList<Flight>>> destroyedPackages) {
            this.partialSolution = partialSolution;
            this.destroyedPackages = destroyedPackages;
        }
        
        public HashMap<Package, ArrayList<Flight>> getPartialSolution() {
            return partialSolution;
        }
        
        public ArrayList<Map.Entry<Package, ArrayList<Flight>>> getDestroyedPackages() {
            return destroyedPackages;
        }
        
        public int getNumDestroyedPackages() {
            return destroyedPackages.size();
        }
    }
}
