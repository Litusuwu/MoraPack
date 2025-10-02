#!/usr/bin/env python3
"""
Analyze results from algorithm simulations.
This script can be used to analyze either real data from Java algorithms or sample data.
"""

import os
import sys
import time
import argparse
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from scipy import stats
from tabulate import tabulate

# Paths
RESULTS_DIR = os.path.join(os.path.dirname(__file__), 'results')
PLOTS_DIR = os.path.join(os.path.dirname(__file__), 'plots')

# Ensure directories exist
os.makedirs(RESULTS_DIR, exist_ok=True)
os.makedirs(PLOTS_DIR, exist_ok=True)

def check_normality(data, algorithm_name):
    """
    Check if the data follows a normal distribution using the Shapiro-Wilk test.
    Returns the test statistic and p-value.
    """
    if len(data) < 3:
        print(f"Warning: Not enough data points for {algorithm_name} to perform Shapiro-Wilk test")
        return None, None
    
    # Perform Shapiro-Wilk test
    statistic, p_value = stats.shapiro(data)
    
    print(f"Shapiro-Wilk test for {algorithm_name}:")
    print(f"  Statistic: {statistic:.6f}")
    print(f"  p-value: {p_value:.6f}")
    print(f"  {'Normally distributed' if p_value > 0.05 else 'Not normally distributed'}")
    
    return statistic, p_value

def plot_qq(data, algorithm_name):
    """Generate a QQ plot to visually assess normality."""
    plt.figure(figsize=(10, 6))
    
    # Create QQ plot
    stats.probplot(data, dist="norm", plot=plt)
    
    plt.title(f"QQ-Plot Fitness {algorithm_name}")
    plt.xlabel("Theoretical Quantiles")
    plt.ylabel("Sample Quantiles")
    
    # Save the plot
    plt.tight_layout()
    qq_path = os.path.join(PLOTS_DIR, f"qq_plot_{algorithm_name.lower()}.png")
    plt.savefig(qq_path)
    print(f"QQ plot saved to {qq_path}")
    plt.close()

def plot_histogram(data, algorithm_name):
    """Generate a histogram with density curve."""
    plt.figure(figsize=(10, 6))
    
    # Plot histogram
    sns.histplot(data, kde=True, color='skyblue' if algorithm_name == 'ALNS' else 'lightcoral')
    
    plt.title(f"Histograma y densidad {algorithm_name}")
    plt.xlabel(f"Fitness {algorithm_name}")
    plt.ylabel("Density")
    
    # Save the plot
    plt.tight_layout()
    hist_path = os.path.join(PLOTS_DIR, f"histogram_{algorithm_name.lower()}.png")
    plt.savefig(hist_path)
    print(f"Histogram saved to {hist_path}")
    plt.close()

def perform_wilcoxon_test(alns_data, tabu_data):
    """Perform Wilcoxon signed-rank test to compare the two algorithms."""
    # Ensure data has the same length
    min_len = min(len(alns_data), len(tabu_data))
    alns_data = alns_data[:min_len]
    tabu_data = tabu_data[:min_len]
    
    # Two-tailed test (are they different?)
    statistic, p_value_two_tailed = stats.wilcoxon(alns_data, tabu_data)
    
    print("\nWilcoxon Signed-Rank Test (Two-tailed):")
    print(f"  Statistic: {statistic}")
    print(f"  p-value: {p_value_two_tailed:.6e}")
    print(f"  {'Significant difference' if p_value_two_tailed < 0.05 else 'No significant difference'}")
    
    # One-tailed test (is one better than the other?)
    # For one-tailed test, we need to divide the p-value by 2 if we have a directional hypothesis
    # We need to check which algorithm has higher median to determine the direction
    if np.median(alns_data) > np.median(tabu_data):
        p_value_one_tailed = p_value_two_tailed / 2
        better_algorithm = "ALNS"
        worse_algorithm = "TabuSearch"
    else:
        p_value_one_tailed = p_value_two_tailed / 2
        better_algorithm = "TabuSearch"
        worse_algorithm = "ALNS"
    
    print("\nWilcoxon Signed-Rank Test (One-tailed):")
    print(f"  Hypothesis: {better_algorithm} is better than {worse_algorithm}")
    print(f"  p-value: {p_value_one_tailed:.6e}")
    print(f"  {'Hypothesis confirmed' if p_value_one_tailed < 0.05 else 'Hypothesis not confirmed'}")
    
    return {
        "two_tailed": {
            "statistic": statistic,
            "p_value": p_value_two_tailed,
            "significant": p_value_two_tailed < 0.05
        },
        "one_tailed": {
            "better_algorithm": better_algorithm,
            "worse_algorithm": worse_algorithm,
            "p_value": p_value_one_tailed,
            "significant": p_value_one_tailed < 0.05
        }
    }

