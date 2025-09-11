package src.model.algorithm;

import src.model.Airport;
import src.model.City;
import src.model.Constants;
import src.model.Flight;
import src.model.Warehouse;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory class para crear objetos del sistema a partir de datos leídos desde archivos
 */
public class SystemDataFactory {
    
    /**
     * Crea las ciudades y continentes basándose en los datos de aeropuertos
     */
    public static List<City> createCities(List<DataReader.AirportData> airportDataList) {
        Map<String, City> cityMap = new HashMap<>();
        int cityIdCounter = 1;
        
        for (DataReader.AirportData airportData : airportDataList) {
            String cityKey = airportData.cityName + "_" + airportData.countryName;
            
            if (!cityMap.containsKey(cityKey)) {
                City city = new City(cityIdCounter++, airportData.cityName, airportData.continent);
                cityMap.put(cityKey, city);
            }
        }
        
        return new ArrayList<>(cityMap.values());
    }
    
    /**
     * Crea los aeropuertos basándose en los datos leídos y las ciudades creadas
     */
    public static List<Airport> createAirports(List<DataReader.AirportData> airportDataList, 
                                              List<City> cities) {
        List<Airport> airports = new ArrayList<>();
        Map<String, City> cityLookup = createCityLookupMap(cities);
        
        for (DataReader.AirportData airportData : airportDataList) {
            String cityKey = airportData.cityName + "_" + airportData.countryName;
            City city = cityLookup.get(cityKey);
            
            if (city != null) {
                Airport airport = new Airport(airportData.id, airportData.codeIATA, 
                                            airportData.alias, city);
                
                // Set additional properties
                airport.setTimezoneUTC(airportData.timezoneUTC);
                airport.setLatitude(airportData.latitude);
                airport.setLongitude(airportData.longitude);
                airport.setMaxCapacity(airportData.capacity);
                
                airports.add(airport);
            }
        }
        
        return airports;
    }
    
    /**
     * Crea las bodegas principales (Lima, Bruselas, Baku) y otras bodegas
     */
    public static List<Warehouse> createWarehouses(List<Airport> airports) {
        List<Warehouse> warehouses = new ArrayList<>();
        int warehouseIdCounter = 1;
        
        for (Airport airport : airports) {
            String cityName = airport.getCity().getName().toLowerCase();
            String warehouseName = airport.getCity().getName() + " Warehouse";
            boolean isMainWarehouse = isMainWarehouse(cityName);
            
            Warehouse warehouse = new Warehouse(warehouseIdCounter++, airport, 
                                              warehouseName, isMainWarehouse);
            
            // Set warehouse capacity based on airport capacity
            int warehouseCapacity = (int) airport.getMaxCapacity();
            if (warehouseCapacity < Constants.WAREHOUSE_MIN_CAPACITY) {
                warehouseCapacity = Constants.WAREHOUSE_MIN_CAPACITY + 
                    (int)(Math.random() * (Constants.WAREHOUSE_MAX_CAPACITY - Constants.WAREHOUSE_MIN_CAPACITY));
            }
            warehouse.setMaxCapacity(warehouseCapacity);
            
            warehouses.add(warehouse);
            airport.setWarehouse(warehouse);
        }
        
        return warehouses;
    }
    
    /**
     * Crea los vuelos basándose en los datos de vuelos y aeropuertos
     */
    public static List<Flight> createFlights(List<DataReader.FlightData> flightDataList, 
                                           List<Airport> airports) {
        List<Flight> flights = new ArrayList<>();
        Map<String, Airport> airportLookup = createAirportLookupMap(airports);
        int flightIdCounter = 1;
        
        for (DataReader.FlightData flightData : flightDataList) {
            Airport originAirport = airportLookup.get(flightData.originCode);
            Airport destinationAirport = airportLookup.get(flightData.destinationCode);
            
            if (originAirport != null && destinationAirport != null) {
                // Calculate frequency (assuming 1 flight per day for simplicity)
                double frequencyPerDay = 1.0;
                
                Flight flight = new Flight(flightIdCounter++, originAirport, 
                                         destinationAirport, frequencyPerDay);
                
                // Override capacity with real data
                flight.setMaxCapacity(flightData.capacity);
                
                // Calculate transport time based on continent
                boolean sameContinent = originAirport.getCity().getContinent() == 
                                      destinationAirport.getCity().getContinent();
                
                if (sameContinent) {
                    flight.setTransportTime(Constants.SAME_CONTINENT_TRANSPORT_TIME);
                } else {
                    flight.setTransportTime(Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME);
                }
                
                // Calculate cost
                double baseCost = flight.getTransportTime() * 100 + flight.getMaxCapacity() * 0.1;
                flight.setCost(baseCost);
                
                flights.add(flight);
            }
        }
        
        return flights;
    }
    
