package com.system.morapack;

import com.system.morapack.schemas.algorithm.ALNS.Solution;
import com.system.morapack.schemas.algorithm.TabuSearch.TabuSearch;
import com.system.morapack.schemas.algorithm.TabuSearch.TabuSolution;
import com.system.morapack.config.Constants;

/**
 * Clase principal para ejecutar los algoritmos de optimización
 */
public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            String algorithm = args[0].toLowerCase();
            
            switch (algorithm) {
                case "alns":
                    runALNS();
                    break;
                case "tabu":
                    runTabuSearch();
                    break;
                case "compare":
                    compareAlgorithms();
                    break;
                default:
                    System.out.println("Algoritmo no reconocido: " + algorithm);
                    System.out.println("Opciones disponibles: alns, tabu, compare");
                    runALNS(); // Por defecto, ejecutar ALNS
            }
        } else {
            // Si no se especifica algoritmo, ejecutar ambos para comparar
            compareAlgorithms();
        }
    }
    
    /**
     * Ejecuta el algoritmo ALNS
     */
    public static void runALNS() {
        System.out.println("===========================================");
        System.out.println("EJECUTANDO ALGORITMO ALNS");
        System.out.println("===========================================");
        
        Solution solution = new Solution();
        solution.solve();
    }
    
    /**
     * Ejecuta el algoritmo Tabu Search
     */
    public static void runTabuSearch() {
        System.out.println("===========================================");
        System.out.println("EJECUTANDO ALGORITMO TABU SEARCH");
        System.out.println("===========================================");
        
        TabuSearch tabuSearch = new TabuSearch(
                Constants.AIRPORT_INFO_FILE_PATH,
                Constants.FLIGHTS_FILE_PATH,
                Constants.PRODUCTS_FILE_PATH,
                1000, // maxIterations
                100,  // maxNoImprovement
                100,  // neighborhoodSize
                50,   // tabuListSize
                10000 // tabuTenure (ms)
        );
        
        TabuSolution bestSolution = tabuSearch.solve();
        tabuSearch.printSolutionDescription(bestSolution, 2);
    }
    
    /**
     * Compara los algoritmos ALNS y Tabu Search
     */
    public static void compareAlgorithms() {
        System.out.println("===========================================");
        System.out.println("COMPARACIÓN DE ALGORITMOS: ALNS vs TABU SEARCH");
        System.out.println("===========================================");
        
        // Ejecutar ALNS
        System.out.println("\n1. EJECUTANDO ALNS:");
        long alnsStartTime = System.currentTimeMillis();
        Solution alnsSolution = new Solution();
        alnsSolution.solve();
        long alnsEndTime = System.currentTimeMillis();
        long alnsTotalTime = (alnsEndTime - alnsStartTime) / 1000;
        
        // Ejecutar Tabu Search
        System.out.println("\n2. EJECUTANDO TABU SEARCH:");
        long tabuStartTime = System.currentTimeMillis();
        TabuSearch tabuSearch = new TabuSearch(
                Constants.AIRPORT_INFO_FILE_PATH,
                Constants.FLIGHTS_FILE_PATH,
                Constants.PRODUCTS_FILE_PATH,
                1000, // maxIterations
                100,  // maxNoImprovement
                100,  // neighborhoodSize
                50,   // tabuListSize
                10000 // tabuTenure (ms)
        );
        TabuSolution tabuSolution = tabuSearch.solve();
        long tabuEndTime = System.currentTimeMillis();
        long tabuTotalTime = (tabuEndTime - tabuStartTime) / 1000;
        
        // Mostrar resumen de comparación
        System.out.println("\n===========================================");
        System.out.println("RESUMEN DE COMPARACIÓN");
        System.out.println("===========================================");
        System.out.println("ALNS:");
        System.out.println("  - Tiempo total: " + alnsTotalTime + " segundos");
        // Aquí podrías añadir más métricas de ALNS
        
        System.out.println("\nTabu Search:");
        System.out.println("  - Tiempo total: " + tabuTotalTime + " segundos");
        System.out.println("  - Paquetes asignados: " + tabuSolution.getAssignedPackagesCount());
        System.out.println("  - Peso de la solución: " + tabuSolution.getScore());
        
        System.out.println("\nCONCLUSIÓN:");
        System.out.println("Los algoritmos utilizan diferentes estrategias para resolver el mismo problema.");
        System.out.println("ALNS se enfoca en destrucción y reconstrucción parcial de soluciones.");
        System.out.println("Tabu Search explora el espacio de soluciones evitando ciclos con memoria tabú.");
    }
}