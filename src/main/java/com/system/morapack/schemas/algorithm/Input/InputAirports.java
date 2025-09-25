package com.system.morapack.schemas.algorithm;

import com.system.morapack.schemas.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InputAirports {

    private ArrayList<Airport> airports;
    private final String filePath;

    public InputAirports(String filePath) {
        this.filePath = filePath;
        this.airports = new ArrayList<>();
    }

    public ArrayList<Airport> readAirports() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            Continent currentContinent = null;
            Map<String, City> cityMap = new HashMap<>();

            // Skip the first two lines (header)
            reader.readLine();
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Check if this is a continent header line
                if (line.contains("America") || line.contains("Europa") || line.contains("Asia")) {
                    if (line.contains("America")) {
                        currentContinent = Continent.America;
                        System.out.println("Continent: " + currentContinent);
                    } else if (line.contains("Europa")) {
                        currentContinent = Continent.Europa;
                        System.out.println("Continent: " + currentContinent);
                    } else if (line.contains("Asia")) {
                        currentContinent = Continent.Asia;
                        System.out.println("Continent: " + currentContinent);
                    }
                    continue;
                }
                
                // Parse airport data
                String[] parts = line.trim().split("\\s+");
                // System.out.println("Parts: " + parts[0]);
                // System.out.println("Parts: " + parts[1]);
                // System.out.println("Parts: " + parts[2]);
                // System.out.println("Parts: " + parts[3]);
                // System.out.println("Parts: " + parts[4]);
                // System.out.println("Parts: " + parts[5]);
                // System.out.println("Parts: " + parts[6]);
                if (parts.length >= 7) {
                    int id = Integer.parseInt(parts[0]);
                    String codeIATA = parts[1];
                    
                    // Extract city name (may contain multiple words)
                    int cityNameEnd = 3;
                    while (!parts[cityNameEnd].contains("GMT") && !Character.isDigit(parts[cityNameEnd].charAt(0))) {
                        cityNameEnd++;
                    }
                    
                    StringBuilder cityNameBuilder = new StringBuilder(parts[2]);
                    for (int i = 3; i < cityNameEnd; i++) {
                        cityNameBuilder.append(" ").append(parts[i]);
                    }
                    String cityName = cityNameBuilder.toString();
                    
                    // Extract country name
                    String countryName = parts[cityNameEnd];
                    
                    // Extract alias
                    String alias = parts[cityNameEnd + 1];
                    
                    // Extract timezone
                    int timezone;
                    try {
                        timezone = Integer.parseInt(parts[5]);
                    } catch (NumberFormatException e) {
                        // Handle timezone format issues
                        String tzStr = parts[5];
                        if (tzStr.startsWith("+")) {
                            timezone = Integer.parseInt(tzStr.substring(1));
                        } else if (tzStr.startsWith("-")) {
                            timezone = Integer.parseInt(tzStr);
                        } else {
                            timezone = 0; // Default if parsing fails
                            System.out.println("Warning: Could not parse timezone for " + codeIATA + ", using default 0");
                        }
                    }
                    
                    // Extract capacity
                    double maxCapacity;
                    try {
                        maxCapacity = Double.parseDouble(parts[6]);
                    } catch (NumberFormatException e) {
                        maxCapacity = 400.0; // Default capacity
                        System.out.println("Warning: Could not parse capacity for " + codeIATA + ", using default 400.0");
                    }
                    
                    // Extract latitude and longitude
                    String latitudeStr = "";
                    String longitudeStr = "";
                    
                    // Find latitude and longitude in the line
                    int latIndex = line.indexOf("Latitude:");
                    int longIndex = line.indexOf("Longitude:");
                    
                    if (latIndex != -1 && longIndex != -1) {
                        latitudeStr = line.substring(latIndex + 10, longIndex).trim();
                        longitudeStr = line.substring(longIndex + 11).trim();
                        
                        // Clean up special characters from latitude and longitude
                        latitudeStr = latitudeStr.replaceAll("[°'\"NSEW]", "").trim();
                        longitudeStr = longitudeStr.replaceAll("[°'\"NSEW]", "").trim();
                    }
                    
                    // Create City object if it doesn't exist
                    String cityKey = cityName + "-" + countryName;
                    City city = cityMap.get(cityKey);
                    if (city == null) {
                        city = new City();
                        city.setId(cityMap.size() + 1);
                        city.setName(cityName);
                        city.setContinent(currentContinent);
                        cityMap.put(cityKey, city);
                    }
                    
                    // Create Warehouse for the airport
                    Warehouse warehouse = new Warehouse();
                    warehouse.setId(id);
                    warehouse.setMaxCapacity((int)maxCapacity);
                    warehouse.setUsedCapacity(0);
                    warehouse.setName(cityName + " Warehouse");
                    warehouse.setMainWarehouse(false);
                    
                    // Create Airport object
                    Airport airport = new Airport();
                    airport.setId(id);
                    airport.setCodeIATA(codeIATA);
                    airport.setAlias(alias);
                    airport.setTimezoneUTC(timezone);
                    airport.setLatitude(latitudeStr);
                    airport.setLongitude(longitudeStr);
                    airport.setCity(city);
                    airport.setState(AirportState.Avaiable);
                    airport.setWarehouse(warehouse);
                    
                    // Set circular reference
                    warehouse.setAirport(airport);
                    
                    airports.add(airport);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error reading airport data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return airports;
    }
}
