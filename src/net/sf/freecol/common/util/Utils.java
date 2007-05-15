package net.sf.freecol.common.util;

/**
 * Collection of small static helper methods.
 */
public class Utils {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


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

}
