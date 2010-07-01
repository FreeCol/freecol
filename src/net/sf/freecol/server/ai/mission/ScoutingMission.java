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

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.ScoutIndianSettlementMessage;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;

/**
 * Mission for controlling a scout.
 * 
 * @see net.sf.freecol.common.model.Unit.Role#SCOUT
 */
public class ScoutingMission extends Mission {

    private static final Logger logger = Logger.getLogger(ScoutingMission.class.getName());

    private boolean valid = true;

    private EquipmentType scoutEquipment;

    private Tile transportDestination = null;

    // Debug variable:
    private String debugAction = "";


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public ScoutingMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }

    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public ScoutingMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>ScoutingMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see AIObject#readFromXML
     */
    public ScoutingMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }

    /**
     * Disposes this <code>Mission</code>.
     */
    public void dispose() {
        super.dispose();
    }

    /**
     * Performs this mission.
     * 
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

        if (getUnit().getRole() != Unit.Role.SCOUT) {
            if (getUnit().getColony() != null) {
                AIColony colony = (AIColony) getAIMain().getAIObject(getUnit().getColony());
                for (EquipmentType equipment : FreeCol.getSpecification().getEquipmentTypeList()) {
                    if (equipment.getRole() == Unit.Role.SCOUT && getUnit().canBeEquippedWith(equipment)
                            && colony.canBuildEquipment(equipment)) {
                        Element equipUnitElement = Message.createNewRootElement("equipUnit");
                        equipUnitElement.setAttribute("unit", getUnit().getId());
                        equipUnitElement.setAttribute("type", equipment.getId());
                        equipUnitElement.setAttribute("amount", "1");
                        try {
                            connection.ask(equipUnitElement);
                            scoutEquipment = equipment;
                        } catch (IOException e) {
                            logger.warning("Could not send \"equipUnit\"-message!");
                        }
                        return;
                    }
                }
                valid = false;
                return;
            }

        }

        if (!isTarget(getUnit().getTile(), getUnit(), scoutEquipment)) {
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
                    boolean target = isTarget(t, getUnit(), scoutEquipment);
                    if (target) {
                        best = pathNode;
                        debugAction = "Target: " + t.getPosition();
                    }
                    return target;
                }
            };
            PathNode bestPath = map.search(getUnit(), getUnit().getTile(), destinationDecider, CostDeciders
                    .avoidIllegal(), Integer.MAX_VALUE);

            if (bestPath != null) {
                transportDestination = null;
                Direction direction = moveTowards(connection, bestPath);
                if (direction != null) {
                    final MoveType mt = getUnit().getMoveType(direction);
                    if (mt == MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT) {
                        ScoutIndianSettlementMessage message = new ScoutIndianSettlementMessage(getUnit(), direction);
                        try {
                            connection.ask(message.toXMLElement());
                        } catch (IOException e) {
                            logger.warning("Could not send \"" + ScoutIndianSettlementMessage.getXMLElementTagName()
                                    + "\"-message!");
                            return;
                        }
                        if (getUnit().isDisposed()) {
                            return;
                        }
                    } else if (mt.isProgress()) {
                        AIMessage.askMove(getAIUnit(), direction);
                    }
                }
            } else {
                if (transportDestination != null && !isTarget(transportDestination, getUnit(), scoutEquipment)) {
                    transportDestination = null;
                }
                if (transportDestination == null) {
                    updateTransportDestination();
                }
            }
        }

        exploreLostCityRumour(connection);
        if (getUnit().isDisposed()) {
            return;
        }

        if (isTarget(getUnit().getTile(), getUnit(), scoutEquipment) && getUnit().getColony() != null) {
            if (scoutEquipment != null) {
                Element equipUnitElement = Message.createNewRootElement("equipUnit");
                equipUnitElement.setAttribute("unit", getUnit().getId());
                equipUnitElement.setAttribute("type", scoutEquipment.getId());
                equipUnitElement.setAttribute("amount", "0");
                try {
                    connection.ask(equipUnitElement);
                    scoutEquipment = null;
                } catch (IOException e) {
                    logger.warning("Could not send \"equipUnit (0)\"-message!");
                    return;
                }
                debugAction = "Awaiting 52 horses";
            }
        }
    }

    private void updateTransportDestination() {
        if (getUnit().getTile() == null) {
            transportDestination = (Tile) getUnit().getOwner().getEntryLocation();
        } else if (getUnit().isOnCarrier()) {
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
                    boolean target = isTarget(t, getUnit(), scoutEquipment);
                    if (target) {
                        best = pathNode;
                        debugAction = "Target: " + t.getPosition();
                    }
                    return target;
                }
            };
            Unit carrier = (Unit) getUnit().getLocation();
            PathNode bestPath = getGame().getMap().search(getUnit(), carrier.getTile(), destinationDecider,
                    CostDeciders.avoidIllegal(), Integer.MAX_VALUE, carrier);
            if (bestPath != null) {
                transportDestination = bestPath.getLastNode().getTile();
                debugAction = "Transport to: " + transportDestination.getPosition();
            } else {
                transportDestination = null;
                valid = false;
            }
        } else {
            Iterator<Position> it = getGame().getMap().getFloodFillIterator(getUnit().getTile().getPosition());
            while (it.hasNext()) {
                Tile t = getGame().getMap().getTile(it.next());
                if (isTarget(t, getUnit(), scoutEquipment)) {
                    transportDestination = t;
                    debugAction = "Transport to: " + transportDestination.getPosition();
                    return;
                }
            }
            transportDestination = null;
            valid = false;
        }
    }

    private static boolean isTarget(Tile t, Unit u, EquipmentType scoutEquipment) {
        if (t.hasLostCityRumour()) {
            return true;
        } else if (scoutEquipment != null && t.getColony() != null && t.getColony().getOwner() == u.getOwner()) {
            for (AbstractGoods goods : scoutEquipment.getGoodsRequired()) {
                if (goods.getType().isBreedable() && !t.getColony().canBreed(goods.getType()) &&
                // TODO: remove assumptions about auto-production implementation
                        t.getColony().getProductionNetOf(goods.getType()) > 1) {
                    return true;
                }
            }
            return false;
        } else if (t.getSettlement() != null && t.getSettlement() instanceof IndianSettlement) {
            IndianSettlement settlement = (IndianSettlement) t.getSettlement();
            return !settlement.hasBeenVisited(u.getOwner());
        } else {
            return false;
        }
    }

    /**
     * Returns the destination for this <code>Transportable</code>. This can
     * either be the target {@link Tile} of the transport or the target for the
     * entire <code>Transportable</code>'s mission. The target for the tansport
     * is determined by {@link TransportMission} in the latter case.
     * 
     * @return The destination for this <code>Transportable</code>.
     */
    public Tile getTransportDestination() {
        if (getUnit().isOnCarrier() || getUnit().getTile() == null) {
            if (transportDestination == null || !transportDestination.isLand()) {
                updateTransportDestination();
            }
            return transportDestination;
        } else if (getUnit().getTile() == transportDestination) {
            transportDestination = null;
            return null;
        } else {
            return null;
        }
    }

    /**
     * Returns the priority of getting the unit to the transport destination.
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
     * @return <code>true</code> if this mission is still valid to perform and
     *         <code>false</code> otherwise.
     */
    public boolean isValid() {
        Unit unit = getUnit();
        // unit no longer has horses and not in a colony where it may get some
        // cannot fulfill role of scout anymore
        if (!unit.isMounted() && unit.getTile().getColony() == null) {
            return false;
        }
        return valid && super.isValid();
    }

    /**
     * Checks if this mission is valid to perform.
     * 
     * @param au The unit to be tested.
     * @return <code>true</code> if this mission is still valid to perform and
     *         <code>false</code> otherwise.
     */
    public static boolean isValid(AIUnit au) {
        if (au.getUnit().getTile() == null) {
            return true;
        }
        Iterator<Position> it = au.getGame().getMap().getFloodFillIterator(au.getUnit().getTile().getPosition());
        while (it.hasNext()) {
            Tile t = au.getGame().getMap().getTile(it.next());
            if (isTarget(t, au.getUnit(), null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related information
     * to an XML-stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("unit", getUnit().getId());

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * 
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return The <code>String</code> "scoutingMission".
     */
    public static String getXMLElementTagName() {
        return "scoutingMission";
    }

    /**
     * Gets debugging information about this mission. This string is a short
     * representation of this object's state.
     * 
     * @return The <code>String</code>.
     */
    public String getDebuggingInfo() {
        return debugAction;
    }

}
