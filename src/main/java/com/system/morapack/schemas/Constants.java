package src.schemas;

public class Constants {
    // Delivery time constraints (in days)
    public static final double SAME_CONTINENT_MAX_DELIVERY_TIME = 2.0;
    public static final double DIFFERENT_CONTINENT_MAX_DELIVERY_TIME = 3.0;
    
    // Transport times (in days)
    public static final double SAME_CONTINENT_TRANSPORT_TIME = 0.5;
    public static final double DIFFERENT_CONTINENT_TRANSPORT_TIME = 1.0;
    
    // Flight capacities (packages per flight)
    public static final int SAME_CONTINENT_MIN_CAPACITY = 200;
    public static final int SAME_CONTINENT_MAX_CAPACITY = 300;
    public static final int DIFFERENT_CONTINENT_MIN_CAPACITY = 250;
    public static final int DIFFERENT_CONTINENT_MAX_CAPACITY = 400;
    
    // Warehouse capacities (packages per warehouse)
    public static final int WAREHOUSE_MIN_CAPACITY = 600;
    public static final int WAREHOUSE_MAX_CAPACITY = 1000;
    
    // Customer pickup time constraint (hours)
    public static final int CUSTOMER_PICKUP_MAX_HOURS = 2;
    
    // Tabu Search parameters
    public static final int TABU_LIST_SIZE = 20;
    public static final int MAX_ITERATIONS = 1000;
    public static final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 100;
    
    // Main warehouses
    public static final String LIMA_WAREHOUSE = "Lima, Peru";
    public static final String BRUSSELS_WAREHOUSE = "Brussels, Belgium";
    public static final String BAKU_WAREHOUSE = "Baku, Azerbaijan";
}
