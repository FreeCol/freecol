/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for bringing a gift to a specified player.
 *
 * The mission has three different tasks to perform:
 * <ol>
 * <li>Get the gift (goods) from the {@link IndianSettlement} that owns the
 * unit.
 * <li>Transport this gift to the given {@link Colony}.
 * <li>Complete the mission by delivering the gift.
 * </ol>
 */
public class IndianBringGiftMission extends Mission {

    private static final Logger logger = Logger.getLogger(IndianBringGiftMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI native gifter";

    /** The Colony to receive the gift. */
    private Colony colony;

    /** Has the gift been collected? */
    private boolean collected;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The <code>Colony</code> receiving the gift.
     */
    public IndianBringGiftMission(AIMain aiMain, AIUnit aiUnit, Colony target) {
        super(aiMain, aiUnit, target);

        this.collected = hasGift();
    }

    /**
     * Creates a new <code>IndianBringGiftMission</code> and reads the given
     * element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public IndianBringGiftMission(AIMain aiMain, AIUnit aiUnit,
                                  FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    public Colony getColony() { return this.colony; }

    /**
     * Checks if the unit is carrying a gift (goods).
     *
     * @return True if the unit is carrying goods.
     */
    private boolean hasGift() {
        return getUnit().hasGoodsCargo();
    }

    /**
     * Why would this mission be invalid with the given unit?
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        IndianSettlement home;
        return (reason != null)
            ? reason
            : ((home = aiUnit.getUnit().getHomeIndianSettlement()) == null
                || home.isDisposed())
            ? "home-destroyed"
            : null;
    }

    /**
     * Why would an IndianBringGiftMission be invalid with the given
     * unit and colony.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param colony The <code>Colony</code> to test.
     * @return A reason why the mission would be invalid with the unit
     *     and colony or null if none found.
     */
    private static String invalidColonyReason(AIUnit aiUnit, Colony colony) {
        String reason = invalidTargetReason(colony);
        if (reason != null) return reason;
        final Unit unit = aiUnit.getUnit();
        final Player owner = unit.getOwner();
        Player targetPlayer = colony.getOwner();
        switch (owner.getStance(targetPlayer)) {
        case UNCONTACTED: case WAR: case CEASE_FIRE:
            return "bad-stance";
        case PEACE: case ALLIANCE:
            Tension tension = unit.getHomeIndianSettlement()
                .getAlarm(targetPlayer);
            if (tension != null && tension.getLevel()
                .compareTo(Tension.Level.HAPPY) > 0) return "unhappy";
        }
        return null;
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        return invalidMissionReason(aiUnit);
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        String reason = invalidMissionReason(aiUnit);
        return (reason != null) ? reason
            : (loc instanceof Colony)
            ? invalidColonyReason(aiUnit, (Colony)loc)
            : (loc instanceof IndianSettlement)
            ? invalidTargetReason(loc, aiUnit.getUnit().getOwner())
            : Mission.TARGETINVALID;
    }


    // Mission interface
    //   Inherit dispose, getBaseTransportPriority, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTarget() {
        return (this.collected) ? this.colony
            : getUnit().getHomeIndianSettlement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTarget(Location target) {
        if (target instanceof Colony) {
            this.colony = (Colony)target;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        return getTarget();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        return invalidReason(getAIUnit(), this.colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        String reason = invalidReason();
        if (reason != null) return lbFail(lb, false, reason);

        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        final IndianSettlement is = unit.getHomeIndianSettlement();

        while (!this.collected) {
            Unit.MoveType mt = travelToTarget(getTarget(), null, lb);
            switch (mt) {
            case MOVE: // Arrived
                break;

            case MOVE_HIGH_SEAS: case MOVE_NO_MOVES:
            case MOVE_NO_REPAIR: case MOVE_ILLEGAL:
                return lbWait(lb);

            case MOVE_NO_TILE:
                return this;
            
            case ATTACK_SETTLEMENT: case ATTACK_UNIT: // A blockage!
                Location blocker = resolveBlockage(aiUnit, getTarget());
                if (blocker != null
                    && AIMessage.askAttack(aiUnit, unit.getTile()
                        .getDirection(blocker.getTile()))) {
                    return lbAttack(lb, blocker);
                }
                moveRandomly(tag, null);
                continue;

            default:
                return lbMove(lb, mt);
            }

            // Load the goods.
            lbAt(lb);
            Goods gift = is.getRandomGift(getAIRandom());
            if (gift == null) return lbFail(lb, false, "found no gift");
            if (!AIMessage.askLoadGoods(is, gift.getType(), gift.getAmount(),
                                        aiUnit) || !hasGift()) {
                return lbFail(lb, false, "failed to collect gift");
            }
            this.collected = true;
            lb.add(", collected gift");
            return lbRetarget(lb);
        }

        // Move to the target's colony and deliver, avoiding trouble
        // by choice of cost decider.
        for (;;) {
            Unit.MoveType mt = travelToTarget(getTarget(),
                CostDeciders.avoidSettlementsAndBlockingUnits(), lb);
            switch (mt) {
            case MOVE_HIGH_SEAS: case MOVE_NO_MOVES:
            case MOVE_NO_REPAIR: case MOVE_ILLEGAL:
                return lbWait(lb);

            case MOVE_NO_TILE:
                return this;
            
            case MOVE: case ATTACK_SETTLEMENT: // Arrived (do not attack!)
                break;

            case ATTACK_UNIT:
                Location blocker = resolveBlockage(aiUnit, getTarget());
                if (blocker != null
                    && AIMessage.askAttack(aiUnit, unit.getTile()
                        .getDirection(blocker.getTile()))) {
                    return lbAttack(lb, blocker);
                }
                moveRandomly(tag, null);
                continue;
            
            default:
                return lbMove(lb, mt);
            }
        
            // Deliver the goods.
            lbAt(lb);
            Settlement settlement = (Settlement)getTarget();
            boolean result = false;
            if (AIMessage.askGetTransaction(aiUnit, settlement)) {
                result = AIMessage.askDeliverGift(aiUnit, settlement,
                    unit.getGoodsList().get(0));
                AIMessage.askCloseTransaction(aiUnit, settlement);
            }
            return (result)
                ? lbDone(lb, false, "delivered")
                : lbFail(lb, false, "delivery");
        }
    }


    // Serialization

    private static final String COLLECTED_TAG = "collected";
    private static final String COLONY_TAG = "colony";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(COLLECTED_TAG, this.collected);

        if (this.colony != null) {
            xw.writeAttribute(COLONY_TAG, this.colony.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.collected = xr.getAttribute(COLLECTED_TAG, false);

        this.colony = xr.getAttribute(getGame(), COLONY_TAG,
                                      Colony.class, (Colony)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "indianBringGiftMission".
     */
    public static String getXMLElementTagName() {
        return "indianBringGiftMission";
    }
}
