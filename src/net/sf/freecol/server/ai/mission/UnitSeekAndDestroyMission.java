/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.server.ai.mission;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
* Mission for attacking a specific target, be it a Unit or a Settlement.
*/
public class UnitSeekAndDestroyMission extends Mission {
    private static final Logger logger = Logger.getLogger(UnitSeekAndDestroyMission.class.getName());
    
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
    @Override
	public void doMission(Connection connection) {
        Unit unit = getUnit();

        if (!isValid()) {
            return;
        }
        
        PathNode pathToTarget = null;
        if (unit.isOnCarrier()) {
            if (unit.getTile() != null) {
                pathToTarget = getDisembarkPath(unit, unit.getTile(), target.getTile(), (Unit) unit.getLocation());
                if (pathToTarget.getTransportDropNode() != pathToTarget) {
                    pathToTarget = null;
                }
            }
        } else {
            if (unit.getTile() != null) {
                pathToTarget = getUnit().findPath(target.getTile());
            }
        }
        
        if (pathToTarget != null) {
            Direction direction = moveTowards(connection, pathToTarget);
            if (direction != null 
                && unit.getMoveType(direction) == MoveType.ATTACK) {
                Tile newTile = unit.getTile().getNeighbourOrNull(direction);
                Unit defender = newTile.getDefendingUnit(unit);
                if (defender == null) {
                    logger.warning("MoveType is ATTACK, but no defender is present!");
                } else {
                    Player enemy = defender.getOwner();
                    if (unit.getOwner().atWarWith(enemy)
                        || ((Ownable) target).getOwner() == enemy) {
                        attack(connection, unit, direction);
                    }
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
                    for (Direction direction : Direction.values()) {
                        Tile attackTile = pathNode.getTile().getNeighbourOrNull(direction);
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
                CostDeciders.avoidIllegal(), Integer.MAX_VALUE, carrier);
    }
    
    /**
    * Check to see if this is a valid hostility with a valid target.
    * @return <code>true</code> if this mission is valid.
    */
    @Override
	public boolean isValid() {
        Player owner = getUnit().getOwner();
        Player targetPlayer;
        if (target == null) {
            return false;
        }     
        if (((FreeColGameObject) target).isDisposed()) {
            return false;
        }
        if (target.getTile() == null) {
            return false;
        }
        if (!getUnit().isOffensiveUnit()) {
            return false;
        }
        
        // do not pursue units in colonies
        if (target instanceof Unit && 
            target.getTile().getSettlement() != null){
        		return false;
        }

        targetPlayer = ((Ownable) target).getOwner();
        Stance stance = owner.getStance(targetPlayer);

        return targetPlayer != owner &&
            (stance == Stance.WAR 
             || owner.isIndian() 
             && owner.getTension(targetPlayer).getLevel().compareTo(Tension.Level.CONTENT) >= 0);
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
    @Override
	public Tile getTransportDestination() {
        if (target == null) {
            return null;
        }
        
        Tile dropTarget = target.getTile();
        if (getUnit().getTile() == null) {
            return dropTarget;
        } else if (getUnit().isOnCarrier()) {
            PathNode p = getDisembarkPath(getUnit(), 
                    getUnit().getTile(), 
                    target.getTile(), 
                    (Unit) getUnit().getLocation());
            if (p != null) {
                dropTarget = p.getTransportDropNode().getTile();
            }
        }
        
        if (getUnit().isOnCarrier()) {
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
    @Override
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
    @Override
	protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        
        out.writeAttribute("unit", getUnit().getId());
        if (getTarget() != null) {
            out.writeAttribute("target", getTarget().getId());
        }

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    @Override
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
    
    /**
     * Gets debugging information about this mission.
     * This string is a short representation of this
     * object's state.
     * 
     * @return The <code>String</code>.
     */
    @Override
	public String getDebuggingInfo() {
        if (target == null) {
            return "No target";
        } else {
            final String name;
            if (target instanceof Unit) {
                name = ((Unit) target).toString();
            } else if (target instanceof Colony) {
                name = ((Colony) target).getName();
            } else {
                name = "";
            }
            return target.getTile().getPosition() + " " + name;
        }
    }
}
