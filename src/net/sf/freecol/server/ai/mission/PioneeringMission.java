
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.GoalDecider;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.TileImprovementPlan;

import org.w3c.dom.Element;


/**
 * Mission for controlling a pioneer.
 * 
 * @see Unit#isPioneer
 */
public class PioneeringMission extends Mission {
    /* 
     * TODO-LATER: "updateTileImprovementPlan" should be called
     *             only once (in the beginning of the turn).
     */
    
    private static final Logger logger = Logger.getLogger(PioneeringMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private TileImprovementPlan tileImprovementPlan = null;
    
    /**
     * Temporary variable for skipping the mission.
     */
    private boolean skipMission = false;


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
        if (tileImprovementPlan != null) {
            tileImprovementPlan.setPioneer(null);
            tileImprovementPlan = null;
        }
        super.dispose();
    }

    /**
     * Sets the <code>TileImprovementPlan</code> which should
     * be the next target.
     * 
     * @param tileImprovementPlan The <code>TileImprovementPlan</code>.
     */
    public void setTileImprovementPlan(TileImprovementPlan tileImprovementPlan) {
        this.tileImprovementPlan = tileImprovementPlan;
    }

    private void updateTileImprovementPlan() {
        if (tileImprovementPlan != null) {
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
                
        TileImprovementPlan bestChoice = null;
        int bestValue = 0;
        Iterator<TileImprovementPlan> tiIterator = aiPlayer.getTileImprovementPlanIterator();            
        while (tiIterator.hasNext()) {
            TileImprovementPlan ti = tiIterator.next();
            if (ti.getPioneer() == null) {
                PathNode path = null;
                int value;
                if (startTile != ti.getTarget()) {
                    path = getGame().getMap().findPath(getUnit(), startTile, ti.getTarget(), carrier);
                    if (path != null) {
                        value = ti.getValue() + 10000 - (path.getTotalTurns()*5);
                        
                        /*
                         * Avoid picking a TileImprovementPlan with a path being blocked 
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
            tileImprovementPlan = bestChoice;
            bestChoice.setPioneer(getAIUnit());
        }    
    }
    
    private PathNode findColonyWithTools() {
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
                boolean target = false;
                if (t.getColony() != null 
                        && t.getColony().getOwner() == u.getOwner()
                        && t.getColony().getGoodsContainer().getGoodsCount(Goods.TOOLS) >= 20) {
                    AIColony ac = (AIColony) getAIMain().getAIObject(t.getColony());
                    if (ac.getAvailableTools() >= 20) {
                        target = true;
                    }
                }
                if (target) {
                    best = pathNode;
                }
                return target;
            }
        };
        return getGame().getMap().search(getUnit(), destinationDecider, Integer.MAX_VALUE);     
    }
    
    /**
     * Performs this mission.
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {       
        if (!isValid()) {
            return;
        }
        
        if (getUnit().getTile() != null) {
            if (getUnit().getNumberOfTools() == 0) {
                // Get tools from a Colony.
                if (getUnit().getColony() == null) {
                    PathNode bestPath = findColonyWithTools();        

                    if (bestPath != null) {
                        int direction = moveTowards(connection, bestPath);
                        if (direction >= 0) {
                            final int mt = getUnit().getMoveType(direction);
                            if (mt != Unit.ILLEGAL_MOVE && mt != Unit.ATTACK) {
                                move(connection, direction);                    
                            }
                        }
                    } else {
                        skipMission = true;
                    }
                }
                if (getUnit().getColony() != null) {
                    AIColony ac = (AIColony) getAIMain().getAIObject(getUnit().getColony());
                    final int tools = ac.getAvailableTools();
                    if (tools >= 20) {                    
                        Element equipUnitElement = Message.createNewRootElement("equipunit");
                        equipUnitElement.setAttribute("unit", getUnit().getID());
                        equipUnitElement.setAttribute("type", Integer.toString(Goods.TOOLS.getIndex()));
                        equipUnitElement.setAttribute("amount", Integer.toString(Math.min(tools - tools % 20, 100)));
                        try {
                            connection.sendAndWait(equipUnitElement);
                        } catch (Exception e) {
                            logger.warning("Could not send equip message.");
                        }
                    } else {
                        skipMission = true;
                    }
                }
                return;
            }
        }
        
        if (tileImprovementPlan == null) {
            updateTileImprovementPlan();
        }
        
        if (tileImprovementPlan != null) {
            if (getUnit().getTile() != null) {
                if (getUnit().getTile() != tileImprovementPlan.getTarget()) {
                    PathNode pathToTarget = getUnit().findPath(tileImprovementPlan.getTarget());
                    if (pathToTarget != null) {
                        int direction = moveTowards(connection, pathToTarget);
                        if (direction >= 0 && (getUnit().getMoveType(direction) == Unit.MOVE
                                || getUnit().getMoveType(direction) == Unit.EXPLORE_LOST_CITY_RUMOUR)) {
                            move(connection, direction);
                        }
                    }
                }
                if (getUnit().getTile() == tileImprovementPlan.getTarget()
                        && getUnit().getState() != Unit.IMPROVING
                        && getUnit().checkSetState(Unit.IMPROVING)) {
                    Element changeStateElement = Message.createNewRootElement("changeState");
                    changeStateElement.setAttribute("unit", getUnit().getID());
                    changeStateElement.setAttribute("state", Integer.toString(Unit.IMPROVING));
                    try {
                        connection.sendAndWait(changeStateElement);
                    } catch (IOException e) {
                        logger.warning("Could not send message!");
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
        updateTileImprovementPlan();
        if (tileImprovementPlan == null) {
            return null;
        }
        if (getUnit().getLocation() instanceof Unit) {
            return tileImprovementPlan.getTarget();
        } else if (getUnit().getTile() == tileImprovementPlan.getTarget()) {
            return null;
        } else if (getUnit().getTile() == null || getUnit().findPath(tileImprovementPlan.getTarget()) == null) {
            return tileImprovementPlan.getTarget();
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
        updateTileImprovementPlan();
        return !skipMission && tileImprovementPlan != null &&
                (getUnit().isPioneer() || getUnit().hasAbility("model.ability.expertPioneer"));
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
        Iterator<TileImprovementPlan> tiIterator = aiPlayer.getTileImprovementPlanIterator();            
        while (tiIterator.hasNext()) {
            TileImprovementPlan ti = tiIterator.next();
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
        if (tileImprovementPlan != null) {
            out.writeAttribute("tileImprovementPlan", tileImprovementPlan.getID());
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
        
        final String tileImprovementPlanStr = in.getAttributeValue(null, "tileImprovementPlan");
        if (tileImprovementPlanStr != null) {
            tileImprovementPlan = (TileImprovementPlan) getAIMain().getAIObject(tileImprovementPlanStr);
            if (tileImprovementPlan == null) {
                tileImprovementPlan = new TileImprovementPlan(getAIMain(), tileImprovementPlanStr);
            }
        } else {
            tileImprovementPlan = null;
        }
        
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * @return The <code>String</code> "wishRealizationMission".
     */
    public static String getXMLElementTagName() {
        return "tileImprovementPlanMission";
    }
    
    /**
     * Gets debugging information about this mission.
     * This string is a short representation of this
     * object's state.
     * 
     * @return The <code>String</code>: 
     *      <ul>
     *          <li>"(x, y) P" (for plowing)</li>
     *          <li>"(x, y) R" (for building road)</li>
     *          <li>"(x, y) Getting tools: (x, y)"</li>
     *      </ul>
     */
    public String getDebuggingInfo() {
        if (tileImprovementPlan != null) {
            final String action = tileImprovementPlan.getType().getName();
            return tileImprovementPlan.getTarget().getPosition().toString() + " " + action;
        } else {
            PathNode bestPath = findColonyWithTools();
            if (bestPath != null) {
                return "Getting tools: " + bestPath.getLastNode().getTile().getPosition().toString();
            } else {
                return "No target";
            }
        }
    }
}
