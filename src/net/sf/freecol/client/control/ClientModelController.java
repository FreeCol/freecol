

package net.sf.freecol.client.control;

import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.model.*;
import net.sf.freecol.client.FreeColClient;
import java.util.logging.Logger;
import java.util.ArrayList;
import org.w3c.dom.*;


/**
* A client-side implementation of the <code>ModelController</code> interface.
*/
public class ClientModelController implements ModelController {
    private static final Logger logger = Logger.getLogger(ClientModelController.class.getName());

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
    */
    public int getRandom(String taskID, int n) {
        Client client = freeColClient.getClient();

        Element getRandomElement = Message.createNewRootElement("getRandom");
        getRandomElement.setAttribute("taskID", taskID);
        getRandomElement.setAttribute("n", Integer.toString(n));

        logger.info("Waiting for the server to reply...");
        Element reply = client.ask(getRandomElement);
        logger.info("Reply received from server.");

        if (!reply.getTagName().equals("getRandomConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }

        return Integer.parseInt(getRandomElement.getAttribute("number"));
    }
    

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

    
    public void exploreTiles(Player player, ArrayList tiles) {
        // Nothing to do on the client side.
    }


    public void update(Tile tile) {
        // Nothing to do on the client side.
    }
}
