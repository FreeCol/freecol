/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.common.util;

/**
 * Collection of small static helper methods.
 */
public class Utils {





    /**
     * Will check if both objects are equal but also checks for null.
     * 
     * @param one First object to compare
     * @param two Second object to compare
     * @return <code>(one == null && two != null) || (one != null && one.equals(two))</code>
     */
    public static boolean equals(Object one, Object two) {
        return one == null ? two == null : one.equals(two);
    }

    /**
     * Generalize this method instead of calling it directly elsewhere.
     * 
     * @return			String
     */
    public static String getUserDirectory() {
    	return System.getProperty("user.home");
    }

    /**
     * Return the minimum of 2 values
     * 
     * @param value1
     * @param value2
     * @return
     */
    public static int min( int value1, int value2 ) {
    	return value1 < value2 ? value1 : value2;
    }
    
    /**
     * Return the maximum of 2 values
     * 
     * @param value1
     * @param value2
     * @return
     */
    public static int max( int value1, int value2 ) {
    	return value1 > value2 ? value1 : value2;
    }
}
