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
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for defending a <code>Settlement</code>.
 *
 * TODO: This Mission should later use sub-missions for
 *       eliminating threats etc.
 */
public class DefendSettlementMission extends Mission {

    private static final Logger logger = Logger.getLogger(DefendSettlementMission.class.getName());

    private String tag = "AI defender";

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
        logger.finest(tag + " started with " + settlement
            + ": " + aiUnit.getUnit());
        uninitialized = false;
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
        uninitialized = getAIUnit() == null;
    }
    
    /**
     * Gets the target settlement.
     *
     * @return The <code>Settlement</code> to be defended by
     *         this <code>Mission</code>.
     */
    public Settlement getTarget() {
        return settlement;
    }


    /**
     * Is a settlement suitable for a defence mission?
     *
     * @param aiUnit The <code>AIUnit</code> to defend with.
     * @param target The target to test.
     * @return True if the target is a suitable settlement.
     */
    public static boolean isTarget(AIUnit aiUnit, Location target) {
        return target != null
            && (target instanceof Settlement
                && aiUnit.getUnit().getOwner().owns((Settlement)target));
    }

    /**
     * Extract a valid target for this mission from a path.
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to extract a target from,
     *     (uses the unit location if null).
     * @return A target for a <code>DefendSettlementMission</code> or null
     *     if none found.
     */
    public static Settlement extractTarget(AIUnit aiUnit, PathNode path) {
        final Tile tile = (path == null) ? aiUnit.getUnit().getTile()
            : path.getLastNode().getTile();
        return (tile == null) ? null
            : (isTarget(aiUnit, tile.getSettlement())) ? tile.getSettlement()
            : null;
    }

    /**
     * Evaluate allocating a unit to the defence of a settlement.
     *
     * @param aiUnit The <code>AIUnit</code> that is to defend.
     * @param path A <code>PathNode</code> to take to the settlement.
     * @return A value for such a mission.
     */
    public static int scorePath(AIUnit aiUnit, PathNode path) {
        Settlement settlement;
        if (aiUnit == null
            || (settlement = extractTarget(aiUnit, path)) == null) {
            return Integer.MIN_VALUE;
        }

        final int turns = (path == null) ? 0 : path.getTotalTurns();
        int value = 1025 - 100 * turns;
        return value;
    }

    /**
     * Finds a path to the best nearby settlement to defend.
     *
     * @param aiUnit The <code>AIUnit</code> that is searching.
     * @param range An upper bound on the number of moves.
     * @return A <code>PathNode</code> to take to the target,
     *     or null if none suitable.
     */
    public static PathNode findTargetPath(AIUnit aiUnit, int range) {
        return Mission.findTargetPath(aiUnit, range,
                                      DefendSettlementMission.class);
    }

    /**
     * Finds a path to the best nearby settlement to defend.
     *
     * @param aiUnit The <code>AIUnit</code> that is searching.
     * @param range An upper bound on the number of moves.
     * @return A suitable target, or null if none found.
     */
    public static Location findTarget(AIUnit aiUnit, int range) {
        PathNode path = findTargetPath(aiUnit, range);
        return (path == null) ? null : extractTarget(aiUnit, path);
    }


    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    @Override
    public Location getTransportDestination() {
        return (settlement == null
            || !shouldTakeTransportToTile(settlement.getTile())) ? null
            : settlement;
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
     * @return True if this mission is valid.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return Mission.isValid(aiUnit)
            && aiUnit.getUnit().isDefensiveUnit()
            && aiUnit.getUnit().getOwner().getNumberOfSettlements() > 0;
    }

    /**
     * Checks if this mission is still valid.
     *
     * @return True if this mission is valid.
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
     */
    public void doMission() {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed()) {
            logger.warning(tag + " broken: " + unit);
            return;
        } else if (!unit.isDefensiveUnit()) {
            logger.finest(tag + " disarmed: " + unit);
            return;
        }

        // Check target.
        if (!isValid()) {
            logger.finest(tag + " has invalid settlement " + settlement
                + ": " + unit);
            return;
        }

        // Go to the target!
        if (travelToTarget(tag, settlement) != Unit.MoveType.MOVE) return;

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
                m = new IdleAtSettlementMission(aiMain, aiUnit);
            }
        }                
        if (m != null) {
            aiUnit.setMission(m);
            m.doMission();
            return; // No log, setMission() logs this mission going away.
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
                logMe = " waiting to fortify at ";
            } else if (AIMessage.askChangeState(aiUnit, UnitState.FORTIFYING)
                && unit.getState() == UnitState.FORTIFYING) {
                logMe = " completed (fortifying) at ";
            } else {
                logMe = " fortify failed at ";
            }
            logger.finest(tag + logMe + settlement.getName() + ": " + unit);
            return;
        }

        // The settlement is well enough defended.  See if the unit
        // should attack a nearby hostile unit.  Remember to prevent a
        // sole unit attacking because if it loses, the settlement
        // will collapse (and the combat model does not understand that).
        if (!unit.isOffensiveUnit()) return;
        final CombatModel cm = unit.getGame().getCombatModel();
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
                float enemyAttack = cm.getOffencePower(enemyUnit, unit);
                float weAttack = cm.getOffencePower(unit, enemyUnit);
                float enemyDefend = cm.getDefencePower(unit, enemyUnit);
                float weDefend = cm.getDefencePower(enemyUnit, unit);
                
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
            logger.finest(tag + " attacking " + bestTarget
                + " from " + settlement.getName() + ": " + unit);
            AIMessage.askAttack(getAIUnit(), bestDirection);
        }
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

        writeAttribute(out, "settlement", settlement);
    }

    /**
     * {@inherit-doc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        settlement = (Settlement) getGame()
            .getFreeColGameObjectSafely(in.getAttributeValue(null, "settlement"));
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
