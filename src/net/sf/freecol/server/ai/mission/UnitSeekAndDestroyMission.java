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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for attacking a specific target, be it a Unit or a Settlement.
 */
public class UnitSeekAndDestroyMission extends Mission {

    private static final Logger logger = Logger.getLogger(UnitSeekAndDestroyMission.class.getName());

    private static final String tag = "AI seek+destroyer";

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
    public UnitSeekAndDestroyMission(AIMain aiMain, AIUnit aiUnit,
        Location target) {
        super(aiMain, aiUnit);

        if (!(target instanceof Unit || target instanceof Settlement)) {
            throw new IllegalArgumentException("Invalid seek+destroy target: "
                + target);
        }
            
        this.target = target;
        logger.finest(tag + " begins with target " + target
            + ": " + aiUnit.getUnit());
        uninitialized = false;
    }

    /**
     * Creates a new <code>UnitSeekAndDestroyMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public UnitSeekAndDestroyMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Extract a valid target for this mission from a path.
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to extract a target from.
     * @return A target for this mission, or null if none found.
     */
    public static Location extractTarget(AIUnit aiUnit, PathNode path) {
        final Tile tile = (path == null) ? null : path.getLastNode().getTile();
        return (tile == null || aiUnit == null || aiUnit.getUnit() == null) 
            ? null
            : (invalidReason(aiUnit, tile.getSettlement()) == null)
            ? tile.getSettlement()
            : (invalidReason(aiUnit,
                    tile.getDefendingUnit(aiUnit.getUnit())) == null)
            ? tile.getDefendingUnit(aiUnit.getUnit())
            : null;
    }

    /**
     * Scores a potential attack on a settlement.
     *
     * Do not cheat and look inside the settlement.
     * Just use visible facts about it.
     *
     * TODO: if we are the REF and there is a significant Tory
     * population inside, assume traitors have briefed us.
     *
     * @param aiUnit The <code>AIUnit</code> to do the mission.
     * @param path The <code>PathNode</code> to take to the settlement.
     * @param settlement The <code>Settlement</code> to attack.
     * @return A score of the desirability of the mission.
     */
    private static int scoreSettlementPath(AIUnit aiUnit, PathNode path,
        Settlement settlement) {
        final Unit unit = aiUnit.getUnit();
        final CombatModel combatModel = unit.getGame().getCombatModel();

        int value = 1020;
        value -= path.getTotalTurns() * 100;

        final float off = combatModel.getOffencePower(unit, settlement);
        value += off * 50;

        if (settlement instanceof Colony) {
            // Favour high population (more loot:-).
            Colony colony = (Colony) settlement;
            value += 50 * colony.getUnitCount();
            if (colony.hasStockade()) { // Avoid fortifications.
                value -= 200 * colony.getStockade().getLevel();
            }
        } else if (settlement instanceof IndianSettlement) {
            // Favour the most hostile settlements
            IndianSettlement is = (IndianSettlement) settlement;
            Tension tension = is.getAlarm(unit.getOwner());
            if (tension != null) value += tension.getValue() / 2;
        }

        //logger.finest("UnitSeekAndDestroyMission settlement score(" + unit
        //    + " v " + settlement + ") = " + value);
        return value;
    }

    /**
     * Scores a potential attack on a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to do the mission.
     * @param path The <code>PathNode</code> to take to the settlement.
     * @param defender The <code>Unit</code> to attack.
     * @return A score of the desirability of the mission.
     */
    private static int scoreUnitPath(AIUnit aiUnit, PathNode path,
        Unit defender) {
        final Unit unit = aiUnit.getUnit();
        final Tile tile = path.getLastNode().getTile();
        final int turns = path.getTotalTurns();
        final CombatModel combatModel = unit.getGame().getCombatModel();
        final float off = combatModel.getOffencePower(unit, defender);
        final float def = combatModel.getDefencePower(unit, defender);
        if (off <= 0) return Integer.MIN_VALUE;

        int value = 1020 - turns * 100;
        value += 100 * (off - def);

        // Add a big bonus for treasure trains on the tile.
        // Do not cheat and look at the value.
        for (Unit u : tile.getUnitList()) {
            if (u.canCarryTreasure() && u.getTreasureAmount() > 0) {
                value += 1000;
                break;
            }
        }

        if (defender.isNaval()) {
            if (tile.isLand()) value += 500; // Easy win
        } else {
            if (defender.hasAbility(Ability.EXPERT_SOLDIER)
                && !defender.isArmed()) value += 100;
        }

        //logger.finest("UnitSeekAndDestroyMission score(" + unit
        //    + " v " + defender + ") = " + value);
        return value;
    }

