

package net.sf.freecol.client.control;

import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;


/**
* A client-side implementation of the <code>ModelController</code> interface.
*/
public class ClientModelController implements ModelController {
    private static final Logger logger = Logger.getLogger(ClientModelController.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private final FreeColClient freeColClient;


    /**
    * Creates a new <code>ClientModelController</code>.
    * @param freeColClient The main controller.
    */
    public ClientModelController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
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
    public int getRandom(String taskID, int n) {
        Client client = freeColClient.getClient();

        Element getRandomElement = Message.createNewRootElement("getRandom");
        getRandomElement.setAttribute("taskID", taskID);
        getRandomElement.setAttribute("n", Integer.toString(n));

        logger.info("TaskID is " + taskID + " Waiting for the server to reply...");
        Element reply = client.ask(getRandomElement);
        logger.info("Reply received from server.");

        if (!reply.getTagName().equals("getRandomConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }
        
        return Integer.parseInt(reply.getAttribute("result"));
    }
    

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
    public Unit createUnit(String taskID, Location location, Player owner, int type) {
        Client client = freeColClient.getClient();

        Element createUnitElement = Message.createNewRootElement("createUnit");
        createUnitElement.setAttribute("taskID", taskID);
        createUnitElement.setAttribute("location", location.getID());
        createUnitElement.setAttribute("owner", owner.getID());
        createUnitElement.setAttribute("type", Integer.toString(type));

        logger.info("Waiting for the server to reply...");
        Element reply = client.ask(createUnitElement);
        logger.info("Reply received from server.");

        if (!reply.getTagName().equals("createUnitConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }

        Unit unit = new Unit(freeColClient.getGame(), (Element) reply.getElementsByTagName(Unit.getXMLElementTagName()).item(0));
        unit.setLocation(unit.getLocation());

        return unit;
    }


    /**
     * Puts the specified <code>Unit</code> in America.
     * 
     * @param unit The <code>Unit</code>.
     * @return The <code>Location</code> where the <code>Unit</code> appears.
     */    
    public Location setToVacantEntryLocation(Unit unit) {
        Game game = freeColClient.getGame();
        Client client = freeColClient.getClient();

        Element createUnitElement = Message.createNewRootElement("getVacantEntryLocation");
        createUnitElement.setAttribute("unit", unit.getID());

        Element reply = client.ask(createUnitElement);

        if (!reply.getTagName().equals("getVacantEntryLocationConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }

        Location entryLocation = (Location) game.getFreeColGameObject(reply.getAttribute("location"));
        unit.setLocation(entryLocation);

        return entryLocation;
    }

    
    /**
     * Updates stances.
     * @param first The first <code>Player</code>.
     * @param second The second <code>Player</code>.
     * @param stance The new stance.
     */
    public void setStance(Player first, Player second, int stance) {
        // Nothing to do.
    }
    
    
    /**
     * Explores the given tiles for the given player.
     * 
     * @param player The <code>Player</code> that should see more tiles.
     * @param tiles The tiles to explore.
     */    
    public void exploreTiles(Player player, ArrayList tiles) {
        // Nothing to do on the client side.
    }


    /**
     * Tells the <code>ModelController</code> that an internal
     * change (that is; not caused by the control) has occured in the model.
     * 
     * @param tile The <code>Tile</code> which will need an update.
     */    
    public void update(Tile tile) {
        // Nothing to do on the client side.
    }
}
