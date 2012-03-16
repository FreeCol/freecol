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
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


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
    }

    /**
     * Gets the object we are trying to destroy.
     *
     * @return The object which should be destroyed.
     */
    public Location getTarget() {
        return target;
    }


    /**
     * Is a location a suitable seek-and-destroy target for an AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to seek-and-destroy with.
     * @param target The candidate <code>Settlement</code> or <code>Unit</code>.
     * @return True if the target is suitable.
     */
    public static boolean isTarget(AIUnit aiUnit, Location target) {
        final Unit unit = aiUnit.getUnit();
        final Player owner = unit.getOwner();
        Player targetPlayer;
        return target != null
            && !((FreeColGameObject)target).isDisposed()
            && target.getTile() != null
            && !(target instanceof Settlement && unit.isNaval())
            && !(target instanceof Unit
                && (target.getTile().getSettlement() != null
                    || ((((Unit)target).isNaval() && !target.getTile().isLand())
                        != unit.isNaval())))
            && (targetPlayer = ((Ownable)target).getOwner()) != null
            && targetPlayer != owner
            && (owner.getStance(targetPlayer) == Stance.WAR
                || (owner.isIndian()
                    && owner.getTension(targetPlayer).getLevel()
                        .compareTo(Tension.Level.CONTENT) > 0));
    }

    /**
     * Extract a valid target for this mission from a path.
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to extract a target from.
     * @return A target for this mission, or null if none found.
     */
    public static Location extractTarget(AIUnit aiUnit, PathNode path) {
        Unit unit;
        Tile tile;
        Location target = (aiUnit == null
            || (unit = aiUnit.getUnit()) == null
            || path == null
            || (tile = path.getLastNode().getTile()) == null) ? null
            : (tile.getSettlement() != null) ? tile.getSettlement()
            : tile.getDefendingUnit(unit);
        return (isTarget(aiUnit, target)) ? target : null;
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
    static private int scoreSettlementTarget(AIUnit aiUnit, PathNode path,
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
        if (unit.getOwner().isIndian()) {
            // Natives prefer to attack when DISPLEASED.
            IndianSettlement is = unit.getIndianSettlement();
            if (is != null && is.getAlarm(settlement.getOwner()) != null) {
                value += is.getAlarm(settlement.getOwner()).getValue()
                    - Tension.Level.DISPLEASED.getLimit();
            }
        }

        logger.finest("UnitSeekAndDestroyMission settlement score(" + unit
            + " v " + settlement + ") = " + value);
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
    static private int scoreUnitTarget(AIUnit aiUnit, PathNode path,
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

        logger.finest("UnitSeekAndDestroyMission score(" + unit
            + " v " + defender + ") = " + value);
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
    public static int scoreTarget(AIUnit aiUnit, PathNode path) {
        Location target = extractTarget(aiUnit, path);
        return (target instanceof Settlement)
            ? scoreSettlementTarget(aiUnit, path, (Settlement)target)
            : (target instanceof Unit)
            ? scoreUnitTarget(aiUnit, path, (Unit)target)
            : Integer.MIN_VALUE;
    }

    /**
     * Finds a suitable seek-and-destroy target for an AI unit.
     *
     * @param aiUnit The <code>AIUnit</code> to find a target for.
     * @param range An upper bound on the number of moves.
     * @return A path to the target, or null if none found.
     */
    public static PathNode findTarget(AIUnit aiUnit, int range) {
        Unit unit;
        Tile startTile;
        if (aiUnit == null
            || (unit = aiUnit.getUnit()) == null || unit.isDisposed() 
            || (startTile = unit.getPathStartTile()) == null) return null;

        return unit.search(startTile,
            getMissionGoalDecider(aiUnit, UnitSeekAndDestroyMission.class),
            CostDeciders.avoidIllegal(), range,
            ((unit.isOnCarrier()) ? ((Unit)unit.getLocation()) : null));
    }

    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    @Override
    public Tile getTransportDestination() {
        Tile tile = (target == null) ? null : target.getTile();
        return (shouldTakeTransportToTile(tile)) ? tile : null;
    }

    // Mission interface

    /**
     * Check to see if this mission is still valid.
     *
     * @return True if this mission is valid.
    */
    public boolean isValid() {
        return super.isValid()
            && getUnit().isOffensiveUnit()
            && isTarget(getAIUnit(), target);
    }

    /**
     * Performs the mission.  Check for a target-of-opportunity within one
     * turn and hit that if possible.  Otherwise, just continue on towards
     * the real target.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    @Override
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed()) {
            logger.warning(tag + " broken: " + unit);
            return;
        } else if (!unit.isOffensiveUnit()) {
            logger.finest(tag + " disarmed: " + unit);
            return;
        }
        final Player player = unit.getOwner();

        // Is there a target-of-opportunity?
        final AIUnit aiUnit = getAIUnit();
        PathNode path = findTarget(aiUnit, 1);
        Location nearbyTarget = (path == null) ? null
            : extractTarget(aiUnit, path);
        if (nearbyTarget == target) nearbyTarget = null;
        if (isValid()) {
            if (nearbyTarget != null) {
                logger.finest(tag + " found target-of-opportunity "
                    + nearbyTarget + ": " + unit);
            }
        } else {
            if (nearbyTarget == null) {
                logger.finest(tag + " can not find a target: " + unit);
                return;
            }
            logger.finest(tag + " abandoning " + target
                + " retargeting " + nearbyTarget + ": " + unit);
            target = nearbyTarget;
            nearbyTarget = null;
        }

        Location currentTarget = (nearbyTarget != null) ? nearbyTarget : target;
        Unit.MoveType mt = travelToTarget(tag, currentTarget.getTile());
        Tile unitTile = unit.getTile();
        Settlement settlement = unitTile.getSettlement();
        switch (mt) {
        case MOVE_NO_MOVES:
            logger.finest(tag + " en route to " + currentTarget + ": " + unit);
            break;
        case ATTACK_UNIT: case ATTACK_SETTLEMENT:
            if (settlement != null && settlement.getUnitCount() < 2) {
                // Do not risk attacking out of a settlement that
                // might collapse.  Defend instead.
                aiUnit.setMission(new DefendSettlementMission(getAIMain(),
                        aiUnit, settlement));
                return;
            }
            logger.finest(tag + " attacking " + currentTarget + ": " + unit);
            AIMessage.askAttack(aiUnit, unitTile.getDirection(target.getTile()));
            break;
        default:
            logger.finest(tag + " unexpected move type: " + mt + ": " + unit);
            break;
        }
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
     * {@inherit-doc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);
        if (getTarget() != null) {
            out.writeAttribute("target", getTarget().getId());
        }
    }

    /**
     * {@inherit-doc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        target = (Location)getGame()
            .getFreeColGameObject(in.getAttributeValue(null, "target"));
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
