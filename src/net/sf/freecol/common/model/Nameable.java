
package net.sf.freecol.common.model;

/**
 * An object that has a name, which also can be changed.
 */
public interface Nameable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
    * Gets the name of this <code>Nameable</code>.
    * @return The name of the <code>Nameable</code>.
    */
    public String getName();

    /**
    * Sets the name for this <code>Nameable</code>.
    * @param newName The new name for the <code>Nameable</code>.
    */
    public void setName(String newName);

}
