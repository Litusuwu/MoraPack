#!/usr/bin/env python3

import os
import sys
import time
import argparse
from jpype import JClass, getDefaultJVMPath, startJVM, isJVMStarted, shutdownJVM

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

def setup_jvm():
    if not isJVMStarted():
        classpath = os.path.join(PROJECT_ROOT, "target", "classes")
        
        print(f"Starting JVM with classpath: {classpath}")
        jvm_options = [
            f"-Djava.class.path={classpath}",
            "-ea",
            "-Xms512m",
            "-Xmx1024m",
            "-XX:MaxDirectMemorySize=512m"
        ]
        startJVM(getDefaultJVMPath(), *jvm_options)
        print("JVM started successfully")
    else:
        print("JVM is already running")

def run_alns():
    try:
        ALNSSolution = JClass("com.system.morapack.schemas.algorithm.ALNS.Solution")
        
        print("Running ALNS simulation...")
        start_time = time.time()
        
        solution = ALNSSolution()
        solution.solve()
        
        solution_map = solution.solution
        weight = list(solution_map.values())[0]
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"ALNS completed in {elapsed_time:.2f} seconds")
        print(f"Objective function value: {weight}")
        
        return weight, elapsed_time
    
    except Exception as e:
        print(f"Error in ALNS simulation: {str(e)}")
        import traceback
        traceback.print_exc()
        return None, None

def run_tabu_search():
    try:
        TabuSearch = JClass("com.system.morapack.schemas.algorithm.TabuSearch.TabuSearch")
        
        print("Running TabuSearch simulation...")
        start_time = time.time()
        
        tabu_search = TabuSearch()
        tabu_search.solve()
        
        best_solution = tabu_search.getBestSolution()
        score = best_solution.getScore()
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"TabuSearch completed in {elapsed_time:.2f} seconds")
        print(f"Objective function value: {score}")
        
        return score, elapsed_time
    
    except Exception as e:
        print(f"Error in TabuSearch simulation: {str(e)}")
        import traceback
        traceback.print_exc()
        return None, None

def main():
    parser = argparse.ArgumentParser(description="Run a single algorithm simulation")
    parser.add_argument("algorithm", choices=["alns", "tabu"], help="Algorithm to run (alns or tabu)")
    args = parser.parse_args()
    
    try:
        setup_jvm()
        
        if args.algorithm.lower() == "alns":
            run_alns()
        elif args.algorithm.lower() == "tabu":
            run_tabu_search()
        
    except Exception as e:
        print(f"Error: {str(e)}")
        import traceback
        traceback.print_exc()
    finally:
        if isJVMStarted():
            print("Shutting down JVM...")
            shutdownJVM()

if __name__ == "__main__":
    main()