    /**
     * Crea un mapa de lookup para ciudades por nombre y país
     */
    private static Map<String, City> createCityLookupMap(List<City> cities) {
        Map<String, City> cityLookup = new HashMap<>();
        
        // Note: We need to handle the mapping differently since we don't have country info in City
        // We'll use a simpler approach matching by city name
        for (City city : cities) {
            cityLookup.put(city.getName(), city);
            
            // Also create entries with potential country combinations
            // This is a workaround since we don't store country in City model
            switch (city.getContinent()) {
                case America:
                    addCityMappings(cityLookup, city, Arrays.asList(
                        "Colombia", "Ecuador", "Venezuela", "Brasil", "Perú", "Bolivia", 
                        "Chile", "Argentina", "Paraguay", "Uruguay"
                    ));
                    break;
                case Europa:
                    addCityMappings(cityLookup, city, Arrays.asList(
                        "Albania", "Alemania", "Austria", "Belgica", "Bielorrusia", "Bulgaria",
                        "Checa", "Croacia", "Dinamarca", "Holanda"
                    ));
                    break;
                case Asia:
                    addCityMappings(cityLookup, city, Arrays.asList(
                        "India", "Siria", "Arabia Saudita", "Emiratos A.U", "Afganistan", 
                        "Oman", "Yemen", "Pakistan", "Azerbaiyan", "Jordania"
                    ));
                    break;
            }
        }
        
        return cityLookup;
    }
    
    private static void addCityMappings(Map<String, City> cityLookup, City city, List<String> countries) {
        for (String country : countries) {
            String key = city.getName() + "_" + country;
            cityLookup.put(key, city);
        }
    }
    
    /**
     * Crea un mapa de lookup para aeropuertos por código IATA
     */
    private static Map<String, Airport> createAirportLookupMap(List<Airport> airports) {
        return airports.stream()
                .collect(Collectors.toMap(Airport::getCodeIATA, airport -> airport));
    }
    
    /**
     * Determina si una ciudad es sede principal de MoraPack
     */
    private static boolean isMainWarehouse(String cityName) {
        return cityName.equals("lima") || 
               cityName.equals("bruselas") || 
               cityName.equals("baku");
    }
    
    /**
     * Crea el sistema completo con datos reales
     */
    public static SystemData createSystemFromFiles(String airportFilePath, String flightFilePath) {
        try {
            // Read raw data
            List<DataReader.AirportData> airportDataList = DataReader.readAirportData(airportFilePath);
            List<DataReader.FlightData> flightDataList = DataReader.readFlightData(flightFilePath);
            
            // Create system objects
            List<City> cities = createCities(airportDataList);
            List<Airport> airports = createAirports(airportDataList, cities);
            List<Warehouse> warehouses = createWarehouses(airports);
            
            // Filter flights to only include valid airport connections
            Set<String> validAirportCodes = airports.stream()
                    .map(Airport::getCodeIATA)
                    .collect(Collectors.toSet());
            List<DataReader.FlightData> validFlights = DataReader.filterValidFlights(
                    flightDataList, validAirportCodes);
            
            List<Flight> flights = createFlights(validFlights, airports);
            
            return new SystemData(cities, airports, warehouses, flights);
            
        } catch (Exception e) {
            System.err.println("Error creating system from files: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Clase de datos del sistema
     */
    public static class SystemData {
        public final List<City> cities;
        public final List<Airport> airports;
        public final List<Warehouse> warehouses;
        public final List<Flight> flights;
        
        public SystemData(List<City> cities, List<Airport> airports, 
                         List<Warehouse> warehouses, List<Flight> flights) {
            this.cities = cities;
            this.airports = airports;
            this.warehouses = warehouses;
            this.flights = flights;
        }
    }
}

