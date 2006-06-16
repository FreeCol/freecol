
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.GoalDecider;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
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
 * Mission for controlling a scout.
 * 
 * @see Unit#isScout
 */
public class ScoutingMission extends Mission {   
    private static final Logger logger = Logger.getLogger(ScoutingMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    private boolean valid = true;
    private Tile transportDestination = null;
    

    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public ScoutingMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }

    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public ScoutingMission(AIMain aiMain, Element element) {
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
                
        if (getUnit().getTile() == null) {
            return;
        }
        
        if (!isValid()) {
            return;
        }
        
        if (!getUnit().isMounted()) {
            if (getUnit().getTile().getColony() != null
                    && getUnit().getTile().getColony().getGoodsContainer().getGoodsCount(Goods.HORSES) >= 50) {
                if (getUnit().getTile().getColony().getFoodProduction() - getUnit().getTile().getColony().getFoodConsumption() < 2
                        || getUnit().getTile().getColony().getGoodsContainer().getGoodsCount(Goods.HORSES) >= 52) {
                    Element equipUnitElement = Message.createNewRootElement("equipunit");
                    equipUnitElement.setAttribute("unit", getUnit().getID());
                    equipUnitElement.setAttribute("type", Integer.toString(Goods.HORSES));
                    equipUnitElement.setAttribute("amount", Integer.toString(50));
                    try {
                        connection.ask(equipUnitElement);
                    } catch (IOException e) {
                        logger.warning("Could not send \"equipUnit (50)\"-message!");
                    }
                    return;
                } else {
                    return;
                }
            } else {
                valid = false;
                return;
            }
        }
        
        if (!isTarget(getUnit().getTile(), getUnit())) {
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
                    boolean target = isTarget(t, getUnit());
                    if (target) {
                        best = pathNode;
                    }
                    return target;
                }
            };
            PathNode bestPath = map.search(getUnit(), destinationDecider, Integer.MAX_VALUE);        
            
            if (bestPath != null) {
                transportDestination = null;
                int direction = moveTowards(connection, bestPath);
                if (direction >= 0) {
                    if (getUnit().getMoveType(direction) == Unit.MOVE
                            || getUnit().getMoveType(direction) == Unit.EXPLORE_LOST_CITY_RUMOUR) {
                        move(connection, direction);                    
                    } else if (getUnit().getMoveType(direction) == Unit.ENTER_INDIAN_VILLAGE_WITH_SCOUT) {
                        Element scoutMessage = Message.createNewRootElement("scoutIndianSettlement");
                        scoutMessage.setAttribute("unit", getUnit().getID());
                        scoutMessage.setAttribute("direction", Integer.toString(direction));
                        scoutMessage.setAttribute("action", "basic");
                        try {
                            connection.ask(scoutMessage);
                        } catch (IOException e) {
                            logger.warning("Could not send \"scoutIndianSettlement\"-message!");
                            return;
                        }
                        scoutMessage.setAttribute("action", "speak");
                        try {
                            connection.ask(scoutMessage);
                        } catch (IOException e) {
                            logger.warning("Could not send \"scoutIndianSettlement (speak)\"-message!");
                            return;
                        }                    
                        if (getUnit().isDisposed()) {
                            return;
                        }
                    }
                }
            } else {
                if (transportDestination != null && !isTarget(transportDestination, getUnit())) {
                    transportDestination = null;
                }
                if (transportDestination == null) {
                    Iterator it = map.getFloodFillIterator(getUnit().getTile().getPosition());
                    while (it.hasNext()) {
                        Tile t = map.getTile((Map.Position) it.next());
                        if (isTarget(t, getUnit())) {
                            transportDestination = t;
                            return;
                        }
                    }
                    valid = false;
                }
            }
        }
        
        exploreLostCityRumour(connection);        
        if (getUnit().isDisposed()) {
            return;
        }
        
        if (isTarget(getUnit().getTile(), getUnit()) && getUnit().getTile().getColony() != null) {
            Element equipUnitElement = Message.createNewRootElement("equipunit");
            equipUnitElement.setAttribute("unit", getUnit().getID());
            equipUnitElement.setAttribute("type", Integer.toString(Goods.HORSES));
            equipUnitElement.setAttribute("amount", Integer.toString(0));
            try {
                connection.ask(equipUnitElement);
            } catch (IOException e) {
                logger.warning("Could not send \"equipUnit (0)\"-message!");
                return;
            }                    
        }
    }
    
    private static boolean isTarget(Tile t, Unit u) {
        if (t.hasLostCityRumour()) {
            return true;
        } else if (t.getColony() != null 
                && t.getColony().getOwner() == u.getOwner()
                && t.getColony().getGoodsContainer().getGoodsCount(Goods.HORSES) <= 1 
                && t.getColony().getFoodProduction() - t.getColony().getFoodConsumption() >= 2) {
            return true;                    
        } else if (t.getSettlement() != null
                && t.getSettlement() instanceof IndianSettlement
                && !((IndianSettlement) t.getSettlement()).hasBeenVisited()) {
            return true;                    
        } else {
            return false;
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
        if (getUnit().getLocation() instanceof Unit) {
            return transportDestination;
        } else if (getUnit().getTile() == transportDestination) {
            transportDestination = null;
            return null;
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
        return valid;
    }

    /**
     * Checks if this mission is valid to perform.
     *
     * @param au The unit to be tested.
     * @return <code>true</code> if this mission is still valid to perform
     *         and <code>false</code> otherwise.
     */
    public static boolean isValid(AIUnit au) { 
        if (au.getUnit().getTile() == null) {
            return true;
        }
        Iterator it = au.getGame().getMap().getFloodFillIterator(au.getUnit().getTile().getPosition());
        while (it.hasNext()) {
            Tile t = au.getGame().getMap().getTile((Map.Position) it.next());
            if (isTarget(t, au.getUnit())) {
                return true;
            }
        }
        return false;
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
     * a <code>ScoutingMission</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
    }

    /**
     * Returns the tag name of the root element representing this object.
     * @return The <code>String</code> "scoutingMission".
     */
    public static String getXMLElementTagName() {
        return "scoutingMission";
    }
}
