package src.model.algorithm;

public enum AlgorithmType {
    GREEDY("Greedy (Algoritmo Constructivo)"),
    TABU_SEARCH("Tabu Search"),
    ALNS("ALNS (Adaptive Large Neighborhood Search)"),
    HYBRID("Híbrido (Greedy + Tabu Search + ALNS)");
    
    private final String displayName;
    
    AlgorithmType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}

