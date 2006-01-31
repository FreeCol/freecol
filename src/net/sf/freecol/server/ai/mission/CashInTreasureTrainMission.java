
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.common.model.GoalDecider;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Mission for cashing in a treasure train.
 */
public class CashInTreasureTrainMission extends Mission { 
    // TODO: Use a transport
    // TODO: Avoid enemy units
    // TODO: Require protection
    
    private static final Logger logger = Logger.getLogger(CashInTreasureTrainMission.class.getName());

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
    public CashInTreasureTrainMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }

    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public CashInTreasureTrainMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }
    
    
    /**
     * Disposes this <code>Mission</code>.
     */
    public void dispose() {
        super.dispose();
    }
    
    /**
     * Performs this mission.
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {     
        Map map = getUnit().getGame().getMap();
                
        if (!isValid()) {
            return;
        }
        
        if (!getUnit().canCashInTreasureTrain()) {
            GoalDecider destinationDecider = new GoalDecider() {
                private PathNode best = null;
                
                public PathNode getGoal() {
                    return best;
                }
                
                public boolean hasSubGoals() {
                    return false;
                }
                
                public boolean check(Unit u, PathNode pathNode) {
                    Tile t = pathNode.getTile();
                    if (u.canCashInTreasureTrain(t)) {
                        best = pathNode;
                        return true;
                    }
                    return false;
                }
            };
            PathNode bestPath = map.search(getUnit(), destinationDecider, Integer.MAX_VALUE);        
            
            if (bestPath != null) {
                int direction = moveTowards(connection, bestPath);
                if (direction >= 0) {
                    if (getUnit().getMoveType(direction) == Unit.MOVE
                            || getUnit().getMoveType(direction) == Unit.EXPLORE_LOST_CITY_RUMOUR) {
                        move(connection, direction);                    
                    } 
                }
            }
        }
        
        if (getUnit().canCashInTreasureTrain()) {
            Element cashInTreasureTrainElement = Message.createNewRootElement("cashInTreasureTrain");
            cashInTreasureTrainElement.setAttribute("unit", getUnit().getID());
            try {
                connection.sendAndWait(cashInTreasureTrainElement);
            } catch (IOException e) {
                logger.warning("Could not send message: \"cashInTreasureTrain\".");
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
        return null;
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
        return !getUnit().isDisposed();
    }

    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("unit", getUnit().getID());

        return element;
    }

    /**
     * Updates this object from an XML-representation of
     * a <code>CashInTreasureTrainMission</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
    }

    /**
     * Returns the tag name of the root element representing this object.
     * @return The <code>String</code> "cashInTreasureTrainMission".
     */
    public static String getXMLElementTagName() {
        return "cashInTreasureTrainMission";
    }
}
