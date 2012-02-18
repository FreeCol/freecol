/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.mission.IdleAtColonyMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;

import org.w3c.dom.Element;


/**
 * Mission for defending a <code>Settlement</code>.
 *
 * TODO: This Mission should later use sub-missions for
 *       eliminating threats etc.
 */
public class DefendSettlementMission extends Mission {

    private static final Logger logger = Logger.getLogger(DefendSettlementMission.class.getName());

    /** The <code>Settlement</code> to be protected. */
    private Settlement settlement;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     * @param settlement The <code>Settlement</code> to defend.
     */
    public DefendSettlementMission(AIMain aiMain, AIUnit aiUnit,
                                   Settlement settlement) {
        super(aiMain, aiUnit);
        this.settlement = settlement;
    }

    /**
     * Creates a new <code>DefendSettlementMission</code>.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public DefendSettlementMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>DefendSettlementMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public DefendSettlementMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }
    

    /**
     * Gets the target settlement.
     *
     * @return The <code>Settlement</code> to be defended by
     *         this <code>Mission</code>.
     */
    public Settlement getSettlement() {
        return settlement;
    }


    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        return (settlement != null
            && shouldTakeTransportToTile(settlement.getTile()))
            ? settlement.getTile()
            : null;
    }

    /**
     * Returns the priority of getting the unit to the
     * transport destination.
     *
     * @return The priority.
     */
    public int getTransportPriority() {
        return (getTransportDestination() == null) ? 0
            : NORMAL_TRANSPORT_PRIORITY + 5;
    }

    // Mission interface

    /**
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @return True if this mission is still valid to perform.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return Mission.isValid(aiUnit)
            && aiUnit.getUnit().isDefensiveUnit()
            && aiUnit.getUnit().getOwner().getNumberOfSettlements() > 0;
    }

    /**
     * Checks if this mission is still valid to perform.
     *
     * @return True if this mission is still valid to perform.
     */
    public boolean isValid() {
        return super.isValid()
            && getUnit().isDefensiveUnit()
            && settlement != null
            && !settlement.isDisposed()
            && settlement.getOwner() == getUnit().getOwner();
    }

    /**
     * Performs this mission.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed()) return;

        // Check target still makes sense.
        if (!isValid()) {
            logger.finest("AI defender has invalid settlement " + settlement
                + ": " + unit);
            return;
        }

        // Go home!
        if (travelToTarget("AI defender", settlement.getTile())
            != Unit.MoveType.MOVE) return;

        // Check if the mission should change?
        // Change to supporting the settlement if the size is marginal.
        final AIMain aiMain = getAIMain();
        final AIUnit aiUnit = getAIUnit();
        Mission m = null;
        if (settlement instanceof Colony) {
            Colony colony = (Colony)settlement;
            if (unit.getLocation() instanceof WorkLocation
                || (unit.isPerson() && settlement.getUnitCount() <= 1)) {
                m = new WorkInsideColonyMission(aiMain, aiUnit,
                    aiMain.getAIColony(colony));
            }
        } else if (settlement instanceof IndianSettlement) {
            if (unit.isPerson() && settlement.getUnitCount() <= 1) {
                m = new IdleAtColonyMission(aiMain, aiUnit);
            }
        }                
        if (m != null) {
            aiUnit.setMission(m);
            m.doMission(aiUnit.getConnection());
            return;
        }

        // Anything more to do?
        if (unit.getState() == UnitState.FORTIFIED
            || unit.getState() == UnitState.FORTIFYING) {
            return; // No log, these happen indefinitely.
        }

        // Check if the settlement is badly defended.  If so, try to fortify.
        int defenderCount = 0, fortifiedCount = 0;
        List<Unit> units = settlement.getUnitList();
        units.addAll(settlement.getTile().getUnitList());
        for (Unit u : units) {
            if (unit.isDefensiveUnit()) {
                defenderCount++;
                if (unit.getState() == UnitState.FORTIFIED) fortifiedCount++;
            }
        }
        if (defenderCount <= 2 || fortifiedCount <= 1) {
            String logMe;
            if (!unit.checkSetState(UnitState.FORTIFYING)) {
                logMe = "waiting to fortify at ";
            } else if (AIMessage.askChangeState(getAIUnit(),
                                                UnitState.FORTIFYING)
                && unit.getState() == UnitState.FORTIFYING) {
                logMe = "fortifying at ";
            } else {
                logMe = "fortify failed at ";
            }
            logger.finest("AI defender " + logMe + settlement.getName()
                + ": " + unit);
            return;
        }

        // The settlement is well enough defended.  See if the unit
        // should attack a nearby hostile unit.  Remember to prevent a
        // sole unit attacking because if it loses, the settlement
        // will collapse (and the combat model does not understand that).
        if (!unit.isOffensiveUnit()) return;
        CombatModel combatModel = unit.getGame().getCombatModel();
        Unit bestTarget = null;
        float bestDifference = Float.MIN_VALUE;
        Direction bestDirection = null;
        for (Direction direction : Direction.getRandomDirections("defendSettlements", getAIRandom())) {
            Tile t = unit.getTile().getNeighbourOrNull(direction);
            if (t == null) continue;
            Unit defender = t.getFirstUnit();
            if (defender != null
                && defender.getOwner().atWarWith(unit.getOwner())
                && unit.getMoveType(direction).isAttack()) {
                Unit enemyUnit = t.getDefendingUnit(unit);
                float enemyAttack = combatModel.getOffencePower(enemyUnit, unit);
                float weAttack = combatModel.getOffencePower(unit, enemyUnit);
                float enemyDefend = combatModel.getDefencePower(unit, enemyUnit);
                float weDefend = combatModel.getDefencePower(enemyUnit, unit);
                
                float difference = weAttack / (weAttack + enemyDefend)
                    - enemyAttack / (enemyAttack + weDefend);
                if (difference > bestDifference) {
                    if (difference > 0 || weAttack > enemyDefend) {
                        bestDifference = difference;
                        bestTarget = enemyUnit;
                        bestDirection = direction;
                    }
                }
            }
        }

        // Attack if a target is available.  Do not log if nothing happening.
        if (bestTarget != null) {
            logger.finest("AI defender attacking " + bestTarget
                + " from " + settlement.getName() + ": " + unit);
            AIMessage.askAttack(getAIUnit(), bestDirection);
        }
    }
    
    /**
     * Gets debugging information about this mission.  This string is
     * a short representation of this object's state.
     *
     * @return The <code>String</code>:
     *      "(x, y) ColonyName"
     *      where <code>x</code> and <code>y</code> is the
     *      coordinates of the settlement for this mission,
     *      and <code>ColonyName</code> is the name
     *      (if available).
     */
    public String getDebuggingInfo() {
        return settlement.getTile().getPosition().toString()
            + " " + settlement.getName();
    }


    // Serialization

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        if (isValid()) {
            toXML(out, getXMLElementTagName());
        }
    }

    /**
     * {@inherit-doc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("settlement", settlement.getId());
    }

    /**
     * {@inherit-doc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        settlement = (Settlement) getGame()
            .getFreeColGameObject(in.getAttributeValue(null, "settlement"));
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "defendSettlementMission".
     */
    public static String getXMLElementTagName() {
        return "defendSettlementMission";
    }
}
