package com.system.morapack.schemas.algorithm.Input;

import com.system.morapack.schemas.Airport;
import com.system.morapack.schemas.Flight;
import com.system.morapack.config.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InputData {
    private ArrayList<Flight> flights;
    private final String filePath;
    private ArrayList<Airport> airports;

    public InputData(String filePath, ArrayList<Airport> airports) {
        this.filePath = filePath;
        this.flights = new ArrayList<>();
        this.airports = airports;
    }

    public ArrayList<Flight> readFlights() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int flightId = 1;
            Map<String, Airport> airportMap = createAirportMap();
            
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Parse flight data
                // Format: ORIGIN-DESTINATION-DEPARTURE-ARRIVAL-CAPACITY
                String[] parts = line.split("-");
                if (parts.length == 5) {
                    String originCode = parts[0];
                    String destinationCode = parts[1];
                    String departureTime = parts[2];
                    String arrivalTime = parts[3];
                    int maxCapacity = Integer.parseInt(parts[4]);
                    
                    // Find airports by IATA code
                    Airport originAirport = airportMap.get(originCode);
                    Airport destinationAirport = airportMap.get(destinationCode);
                    
                    if (originAirport != null && destinationAirport != null) {
                        // Calculate transport time in hours
                        double transportTime = calculateTransportTime(departureTime, arrivalTime);
                        
                        // Calculate cost (this is a placeholder - you might want to implement a more sophisticated cost model)
                        double cost = calculateFlightCost(originAirport, destinationAirport, maxCapacity);
                        
                        // Create Flight object
                        Flight flight = new Flight();
                        flight.setId(flightId++);
                        flight.setFrequencyPerDay(1.0); // Default frequency
                        flight.setOriginAirport(originAirport);
                        flight.setDestinationAirport(destinationAirport);
                        flight.setMaxCapacity(maxCapacity);
                        flight.setUsedCapacity(0);
                        flight.setTransportTime(transportTime);
                        flight.setCost(cost);
                        
                        flights.add(flight);
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error reading flight data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return flights;
    }
    
    private Map<String, Airport> createAirportMap() {
        Map<String, Airport> map = new HashMap<>();
        for (Airport airport : airports) {
            map.put(airport.getCodeIATA(), airport);
        }
        return map;
    }
    
    private double calculateTransportTime(String departureTime, String arrivalTime) {
        LocalTime departure = parseTime(departureTime);
        LocalTime arrival = parseTime(arrivalTime);
        
        // Calculate duration between departure and arrival
        long minutes;
        if (arrival.isBefore(departure)) {
            // Flight crosses midnight
            minutes = Duration.between(departure, LocalTime.of(23, 59, 59)).toMinutes() + 
                     Duration.between(LocalTime.of(0, 0), arrival).toMinutes() + 1;
        } else {
            minutes = Duration.between(departure, arrival).toMinutes();
        }
        
        // Convert minutes to hours
        return minutes / 60.0;
    }
    
    private LocalTime parseTime(String timeStr) {
        int hours = Integer.parseInt(timeStr.substring(0, 2));
        int minutes = Integer.parseInt(timeStr.substring(3, 5));
        return LocalTime.of(hours, minutes);
    }
    
    private double calculateFlightCost(Airport origin, Airport destination, int capacity) {
        // Simple cost model based on whether airports are in the same continent and capacity
        boolean sameContinentFlight = origin.getCity().getContinent() == destination.getCity().getContinent();
        
        double baseCost;
        if (sameContinentFlight) {
            baseCost = Constants.SAME_CONTINENT_TRANSPORT_TIME * 100;
        } else {
            baseCost = Constants.DIFFERENT_CONTINENT_TRANSPORT_TIME * 150;
        }
        
        // Adjust cost based on capacity
        double capacityFactor = capacity / 300.0;
        
        return baseCost * capacityFactor;
    }
}
