
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


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
    * 
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public UnitWanderMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }

    
    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     * 		XML-representation of this object.
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
    

    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     * 		the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("unit", getUnit().getID());

        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>UnitWanderMission</code>.
     * 
     * @param element The XML-representation.
     */    
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
