#!/usr/bin/env python3

import os
import sys
from jpype import JClass, getDefaultJVMPath, startJVM, isJVMStarted, shutdownJVM

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

def main():
    try:
        # Start the JVM
        classpath = os.path.join(PROJECT_ROOT, "target", "classes")
        print(f"Starting JVM with classpath: {classpath}")
        
        jvm_options = [
            f"-Djava.class.path={classpath}",
            "-ea",
            "-Xms256m",
            "-Xmx512m"
        ]
        startJVM(getDefaultJVMPath(), *jvm_options)
        print("JVM started successfully")
        
        # Load and use the test class
        TestJPype = JClass("com.system.morapack.TestJPype")
        result = TestJPype.testMethod()
        print(f"Result from Java: {result}")
        
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
