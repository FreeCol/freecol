
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

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
     *      XML-representation of this object.
     */
    public UnitWanderMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>UnitWanderMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see #readFromXML
     */
    public UnitWanderMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
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
    * @return The <code>String</code> "unitWanderMission".
    */
    public static String getXMLElementTagName() {
        return "unitWanderMission";
    }
}
