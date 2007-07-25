
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
* Mission for attacking any unit owned by a player we do not like that is within
* a radius of 1 tile. If no such unit can be found; just wander
* around. 
*/
public class UnitWanderHostileMission extends Mission {
    private static final Logger logger = Logger.getLogger(UnitWanderHostileMission.class.getName());

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
    public UnitWanderHostileMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }


    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public UnitWanderHostileMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>UnitWanderHostileMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */
    public UnitWanderHostileMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
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

        if (!(unit.getLocation() instanceof Tile)) {
            return;
        }
        
        PathNode pathToTarget = null;
        if (unit.isOffensiveUnit()) {
            pathToTarget = findTarget(5);
        }
        
        if (pathToTarget != null) {
            int direction = moveTowards(connection, pathToTarget);
            if (direction >= 0 && unit.getMoveType(direction) == Unit.ATTACK) {
                Element element = Message.createNewRootElement("attack");
                element.setAttribute("unit", unit.getID());
                element.setAttribute("direction", Integer.toString(direction));

                try {
                    connection.ask(element);
                } catch (IOException e) {
                    logger.warning("Could not send message!");
                }

            }
        } else {
            // Just make a random move if no target can be found.
            int[] directions = map.getRandomDirectionArray();

            int direction = directions[0];
            int j;
            for (j = 0; j < 8 && (map.getNeighbourOrNull(direction, thisTile) == null 
                    || (unit.getMoveType(direction) != Unit.MOVE
                       && unit.getMoveType(direction) != Unit.DISEMBARK));
                    j++) {
                direction = directions[j];
            }
            if (j == 8) {
                return; // Not possible to move in any directions.
            }
            thisTile = map.getNeighbourOrNull(direction, thisTile);

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

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related 
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        
        out.writeAttribute("unit", getUnit().getID());

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));
        in.nextTag();
    }

    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "unitWanderHostileMission".
    */
    public static String getXMLElementTagName() {
        return "unitWanderHostileMission";
    }
}
