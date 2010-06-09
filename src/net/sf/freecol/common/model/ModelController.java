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


package net.sf.freecol.common.model;

import java.util.ArrayList;

import net.sf.freecol.common.model.Player.Stance;

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

    /**
    * Creates a new unit.
    *
    * @param taskID The <code>taskID</code> should be a unique identifier.
    *               One method to make a unique <code>taskID</code>:
    *               <br><br>
    *               getId() + "methodName:taskDescription"
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
    public Unit createUnit(String taskID, Location location, Player owner, UnitType type);

    /**
    * Creates a new building.
    *
    * @param taskID The <code>taskID</code> should be a unique identifier.
    *               One method to make a unique <code>taskID</code>:
    *               <br><br>
    *               getId() + "methodName:taskDescription"
    *               <br><br>
    *               As long as the "taskDescription" is unique
    *               within the method ("methodName"), you get a unique
    *               identifier.
    * @param colony The <code>Colony</code> where the <code>Building</code>
    *               will be created.
    * @param type   The type of building.
    * @return The created <code>Building</code>.
    */
    public Building createBuilding(String taskID, Colony colony, BuildingType type);

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
     * Tells the <code>ModelController</code> that a tile improvement was finished
     * @param unit an <code>Unit</code> value
     * @param improvement a <code>TileImprovement</code> value
     */
    public void tileImprovementFinished(Unit unit, TileImprovement improvement);
    
    /**
     * Updates stances.
     * @param first The first <code>Player</code>.
     * @param second The second <code>Player</code>.
     * @param stance The new stance.
     */
    public void setStance(Player first, Player second, Stance stance);
    
    
    /**
    * Returns a pseudorandom int, uniformly distributed between 0
    * (inclusive) and the specified value (exclusive).
    * 
    * @param taskID The <code>taskID</code> should be a unique identifier.
    *               One method to make a unique <code>taskID</code>:
    *               <br><br>
    *               getId() + "methodName:taskDescription"
    *               <br><br>
    *               As long as the "taskDescription" is unique
    *               within the method ("methodName"), you get a unique
    *               identifier.
    * @param n The specified value. 
    * @return The generated number.
    */
    public int getRandom(String taskID, int n);

    /**
     * Get a new <code>TradeRoute</code> object.
     */
    public TradeRoute getNewTradeRoute(Player player);

}