    /**
     * Evaluate a potential seek and destroy mission for a given unit
     * to a given tile.
     *
     * TODO: revisit and rebalance the mass of magic numbers.
     *
     * @param aiUnit The <code>AIUnit</code> to do the mission.
     * @param path A <code>PathNode</code> to take to the target.
     * @return A score for the proposed mission.
     */
    public static int scorePath(AIUnit aiUnit, PathNode path) {
        Location target = extractTarget(aiUnit, path);
        return (target instanceof Settlement)
            ? scoreSettlementPath(aiUnit, path, (Settlement)target)
            : (target instanceof Unit)
            ? scoreUnitPath(aiUnit, path, (Unit)target)
            : Integer.MIN_VALUE;
    }

    /**
     * Finds a suitable seek-and-destroy target path for an AI unit.
     *
     * @param aiUnit The <code>AIUnit</code> to find a target for.
     * @param range An upper bound on the number of moves.
     * @return A path to the target, or null if none found.
     */
    public static PathNode findTargetPath(AIUnit aiUnit, int range) {
        return Mission.findTargetPath(aiUnit, range,
            UnitSeekAndDestroyMission.class);
    }

    /**
     * Finds a suitable seek-and-destroy target for an AI unit.
     *
     * @param aiUnit The <code>AIUnit</code> to find a target for.
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
        return (target == null
            || !shouldTakeTransportToTile(target.getTile())) ? null
            : target;
    }

    // Mission interface

    /**
     * Gets the target of this mission.
     *
     * @return The mission target.
     */
    public Location getTarget() {
        return target;
    }

