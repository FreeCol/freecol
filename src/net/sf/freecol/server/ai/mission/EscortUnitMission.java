/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for attacking a specific target, be it a Unit or a Settlement.
 */
public final class EscortUnitMission extends Mission {

    private static final Logger logger = Logger.getLogger(EscortUnitMission.class.getName());

    public static final String TAG = "escortUnitMission";

    /** The tag for this mission. */
    private static final String tag = "AI escorter";

    /**
     * The object we are trying to escort. This can only be a {@code Unit}.
     */
    private Location target, transportTarget;


    /**
     * Creates a mission for the given {@code AIUnit}.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The {@code AIUnit} this mission is created for.
     * @param target The object we are trying to escort.
     */
    public EscortUnitMission(AIMain aiMain, AIUnit aiUnit, Unit target) {
        super(aiMain, aiUnit);

        setTarget(target);
    }

    /**
     * Creates a new {@code EscortUnitMission} from a reader.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The {@code AIUnit} this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public EscortUnitMission(AIMain aiMain, AIUnit aiUnit, FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Why would a EscortUnitMission be invalid with the given unit.
     *
     * @param aiUnit The {@code AIUnit} to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidUnitReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        return (reason != null)
            ? reason
            : (!aiUnit.getUnit().isOffensiveUnit())
            ? Mission.UNITNOTOFFENSIVE
            : null;
    }

    /**
     * Why would a EscortUnitMission be invalid with the given unit
     * and target unit.
     *
     * @param aiUnit The {@code AIUnit} to seek-and-destroy with.
     * @param unit The target {@code Unit} to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidTargetReason(AIUnit aiUnit, Unit unit) {
        String reason = invalidTargetReason(unit);
        if (reason != null) return reason;
        final Tile tile = unit.getTile();
        return (tile == null)
            ? "target-not-on-map"
            : (aiUnit.getUnit().getOwner() != unit.getOwner())
            ? Mission.TARGETOWNERSHIP
            : (aiUnit.getUnit().isNaval() != unit.isNaval())
            ? "target-incompatible"
            : null;
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidMissionReason(AIUnit aiUnit) {
        return invalidUnitReason(aiUnit);
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param loc The {@code Location} to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidMissionReason(AIUnit aiUnit, Location loc) {
        final String reason = invalidMissionReason(aiUnit);
        if (reason != null) {
            return reason;
        }
        return invalidTargetReason(aiUnit, (Unit) loc);
    }

    
    // Implement Mission
    //   Inherit dispose, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseTransportPriority() {
        return NORMAL_TRANSPORT_PRIORITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        if (!isValid()) return null;
        Location loc = (transportTarget != null) ? transportTarget : target;
        return (getUnit().shouldTakeTransportTo(loc)) ? loc : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTarget(Location target) {
        if (target != null && !(target instanceof Unit)) {
            throw new IllegalArgumentException("Target not supported: " + target);
        }
        
        this.target = target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        return invalidMissionReason(getAIUnit(), getTarget());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        final AIUnit aiUnit = getAIUnit();
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            return lbDrop(lb, reason);
        } else if (reason != null) {
            return lbFail(lb, false, reason);
        }

        // Go to the target.
        final Location currentTarget = getTarget();
        
        // Note avoiding other targets by choice of cost decider.
        final Unit.MoveType mt = travelToTarget(currentTarget, CostDeciders.avoidSettlementsAndBlockingUnits(), lb);
        switch (mt) {
        case MOVE_HIGH_SEAS: case MOVE_NO_MOVES: case MOVE_ILLEGAL:
            lbWait(lb);
            break;
        case MOVE_NO_REPAIR:
            return lbFail(lb, false, AIUNITDIED);
        case MOVE_NO_ACCESS_EMBARK: case MOVE_NO_TILE:
            break;
        case ATTACK_UNIT: case ATTACK_SETTLEMENT:
            final Tile unitTile = getUnit().getTile();
            Direction d = unitTile.getDirection(currentTarget.getTile());
            if (d == null) {
                logger.warning("SDDM bogus " + mt + " with " + getUnit()
                    + " from " + unitTile + " to " + currentTarget
                    + " at " + currentTarget.getTile());
                return lbWait(lb);
            }
            AIMessage.askAttack(aiUnit, d);
            return lbAttack(lb, currentTarget);
        default:
            lbMove(lb, mt);
            break;
        }
        
        // TODO: Consider attacking adjacent enemy units here.
        
        return this;
    }


    // Serialization

    private static final String TARGET_TAG = "target";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (target != null) {
            xw.writeAttribute(TARGET_TAG, target.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        target = xr.getLocationAttribute(getGame(), TARGET_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }
}
