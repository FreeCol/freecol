
package net.sf.freecol.common.model;

import java.util.ArrayList;
import net.sf.freecol.common.PseudoRandom;

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
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
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
    public void exploreTiles(Player player, ArrayList<Tile> tiles);
    
    
    /**
     * Updates stances.
     * @param first The first <code>Player</code>.
     * @param second The second <code>Player</code>.
     * @param stance The new stance.
     */
    public void setStance(Player first, Player second, int stance);
    
    
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

    /**
     * Get a pseudo-random number generator.
     * <p> 
     * Use {@link #getRandom(String, int)} in order to get the
     * same value for a specific token on multiple calls, use
     * the generator directly if this is not important if
     * performance is a factor.
     * 
     * @return random number generator.
     */
    PseudoRandom getPseudoRandom();

    /**
     * Get a new <code>TradeRoute</code> object.
     */
    public TradeRoute getNewTradeRoute(Player player);

}
