#!/usr/bin/env python3
"""
Generate sample data for testing the statistical analysis and visualization code.
This script creates synthetic data that mimics the output of the ALNS and TabuSearch algorithms.
"""

import os
import time
import numpy as np
import pandas as pd

# Configuration
NUM_SIMULATIONS = 41  # Match the number in the example (41 simulations)
SAVE_PATH = os.path.join(os.path.dirname(__file__), 'results', 'sample_data.csv')

def generate_sample_data():
    """Generate synthetic data for ALNS and TabuSearch algorithms."""
    # Create a list to store results
    results = []
    
    # Set random seed for reproducibility
    np.random.seed(42)
    
    # Generate ALNS data (lower performance)
    # Based on the table in the images:
    # Row 1: 1879752, 2032548
    # Row 2: 2842474, 3610644
    # Row 3: 3449276, 6925394
    # Row 4: 3068446, 8480934
    # Row 5: 199720, 8038892
    
    # Calculate mean and std from the example data
    alns_values = np.array([1879752, 2842474, 3449276, 3068446, 199720])
    alns_mean = np.mean(alns_values)
    alns_std = np.std(alns_values)
    
    # Generate more data points with similar distribution
    for i in range(1, NUM_SIMULATIONS + 1):
        # Generate a value from a normal distribution with the same mean and std
        # but ensure it's positive by taking the absolute value
        value = abs(np.random.normal(alns_mean, alns_std))
        
        # Add some random noise to make it look more realistic
        runtime = np.random.uniform(30, 60)  # Random runtime between 30-60 seconds
        
        results.append({
            "algorithm": "ALNS",
            "simulation_id": i,
            "objective_value": int(value),
            "runtime_seconds": runtime
        })
    
    # Generate TabuSearch data (higher performance)
    tabu_values = np.array([2032548, 3610644, 6925394, 8480934, 8038892])
    tabu_mean = np.mean(tabu_values)
    tabu_std = np.std(tabu_values)
    
    for i in range(1, NUM_SIMULATIONS + 1):
        # Generate a value from a normal distribution with the same mean and std
        # but ensure it's positive by taking the absolute value
        value = abs(np.random.normal(tabu_mean, tabu_std))
        
        # Add some random noise to make it look more realistic
        runtime = np.random.uniform(40, 70)  # Random runtime between 40-70 seconds
        
        results.append({
            "algorithm": "TabuSearch",
            "simulation_id": i,
            "objective_value": int(value),
            "runtime_seconds": runtime
        })
    
    # Convert to DataFrame
    df = pd.DataFrame(results)
    
    # Ensure the directory exists
    os.makedirs(os.path.dirname(SAVE_PATH), exist_ok=True)
    
    # Save to CSV
    df.to_csv(SAVE_PATH, index=False)
    print(f"Sample data saved to {SAVE_PATH}")
    
    return df

if __name__ == "__main__":
    generate_sample_data()
