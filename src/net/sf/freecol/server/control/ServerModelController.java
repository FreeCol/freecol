
package net.sf.freecol.server.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
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

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The number of turns before a <code>TaskEntry</code> is removed. */
    private static final int TASK_ENTRY_TIME_OUT = 5;

    private final FreeColServer freeColServer;

    private HashMap taskRegister = new HashMap();
    private final Random random = new Random();


    /**
    * Creates a new <code>ServerModelController</code>.
    * @param freeColServer The main controller.
    */
    public ServerModelController(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }


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
    public synchronized int getRandom(String taskID, int n) {
        String extendedTaskID = taskID + Integer.toString(freeColServer.getGame().getTurn().getNumber());

        logger.info("Entering getRandom");

        if (taskRegister.containsKey(extendedTaskID)) {
            //return ((Integer) taskRegister.remove(extendedTaskID)).intValue();
            return ((Integer) ((TaskEntry) taskRegister.get(extendedTaskID)).entry).intValue();
        } else {
            int value = random.nextInt(n);
            taskRegister.put(extendedTaskID, new TaskEntry(extendedTaskID, freeColServer.getGame().getTurn().getNumber(), true, new Integer(value)));
            return value;
        }

    }


    /**
    * Removes any entries older than {@link #TASK_ENTRY_TIME_OUT}.
    */
    public synchronized void clearTaskRegister() {
        int currentTurn = freeColServer.getGame().getTurn().getNumber();

        String log = null;
        Iterator it = taskRegister.values().iterator();
        while (it.hasNext()) {
            TaskEntry te = (TaskEntry) it.next();
            if (te.createdTurn + TASK_ENTRY_TIME_OUT < currentTurn) {
                if (!te.secure) {
                    logger.warning("Possibly a cheating attempt.");
                }
                it.remove();
                if (log == null) {
                    log = "Clearing the task register. Removing the following items: ";
                }
                log += te.taskID + " ";
            }
        }

        if (log != null) {
            logger.info(log);
        }
    }

    
    /**
    * Creates a new unit. This method is the same as running
    * {@link #createUnit(String, Location, Player, int, boolean, Connection)}
    * with <code>secure = true</code> and <code>connection = null</code>.
    *
    * @param taskID The <code>taskID</code> should be a unique identifier.
    *               One method to make a unique <code>taskID</code>:
    *               <br><br>
    *               getID() + "methodName:taskDescription"
    *               <br>br>
    *               As long as the "taskDescription" is unique
    *               within the method ("methodName"), you get a unique
    *               identifier.
    * @param location The <code>Location</code> where the <code>Unit</code>
    *               will be created.
    * @param owner  The <code>Player</code> owning the <code>Unit</code>.
    * @param type   The type of unit (Unit.FREE_COLONIST...).
    * @return A reference to the <code>Unit</code> which has been created.
    */
    public synchronized Unit createUnit(String taskID, Location location, Player owner, int type) {
        return createUnit(taskID, location, owner, type, true, null);
    }


    /**
    * Creates a new unit.
    *
    * @param taskID The <code>taskID</code> should be a unique identifier.
    *               One method to make a unique <code>taskID</code>:
    *               <br><br>
    *               getID() + "methodName:taskDescription"
    *               <br>br>
    *               As long as the "taskDescription" is unique
    *               within the method ("methodName"), you get a unique
    *               identifier.
    * @param location The <code>Location</code> where the <code>Unit</code>
    *               will be created.
    * @param owner  The <code>Player</code> owning the <code>Unit</code>.
    * @param type   The type of unit (Unit.FREE_COLONIST...).
    * @param secure This variable should be set to <code>false</code> in case this method
    *               is called when serving a client. Setting this variable to <code>false</code>
    *               signals that the request might be illegal.
    * @param connection The connection that has requested to create the unit, or null if this
    *               request is internal to the server.
    * @return A reference to the <code>Unit</code> which has been created.
    */
    public synchronized Unit createUnit(String taskID, Location location, Player owner, int type, boolean secure, Connection connection) {
        String extendedTaskID = taskID + owner.getID() + Integer.toString(freeColServer.getGame().getTurn().getNumber());
        Unit unit;
        TaskEntry taskEntry;

        logger.info("Entering createUnit.");

        if (taskRegister.containsKey(extendedTaskID)) {
            taskEntry = (TaskEntry) taskRegister.get(extendedTaskID);
            unit = (Unit) taskEntry.entry;

            if (unit.getLocation().getTile() != location.getTile() || unit.getOwner() != owner || unit.getType() != type) {
                logger.warning("Unsynchronization between the client and the server. Maybe a cheating attempt! Differences: " +
                        ((unit.getLocation().getTile() != location.getTile()) ? "location: " + unit.getLocation().getTile() + "!=" + location.getTile(): "") +
                        ((unit.getOwner() != owner) ? "owner: " + unit.getOwner() + "!=" + owner : "") +
                        ((unit.getType() != type) ? "type: " + unit.getType() + "!=" + type : ""));

                taskRegister.remove(extendedTaskID);
                unit.dispose();
                return null;
            }

            if (secure) {
                taskEntry.secure = true;
            }
        } else {
            unit = new Unit(freeColServer.getGame(), location, owner, type, Unit.ACTIVE);
            taskEntry = new TaskEntry(extendedTaskID, freeColServer.getGame().getTurn().getNumber(), secure, unit);
            taskRegister.put(extendedTaskID, taskEntry);
        }

        /*
        if (connection != null) {
            update(unit, freeColServer.getPlayer(connection));
        }
        */ 

        return unit;
    }

    
    /**
    * Puts the specified <code>Unit</code> in America.
    * @param unit The <code>Unit</code>.
    * @return The <code>Location</code> where the <code>Unit</code> appears.
    */
    public synchronized Location setToVacantEntryLocation(Unit unit) {
        Game game = freeColServer.getGame();
        ServerPlayer player = (ServerPlayer) unit.getOwner();
        Location entryLocation;
        String taskID = unit.getID() + Integer.toString(freeColServer.getGame().getTurn().getNumber());

        if (taskRegister.containsKey(taskID)) {
            entryLocation = (Location) ((TaskEntry) taskRegister.get(taskID)).entry;

            //taskRegister.remove(taskID);
        } else {
            entryLocation = unit.getVacantEntryLocation();
            taskRegister.put(taskID, new TaskEntry(taskID, freeColServer.getGame().getTurn().getNumber(), true, entryLocation));
        }

        unit.setLocation(entryLocation);
        unit.setState(Unit.ACTIVE);

        // Display the tiles surrounding the Unit:
        Element updateElement = Message.createNewRootElement("update");
        Vector surroundingTiles = game.getMap().getSurroundingTiles(unit.getTile(), unit.getLineOfSight());

        for (int i=0; i<surroundingTiles.size(); i++) {
            Tile t = (Tile) surroundingTiles.get(i);
            updateElement.appendChild(t.toXMLElement(player, updateElement.getOwnerDocument()));
        }

        try {
            player.getConnection().send(updateElement);
        } catch (IOException e) {
            logger.warning("Could not send message to: " + player.getName() + " with connection " + player.getConnection());
        }

        // Send update to enemy players:
        update(unit.getTile(), player);

        return entryLocation;
    }


    /**
     * Sends an update of the given <code>Tile</code>
     * to all the players.
     * 
     * @param tile The <code>Tile</code> to be updated.
     */          
    public void update(Tile tile) {
        update(tile, null);
    }


    /**
    * Explores the given tiles for the given player.
    * @param player The <code>Player</code> that should see more tiles.
    * @param tiles The tiles to explore.
    */
    public void exploreTiles(Player player, ArrayList tiles) {
        Element updateElement = Message.createNewRootElement("update");
        for (int i=0; i<tiles.size(); i++) {
            Tile t = (Tile) tiles.get(i);
            updateElement.appendChild(t.toXMLElement(((ServerPlayer) player), updateElement.getOwnerDocument()));
        }

        try {
            ((ServerPlayer) player).getConnection().send(updateElement);
        } catch (IOException e) {
            logger.warning("Could not send message to: " + ((ServerPlayer) player).getName() + " with connection " + ((ServerPlayer) player).getConnection());
        }
    }


    /**
     * Sends an update of the given <code>Tile</code>
     * to the other players.
     * 
     * @param newTile The <code>Tile</code> to be updated.
     * @param p The player which should not receive an update (the source of the change).
     */    
    public void update(Tile newTile, Player p) {
        ServerPlayer player = (ServerPlayer) p;
        Game game = freeColServer.getGame();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player != null && player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(newTile)) {
                    Element updateElement = Message.createNewRootElement("update");
                    updateElement.appendChild(newTile.toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));

                    enemyPlayer.getConnection().send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() 
                			   + " with connection " + enemyPlayer.getConnection());
            }
        }
    }


    /**
     * Sends an update of the unit to the other players.
     * 
     * @param unit The <code>Unit</code> to be updated.
     * @param p The player which should not receive an update (the source of the change).
     */
    public void update(Unit unit, Player p) {
        ServerPlayer player = (ServerPlayer) p;
        Game game = freeColServer.getGame();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player != null && player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (unit.isVisibleTo(enemyPlayer)) {
                    Element updateElement = Message.createNewRootElement("update");
                    updateElement.appendChild(unit.getTile().toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));

                    enemyPlayer.getConnection().send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }
    }





    /**
    * A single entry in the task register.
    */
    private class TaskEntry {
        String taskID;
        int createdTurn;
        boolean secure;
        Object entry;

        TaskEntry(String taskID, int createdTurn, boolean secure, Object entry) {
            this.taskID = taskID;
            this.createdTurn = createdTurn;
            this.secure = secure;
            this.entry = entry;
        }
    }
}