def generate_comparison_plot(alns_data, tabu_data):
    """Generate a box plot comparing the two algorithms."""
    plt.figure(figsize=(10, 6))
    
    # Create a DataFrame for seaborn
    df = pd.DataFrame({
        'ALNS': alns_data,
        'TabuSearch': tabu_data
    })
    
    # Melt the DataFrame for seaborn
    df_melted = pd.melt(df, var_name='Algorithm', value_name='Objective Value')
    
    # Create box plot
    sns.boxplot(x='Algorithm', y='Objective Value', data=df_melted)
    
    # Add individual data points
    sns.stripplot(x='Algorithm', y='Objective Value', data=df_melted, 
                 color='black', alpha=0.5, jitter=True)
    
    plt.title("Comparison of Algorithm Performance")
    plt.ylabel("Objective Function Value")
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    
    # Save the plot
    plt.tight_layout()
    comparison_path = os.path.join(PLOTS_DIR, "algorithm_comparison.png")
    plt.savefig(comparison_path)
    print(f"Comparison plot saved to {comparison_path}")
    plt.close()

def generate_report(results_df, normality_results, wilcoxon_results):
    """Generate a comprehensive report of the experiment."""
    timestamp = time.strftime("%Y%m%d-%H%M%S")
    report_path = os.path.join(RESULTS_DIR, f"experiment_report_{timestamp}.md")
    
    # Extract data
    alns_data = results_df[results_df['algorithm'] == 'ALNS']['objective_value'].dropna().values
    tabu_data = results_df[results_df['algorithm'] == 'TabuSearch']['objective_value'].dropna().values
    
    # Calculate statistics
    alns_stats = {
        'mean': np.mean(alns_data),
        'median': np.median(alns_data),
        'std': np.std(alns_data),
        'min': np.min(alns_data),
        'max': np.max(alns_data)
    }
    
    tabu_stats = {
        'mean': np.mean(tabu_data),
        'median': np.median(tabu_data),
        'std': np.std(tabu_data),
        'min': np.min(tabu_data),
        'max': np.max(tabu_data)
    }
    
    # Create the report
    with open(report_path, 'w') as f:
        f.write("# MoraPack Algorithm Comparison Experiment Report\n\n")
        f.write(f"Date: {time.strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        
        f.write("## Experiment Configuration\n\n")
        f.write(f"- Number of simulations: {len(alns_data)}\n")
        
        f.write("\n## Summary Statistics\n\n")
        
        # Create a table with statistics
        stats_table = [
            ["Metric", "ALNS", "TabuSearch"],
            ["Mean", f"{alns_stats['mean']:.2f}", f"{tabu_stats['mean']:.2f}"],
            ["Median", f"{alns_stats['median']:.2f}", f"{tabu_stats['median']:.2f}"],
            ["Std Dev", f"{alns_stats['std']:.2f}", f"{tabu_stats['std']:.2f}"],
            ["Min", f"{alns_stats['min']:.2f}", f"{tabu_stats['min']:.2f}"],
            ["Max", f"{alns_stats['max']:.2f}", f"{tabu_stats['max']:.2f}"]
        ]
        
        f.write(tabulate(stats_table, headers="firstrow", tablefmt="pipe"))
        
        f.write("\n\n## Normality Tests\n\n")
        f.write("### ALNS\n\n")
        f.write(f"- Shapiro-Wilk test statistic: {normality_results['ALNS']['statistic']:.6f}\n")
        f.write(f"- p-value: {normality_results['ALNS']['p_value']:.6e}\n")
        f.write(f"- Conclusion: {'Normally distributed' if normality_results['ALNS']['p_value'] > 0.05 else 'Not normally distributed'}\n\n")
        
        f.write("### TabuSearch\n\n")
        f.write(f"- Shapiro-Wilk test statistic: {normality_results['TabuSearch']['statistic']:.6f}\n")
        f.write(f"- p-value: {normality_results['TabuSearch']['p_value']:.6e}\n")
        f.write(f"- Conclusion: {'Normally distributed' if normality_results['TabuSearch']['p_value'] > 0.05 else 'Not normally distributed'}\n\n")
        
        f.write("## Statistical Comparison\n\n")
        
        f.write("### Wilcoxon Signed-Rank Test (Two-tailed)\n\n")
        f.write("H0: The medians of ALNS and TabuSearch are equal.\n")
        f.write("H1: The medians of ALNS and TabuSearch are different.\n\n")
        f.write(f"- Test statistic: {wilcoxon_results['two_tailed']['statistic']}\n")
        f.write(f"- p-value: {wilcoxon_results['two_tailed']['p_value']:.6e}\n")
        f.write(f"- Conclusion: {'Reject H0' if wilcoxon_results['two_tailed']['significant'] else 'Fail to reject H0'}\n\n")
        
        f.write("### Wilcoxon Signed-Rank Test (One-tailed)\n\n")
        better = wilcoxon_results['one_tailed']['better_algorithm']
        worse = wilcoxon_results['one_tailed']['worse_algorithm']
        f.write(f"H0: The median of {better} is less than or equal to the median of {worse}.\n")
        f.write(f"H1: The median of {better} is greater than the median of {worse}.\n\n")
        f.write(f"- p-value: {wilcoxon_results['one_tailed']['p_value']:.6e}\n")
        f.write(f"- Conclusion: {'Reject H0' if wilcoxon_results['one_tailed']['significant'] else 'Fail to reject H0'}\n\n")
        
        f.write("## Conclusion\n\n")
        if wilcoxon_results['two_tailed']['significant']:
            f.write("There is a statistically significant difference between the performance of ALNS and TabuSearch algorithms.\n\n")
            if wilcoxon_results['one_tailed']['significant']:
                f.write(f"The {better} algorithm consistently outperforms the {worse} algorithm in terms of objective function value.\n")
        else:
            f.write("There is no statistically significant difference between the performance of ALNS and TabuSearch algorithms.\n")
        
        # Add references to plots
        f.write("\n## Visualizations\n\n")
        f.write("### QQ Plots\n\n")
        f.write("![ALNS QQ Plot](../plots/qq_plot_alns.png)\n\n")
        f.write("![TabuSearch QQ Plot](../plots/qq_plot_tabusearch.png)\n\n")
        
        f.write("### Histograms\n\n")
        f.write("![ALNS Histogram](../plots/histogram_alns.png)\n\n")
        f.write("![TabuSearch Histogram](../plots/histogram_tabusearch.png)\n\n")
        
        f.write("### Comparison\n\n")
        f.write("![Algorithm Comparison](../plots/algorithm_comparison.png)\n")
    
    print(f"Report generated and saved to {report_path}")
    return report_path

def analyze_data(csv_path):
    """Analyze data from a CSV file."""
    print(f"Analyzing data from {csv_path}...")
    
    # Load data
    results_df = pd.read_csv(csv_path)
    
    # Extract data for each algorithm
    alns_data = results_df[results_df['algorithm'] == 'ALNS']['objective_value'].dropna().values
    tabu_data = results_df[results_df['algorithm'] == 'TabuSearch']['objective_value'].dropna().values
    
    print(f"Found {len(alns_data)} ALNS data points and {len(tabu_data)} TabuSearch data points")
    
    # Check normality
    print("\nChecking normality of data...")
    alns_normality = check_normality(alns_data, "ALNS")
    tabu_normality = check_normality(tabu_data, "TabuSearch")
    
    normality_results = {
        "ALNS": {
            "statistic": alns_normality[0],
            "p_value": alns_normality[1]
        },
        "TabuSearch": {
            "statistic": tabu_normality[0],
            "p_value": tabu_normality[1]
        }
    }
    
    # Generate plots
    print("\nGenerating plots...")
    plot_qq(alns_data, "ALNS")
    plot_qq(tabu_data, "TabuSearch")
    plot_histogram(alns_data, "ALNS")
    plot_histogram(tabu_data, "TabuSearch")
    generate_comparison_plot(alns_data, tabu_data)
    
    # Perform statistical tests
    print("\nPerforming statistical tests...")
    wilcoxon_results = perform_wilcoxon_test(alns_data, tabu_data)
    
    # Generate report
    print("\nGenerating final report...")
    report_path = generate_report(results_df, normality_results, wilcoxon_results)
    
    print("\nAnalysis completed successfully!")
    print(f"Report available at: {report_path}")
    
    return report_path

def main():
    """Main function."""
    parser = argparse.ArgumentParser(description="Analyze algorithm simulation results.")
    parser.add_argument("--csv", help="Path to CSV file with simulation results", required=True)
    args = parser.parse_args()
    
    try:
        analyze_data(args.csv)
    except Exception as e:
        print(f"Error during analysis: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