    /**
     * Why would a UnitSeekAndDestroyMission be invalid with the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidDestroyReason(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        return (!unit.isOffensiveUnit()) ? Mission.UNITNOTOFFENSIVE
            : null;
    }

    /**
     * Why would a UnitSeekAndDestroyMission be invalid with the given unit
     * with respect to another player.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param other The other <code>Player</code> to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidStanceReason(AIUnit aiUnit, Player other) {
        final Player owner = aiUnit.getUnit().getOwner();
        return (owner == other) ? Mission.TARGETOWNERSHIP
            : (owner.getStance(other) == Stance.WAR) ? null
            : (owner.isIndian() || owner.getTension(other).getLevel()
                .compareTo(Tension.Level.CONTENT) > 0) ? null
            : "target-at-peace";
    }

    /**
     * Why would a UnitSeekAndDestroyMission be invalid with the given unit
     * and settlement.
     *
     * @param aiUnit The <code>AIUnit</code> to seek-and-destroy with.
     * @param settlement The <code>Settlement</code> to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidSettlementReason(AIUnit aiUnit,
                                                  Settlement settlement) {
        final Unit unit = aiUnit.getUnit();
        final Player player = unit.getOwner();
        String reason;
        return (unit.isNaval()) ? "unit-is-naval"
            : ((reason = invalidTargetReason(settlement, null))
                != null) ? reason
            : (settlement.getOwner() == player) ? Mission.TARGETOWNERSHIP
            : ((reason = invalidStanceReason(aiUnit, settlement.getOwner()))
                != null) ? reason
            : null;
    }

    /**
     * Why would a UnitSeekAndDestroyMission be invalid with the given unit
     * and target unit.
     *
     * @param aiUnit The <code>AIUnit</code> to seek-and-destroy with.
     * @param target The target <code>Unit</code> to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidUnitReason(AIUnit aiUnit, Unit target) {
        final Unit unit = aiUnit.getUnit();
        Tile tile = target.getTile();
        String reason;
        return ((reason = invalidTargetReason(target, null)) != null) ? reason
            : ((reason = invalidStanceReason(aiUnit, target.getOwner()))
                != null) ? reason
            : (tile == null) ? "target-not-on-map"
            : (tile.getSettlement() != null) ? "target-in-settlement"
            : ((target.isNaval() && !tile.isLand())
                != unit.isNaval()) ? "target-incompatible"
            : null;
    }

    /**
     * Why is this mission invalid?
     *
     * @return A reason for mission invalidity, or null if none found.
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), target);
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        String reason;
        return ((reason = Mission.invalidReason(aiUnit)) != null) ? reason
            : ((reason = invalidDestroyReason(aiUnit)) != null) ? reason
            : null;
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        String reason;
        return ((reason = invalidAIUnitReason(aiUnit)) != null) ? reason
            : ((reason = invalidDestroyReason(aiUnit)) != null) ? reason
            : (loc instanceof Settlement)
            ? invalidSettlementReason(aiUnit, (Settlement)loc)
            : (loc instanceof Unit)
            ? invalidUnitReason(aiUnit, (Unit)loc)
            : Mission.TARGETINVALID;
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * Performs the mission.
     *
     * Check for a target-of-opportunity within one turn and hit that
     * if possible.  Otherwise, just continue on towards the real
     * target.
     */
    @Override
    public void doMission() {
        final Unit unit = getUnit();
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            target = null;
        } else if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + unit);
            return;
        }

        // Is there a target-of-opportunity?
        final AIUnit aiUnit = getAIUnit();
        Location nearbyTarget = findTarget(aiUnit, 1);
        if (nearbyTarget != null) {
            if (target == null) {
                logger.finest(tag + " retargeted " + nearbyTarget
                    + ": " + unit);
                target = nearbyTarget;
                nearbyTarget = null;
            } else if (nearbyTarget == target) {
                nearbyTarget = null;
            } else {
                logger.finest(tag + " found target-of-opportunity "
                    + nearbyTarget + ": " + unit);
            }
        } else if (reason != null) {
            logger.finest(tag + " " + reason + ": " + unit);
            return;
        }

        // Go to the target.
        Location currentTarget = (nearbyTarget != null) ? nearbyTarget : target;
        Unit.MoveType mt = travelToTarget(tag, currentTarget);
        switch (mt) {
        case MOVE_NO_MOVES: case MOVE_ILLEGAL:
            break;
        case ATTACK_UNIT: case ATTACK_SETTLEMENT:
            Tile unitTile = unit.getTile();
            Settlement settlement = unitTile.getSettlement();
            if (settlement != null && settlement.getUnitCount() < 2) {
                // Do not risk attacking out of a settlement that
                // might collapse.  Defend instead.
                aiUnit.setMission(new DefendSettlementMission(getAIMain(),
                        aiUnit, settlement));
                return;
            }
            Direction dirn = unitTile.getDirection(currentTarget.getTile());
            if (dirn == null) {
                throw new IllegalStateException("No direction " + unitTile
                    + " to " + currentTarget.getTile());
            }
            logger.finest(tag + " attacking " + currentTarget + ": " + unit);
            AIMessage.askAttack(aiUnit, dirn);
            break;
        default:
            logger.finest(tag + " unexpected move type: " + mt + ": " + unit);
            break;
        }
    }


    // Serialization

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
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        if (target != null) {
            writeAttribute(out, "target", (FreeColGameObject)target);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        String str = in.getAttributeValue(null, "target");
        target = getGame().getFreeColLocation(str);
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "unitSeekAndDestroyMission".
     */
    public static String getXMLElementTagName() {
        return "unitSeekAndDestroyMission";
    }
}
