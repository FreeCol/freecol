

package net.sf.freecol.server.control;

import net.sf.freecol.common.model.*;
import net.sf.freecol.server.model.*;
import net.sf.freecol.server.FreeColServer;
import org.w3c.dom.*;
import java.util.HashMap;
import java.util.logging.Logger;


/**
* A server-side implementation of the <code>ModelController</code> interface.
*/
public class ServerModelController implements ModelController {
    private static final Logger logger = Logger.getLogger(ServerModelController.class.getName());

    private final FreeColServer freeColServer;

    private HashMap taskRegister = new HashMap();


    /**
    * Creates a new <code>ServerModelController</code>.
    * @param freeColServer The main controller.
    */
    public ServerModelController(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }

    
    public Unit createUnit(String taskID, Location location, Player owner, int type) {
        String extendedTaskID = taskID + Integer.toString(freeColServer.getGame().getTurn().getNumber());
        ServerUnit unit;

        if (taskRegister.containsKey(extendedTaskID)) {
            unit = (ServerUnit) taskRegister.get(extendedTaskID);

            if (unit.getLocation().getTile() != location.getTile() || unit.getOwner() != owner || unit.getType() != type) {
                logger.warning("Unsynchronization between the client and the server. Maybe a cheating attempt! Differences: " +
                        ((unit.getLocation().getTile() != location.getTile()) ? "location: " + unit.getLocation().getTile() + "!=" + location.getTile(): "") +
                        ((unit.getOwner() != owner) ? "owner: " + unit.getOwner() + "!=" + owner : "") +
                        ((unit.getType() != type) ? "type: " + unit.getType() + "!=" + type : ""));

                unit.dispose();
                return null;
            }

            taskRegister.remove(extendedTaskID);
        } else {
            unit = new ServerUnit(freeColServer.getGame(), location, owner, type, Unit.ACTIVE);
            taskRegister.put(extendedTaskID, unit);
        }

        return unit;
    }
}
