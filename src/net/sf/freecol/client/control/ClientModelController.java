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

package net.sf.freecol.client.control;

import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;

/**
 * A client-side implementation of the <code>ModelController</code> interface.
 */
public class ClientModelController implements ModelController {

    private static final Logger logger = Logger.getLogger(ClientModelController.class.getName());


    private final FreeColClient freeColClient;


    /**
     * Creates a new <code>ClientModelController</code>.
     * 
     * @param freeColClient The main controller.
     */
    public ClientModelController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }

    /**
     * Returns a pseudo-random int, uniformly distributed between 0 (inclusive)
     * and the specified value (exclusive).
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            <br>
     *            As long as the "taskDescription" is unique within the method
     *            ("methodName"), you get a unique identifier.
     * @param n The specified value.
     * @return The generated number.
     */
    public int getRandom(String taskID, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n most be positive");
        } else if (n == 1) {
            return 0;
        }

        Element getRandomElement = Message.createNewRootElement("getRandom");
        getRandomElement.setAttribute("taskID", taskID);
        getRandomElement.setAttribute("n", Integer.toString(n));

        Client client = freeColClient.getClient();
        Element reply = client.ask(getRandomElement);
        if (reply == null || !reply.getTagName().equals("getRandomConfirmed")) {
            throw new IllegalStateException("Expecting getRandomConfirmed");
        }

        int value;
        try {
            value = Integer.parseInt(reply.getAttribute("result"));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Bad result: " + e.getMessage());
        }
 
        logger.finest("getRandom(" + taskID + ", " + n + ") -> " + value);
        return value;
    }

    /**
     * Creates a new unit.
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            <br>
     *            As long as the "taskDescription" is unique within the method
     *            ("methodName"), you get a unique identifier.
     * @param location The <code>Location</code> where the <code>Unit</code>
     *            will be created.
     * @param owner The <code>Player</code> owning the <code>Unit</code>.
     * @param type The type of unit (Unit.FREE_COLONIST...).
     * @return The created <code>Unit</code>.
     */
    public Unit createUnit(String taskID, Location location, Player owner, UnitType type) {

        Element createUnitElement = Message.createNewRootElement("createUnit");
        createUnitElement.setAttribute("taskID", taskID);
        createUnitElement.setAttribute("location", location.getId());
        createUnitElement.setAttribute("owner", owner.getId());
        createUnitElement.setAttribute("type", type.getId());

        logger.info("Waiting for the server to reply...");
        Element reply = freeColClient.getClient().ask(createUnitElement);
        logger.info("Reply received from server.");

        if (!reply.getTagName().equals("createUnitConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }

        Element unitElement = (Element) reply.getElementsByTagName(Unit.getXMLElementTagName()).item(0);
        Unit unit = new Unit(freeColClient.getGame(), unitElement);
        // unit is not really at location yet, set it carefully
        unit.setLocationNoUpdate(null);
        unit.setLocation(location);

        return unit;
    }

    /**
     * Creates a new building.
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            <br>
     *            As long as the "taskDescription" is unique within the method
     *            ("methodName"), you get a unique identifier.
     * @param colony The <code>Colony</code> where the <code>Building</code>
     *            will be created.
     * @param type The type of building (Building.FREE_COLONIST...).
     * @return The created <code>Building</code>.
     */
    public Building createBuilding(String taskID, Colony colony, BuildingType type) {

        Element createBuildingElement = Message.createNewRootElement("createBuilding");
        createBuildingElement.setAttribute("taskID", taskID);
        createBuildingElement.setAttribute("colony", colony.getId());
        createBuildingElement.setAttribute("type", type.getId());

        logger.info("Waiting for the server to reply...");
        Element reply = freeColClient.getClient().ask(createBuildingElement);
        logger.info("Reply received from server.");

        if (!reply.getTagName().equals("createBuildingConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }
        
        Element buildingElement = (Element) reply.getElementsByTagName(Building.getXMLElementTagName()).item(0);
        Building building = new Building(freeColClient.getGame(), buildingElement);
        return building;
    }

    /**
     * Puts the specified <code>Unit</code> in America.
     * 
     * @param unit The <code>Unit</code>.
     * @return The <code>Location</code> where the <code>Unit</code>
     *         appears.
     */
    public Location setToVacantEntryLocation(Unit unit) {
        Element createUnitElement = Message.createNewRootElement("getVacantEntryLocation");
        createUnitElement.setAttribute("unit", unit.getId());

        Element reply = freeColClient.getClient().ask(createUnitElement);
        if (reply == null) {
            throw new IllegalStateException("No reply for getVacantEntryLocation!");
        } else if (!"getVacantEntryLocationConfirmed".equals(reply.getTagName())) {
            throw new IllegalStateException("Unexpected reply type for getVacantEntryLocation: " + reply.getTagName());
        }

        Location entryLocation = (Location) freeColClient.getGame()
                .getFreeColGameObject(reply.getAttribute("location"));
        unit.setLocation(entryLocation);

        return entryLocation;
    }

    /**
     * Explores the given tiles for the given player.
     * 
     * @param player The <code>Player</code> that should see more tiles.
     * @param tiles The tiles to explore.
     */
    public void exploreTiles(Player player, ArrayList<Tile> tiles) {
        // Nothing to do on the client side.
    }

    /**
     * Tells the <code>ModelController</code> that an internal change (that
     * is; not caused by the control) has occured in the model.
     * 
     * @param tile The <code>Tile</code> which will need an update.
     */
    public void update(Tile tile) {
        // Nothing to do on the client side.
    }
    
    /**
     * Tells the <code>ModelController</code> that a tile improvement was finished
     * @param unit an <code>Unit</code> value
     * @param improvement a <code>TileImprovement</code> value
     */
    public void tileImprovementFinished(Unit unit, TileImprovement improvement){
        // Perform TileType change if any
        Tile tile = unit.getTile();
        TileType changeType = improvement.getChange(tile.getType());
        if (changeType != null) {
            // "model.improvement.clearForest"
            tile.setType(changeType);
        } else {
            // "model.improvement.road", "model.improvement.plow"
            tile.add(improvement);
            // FIXME: how should we compute the style better?
        }
    }

    /**
     * Returns a new <code>TradeRoute</code> object.
     * 
     * @return a new <code>TradeRoute</code> object.
     */
    public TradeRoute getNewTradeRoute(Player player) {
        Game game = freeColClient.getGame();
        Client client = freeColClient.getClient();

        Element getNewTradeRouteElement = Message.createNewRootElement("getNewTradeRoute");
        Element reply = client.ask(getNewTradeRouteElement);

        if (!reply.getTagName().equals("getNewTradeRouteConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }

        Element routeElement = (Element) reply.getElementsByTagName(TradeRoute.getXMLElementTagName()).item(0);
        TradeRoute tradeRoute = new TradeRoute(game, routeElement);

        return tradeRoute;
    }

}
