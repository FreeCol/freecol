
package net.sf.freecol.common.model;



/**
* An object that can be put in a <code>Location</code>.
*/
public interface Locatable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
    * Sets the location for this <code>Locatable</code>.
    * @param newLocation The new <code>Location</code> for the <code>Locatable</code>.
    */
    public void setLocation(Location newLocation);


    /**
    * Gets the location of this <code>Locatable</code>.
    * @return The location of this <code>Locatable</code>.
    */
    public Location getLocation();
    
    
    /**
    * Gets the amount of space this <code>Locatable</code> take.
    */
    public int getTakeSpace();
}
