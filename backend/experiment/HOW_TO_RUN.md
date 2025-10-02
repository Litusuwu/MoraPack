# How to Run the MoraPack Algorithm Comparison Experiment

This document provides step-by-step instructions for running the MoraPack algorithm comparison experiment.

## Prerequisites

- Java 8 or higher
- Python 3.6 or higher
- Maven (for building the Java project)

## Setup

1. **Build the Java project**

   Make sure the MoraPack Java project is built:
   ```
   cd /Users/litus/Documents/Universidad/9-Semester/MoraPack
   mvn clean compile
   ```

2. **Set up the Python environment**

   ```
   cd /Users/litus/Documents/Universidad/9-Semester/MoraPack/experiment
   python3 -m venv venv
   source venv/bin/activate  # On Unix/macOS
   pip install -r requirements.txt
   ```

## Running the Experiment

### Option 1: Run the full experiment (Java integration)

This option will run the actual Java algorithms and collect real data:

```
cd /Users/litus/Documents/Universidad/9-Semester/MoraPack/experiment
source venv/bin/activate
python run_simulations.py
```

The script will:
1. Start the JVM and load the Java classes
2. Run 5 simulations of each algorithm (ALNS and TabuSearch)
3. Collect the objective function values
4. Perform statistical tests
5. Generate visualizations
6. Create a comprehensive report

### Option 2: Run with sample data (for testing)

If you want to test the analysis pipeline without running the actual Java algorithms:

1. **Generate sample data**
   ```
   cd /Users/litus/Documents/Universidad/9-Semester/MoraPack/experiment
   source venv/bin/activate
   python generate_sample_data.py
   ```

2. **Run the analysis**
   ```
   python analyze_results.py --csv results/sample_data.csv
   ```

## Output Files

The experiment generates the following output files:

- **CSV Results**: `experiment/results/simulation_results_*.csv`
- **Reports**: `experiment/results/experiment_report_*.md`
- **Final Report**: `experiment/results/final_report.md`
- **Visualizations**:
  - QQ Plots: `experiment/plots/qq_plot_*.png`
  - Histograms: `experiment/plots/histogram_*.png`
  - Comparison Plot: `experiment/plots/algorithm_comparison.png`

## Customizing the Experiment

You can modify the following parameters in `run_simulations.py`:

- `NUM_SIMULATIONS`: Number of simulations to run for each algorithm (default: 5)
- `SAVE_RESULTS`: Whether to save results to CSV (default: True)
- `GENERATE_PLOTS`: Whether to generate plots (default: True)
- `VERBOSE`: Whether to print detailed information (default: True)

For the sample data generator, you can modify:

- `NUM_SIMULATIONS`: Number of simulations to generate (default: 41)

## Troubleshooting

- **JVM Issues**: If you encounter JVM-related errors, make sure the Java project is properly built and the classpath in `run_simulations.py` is correct.
- **Python Dependencies**: If you get import errors, make sure all dependencies are installed with `pip install -r requirements.txt`.
- **File Permissions**: If you can't execute the Python scripts, make them executable with `chmod +x *.py`.

## Contact

If you encounter any issues or have questions, please contact the MoraPack development team.
