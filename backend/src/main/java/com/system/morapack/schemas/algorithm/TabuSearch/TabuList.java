package com.system.morapack.schemas.algorithm.TabuSearch;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Implementa la lista tabú que almacena los movimientos prohibidos temporalmente
 */
public class TabuList {
    private final int maxSize; // Tamaño máximo de la lista tabú (tabu tenure)
    private final Queue<TabuMove> tabuMoves; // Lista de movimientos tabú
    private final long tabuTenure; // Tiempo que un movimiento permanece en la lista tabú (en ms)
    
    /**
     * Constructor con parámetros configurables
     * @param maxSize número máximo de movimientos en la lista tabú
     * @param tabuTenure tiempo en milisegundos que un movimiento permanece tabú
     */
    public TabuList(int maxSize, long tabuTenure) {
        this.maxSize = maxSize;
        this.tabuTenure = tabuTenure;
        this.tabuMoves = new LinkedList<>();
    }
    
    /**
     * Constructor con valores por defecto
     */
    public TabuList() {
        this(100, 1000); // 100 movimientos, 1 segundo de tenure
    }
    
    /**
     * Añade un movimiento a la lista tabú
     */
    public void add(TabuMove move) {
        // Purgar movimientos expirados antes de añadir uno nuevo
        purgeExpiredMoves();
        
        // Añadir nuevo movimiento
        tabuMoves.add(move);
        
        // Mantener el tamaño máximo
        while (tabuMoves.size() > maxSize) {
            tabuMoves.poll();
        }
    }
    
    /**
     * Verifica si un movimiento está en la lista tabú
     */
    public boolean contains(TabuMove move) {
        // Primero purgar movimientos expirados
        purgeExpiredMoves();
        
        // Verificar si el movimiento está en la lista
        for (TabuMove tabuMove : tabuMoves) {
            if (tabuMove.equals(move)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Limpia la lista tabú eliminando movimientos expirados
     */
    private void purgeExpiredMoves() {
        long currentTime = System.currentTimeMillis();
        while (!tabuMoves.isEmpty() && (currentTime - tabuMoves.peek().getTimestamp()) > tabuTenure) {
            tabuMoves.poll();
        }
    }
    
    /**
     * Elimina todos los movimientos de la lista tabú
     */
    public void clear() {
        tabuMoves.clear();
    }
    
    /**
     * Devuelve el número de movimientos en la lista tabú
     */
    public int size() {
        purgeExpiredMoves();
        return tabuMoves.size();
    }
    
    /**
     * Devuelve el tamaño máximo de la lista tabú
     */
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * Devuelve la duración de tenure tabú en milisegundos
     */
    public long getTabuTenure() {
        return tabuTenure;
    }
}
