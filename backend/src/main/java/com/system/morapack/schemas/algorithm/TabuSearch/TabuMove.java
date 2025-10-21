package com.system.morapack.schemas.algorithm.TabuSearch;

import com.system.morapack.schemas.FlightSchema;
import com.system.morapack.schemas.OrderSchema;

import java.util.ArrayList;

/**
 * Representa un movimiento en el algoritmo Tabu Search
 * Puede ser inserción, eliminación o intercambio de rutas de paquetes
 */
public class TabuMove {
    public enum MoveType {
        INSERT,    // Insertar paquete a solución
        REMOVE,    // Eliminar paquete de solución
        REASSIGN,  // Reasignar paquete a otra ruta
        SWAP       // Intercambiar rutas de dos paquetes
    }
    
    private final MoveType moveType;
    private final OrderSchema primaryOrderSchema;
    private final ArrayList<FlightSchema> primaryRoute;
    private final OrderSchema secondaryOrderSchema; // Usado solo para SWAP
    private final ArrayList<FlightSchema> secondaryRoute; // Usado para REASSIGN y SWAP
    private final int hash;
    private final long timestamp;
    
    /**
     * Constructor para movimientos INSERT y REMOVE
     */
    public TabuMove(MoveType moveType, OrderSchema pkg, ArrayList<FlightSchema> route) {
        this.moveType = moveType;
        this.primaryOrderSchema = pkg;
        this.primaryRoute = route != null ? new ArrayList<>(route) : new ArrayList<>();
        this.secondaryOrderSchema = null;
        this.secondaryRoute = null;
        this.timestamp = System.currentTimeMillis();
        this.hash = calculateHash();
    }
    
    /**
     * Constructor para movimientos REASSIGN
     */
    public TabuMove(MoveType moveType, OrderSchema pkg, ArrayList<FlightSchema> oldRoute, ArrayList<FlightSchema> newRoute) {
        this.moveType = moveType;
        this.primaryOrderSchema = pkg;
        this.primaryRoute = oldRoute != null ? new ArrayList<>(oldRoute) : new ArrayList<>();
        this.secondaryOrderSchema = null;
        this.secondaryRoute = newRoute != null ? new ArrayList<>(newRoute) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.hash = calculateHash();
    }
    
    /**
     * Constructor para movimientos SWAP
     */
    public TabuMove(MoveType moveType, OrderSchema pkg1, ArrayList<FlightSchema> route1, OrderSchema pkg2, ArrayList<FlightSchema> route2) {
        this.moveType = moveType;
        this.primaryOrderSchema = pkg1;
        this.primaryRoute = route1 != null ? new ArrayList<>(route1) : new ArrayList<>();
        this.secondaryOrderSchema = pkg2;
        this.secondaryRoute = route2 != null ? new ArrayList<>(route2) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.hash = calculateHash();
    }
    
    public MoveType getMoveType() {
        return moveType;
    }
    
    public OrderSchema getPrimaryPackage() {
        return primaryOrderSchema;
    }
    
    public ArrayList<FlightSchema> getPrimaryRoute() {
        return primaryRoute;
    }
    
    public OrderSchema getSecondaryPackage() {
        return secondaryOrderSchema;
    }
    
    public ArrayList<FlightSchema> getSecondaryRoute() {
        return secondaryRoute;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Crea un movimiento inverso que deshace este movimiento
     */
    public TabuMove getInverseMove() {
        switch (moveType) {
            case INSERT:
                return new TabuMove(MoveType.REMOVE, primaryOrderSchema, primaryRoute);
            case REMOVE:
                return new TabuMove(MoveType.INSERT, primaryOrderSchema, primaryRoute);
            case REASSIGN:
                return new TabuMove(MoveType.REASSIGN, primaryOrderSchema, secondaryRoute, primaryRoute);
            case SWAP:
                return new TabuMove(MoveType.SWAP, secondaryOrderSchema, secondaryRoute, primaryOrderSchema, primaryRoute);
            default:
                return null;
        }
    }
    
    private int calculateHash() {
        int hash = 7;
        hash = 31 * hash + moveType.hashCode();
        hash = 31 * hash + (primaryOrderSchema != null ? primaryOrderSchema.getId() : 0);
        hash = 31 * hash + (secondaryOrderSchema != null ? secondaryOrderSchema.getId() : 0);
        
        // Crear un hash que refleja la estructura de la ruta, no los objetos específicos
        if (primaryRoute != null && !primaryRoute.isEmpty()) {
            int routeHash = 0;
            for (FlightSchema f : primaryRoute) {
                routeHash += f.getOriginAirportSchema().getId() * 31 + f.getDestinationAirportSchema().getId();
            }
            hash = 31 * hash + routeHash;
        }
        
        if (secondaryRoute != null && !secondaryRoute.isEmpty()) {
            int routeHash = 0;
            for (FlightSchema f : secondaryRoute) {
                routeHash += f.getOriginAirportSchema().getId() * 31 + f.getDestinationAirportSchema().getId();
            }
            hash = 31 * hash + routeHash;
        }
        
        return hash;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        TabuMove tabuMove = (TabuMove) o;
        
        if (moveType != tabuMove.moveType) return false;
        
        if (primaryOrderSchema != null && tabuMove.primaryOrderSchema != null) {
            if (primaryOrderSchema.getId() != tabuMove.primaryOrderSchema.getId()) return false;
        } else if (primaryOrderSchema != tabuMove.primaryOrderSchema) {
            return false;
        }
        
        if (secondaryOrderSchema != null && tabuMove.secondaryOrderSchema != null) {
            if (secondaryOrderSchema.getId() != tabuMove.secondaryOrderSchema.getId()) return false;
        } else if (secondaryOrderSchema != tabuMove.secondaryOrderSchema) {
            return false;
        }
        
        // Para comparación de rutas, verificar solo si tienen la misma estructura
        // (mismo origen-destino) pero no necesariamente los mismos objetos
        return routesHaveSameStructure(primaryRoute, tabuMove.primaryRoute) &&
               routesHaveSameStructure(secondaryRoute, tabuMove.secondaryRoute);
    }
    
    private boolean routesHaveSameStructure(ArrayList<FlightSchema> route1, ArrayList<FlightSchema> route2) {
        if (route1 == null && route2 == null) return true;
        if (route1 == null || route2 == null) return false;
        if (route1.size() != route2.size()) return false;
        
        for (int i = 0; i < route1.size(); i++) {
            FlightSchema f1 = route1.get(i);
            FlightSchema f2 = route2.get(i);
            
            if (f1.getOriginAirportSchema().getId() != f2.getOriginAirportSchema().getId() ||
                f1.getDestinationAirportSchema().getId() != f2.getDestinationAirportSchema().getId()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        return hash;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TabuMove{");
        sb.append(moveType);
        sb.append(", pkg=").append(primaryOrderSchema != null ? primaryOrderSchema.getId() : "null");
        
        if (moveType == MoveType.SWAP) {
            sb.append(", pkg2=").append(secondaryOrderSchema != null ? secondaryOrderSchema.getId() : "null");
        }
        
        sb.append('}');
        return sb.toString();
    }
}
