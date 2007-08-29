
package net.sf.freecol.common.model;

/**
 * An object that has a name.
 */
public interface Named {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
    * Gets the name of this <code>Named</code>.
    * @return The name of the <code>Named</code>.
    */
    public String getName();
}
