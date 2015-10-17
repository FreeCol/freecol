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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for idling in a settlement.
 */
public class IdleAtSettlementMission extends Mission {

    private static final Logger logger = Logger.getLogger(IdleAtSettlementMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI idler";


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public IdleAtSettlementMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit, null);
    }

    /**
     * Creates a new <code>IdleAtSettlementMission</code> and reads the
     * given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public IdleAtSettlementMission(AIMain aiMain, AIUnit aiUnit,
                                   FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Is the unit in a safe location where it can idle, or should it move?
     *
     * @return True if the unit is safe.
     */
    private boolean isSafe() {
        final Unit unit = getUnit();
        return unit.isInEurope() || !unit.hasTile()
            || unit.getTile().hasSettlement();
    }


    // Implement Mission
    //   Inherit dispose, getTransportDestination

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseTransportPriority() {
        return MINIMUM_TRANSPORT_PRIORITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTarget() {
        return (isSafe()) ? null : findTarget();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTarget(Location target) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        if (isSafe()) return null;

        final Unit unit = getAIUnit().getUnit();
        PathNode path = unit.findOurNearestOtherSettlement();
        return (path == null) ? null
            : Location.upLoc(path.getLastNode().getLocation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOneTime() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        return invalidAIUnitReason(getAIUnit());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        String reason = invalidReason();
        if (reason != null) return lbFail(lb, false, reason);

        // If safe, do nothing but do not use lbWait in case a useful
        // mission is found.
        if (isSafe()) {
            lb.add(", idling");
            return lbAt(lb);
        }

        Location target = getTarget();
        if (target == null) {
            // Just make a random moves if no target can be found.
            moveRandomlyTurn(tag);
            return lbWait(lb);
        }

        Unit.MoveType mt = travelToTarget(getTarget(), null, lb);
        switch (mt) {
        case MOVE: // Arrived
            break;

        case MOVE_HIGH_SEAS: case MOVE_NO_MOVES:
        case MOVE_NO_REPAIR: case MOVE_ILLEGAL:
            return lbWait(lb);

        case MOVE_NO_ACCESS_EMBARK: case MOVE_NO_TILE:
            return this;

        default:
            return lbMove(lb, mt);
        }

        return lbAt(lb);
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "idleAtSettlementMission".
     */
    public static String getXMLElementTagName() {
        return "idleAtSettlementMission";
    }
}
