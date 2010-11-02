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

package net.sf.freecol.server.control;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;

/**
 * A server-side implementation of the <code>ModelController</code> interface.
 */
public class ServerModelController implements ModelController {

    private static final Logger logger = Logger.getLogger(ServerModelController.class.getName());

    private final FreeColServer freeColServer;

    private final HashMap<String, TaskEntry> taskRegister = new HashMap<String, TaskEntry>();


    /**
     * Creates a new <code>ServerModelController</code>.
     * 
     * @param freeColServer The main controller.
     */
    public ServerModelController(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }

    /**
     * Removes any entries older than {@link TaskEntry#TASK_ENTRY_TIME_OUT}.
     */
    public synchronized void clearTaskRegister() {
        int currentTurn = freeColServer.getGame().getTurn().getNumber();
        List<String> idsToRemove = new ArrayList<String>();
        for (TaskEntry te : taskRegister.values()) {
            if (te.hasExpired(currentTurn)) {
                if (!te.isSecure()) {
                    logger.warning("Possibly a cheating attempt.");
                }
                idsToRemove.add(te.taskID);
            }
        }
        if (!idsToRemove.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Clearing the task register. Removing the following items:");
            for (String id : idsToRemove) {
                taskRegister.remove(id);
                sb.append(" ");
                sb.append(id);
            }
            logger.info(sb.toString());
        }
    }

    /**
     * Puts the specified <code>Unit</code> in America.
     * 
     * @param unit The <code>Unit</code>.
     * @return The <code>Location</code> where the <code>Unit</code>
     *         appears.
     */
    public synchronized Location setToVacantEntryLocation(Unit unit) {
        ServerPlayer player = (ServerPlayer) unit.getOwner();
        Location entryLocation;
        String taskID = unit.getId() + Integer.toString(freeColServer.getGame().getTurn().getNumber());

        if (taskRegister.containsKey(taskID)) {
            entryLocation = (Location) taskRegister.get(taskID).entry;

            // taskRegister.remove(taskID);
        } else {
            entryLocation = unit.getEntryLocation();
            taskRegister.put(taskID, new TaskEntry(taskID, freeColServer.getGame().getTurn().getNumber(), true,
                    entryLocation));
        }

        unit.setLocation(entryLocation);
        unit.setState(UnitState.ACTIVE);

        // Display the tiles surrounding the Unit:
        Element updateElement = Message.createNewRootElement("update");

        for (Tile t: unit.getTile().getSurroundingTiles(unit.getLineOfSight())) {
            updateElement.appendChild(t.toXMLElement(player, updateElement.getOwnerDocument()));
        }

        try {
            player.getConnection().send(updateElement);
        } catch (Exception e) {
            logger.warning("Could not send message to: " + player.getName() + " with connection "
                    + player.getConnection());
        }

        // Send update to enemy players:
        update(unit.getTile(), player);

        return entryLocation;
    }

    /**
     * Sends an update of the given <code>Tile</code> to all the players.
     * 
     * @param tile The <code>Tile</code> to be updated.
     */
    public void update(Tile tile) {
        update(tile, null);
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
        // The server only need to inform the other players about the change
        update(tile,unit.getOwner());
    }

    /**
     * Explores the given tiles for the given player.
     * 
     * @param player The <code>Player</code> that should see more tiles.
     * @param tiles The tiles to explore.
     */
    public void exploreTiles(Player player, ArrayList<Tile> tiles) {
        Element updateElement = Message.createNewRootElement("update");
        for (int i = 0; i < tiles.size(); i++) {
            Tile t = tiles.get(i);
            t.setExploredBy(player, true);
            updateElement.appendChild(t.toXMLElement(player, updateElement.getOwnerDocument()));
        }

        try {
            ((ServerPlayer) player).getConnection().send(updateElement);
        } catch (IOException e) {
            logger.warning("Could not send message to: " + ((ServerPlayer) player).getName() + " with connection "
                    + ((ServerPlayer) player).getConnection());
        }
    }

    /**
     * Sends an update of the given <code>Tile</code> to the other players.
     * 
     * @param newTile The <code>Tile</code> to be updated.
     * @param p The player which should not receive an update (the source of the
     *            change).
     */
    public void update(Tile newTile, Player p) {
        ServerPlayer player = (ServerPlayer) p;
        Game game = freeColServer.getGame();

        Iterator<Player> enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();
            if (enemyPlayer.getConnection() == null
                || (player != null && player.equals(enemyPlayer))) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(newTile)) {
                    Element updateElement = Message.createNewRootElement("update");
                    updateElement.appendChild(newTile.toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));

                    enemyPlayer.getConnection().send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection "
                        + enemyPlayer.getConnection());
            }
        }
    }

    /**
     * Sends an update of the unit to the other players.
     * 
     * @param unit The <code>Unit</code> to be updated.
     * @param p The player which should not receive an update (the source of the
     *            change).
     */
    public void update(Unit unit, Player p) {
        ServerPlayer player = (ServerPlayer) p;
        Game game = freeColServer.getGame();

        Iterator<Player> enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player != null && player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (unit.isVisibleTo(enemyPlayer)) {
                    Element updateElement = Message.createNewRootElement("update");
                    updateElement.appendChild(unit.getTile()
                            .toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));

                    enemyPlayer.getConnection().send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection "
                        + enemyPlayer.getConnection());
            }
        }
    }

    /**
     * Returns a new <code>TradeRoute</code> object.
     * 
     * @return a new <code>TradeRoute</code> object.
     */
    public TradeRoute getNewTradeRoute(Player player) {
        Game game = freeColServer.getGame();
        TradeRoute t = new TradeRoute(game, "", player);
        taskRegister.put(t.getId(), new TaskEntry(t.getId(), game.getTurn().getNumber(), true, t));
        return t;
    }

    /**
     * A single entry in the task register.
     */
    private static class TaskEntry {
        final String taskID;

        final int createdTurn;

        final Object entry;

        private boolean secure;


        TaskEntry(String taskID, int createdTurn, boolean secure, Object entry) {
            this.taskID = taskID;
            this.createdTurn = createdTurn;
            this.secure = secure;
            this.entry = entry;
        }

        synchronized boolean isSecure() {
            return this.secure;
        }

        boolean hasExpired(int currentTurn) {
            return createdTurn + TASK_ENTRY_TIME_OUT < currentTurn;
        }


        /** The number of turns before a <code>TaskEntry</code> has expired. */
        private static final int TASK_ENTRY_TIME_OUT = 5;
    }

}
