package com.system.morapack.config;

public class Constants {
    // File paths
    public static final String AIRPORT_INFO_FILE_PATH = "data/airportInfo.txt";
    public static final String FLIGHTS_FILE_PATH = "data/flights.txt";
    public static final String PRODUCTS_FILE_PATH = "data/products.txt";
    
    // Algorithm constants

    public static final int LOWERBOUND_SOLUTION_SPACE = 100;
    public static final int UPPERBOUND_SOLUTION_SPACE = 200;
    
    // ALNS Destruction parameters optimized for MoraPack
    public static final double DESTRUCTION_RATIO = 0.15;        // 15% - Ratio moderado para ALNS
    public static final int DESTRUCTION_MIN_PACKAGES = 10;      // Mínimo 10 paquetes
    public static final int DESTRUCTION_MAX_PACKAGES = 500;     // Máximo 500 paquetes (ajustable según problema)
    public static final int DESTRUCTION_MAX_PACKAGES_EXPANSION = 100;  // Para expansiones más controladas
    
    // Delivery time constants
    public static final double SAME_CONTINENT_MAX_DELIVERY_TIME = 2.0;
    public static final double DIFFERENT_CONTINENT_MAX_DELIVERY_TIME = 3.0;
    
    public static final double SAME_CONTINENT_TRANSPORT_TIME = 0.5;
    public static final double DIFFERENT_CONTINENT_TRANSPORT_TIME = 1.0;
    
    public static final int SAME_CONTINENT_MIN_CAPACITY = 200;
    public static final int SAME_CONTINENT_MAX_CAPACITY = 300;
    public static final int DIFFERENT_CONTINENT_MIN_CAPACITY = 250;
    public static final int DIFFERENT_CONTINENT_MAX_CAPACITY = 400;
    
    public static final int WAREHOUSE_MIN_CAPACITY = 600;
    public static final int WAREHOUSE_MAX_CAPACITY = 1000;
    
    public static final int CUSTOMER_PICKUP_MAX_HOURS = 2;
    
    // NEW: Control de tipo de solución inicial
    public static final boolean USE_GREEDY_INITIAL_SOLUTION = false; // true=greedy, false=random
    public static final double RANDOM_ASSIGNMENT_PROBABILITY = 0.3; // Para solución random: 30% de asignación
    
    // Control de logs
    public static final boolean VERBOSE_LOGGING = false; // true=logs detallados, false=logs mínimos
    public static final int LOG_ITERATION_INTERVAL = 100; // Mostrar solo cada X iteraciones

    public static final String LIMA_WAREHOUSE = "Lima, Peru";
    public static final String BRUSSELS_WAREHOUSE = "Brussels, Belgium";
    public static final String BAKU_WAREHOUSE = "Baku, Azerbaijan";
}
