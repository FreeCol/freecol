
package net.sf.freecol.common.model;

import java.util.Iterator;


/**
* A place where a <code>Locatable</code> can be put.
*
* @see Locatable
*/
public interface Location {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
    * Returns the Tile where this Location is located. Or null if
    * no Tile applies.
    *
    * @return The Tile where this Location is located. Or null if
    * no Tile applies.
    */
    public Tile getTile();


    /**
    * Adds a <code>Locatable</code> to this Location.
    *
    * @param locatable The code>Locatable</code> to add to this Location.
    */
    public void add(Locatable locatable);


    /**
    * Removes a <code>Locatable</code> from this Location.
    *
    * @param locatable The <code>Locatable</code> to remove from this Location.
    */
    public void remove(Locatable locatable);


    /**
    * Checks if this <code>Location</code> contains the specified
    * <code>Locatable</code>.
    *
    * @param locatable The <code>Locatable</code> to test the
    *        presence of.
    * @return <ul>
    *           <li><i>true</i>  if the specified <code>Locatable</code>
    *                            is on this <code>Location</code> and
    *           <li><i>false</i> otherwise.
    *         </ul>
    */
    public boolean contains(Locatable locatable);

    
    /**
    * Checks wether or not the specified locatable may be added to this
    * <code>Location</code>.
    *
    * @param locatable The <code>Locatable</code> to test the addabillity of.
    * @return The result.
    */
    public boolean canAdd(Locatable locatable);
    
    
    /**
    * Returns the amount of Units at this Location.
    *
    * @return The amount of Units at this Location.
    */
    public int getUnitCount();


    /**
    * Gets a <code>Iterator</code> of every <code>Unit</code> directly located on this
    * <code>Location</code>.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getUnitIterator();

    
    /**
    * @see FreeColGameObject#getID
    */
    public String getID();

    public GoodsContainer getGoodsContainer();
}
