package src.model.algorithm;

import src.model.Continent;

import java.io.*;
import java.util.*;

/**
 * Clase para leer datos desde archivos de texto
 * Lee información de aeropuertos y vuelos desde archivos de datos reales
 */
public class DataReader {
    
    /**
     * Lee datos de aeropuertos desde archivo
     */
    public static List<AirportData> readAirportData(String filePath) {
        List<AirportData> airportDataList = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int idCounter = 1;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }
                
                AirportData airportData = parseAirportLine(line, idCounter++);
                if (airportData != null) {
                    airportDataList.add(airportData);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error reading airport data from " + filePath + ": " + e.getMessage());
        }
        
        System.out.println("Loaded " + airportDataList.size() + " airports from " + filePath);
        return airportDataList;
    }
    
    /**
     * Lee datos de vuelos desde archivo
     */
    public static List<FlightData> readFlightData(String filePath) {
        List<FlightData> flightDataList = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }
                
                FlightData flightData = parseFlightLine(line);
                if (flightData != null) {
                    flightDataList.add(flightData);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error reading flight data from " + filePath + ": " + e.getMessage());
        }
        
        System.out.println("Loaded " + flightDataList.size() + " flights from " + filePath);
        return flightDataList;
    }
    
    /**
     * Filtra vuelos para incluir solo aeropuertos válidos
     */
    public static List<FlightData> filterValidFlights(List<FlightData> flightDataList, 
                                                      Set<String> validAirportCodes) {
        List<FlightData> validFlights = new ArrayList<>();
        
        for (FlightData flight : flightDataList) {
            if (validAirportCodes.contains(flight.originCode) && 
                validAirportCodes.contains(flight.destinationCode)) {
                validFlights.add(flight);
            }
        }
        
        System.out.println("Filtered to " + validFlights.size() + " valid flights");
        return validFlights;
    }
    
    /**
     * Parsea una línea de datos de aeropuerto
     * Formato real: 01   SKBO   Bogota              Colombia        bogo    -5     430     Latitude: 04° 42' 05" N   Longitude:  74° 08' 49" W
     */
    private static AirportData parseAirportLine(String line, int defaultId) {
        try {
            // Skip header lines and empty lines
            if (line.contains("*") || line.contains("GMT") || line.contains("America") || 
                line.contains("Europa") || line.contains("Asia") || line.trim().length() < 10) {
                return null;
            }
            
            // Split by multiple spaces to get meaningful parts
            String[] parts = line.trim().split("\\s+");
            
            if (parts.length >= 6) {
                int id = parseIntSafely(parts[0], defaultId);
                String codeIATA = parts[1].trim().toUpperCase();
                String cityName = parts[2].trim();
                String countryName = parts[3].trim();
                
                // Find timezone (negative number)
                int timezoneUTC = 0;
                int capacity = 600; // default
                
                for (int i = 4; i < parts.length; i++) {
                    String part = parts[i].trim();
                    // Look for timezone (negative number)
                    if (part.startsWith("-") && part.length() <= 3) {
                        timezoneUTC = parseIntSafely(part, 0);
                    }
                    // Look for capacity (3-digit positive number)
                    else if (!part.contains(":") && !part.contains("°") && !part.contains("'") && 
                             part.length() == 3 && parseIntSafely(part, -1) > 0) {
                        capacity = parseIntSafely(part, 600);
                    }
                }
                
                // Extract coordinates if present (simplified)
                String latitude = "0.0";
                String longitude = "0.0";
                
                // Determine continent based on country or city name
                Continent continent = determineContinent(countryName, cityName);
                
                String alias = cityName + " Airport";
                
                return new AirportData(id, codeIATA, alias, cityName, countryName, 
                                     latitude, longitude, timezoneUTC, capacity, continent);
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing airport line: " + line + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Parsea una línea de datos de vuelo  
     * Formato esperado: ORIGEN-DESTINO-HORA_SALIDA-HORA_LLEGADA-CAPACIDAD
     */
    private static FlightData parseFlightLine(String line) {
        try {
            String[] parts = line.split("-");
            
            if (parts.length >= 2) {
                String originCode = parts[0].trim().toUpperCase();
                String destinationCode = parts[1].trim().toUpperCase();
                
                // Default capacity if not specified
                int capacity = 250;
                if (parts.length >= 5) {
                    capacity = parseIntSafely(parts[4].trim(), 250);
                }
                
                return new FlightData(originCode, destinationCode, capacity);
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing flight line: " + line + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Determina el continente basado en el país o ciudad
     */
    private static Continent determineContinent(String countryName, String cityName) {
        String country = countryName.toLowerCase();
        String city = cityName.toLowerCase();
        
        // América del Sur (según el archivo de datos)
        if (country.contains("colombia") || country.contains("ecuador") || 
            country.contains("venezuela") || country.contains("brasil") || country.contains("brazil") ||
            country.contains("perú") || country.contains("peru") || country.contains("bolivia") ||
            country.contains("chile") || country.contains("argentina") || country.contains("paraguay") ||
            country.contains("uruguay") || 
            city.contains("lima") || city.contains("bogota") || city.contains("quito") ||
            city.contains("caracas") || city.contains("brasilia") || city.contains("santiago") ||
            city.contains("buenos") || city.contains("montevideo") || city.contains("asuncion")) {
            return Continent.America;
        }
        
        // Europa (según el archivo de datos)
        if (country.contains("albania") || country.contains("alemania") || country.contains("germany") ||
            country.contains("austria") || country.contains("belgica") || country.contains("belgium") ||
            country.contains("bielorrusia") || country.contains("belarus") || country.contains("bulgaria") ||
            country.contains("checa") || country.contains("czech") || country.contains("croacia") ||
            country.contains("dinamarca") || country.contains("denmark") || country.contains("holanda") ||
            country.contains("netherlands") || country.contains("francia") || country.contains("france") ||
            country.contains("españa") || country.contains("spain") || country.contains("italia") ||
            country.contains("reino") || country.contains("united") || country.contains("suecia") ||
            city.contains("bruselas") || city.contains("brussels") || city.contains("berlín") ||
            city.contains("berlin") || city.contains("paris") || city.contains("madrid") ||
            city.contains("roma") || city.contains("london") || city.contains("londres") ||
            city.contains("amsterdam") || city.contains("viena") || city.contains("praga") ||
            city.contains("varsovia") || city.contains("estocolmo") || city.contains("copenhague")) {
            return Continent.Europa;
        }
        
        // Asia (según el archivo de datos)
        if (country.contains("india") || country.contains("siria") || country.contains("arabia") ||
            country.contains("emiratos") || country.contains("afganistan") || country.contains("afghanistan") ||
            country.contains("oman") || country.contains("yemen") || country.contains("pakistan") ||
            country.contains("azerbaiyan") || country.contains("azerbaijan") || country.contains("jordania") ||
            country.contains("iran") || country.contains("iraq") || country.contains("turquia") ||
            country.contains("kazajstan") || country.contains("uzbekistan") ||
            city.contains("baku") || city.contains("delhi") || city.contains("dubai") ||
            city.contains("doha") || city.contains("kuwait") || city.contains("riyadh") ||
            city.contains("tehran") || city.contains("bagdad") || city.contains("ankara") ||
            city.contains("almaty") || city.contains("tashkent") || city.contains("kabul")) {
            return Continent.Asia;
        }
        
        // Default: try to guess based on position in file or other hints
        return Continent.America; // Default fallback
    }
    
    /**
     * Parse integer de forma segura con valor por defecto
     */
    private static int parseIntSafely(String str, int defaultValue) {
        try {
            return Integer.parseInt(str.replaceAll("\\D", "")); // Remove non-digits
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Clase de datos para aeropuertos
     */
    public static class AirportData {
        public final int id;
        public final String codeIATA;
        public final String alias;
        public final String cityName;
        public final String countryName;
        public final String latitude;
        public final String longitude;
        public final int timezoneUTC;
        public final int capacity;
        public final Continent continent;
        
        public AirportData(int id, String codeIATA, String alias, String cityName, 
                          String countryName, String latitude, String longitude, 
                          int timezoneUTC, int capacity, Continent continent) {
            this.id = id;
            this.codeIATA = codeIATA;
            this.alias = alias;
            this.cityName = cityName;
            this.countryName = countryName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timezoneUTC = timezoneUTC;
            this.capacity = capacity;
            this.continent = continent;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%s) - %s, %s [%s]", 
                               codeIATA, alias, cityName, countryName, continent);
        }
    }
    
    /**
     * Clase de datos para vuelos
     */
    public static class FlightData {
        public final String originCode;
        public final String destinationCode;
        public final int capacity;
        
        public FlightData(String originCode, String destinationCode, int capacity) {
            this.originCode = originCode;
            this.destinationCode = destinationCode;
            this.capacity = capacity;
        }
        
        @Override
        public String toString() {
            return String.format("%s → %s (Capacity: %d)", 
                               originCode, destinationCode, capacity);
        }
    }
}

