
package net.sf.freecol.common.model;

import java.util.ArrayList;

/**
* The <code>ModelController</code> is used by the model to perform
* tasks which cannot be done by the model.
*
* <br><br>
*
* The tasks might not be allowed to perform within the model (like generating
* random numbers or creating new {@link FreeColGameObject FreeColGameObjects}),
* or the model might have insufficient data.
*
* <br><br>
*
* Any {@link FreeColGameObject} may get access to the <code>ModelController</code>
* by using {@link Game#getModelController getGame().getModelController()}.
*/
public interface ModelController {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
    * Creates a new unit.
    *
    * @param taskID The <code>taskID</code> should be a unique identifier.
    *               One method to make a unique <code>taskID</code>:
    *               <br><br>
    *               getID() + "methodName:taskDescription"
    *               <br><br>
    *               As long as the "taskDescription" is unique
    *               within the method ("methodName"), you get a unique
    *               identifier.
    * @param location The <code>Location</code> where the <code>Unit</code>
    *               will be created.
    * @param owner  The <code>Player</code> owning the <code>Unit</code>.
    * @param type   The type of unit (Unit.FREE_COLONIST...).
    * @return The created <code>Unit</code>.
    */
    public Unit createUnit(String taskID, Location location, Player owner, int type);

    /**
    * Puts the specified <code>Unit</code> in America.
    * @param unit The <code>Unit</code>.
    * @return The <code>Location</code> where the <code>Unit</code> appears.
    */
    public Location setToVacantEntryLocation(Unit unit);

    /**
    * Tells the <code>ModelController</code> that an internal
    * change (that is; not caused by the control) has occured in the model.
    */
    //public void update(Tile tile);
    

    /**
    * Explores the given tiles for the given player.
    * @param player The <code>Player</code> that should see more tiles.
    * @param tiles The tiles to explore.
    */
    public void exploreTiles(Player player, ArrayList tiles);
    
    
    /**
    * Returns a pseudorandom int, uniformly distributed between 0
    * (inclusive) and the specified value (exclusive).
    * 
    * @param taskID The <code>taskID</code> should be a unique identifier.
    *               One method to make a unique <code>taskID</code>:
    *               <br><br>
    *               getID() + "methodName:taskDescription"
    *               <br><br>
    *               As long as the "taskDescription" is unique
    *               within the method ("methodName"), you get a unique
    *               identifier.
    * @param n The specified value. 
    * @return The generated number.
    */
    public int getRandom(String taskID, int n);
}
