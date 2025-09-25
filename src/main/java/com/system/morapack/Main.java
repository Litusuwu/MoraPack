package com.system.morapack;

import com.system.morapack.schemas.algorithm.ALNS.Solution;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting MoraPack Solution");
        
        // Create a new solution instance
        Solution solution = new Solution();
        
        // Run the solve method
        solution.solve();
        
        System.out.println("MoraPack Solution completed");
    }
}
