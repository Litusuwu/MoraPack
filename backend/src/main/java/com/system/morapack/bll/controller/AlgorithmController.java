package com.system.morapack.bll.controller;

import com.system.morapack.config.Constants;
import com.system.morapack.schemas.*;
import com.system.morapack.schemas.algorithm.ALNS.Solution;
import com.system.morapack.schemas.algorithm.TabuSearch.TabuSearch;
import com.system.morapack.schemas.algorithm.TabuSearch.TabuSolution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AlgorithmController {

  /**
   * Execute algorithm based on request parameters
   */
  public AlgorithmResultSchema executeAlgorithm(AlgorithmRequest request) {
    LocalDateTime startTime = LocalDateTime.now();

    try {
      // Set data source mode if specified
      if (request.getUseDatabase() != null) {
        // This would require modifying Constants or passing to algorithm constructors
        System.out.println("Using data source: " + (request.getUseDatabase() ? "DATABASE" : "FILE"));
      }

      String algorithmType = request.getAlgorithmType() != null ?
          request.getAlgorithmType().toUpperCase() : "TABU";

      AlgorithmResultSchema result;

      switch (algorithmType) {
        case "ALNS":
          result = executeALNS(request, startTime);
          break;
        case "TABU":
        default:
          result = executeTabuSearch(request, startTime);
          break;
      }

      return result;

    } catch (Exception e) {
      LocalDateTime endTime = LocalDateTime.now();
      return AlgorithmResultSchema.builder()
          .success(false)
          .message("Algorithm execution failed: " + e.getMessage())
          .algorithmType(request.getAlgorithmType())
          .executionStartTime(startTime)
          .executionEndTime(endTime)
          .executionTimeSeconds(ChronoUnit.SECONDS.between(startTime, endTime))
          .build();
    }
  }

  /**
   * Execute ALNS algorithm
   */
  private AlgorithmResultSchema executeALNS(AlgorithmRequest request, LocalDateTime startTime) {
    System.out.println("===========================================");
    System.out.println("EXECUTING ALNS ALGORITHM VIA API");
    System.out.println("===========================================");

    Solution alnsSolution = new Solution();
    alnsSolution.solve();

    LocalDateTime endTime = LocalDateTime.now();
    long executionTime = ChronoUnit.SECONDS.between(startTime, endTime);

    // ALNS returns HashMap<HashMap<OrderSchema, ArrayList<FlightSchema>>, Integer>
    // We need to extract the best solution
    // Note: This is a simplified conversion - you may need to adjust based on actual ALNS output

    return AlgorithmResultSchema.builder()
        .success(true)
        .message("ALNS algorithm executed successfully")
        .algorithmType("ALNS")
        .executionStartTime(startTime)
        .executionEndTime(endTime)
        .executionTimeSeconds(executionTime)
        .productRoutes(new ArrayList<>()) // ALNS output needs proper conversion
        .build();
  }

  /**
   * Execute Tabu Search algorithm
   */
  private AlgorithmResultSchema executeTabuSearch(AlgorithmRequest request, LocalDateTime startTime) {
    System.out.println("===========================================");
    System.out.println("EXECUTING TABU SEARCH ALGORITHM VIA API");
    System.out.println("===========================================");

    // Set default parameters if not provided
    int maxIterations = request.getMaxIterations() != null ? request.getMaxIterations() : 1000;
    int maxNoImprovement = request.getMaxNoImprovement() != null ? request.getMaxNoImprovement() : 100;
    int neighborhoodSize = request.getNeighborhoodSize() != null ? request.getNeighborhoodSize() : 100;
    int tabuListSize = request.getTabuListSize() != null ? request.getTabuListSize() : 50;
    long tabuTenure = request.getTabuTenure() != null ? request.getTabuTenure() : 10000L;

    TabuSearch tabuSearch = new TabuSearch(
        Constants.AIRPORT_INFO_FILE_PATH,
        Constants.FLIGHTS_FILE_PATH,
        Constants.PRODUCTS_FILE_PATH,
        maxIterations,
        maxNoImprovement,
        neighborhoodSize,
        tabuListSize,
        tabuTenure
    );

    TabuSolution bestSolution = tabuSearch.solve();
    LocalDateTime endTime = LocalDateTime.now();
    long executionTime = ChronoUnit.SECONDS.between(startTime, endTime);

    // Convert TabuSolution to our response format
    return convertTabuSolutionToResult(bestSolution, startTime, endTime, executionTime);
  }

  /**
   * Convert TabuSolution to AlgorithmResultSchema with product routes
   */
  private AlgorithmResultSchema convertTabuSolutionToResult(
      TabuSolution tabuSolution,
      LocalDateTime startTime,
      LocalDateTime endTime,
      long executionTime) {

    HashMap<OrderSchema, ArrayList<FlightSchema>> solution = tabuSolution.getSolution();

    List<ProductRouteSchema> productRoutes = new ArrayList<>();
    int assignedCount = 0;

    // Convert each order's route to ProductRouteSchema
    for (Map.Entry<OrderSchema, ArrayList<FlightSchema>> entry : solution.entrySet()) {
      OrderSchema order = entry.getKey();
      ArrayList<FlightSchema> flights = entry.getValue();

      if (flights != null && !flights.isEmpty()) {
        assignedCount++;

        // Get product information from the order
        ArrayList<ProductSchema> products = order.getProductSchemas();

        if (products != null && !products.isEmpty()) {
          // Create a route for each product in the order
          for (ProductSchema product : products) {
            ProductRouteSchema productRoute = ProductRouteSchema.builder()
                .productId(product.getId())
                .orderId(order.getId())
                .orderName(order.getCustomerSchema() != null ?
                    order.getCustomerSchema().getName() : "Order-" + order.getId())
                .flights(new ArrayList<>(flights)) // Copy the flight list
                .originCity(order.getCurrentLocation() != null ?
                    order.getCurrentLocation().getName() : "Unknown")
                .destinationCity(order.getDestinationCitySchema() != null ?
                    order.getDestinationCitySchema().getName() : "Unknown")
                .flightCount(flights.size())
                .build();

            productRoutes.add(productRoute);
          }
        } else {
          // If no products, create one route for the order itself
          ProductRouteSchema productRoute = ProductRouteSchema.builder()
              .productId(null)
              .orderId(order.getId())
              .orderName("Order-" + order.getId())
              .flights(new ArrayList<>(flights))
              .originCity(order.getCurrentLocation() != null ?
                  order.getCurrentLocation().getName() : "Unknown")
              .destinationCity(order.getDestinationCitySchema() != null ?
                  order.getDestinationCitySchema().getName() : "Unknown")
              .flightCount(flights.size())
              .build();

          productRoutes.add(productRoute);
        }
      }
    }

    int unassignedCount = tabuSolution.getUnassignedPackagesCount();
    int totalOrders = assignedCount + unassignedCount;

    return AlgorithmResultSchema.builder()
        .success(true)
        .message("Tabu Search algorithm executed successfully")
        .algorithmType("TABU")
        .executionStartTime(startTime)
        .executionEndTime(endTime)
        .executionTimeSeconds(executionTime)
        .totalOrders(totalOrders)
        .assignedOrders(assignedCount)
        .unassignedOrders(unassignedCount)
        .totalProducts(productRoutes.size())
        .score((double) tabuSolution.getScore())
        .productRoutes(productRoutes)
        .build();
  }
}
