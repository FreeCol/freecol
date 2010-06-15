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
public class ServerModelController implements ModelController,PropertyChangeListener {

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
    public synchronized int getRandom(String taskID, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n most be positive");
        } else if (n == 1) {
            return 0;
        }
        int turn = freeColServer.getGame().getTurn().getNumber();
        String extendedTaskID = taskID + Integer.toString(turn);
        int value;
        if (taskRegister.containsKey(extendedTaskID)) {
            value = ((Integer) taskRegister.get(extendedTaskID).entry).intValue();
        } else {
            value = freeColServer.getServerRandom().nextInt(n);
            taskRegister.put(extendedTaskID,
                             new TaskEntry(extendedTaskID, turn, true, new Integer(value)));
        }
        logger.finest("getRandom(" + taskID + ", " + n + ") -> " + value);
        return value;
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
     * Creates a new unit. This method is the same as running
     * {@link #createUnit(String, Location, Player, UnitType, boolean, Connection)}
     * with <code>secure = true</code> and <code>connection = null</code>.
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            br> As long as the "taskDescription" is unique within the
     *            method ("methodName"), you get a unique identifier.
     * @param location The <code>Location</code> where the <code>Unit</code>
     *            will be created.
     * @param owner The <code>Player</code> owning the <code>Unit</code>.
     * @param type The type of unit (Unit.FREE_COLONIST...).
     * @return A reference to the <code>Unit</code> which has been created.
     */
    public synchronized Unit createUnit(String taskID, Location location, Player owner, UnitType type) {
        return createUnit(taskID, location, owner, type, true, null);
    }

    /**
     * Creates a new unit.
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            br> As long as the "taskDescription" is unique within the
     *            method ("methodName"), you get a unique identifier.
     * @param location The <code>Location</code> where the <code>Unit</code>
     *            will be created.
     * @param owner The <code>Player</code> owning the <code>Unit</code>.
     * @param type The type of unit (Unit.FREE_COLONIST...).
     * @param secure This variable should be set to <code>false</code> in case
     *            this method is called when serving a client. Setting this
     *            variable to <code>false</code> signals that the request
     *            might be illegal.
     * @param connection The connection that has requested to create the unit,
     *            or null if this request is internal to the server.
     * @return A reference to the <code>Unit</code> which has been created.
     */
    public synchronized Unit createUnit(String taskID, Location location, Player owner, UnitType type, boolean secure,
            Connection connection) {
        String extendedTaskID = taskID + owner.getId()
                + Integer.toString(freeColServer.getGame().getTurn().getNumber());
        Unit unit;
        TaskEntry taskEntry;

        logger.info("Entering createUnit.");

        if (taskRegister.containsKey(extendedTaskID)) {
            taskEntry = taskRegister.get(extendedTaskID);
            unit = (Unit) taskEntry.entry;

            if (unit.getLocation().getTile() != location.getTile() || unit.getOwner() != owner
                    || unit.getType() != type) {
                logger
                        .warning("Unsynchronization between the client and the server. Maybe a cheating attempt! Differences: "
                                + ((unit.getLocation().getTile() != location.getTile()) ? "location: "
                                        + unit.getLocation().getTile() + "!=" + location.getTile() : "")
                                + ((unit.getOwner() != owner) ? "owner: " + unit.getOwner() + "!=" + owner : "")
                                + ((unit.getType() != type) ? "type: " + unit.getType() + "!=" + type : ""));

                taskRegister.remove(extendedTaskID);
                unit.dispose();
                return null;
            }

            if (secure) {
                taskEntry.secure = true;
            }
        } else {
            unit = new Unit(freeColServer.getGame(), location, owner, type, UnitState.ACTIVE);
            taskEntry = new TaskEntry(extendedTaskID, freeColServer.getGame().getTurn().getNumber(), secure, unit);
            taskRegister.put(extendedTaskID, taskEntry);
        }

        /*
         * if (connection != null) { update(unit,
         * freeColServer.getPlayer(connection)); }
         */

        return unit;
    }

    /**
     * Creates a new building. This method is the same as running
     * {@link #createBuilding(String, Colony, BuildingType, boolean, Connection)}
     * with <code>secure = true</code> and <code>connection = null</code>.
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            <br> As long as the "taskDescription" is unique within the
     *            method ("methodName"), you get a unique identifier.
     * @param colony The <code>Colony</code> where the <code>Building</code>
     *            will be created.
     * @param type The type of building.
     * @return A reference to the <code>Building</code> which has been created.
     */
    public synchronized Building createBuilding(String taskID, Colony colony, BuildingType type) {
        return createBuilding(taskID, colony, type, true, null);
    }

    /**
     * Creates a new building.
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            <br> As long as the "taskDescription" is unique within the
     *            method ("methodName"), you get a unique identifier.
     * @param colony The <code>Colony</code> where the <code>Building</code>
     *            will be created.
     * @param type The type of building.
     * @param secure This variable should be set to <code>false</code> in case
     *            this method is called when serving a client. Setting this
     *            variable to <code>false</code> signals that the request
     *            might be illegal.
     * @param connection The connection that has requested to create the building,
     *            or null if this request is internal to the server.
     * @return A reference to the <code>Building</code> which has been created.
     */
    public synchronized Building createBuilding(String taskID, Colony colony, BuildingType type, boolean secure,
                                                Connection connection) {
        String extendedTaskID = taskID + colony.getOwner().getId()
                + Integer.toString(freeColServer.getGame().getTurn().getNumber());
        Building building;
        TaskEntry taskEntry;
        Player owner = colony.getOwner();

        logger.info("Entering createBuilding.");

        if (taskRegister.containsKey(extendedTaskID)) {
            taskEntry = taskRegister.get(extendedTaskID);
            building = (Building) taskEntry.entry;

            if (building.getColony().getTile() != colony.getTile() ||
                building.getOwner() != colony.getOwner() ||
                building.getType() != type) {
                logger.warning("Unsynchronization between the client and the server. Maybe a cheating attempt! Differences: "
                               + ((building.getColony().getTile() != colony.getTile()) ? "colony: "
                                  + building.getColony().getTile() + "!=" + colony.getTile() : "")
                               + ((building.getOwner() != owner) ? "owner: " + building.getOwner() + "!=" + owner : "")
                               + ((building.getType() != type) ? "type: " + building.getType() + "!=" + type : ""));
                
                taskRegister.remove(extendedTaskID);
                building.dispose();
                return null;
            }

            if (secure) {
                taskEntry.secure = true;
            }
        } else {
            building = new Building(freeColServer.getGame(), colony,type);
            taskEntry = new TaskEntry(extendedTaskID, freeColServer.getGame().getTurn().getNumber(), secure, building);
            taskRegister.put(extendedTaskID, taskEntry);
        }

        /*
         * if (connection != null) { update(building,
         * freeColServer.getPlayer(connection)); }
         */

        return building;
    }

    /**
     * Puts the specified <code>Unit</code> in America.
     * 
     * @param unit The <code>Unit</code>.
     * @return The <code>Location</code> where the <code>Unit</code>
     *         appears.
     */
    public synchronized Location setToVacantEntryLocation(Unit unit) {
        Game game = freeColServer.getGame();
        ServerPlayer player = (ServerPlayer) unit.getOwner();
        Location entryLocation;
        String taskID = unit.getId() + Integer.toString(freeColServer.getGame().getTurn().getNumber());

        if (taskRegister.containsKey(taskID)) {
            entryLocation = (Location) taskRegister.get(taskID).entry;

            // taskRegister.remove(taskID);
        } else {
            entryLocation = unit.getVacantEntryLocation();
            taskRegister.put(taskID, new TaskEntry(taskID, freeColServer.getGame().getTurn().getNumber(), true,
                    entryLocation));
        }

        unit.setLocation(entryLocation);
        unit.setState(UnitState.ACTIVE);

        // Display the tiles surrounding the Unit:
        Element updateElement = Message.createNewRootElement("update");
        List<Tile> surroundingTiles = game.getMap().getSurroundingTiles(unit.getTile(), unit.getLineOfSight());

        for (int i = 0; i < surroundingTiles.size(); i++) {
            Tile t = surroundingTiles.get(i);
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
     * Updates stances.
     * 
     * @param first The first <code>Player</code>.
     * @param second The second <code>Player</code>.
     * @param stance The new stance.
     */
    public void setStance(Player first, Player second, Stance stance) {
        Element element = Message.createNewRootElement("setStance");
        element.setAttribute("stance", stance.toString());
        element.setAttribute("first", first.getId());
        element.setAttribute("second", second.getId());

        Iterator<Player> enemyPlayerIterator = first.getGame().getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();
            if (!enemyPlayer.equals(first) && enemyPlayer.isConnected()) {
                try {
                    enemyPlayer.getConnection().send(element);
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection "
                            + enemyPlayer.getConnection());
                }
            }
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
        String name = "";
        return new TradeRoute(game, name, player);
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
    
    public void updateModelListening(){
        for(Player player : freeColServer.getGame().getPlayers()){
            if(!player.isIndian()){
                continue;
            }
            for(Settlement settlement : player.getIndianSettlements()){
                settlement.addPropertyChangeListener("alarmLevel", this);
            }
        }
    }

    
    public void propertyChange(PropertyChangeEvent e) {
        if(e.getPropertyName() == "alarmLevel"){
            IndianSettlement settlement = (IndianSettlement) e.getSource();
            update(settlement.getTile());
        }
    }
}
