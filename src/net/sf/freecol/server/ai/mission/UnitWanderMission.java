
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.server.ai.*;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import org.w3c.dom.*;


/**
* Mission for wandering in random directions.
*/
public class UnitWanderMission extends Mission {
    private static final Logger logger = Logger.getLogger(UnitWanderMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public UnitWanderMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }

    
    /**
    * Loads a mission from the given element.
    */
    public UnitWanderMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    
    /**
    * Performs the mission. This is done by moving in a random direction
    * until the move points are zero or the unit gets stuck.
    *
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Tile thisTile = getUnit().getTile();
        Unit unit = getUnit();
        
        while(unit.getMovesLeft() > 0) {
            int direction = (int) (Math.random() * 8);
            int j;
            for (j = 8; j > 0 && ((unit.getGame().getMap().getNeighbourOrNull(direction, thisTile) == null) || (unit.getMoveType(direction) != Unit.MOVE)); j--) {
                direction = (int) (Math.random() * 8);
            }
            if (j == 0) break;
            thisTile = unit.getGame().getMap().getNeighbourOrNull(direction, thisTile);

            Element moveElement = Message.createNewRootElement("move");
            moveElement.setAttribute("unit", unit.getID());
            moveElement.setAttribute("direction", Integer.toString(direction));
            
            try {
                connection.sendAndWait(moveElement);
            } catch (IOException e) {
                logger.warning("Could not send \"move\"-message!");
            }
        }
    }
    

    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("unit", getUnit().getID());

        return element;
    }


    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "unitWanderMission".
    */
    public static String getXMLElementTagName() {
        return "unitWanderMission";
    }
}
