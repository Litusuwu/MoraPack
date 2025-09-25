# MoraPack Algorithm Comparison Experiment

This directory contains the experimental setup for comparing ALNS and TabuSearch algorithms in the MoraPack package routing system.

## Setup

1. Create a Python virtual environment:
   ```
   python3 -m venv venv
   ```

2. Activate the virtual environment:
   ```
   source venv/bin/activate  # On Unix/macOS
   venv\Scripts\activate     # On Windows
   ```

3. Install dependencies:
   ```
   pip install -r requirements.txt
   ```

## Running the Experiment

To run the experiment:

```
python run_simulations.py
```

This will:
1. Run multiple simulations of both ALNS and TabuSearch algorithms
2. Collect objective function values
3. Perform statistical tests (Shapiro-Wilk for normality, Wilcoxon for comparison)
4. Generate visualizations (QQ plots, histograms, comparison plots)
5. Create a comprehensive report

## Directory Structure

- `venv/` - Python virtual environment
- `results/` - CSV files with simulation results and reports
- `plots/` - Generated visualizations

## Configuration

You can modify the following parameters in `run_simulations.py`:

- `NUM_SIMULATIONS` - Number of simulations to run for each algorithm
- `SAVE_RESULTS` - Whether to save results to CSV
- `GENERATE_PLOTS` - Whether to generate plots
- `VERBOSE` - Whether to print detailed information

## Statistical Analysis

The experiment performs the following statistical analyses:

1. Shapiro-Wilk test for normality of objective function values
2. Wilcoxon signed-rank test for comparing the performance of the two algorithms
3. Visual analysis using QQ plots and histograms
