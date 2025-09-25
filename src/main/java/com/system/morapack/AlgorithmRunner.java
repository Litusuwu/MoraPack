package com.system.morapack;

import com.system.morapack.schemas.algorithm.ALNS.Solution;
import com.system.morapack.schemas.algorithm.TabuSearch.TabuSearch;
import com.system.morapack.schemas.algorithm.TabuSearch.TabuSolution;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AlgorithmRunner {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java AlgorithmRunner <algorithm> <num_runs>");
            System.out.println("  algorithm: 'alns' or 'tabu'");
            System.out.println("  num_runs: number of simulations to run");
            return;
        }
        
        String algorithm = args[0].toLowerCase();
        int numRuns;
        try {
            numRuns = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: num_runs must be an integer");
            return;
        }
        
        String outputFile = "results_" + algorithm + ".csv";
        
        System.out.println("Running " + numRuns + " simulations of " + algorithm);
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            // Write CSV header
            writer.write("simulation_id,algorithm,objective_value,runtime_seconds\n");
            
            for (int i = 1; i <= numRuns; i++) {
                System.out.println("\nSimulation " + i + "/" + numRuns);
                
                long startTime = System.currentTimeMillis();
                int objectiveValue = 0;
                
                if (algorithm.equals("alns")) {
                    objectiveValue = runALNS();
                } else if (algorithm.equals("tabu")) {
                    objectiveValue = runTabuSearch();
                } else {
                    System.out.println("Error: algorithm must be 'alns' or 'tabu'");
                    return;
                }
                
                long endTime = System.currentTimeMillis();
                double runtimeSeconds = (endTime - startTime) / 1000.0;
                
                // Write result to CSV
                writer.write(i + "," + algorithm + "," + objectiveValue + "," + runtimeSeconds + "\n");
                writer.flush();
                
                System.out.println("Simulation " + i + " completed in " + runtimeSeconds + " seconds");
                System.out.println("Objective value: " + objectiveValue);
            }
            
            System.out.println("\nAll simulations completed. Results saved to " + outputFile);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }
    
    private static int runALNS() {
        System.out.println("Running ALNS algorithm...");
        
        Solution solution = new Solution();
        solution.solve();
        
        // Extract the weight from the solution HashMap
        HashMap<?, Integer> solutionMap = solution.solution;
        return solutionMap.values().iterator().next();
    }
    
    private static int runTabuSearch() {
        System.out.println("Running TabuSearch algorithm...");
        
        TabuSearch tabuSearch = new TabuSearch();
        TabuSolution bestSolution = tabuSearch.solve();
        
        return bestSolution.getScore();
    }
}
