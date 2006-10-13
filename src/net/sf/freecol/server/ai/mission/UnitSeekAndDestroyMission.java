
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.GoalDecider;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
* Mission for attacking a specific target, be it a Unit or a Settlement.
*/
public class UnitSeekAndDestroyMission extends Mission {
    private static final Logger logger = Logger.getLogger(UnitSeekAndDestroyMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * The object we are trying to destroy. This can be a
     * either <code>Settlement</code> or a <code>Unit</code>.
     */
    private Location target;

    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * 
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    * @param target The object we are trying to destroy. This can be either a
    *        <code>Settlement</code> or a <code>Unit</code>.
    */
    public UnitSeekAndDestroyMission(AIMain aiMain, AIUnit aiUnit, Location target) {
        super(aiMain, aiUnit);
        this.target = target; 
        
        if (!(target instanceof Ownable)) {
            logger.warning("!(target instanceof Ownable)");
            throw new IllegalArgumentException("!(target instanceof Ownable)");
        }        
        if (!(target instanceof Unit || target instanceof Settlement)) {
            logger.warning("!(target instanceof Unit || target instanceof Settlement)");
            throw new IllegalArgumentException("!(target instanceof Unit || target instanceof Settlement)");
            
        }
    }


    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public UnitSeekAndDestroyMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>UnitSeekAndDestroyMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */
    public UnitSeekAndDestroyMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
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
        Unit unit = getUnit();

        if (!isValid()) {
            return;
        }
        
        PathNode pathToTarget = null;
        if (unit.getLocation() instanceof Unit) {
            pathToTarget = getDisembarkPath(unit, unit.getTile(), target.getTile(), (Unit) unit.getLocation());            
        } else {
            pathToTarget = getUnit().findPath(target.getTile());
        }
        
        if (pathToTarget != null) {
            int direction = moveTowards(connection, pathToTarget);
            while (direction >= 0) {
                Tile newTile = getGame().getMap().getNeighbourOrNull(direction, unit.getTile());
                if (unit.getMoveType(direction) == Unit.ATTACK
                        && (unit.getOwner().getStance(newTile.getDefendingUnit(unit).getOwner()) == Player.WAR
                                || ((Ownable) target).getOwner() == newTile.getDefendingUnit(unit).getOwner())) {
                    Element element = Message.createNewRootElement("attack");
                    element.setAttribute("unit", unit.getID());
                    element.setAttribute("direction", Integer.toString(direction));

                    try {
                        connection.ask(element);
                    } catch (IOException e) {
                        logger.warning("Could not send message!");
                    }
                    break;
                } else if (unit.getMoveType(direction) == Unit.MOVE
                        || unit.getMoveType(direction) == Unit.EXPLORE_LOST_CITY_RUMOUR
                        || unit.getMoveType(direction) == Unit.DISEMBARK) {
                    Element element = Message.createNewRootElement("move");
                    element.setAttribute("unit", unit.getID());
                    element.setAttribute("direction", Integer.toString(direction));

                    try {
                        connection.sendAndWait(element);
                    } catch (IOException e) {
                        logger.warning("Could not send message!");
                    }

                    // Hmmm... if we are an AI player, our Unit will be updated 
                    // immediately. Therefore we don't need to check a server
                    // response.
                    pathToTarget = getUnit().findPath(target.getTile());
                    if (pathToTarget != null) {
                        direction = moveTowards(connection, pathToTarget);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
    }

    
    private PathNode getDisembarkPath(Unit unit, Tile start, final Tile end, Unit carrier) {
        GoalDecider gd = new GoalDecider() {
            private PathNode goal = null;
            
            public PathNode getGoal() {
                return goal;
            }
            
            public boolean hasSubGoals() {
                return false;
            }
            
            public boolean check(Unit u, PathNode pathNode) {
                goal = pathNode;
                if (pathNode.getTile().getSettlement() == null) {
                    for (int direction=0; direction < Map.NUMBER_OF_DIRECTIONS; direction++) {
                        Tile attackTile = u.getGame().getMap().getNeighbourOrNull(direction, pathNode.getTile());
                        if (end == attackTile 
                                && attackTile.getSettlement() != null 
                                && pathNode.getTile().isLand()) {
                            int cost = pathNode.getCost();
                            int movesLeft = pathNode.getMovesLeft();
                            int turns = pathNode.getTurns();
                            goal = new PathNode(attackTile, cost, cost, direction, movesLeft, turns);
                            goal.previous = pathNode;
                            return true;
                        }                        
                    }           
                }
                return pathNode.getTile() == end;
            }
        };
        return getGame().getMap().search(unit, start, gd, 
                getGame().getMap().getDefaultCostDecider(), Integer.MAX_VALUE, carrier);    
    }
    
    /**
    * Check to see if this is a valid hostility with a valid target.
    * @return <code>true</code> if this mission is valid.
    */
    public boolean isValid() {
        Player owner = getUnit().getOwner();
        Player targetPlayer;

        if (((FreeColGameObject) target).isDisposed()) {
            return false;
        }
        if (target == null) {
            return false;
        }     
        if (target.getTile() == null) {
            return false;
        }
        if (!getUnit().isOffensiveUnit()) {
            return false;
        }

        targetPlayer = ((Ownable) target).getOwner();
        int stance = owner.getStance(targetPlayer);

        return targetPlayer != owner && (stance == Player.WAR 
                || owner.isIndian() && owner.getTension(targetPlayer).getLevel() >= Tension.CONTENT);
    }

    
    /**
     * Returns the destination for this <code>Transportable</code>.
     * This can either be the target {@link Tile} of the transport
     * or the target for the entire <code>Transportable</code>'s
     * mission. The target for the transport is determined by
     * {@link TransportMission} in the latter case.
     *
     * @return The destination for this <code>Transportable</code>.
     */    
    public Tile getTransportDestination() {
        if (target == null) {
            return null;
        }
        
        Tile dropTarget = target.getTile();
        if (getUnit().getTile() == null) {
            return dropTarget;
        } else if (getUnit().getLocation() instanceof Unit) {
            PathNode p = getDisembarkPath(getUnit(), 
                    getUnit().getTile(), 
                    target.getTile(), 
                    (Unit) getUnit().getLocation());
            if (p != null) {
                dropTarget = p.getTransportDropNode().getTile();
            }
        }
        
        if (getUnit().getLocation() instanceof Unit) {
            return dropTarget;
        } else if (getUnit().getLocation().getTile() == target) {
            return null;
        } else if (getUnit().findPath(target.getTile()) == null) {
            return dropTarget;
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
     * Returns the object we are trying to destroy. 
     * 
     * @return The object which should be destroyed. 
     *      This can be either a <code>Settlement</code> 
     *      or a <code>Unit</code>.
     */    
    public Location getTarget() {
        return target;
    }    

    
    /**
     * Sets the object we are trying to destroy. 
     * 
     * @param target The object which should be destroyed. 
     *      This can be either a <code>Settlement</code> 
     *      or a <code>Unit</code>.
     */
    public void setTarget(Location target) {
        this.target = target;
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
        out.writeAttribute("target", getTarget().getID());

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));        
        setTarget((Location) getGame().getFreeColGameObject(in.getAttributeValue(null, "target")));
        in.nextTag();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "unitSeekAndDestroyMission".
    */
    public static String getXMLElementTagName() {
        return "unitSeekAndDestroyMission";
    }
}
