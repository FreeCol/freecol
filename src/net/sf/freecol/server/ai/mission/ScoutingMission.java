/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
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
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
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
                AIColony colony = getAIMain().getAIColony(getUnit().getColony());
                for (EquipmentType equipment : getAIMain().getGame().getSpecification().getEquipmentTypeList()) {
                    if (equipment.getRole() == Unit.Role.SCOUT && getUnit().canBeEquippedWith(equipment)
                        && getUnit().getColony().canProvideEquipment(equipment)) {
                        AIMessage.askEquipUnit(getAIUnit(), equipment, 1);
                        if (getUnit().getEquipmentCount(equipment) > 0) {
                            return;
                        }
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
            PathNode bestPath = getUnit().search(getUnit().getTile(),
                destinationDecider, CostDeciders.avoidIllegal(),
                Integer.MAX_VALUE, null);

            if (bestPath != null) {
                transportDestination = null;
                Direction direction = moveTowards(bestPath);
                if (direction != null) {
                    final MoveType mt = getUnit().getMoveType(direction);
                    if (mt == MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT) {
                        AIMessage.askScoutIndianSettlement(getAIUnit(),
                                                           direction);
                    } else {
                        if (!moveButDontAttack(direction)) return;
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

        if (getUnit().isDisposed()) {
            return;
        }

        if (isTarget(getUnit().getTile(), getUnit(), scoutEquipment) && getUnit().getColony() != null) {
            if (scoutEquipment != null) {
                AIMessage.askEquipUnit(getAIUnit(), scoutEquipment,
                                       -getUnit().getEquipmentCount(scoutEquipment));
                if (getUnit().getEquipmentCount(scoutEquipment) == 0) {
                    scoutEquipment = null;
                }
            }
        }
    }

    private void updateTransportDestination() {
        if (getUnit().getTile() == null) {
            transportDestination = getUnit().getFullEntryLocation();
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
            PathNode bestPath = getUnit().search(carrier.getTile(),
                                                 destinationDecider,
                                                 CostDeciders.avoidIllegal(),
                                                 INFINITY, carrier);
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
                    t.getColony().getNetProductionOf(goods.getType()) > 1) {
                    return true;
                }
            }
            return false;
        } else if (t.getIndianSettlement() != null) {
            return !t.getIndianSettlement().hasSpokenToChief(u.getOwner());
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
     * Unit must be mounted.
     *
     * @return True if this mission is still valid to perform.
     */
    public boolean isValid() {
        return super.isValid() && valid && getUnit().isMounted();
    }

    /**
     * Checks if this mission is valid to perform.
     *
     * @param au The unit to be tested.
     * @return <code>true</code> if this mission is still valid to perform and
     *         <code>false</code> otherwise.
     */
    public static boolean isValid(AIUnit au) {
        if (!au.getUnit().hasAbility("model.ability.scoutIndianSettlement")) {
            return false;
        }
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
     * Gets debugging information about this mission. This string is a short
     * representation of this object's state.
     *
     * @return The <code>String</code>.
     */
    public String getDebuggingInfo() {
        return debugAction;
    }

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXML(out, getXMLElementTagName());
    }


    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "scoutingMission".
     */
    public static String getXMLElementTagName() {
        return "scoutingMission";
    }
}
