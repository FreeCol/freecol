package net.sf.freecol.common;

public class MemoryManager {
    
    private static final long MEMORY_LOW_LIMIT = 2000000000; // a bit less than 2GB

    private static final boolean lowMemory = Runtime.getRuntime().maxMemory() < MEMORY_LOW_LIMIT;
    
    
    /**
     * Checks if the game is being started with low memory.
     * @return <code>true</code> if low memory is available.
     */
    public static boolean isMaximumMemoryLow() {
        return lowMemory;
    }
    
    /**
     * Checks if high quality graphics has been deactivated.
     * @return <code>true</code> if high quality graphics has been deactivated.
     */
    public static boolean isHighQualityGraphicsDeactivated() {
        return isMaximumMemoryLow();
    }
    
    public static boolean isSmoothScrollingEnabled() {
        return !isMaximumMemoryLow();
    }
}
