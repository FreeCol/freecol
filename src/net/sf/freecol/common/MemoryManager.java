/**
 *  Copyright (C) 2002-2024   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common;

/**
 * Calculates how much memory is available to various tasks.
 */
public class MemoryManager {
    
    private static final long MEMORY_LOW_LIMIT = 2000000000; // a bit less than 2GB

    /**
     * <code>true</code> if the game has been started with low memory allocated.
     */
    private static final boolean lowMemory = Runtime.getRuntime().maxMemory() < MEMORY_LOW_LIMIT;
    
        
    /**
     * Checks if high quality graphics is enabled.
     * 
     * No image file named "sizeX" (where X is any number) should be loaded if
     * this method returns false.
     * 
     * @return <code>true</code> if high quality graphics has been enabled for the map.
     */
    public static boolean isHighQualityGraphicsEnabled() {
        return !lowMemory;
    }
    
    /**
     * Checks if smooth scrolling is enabled.
     * @return <code>true</code> if there is sufficient memory for the image buffer
     *      that is needed to support smooth scrolling.
     */
    public static boolean isSmoothScrollingEnabled() {
        return !lowMemory;
    }
}
