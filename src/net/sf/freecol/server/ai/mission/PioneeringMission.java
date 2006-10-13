
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.TileImprovement;

import org.w3c.dom.Element;


/**
 * Mission for controlling a pioneer.
 * 
 * @see Unit#isPioneer
 */
public class PioneeringMission extends Mission {
    /* 
     * TODO-LATER: "updateTileImprovement" should be called
     *             only once (in the beginning of the turn).
     */
    
    private static final Logger logger = Logger.getLogger(PioneeringMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private TileImprovement tileImprovement = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public PioneeringMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }


    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public PioneeringMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>PioneeringMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */
    public PioneeringMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }
    
    
    /**
     * Disposes this <code>Mission</code>.
     */
    public void dispose() {
        if (tileImprovement != null) {
            tileImprovement.setPioneer(null);
            tileImprovement = null;
        }
        super.dispose();
    }

    /**
     * Sets the <code>TileImprovement</code> which should
     * be the next target.
     * 
     * @param tileImprovement The <code>TileImprovement</code>.
     */
    public void setTileImprovement(TileImprovement tileImprovement) {
        this.tileImprovement = tileImprovement;
    }

    private void updateTileImprovement() {
        if (tileImprovement != null) {
            return;
        }
        final AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getID());
        final Unit carrier = (getUnit().getLocation() instanceof Unit) ? (Unit) getUnit().getLocation() : null;
        
        final Tile startTile;
        if (getUnit().getTile() == null) {
            if (getUnit().getLocation() instanceof Unit) {
                startTile = (Tile) ((Unit) getUnit().getLocation()).getEntryLocation();
            } else {
                startTile = (Tile) getUnit().getOwner().getEntryLocation();
            }
        } else {
            startTile = getUnit().getTile();
        }
                
        TileImprovement bestChoice = null;
        int bestValue = 0;
        Iterator tiIterator = aiPlayer.getTileImprovementIterator();            
        while (tiIterator.hasNext()) {
            TileImprovement ti = (TileImprovement) tiIterator.next();
            if (ti.getPioneer() == null) {
                PathNode path = null;
                int value;
                if (startTile != ti.getTarget()) {
                    path = getGame().getMap().findPath(getUnit(), startTile, ti.getTarget(), carrier);
                    if (path != null) {
                        value = ti.getValue() + 10000 - (path.getTotalTurns()*5);
                        
                        /*
                         * Avoid picking a TileImprovement with a path being blocked 
                         * by an enemy unit (apply a penalty to the value):
                         */
                        PathNode pn = path;
                        while (pn != null) {
                            if (pn.getTile().getFirstUnit() != null
                                    && pn.getTile().getFirstUnit().getOwner() != getUnit().getOwner()) {
                                value -= 1000;
                            }
                            pn = pn.next;
                        }
                    } else {
                        value = ti.getValue();
                    }
                } else {
                    value = ti.getValue() + 10000;
                }                
                if (value > bestValue) {
                    bestChoice = ti;
                    bestValue = value;
                }
            }
        }
        
        if (bestChoice != null) {
            tileImprovement = bestChoice;
            bestChoice.setPioneer(getAIUnit());
        }    
    }
    
    /**
     * Performs this mission.
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {       
        if (!isValid()) {
            return;
        }
               
        if (!getUnit().isPioneer()) {
            // TODO: Get tools from a Colony
            return;
        }
        
        if (tileImprovement == null) {
            updateTileImprovement();
        }

        if (getUnit().getState() != Unit.ACTIVE) {
            return;
        }
        
        if (tileImprovement != null) {
            if (getUnit().getTile() != null) {
                if (getUnit().getTile() == tileImprovement.getTarget()) {
                    Element changeStateElement = Message.createNewRootElement("changeState");
                    changeStateElement.setAttribute("unit", getUnit().getID());
                    changeStateElement.setAttribute("state", Integer.toString(tileImprovement.getType()));
                    try {
                        connection.sendAndWait(changeStateElement);
                    } catch (IOException e) {
                        logger.warning("Could not send message!");
                    }
                } else {
                    PathNode pathToTarget = getUnit().findPath(tileImprovement.getTarget());
                    if (pathToTarget != null) {
                        int direction = moveTowards(connection, pathToTarget);
                        if (direction >= 0 && getUnit().getMoveType(direction) == Unit.MOVE) {
                            move(connection, direction);
                        }
                    }
                }
            }
        }        
    }


    /**
     * Returns the destination for this <code>Transportable</code>.
     * This can either be the target {@link Tile} of the transport
     * or the target for the entire <code>Transportable</code>'s
     * mission. The target for the tansport is determined by
     * {@link TransportMission} in the latter case.
     *
     * @return The destination for this <code>Transportable</code>.
     */    
    public Tile getTransportDestination() {
        updateTileImprovement();
        if (tileImprovement == null) {
            return null;
        }
        if (getUnit().getLocation() instanceof Unit) {
            return tileImprovement.getTarget();
        } else if (getUnit().getTile() == tileImprovement.getTarget()) {
            return null;
        } else if (getUnit().getTile() == null || getUnit().findPath(tileImprovement.getTarget()) == null) {
            return tileImprovement.getTarget();
        } else {
            return null;
        }
    }

    /**
     * Returns the priority of getting the unit to the
     * transport destination.
     *
     * @return The priority.
     */
    public int getTransportPriority() {
        if (getTransportDestination() != null) {
            return NORMAL_TRANSPORT_PRIORITY;
        } else {
            return 0;
        }
    }

    /**
     * Checks if this mission is still valid to perform.
     *
     * @return <code>true</code> if this mission is still valid to perform
     *         and <code>false</code> otherwise.
     */
    public boolean isValid() {  
        updateTileImprovement();        
        //return tileImprovement != null;
        // TODO: Remove the second test after code for getting tools has been added:
        return (tileImprovement != null) && getUnit().isPioneer();
    }

    /**
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The unit.
     * @return <code>true</code> if this mission is still valid to perform
     *         and <code>false</code> otherwise.
     */    
    public static boolean isValid(AIUnit aiUnit) {
        AIPlayer aiPlayer = (AIPlayer) aiUnit.getAIMain().getAIObject(aiUnit.getUnit().getOwner().getID());
        Iterator tiIterator = aiPlayer.getTileImprovementIterator();            
        while (tiIterator.hasNext()) {
            TileImprovement ti = (TileImprovement) tiIterator.next();
            if (ti.getPioneer() == null) {
                return true;
            }
        }
        return false;
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
        if (tileImprovement != null) {
            out.writeAttribute("tileImprovement", tileImprovement.getID());
        }

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));
        
        final String tileImprovementStr = in.getAttributeValue(null, "tileImprovement");
        if (tileImprovementStr != null) {
            tileImprovement = (TileImprovement) getAIMain().getAIObject(tileImprovementStr);
            if (tileImprovement == null) {
                tileImprovement = new TileImprovement(getAIMain(), tileImprovementStr);
            }
        } else {
            tileImprovement = null;
        }
        
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * @return The <code>String</code> "wishRealizationMission".
     */
    public static String getXMLElementTagName() {
        return "tileImprovementMission";
    }
}
