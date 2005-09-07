
package net.sf.freecol.common.model;


/**
* Interface for objects which can be owned by a <code>Player</code>.
* @see Player
*/
public interface Ownable {

    /**
    * Gets the owner of this <code>Ownable</code>.
    *
    * @return The <code>Player</code> controlling this
    *         {@link Ownable}.
    */
    public Player getOwner();
}
