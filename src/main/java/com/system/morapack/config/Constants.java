package com.system.morapack.config;

public class Constants {
    // File paths
    public static final String AIRPORT_INFO_FILE_PATH = "data/airportInfo.txt";
    public static final String FLIGHTS_FILE_PATH = "data/flights.txt";
    public static final String PRODUCTS_FILE_PATH = "data/products.txt";
    
    // Algorithm constants

    public static final int LOWERBOUND_SOLUTION_SPACE = 100;
    public static final int UPPERBOUND_SOLUTION_SPACE = 200;
    
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

    public static final String LIMA_WAREHOUSE = "Lima, Peru";
    public static final String BRUSSELS_WAREHOUSE = "Brussels, Belgium";
    public static final String BAKU_WAREHOUSE = "Baku, Azerbaijan";
}
