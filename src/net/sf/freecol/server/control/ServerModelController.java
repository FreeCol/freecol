
package net.sf.freecol.server.control;

import net.sf.freecol.common.model.*;
import net.sf.freecol.server.model.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.common.networking.Message;
import org.w3c.dom.*;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.*;
import java.util.*;


/**
* A server-side implementation of the <code>ModelController</code> interface.
*/
public class ServerModelController implements ModelController {
    private static final Logger logger = Logger.getLogger(ServerModelController.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

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
    */
    public synchronized int getRandom(String taskID, int n) {
        String extendedTaskID = taskID + Integer.toString(freeColServer.getGame().getTurn().getNumber());
        
        logger.info("Entering getRandom");
        
        if (taskRegister.containsKey(extendedTaskID)) {
            //return ((Integer) taskRegister.remove(extendedTaskID)).intValue();
            return ((Integer) taskRegister.get(extendedTaskID)).intValue();
        } else {
            int value = random.nextInt(n);
            taskRegister.put(extendedTaskID, new Integer(value));
            return value;
        }

    }

    
    public void clearTaskRegister() {
        String log = null;
        Iterator it = taskRegister.keySet().iterator();
        
        if (it.hasNext()) {
            log = "Clearing the task register. Removing the following items: ";
        }

        while (it.hasNext()) {
            log += it.next().toString() + " ";
        }
        
        if (log != null) {
            logger.info(log);
        }

        taskRegister.clear();
    }


    public synchronized Unit createUnit(String taskID, Location location, Player owner, int type) {
        String extendedTaskID = taskID + owner.getID() + Integer.toString(freeColServer.getGame().getTurn().getNumber());
        Unit unit;

        logger.info("Entering createUnit.");

        if (taskRegister.containsKey(extendedTaskID)) {
            unit = (Unit) taskRegister.get(extendedTaskID);

            if (unit.getLocation().getTile() != location.getTile() || unit.getOwner() != owner || unit.getType() != type) {
                logger.warning("Unsynchronization between the client and the server. Maybe a cheating attempt! Differences: " +
                        ((unit.getLocation().getTile() != location.getTile()) ? "location: " + unit.getLocation().getTile() + "!=" + location.getTile(): "") +
                        ((unit.getOwner() != owner) ? "owner: " + unit.getOwner() + "!=" + owner : "") +
                        ((unit.getType() != type) ? "type: " + unit.getType() + "!=" + type : ""));

                unit.dispose();
                return null;
            }

            //taskRegister.remove(extendedTaskID);
        } else {
            unit = new Unit(freeColServer.getGame(), location, owner, type, Unit.ACTIVE);
            taskRegister.put(extendedTaskID, unit);
        }

        return unit;
    }


    public synchronized Location setToVacantEntryLocation(Unit unit) {
        Game game = freeColServer.getGame();
        ServerPlayer player = (ServerPlayer) unit.getOwner();
        Location entryLocation;
        String taskID = unit.getID() + Integer.toString(freeColServer.getGame().getTurn().getNumber());

        if (taskRegister.containsKey(taskID)) {
            entryLocation = (Location) taskRegister.get(taskID);
            //taskRegister.remove(taskID);
        } else {
            entryLocation = unit.getVacantEntryLocation();
            taskRegister.put(taskID, entryLocation);
        }

        unit.setLocation(entryLocation);

        // Display the tiles surrounding the Unit:
        Element updateElement = Message.createNewRootElement("update");
        Vector surroundingTiles = game.getMap().getSurroundingTiles(unit.getTile(), unit.getLineOfSight());

        for (int i=0; i<surroundingTiles.size(); i++) {
            Tile t = (Tile) surroundingTiles.get(i);
            player.setExplored(t);
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
            ((ServerPlayer) player).setExplored(t);
            updateElement.appendChild(t.toXMLElement(((ServerPlayer) player), updateElement.getOwnerDocument()));
        }

        try {
            ((ServerPlayer) player).getConnection().send(updateElement);
        } catch (IOException e) {
            logger.warning("Could not send message to: " + ((ServerPlayer) player).getName() + " with connection " + ((ServerPlayer) player).getConnection());
        }
    }


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
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }
    }


}

