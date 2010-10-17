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
