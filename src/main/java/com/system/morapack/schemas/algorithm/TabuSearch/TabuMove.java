package com.system.morapack.schemas.algorithm.TabuSearch;

import com.system.morapack.schemas.Flight;
import com.system.morapack.schemas.Package;

import java.util.ArrayList;
import java.util.Objects;

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
    private final Package primaryPackage;
    private final ArrayList<Flight> primaryRoute;
    private final Package secondaryPackage; // Usado solo para SWAP
    private final ArrayList<Flight> secondaryRoute; // Usado para REASSIGN y SWAP
    private final int hash;
    private final long timestamp;
    
    /**
     * Constructor para movimientos INSERT y REMOVE
     */
    public TabuMove(MoveType moveType, Package pkg, ArrayList<Flight> route) {
        this.moveType = moveType;
        this.primaryPackage = pkg;
        this.primaryRoute = route != null ? new ArrayList<>(route) : new ArrayList<>();
        this.secondaryPackage = null;
        this.secondaryRoute = null;
        this.timestamp = System.currentTimeMillis();
        this.hash = calculateHash();
    }
    
    /**
     * Constructor para movimientos REASSIGN
     */
    public TabuMove(MoveType moveType, Package pkg, ArrayList<Flight> oldRoute, ArrayList<Flight> newRoute) {
        this.moveType = moveType;
        this.primaryPackage = pkg;
        this.primaryRoute = oldRoute != null ? new ArrayList<>(oldRoute) : new ArrayList<>();
        this.secondaryPackage = null;
        this.secondaryRoute = newRoute != null ? new ArrayList<>(newRoute) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.hash = calculateHash();
    }
    
    /**
     * Constructor para movimientos SWAP
     */
    public TabuMove(MoveType moveType, Package pkg1, ArrayList<Flight> route1, Package pkg2, ArrayList<Flight> route2) {
        this.moveType = moveType;
        this.primaryPackage = pkg1;
        this.primaryRoute = route1 != null ? new ArrayList<>(route1) : new ArrayList<>();
        this.secondaryPackage = pkg2;
        this.secondaryRoute = route2 != null ? new ArrayList<>(route2) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.hash = calculateHash();
    }
    
    public MoveType getMoveType() {
        return moveType;
    }
    
    public Package getPrimaryPackage() {
        return primaryPackage;
    }
    
    public ArrayList<Flight> getPrimaryRoute() {
        return primaryRoute;
    }
    
    public Package getSecondaryPackage() {
        return secondaryPackage;
    }
    
    public ArrayList<Flight> getSecondaryRoute() {
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
                return new TabuMove(MoveType.REMOVE, primaryPackage, primaryRoute);
            case REMOVE:
                return new TabuMove(MoveType.INSERT, primaryPackage, primaryRoute);
            case REASSIGN:
                return new TabuMove(MoveType.REASSIGN, primaryPackage, secondaryRoute, primaryRoute);
            case SWAP:
                return new TabuMove(MoveType.SWAP, secondaryPackage, secondaryRoute, primaryPackage, primaryRoute);
            default:
                return null;
        }
    }
    
    private int calculateHash() {
        int hash = 7;
        hash = 31 * hash + moveType.hashCode();
        hash = 31 * hash + (primaryPackage != null ? primaryPackage.getId() : 0);
        hash = 31 * hash + (secondaryPackage != null ? secondaryPackage.getId() : 0);
        
        // Crear un hash que refleja la estructura de la ruta, no los objetos específicos
        if (primaryRoute != null && !primaryRoute.isEmpty()) {
            int routeHash = 0;
            for (Flight f : primaryRoute) {
                routeHash += f.getOriginAirport().getId() * 31 + f.getDestinationAirport().getId();
            }
            hash = 31 * hash + routeHash;
        }
        
        if (secondaryRoute != null && !secondaryRoute.isEmpty()) {
            int routeHash = 0;
            for (Flight f : secondaryRoute) {
                routeHash += f.getOriginAirport().getId() * 31 + f.getDestinationAirport().getId();
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
        
        if (primaryPackage != null && tabuMove.primaryPackage != null) {
            if (primaryPackage.getId() != tabuMove.primaryPackage.getId()) return false;
        } else if (primaryPackage != tabuMove.primaryPackage) {
            return false;
        }
        
        if (secondaryPackage != null && tabuMove.secondaryPackage != null) {
            if (secondaryPackage.getId() != tabuMove.secondaryPackage.getId()) return false;
        } else if (secondaryPackage != tabuMove.secondaryPackage) {
            return false;
        }
        
        // Para comparación de rutas, verificar solo si tienen la misma estructura
        // (mismo origen-destino) pero no necesariamente los mismos objetos
        return routesHaveSameStructure(primaryRoute, tabuMove.primaryRoute) &&
               routesHaveSameStructure(secondaryRoute, tabuMove.secondaryRoute);
    }
    
    private boolean routesHaveSameStructure(ArrayList<Flight> route1, ArrayList<Flight> route2) {
        if (route1 == null && route2 == null) return true;
        if (route1 == null || route2 == null) return false;
        if (route1.size() != route2.size()) return false;
        
        for (int i = 0; i < route1.size(); i++) {
            Flight f1 = route1.get(i);
            Flight f2 = route2.get(i);
            
            if (f1.getOriginAirport().getId() != f2.getOriginAirport().getId() ||
                f1.getDestinationAirport().getId() != f2.getDestinationAirport().getId()) {
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
        sb.append(", pkg=").append(primaryPackage != null ? primaryPackage.getId() : "null");
        
        if (moveType == MoveType.SWAP) {
            sb.append(", pkg2=").append(secondaryPackage != null ? secondaryPackage.getId() : "null");
        }
        
        sb.append('}');
        return sb.toString();
    }
}
