
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.server.ai.*;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import org.w3c.dom.*;


/**
* Mission for attacking any unit owned by a player we do not like that is within
* a radius of 1 tile. If no such unit can be found; just wander
* around. 
*/
public class UnitWanderHostileMission extends Mission {
    private static final Logger logger = Logger.getLogger(UnitWanderHostileMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public UnitWanderHostileMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }


    /**
    * Loads a mission from the given element.
    */
    public UnitWanderHostileMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }
    


    /**
    * Performs the mission. This is done by searching for hostile units
    * that are located within one tile and attacking them. If no such units
    * are found, then wander in a random direction.
    *
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Tile thisTile = getUnit().getTile();
        Unit unit = getUnit();
        Map map = getGame().getMap();

        while(unit.getMovesLeft() > 0) {
            // Create an array of the 8 directions in random order:
            int[] directions = map.getRandomDirectionArray();

            // Search for a target:
            boolean hasAttacked = false;
            for (int j = 0; j < 8; j++) {
                if (unit.getMoveType(directions[j]) == Unit.ATTACK && unit.getOwner().getStance(unit.getGame().getMap().getNeighbourOrNull(directions[j], unit.getTile()).getDefendingUnit(unit).getOwner()) == Player.WAR) {
                    Element element = Message.createNewRootElement("attack");
                    element.setAttribute("unit", unit.getID());
                    element.setAttribute("direction", Integer.toString(directions[j]));

                    try {
                        connection.send(element);
                        hasAttacked = true;
                    } catch (IOException e) {
                        logger.warning("Could not send message!");
                    }
                }
            }
            
            if (!hasAttacked) {
                int direction = directions[0];
                int j;
                for (j = 0; j < 8 && (unit.getGame().getMap().getNeighbourOrNull(direction, thisTile) == null || unit.getMoveType(direction) != Unit.MOVE); j++) {
                    direction = directions[j];
                }
                if (j == 8) return; // Not possible to move in any directions.
                thisTile = unit.getGame().getMap().getNeighbourOrNull(direction, thisTile);

                Element element = Message.createNewRootElement("move");
                element.setAttribute("unit", unit.getID());
                element.setAttribute("direction", Integer.toString(direction));

                try {
                    connection.ask(element);
                } catch (IOException e) {
                    logger.warning("Could not send message!");
                }
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
    * @return The <code>String</code> "unitWanderHostileMission".
    */
    public static String getXMLElementTagName() {
        return "unitWanderHostileMission";
    }
}
