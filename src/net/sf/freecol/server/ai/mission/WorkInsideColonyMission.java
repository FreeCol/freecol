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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for working inside an AI colony.
 */
public class WorkInsideColonyMission extends Mission {

    private static final Logger logger = Logger.getLogger(WorkInsideColonyMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI worker";

    /** The target colony to work inside. */
    private Colony colony;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param aiColony The <code>AIColony</code> the unit should be
     *     working in.
     */
    public WorkInsideColonyMission(AIMain aiMain, AIUnit aiUnit,
                                   AIColony aiColony) {
        super(aiMain, aiUnit, aiColony.getColony());
    }

    /**
     * Creates a new <code>WorkInsideColonyMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public WorkInsideColonyMission(AIMain aiMain, AIUnit aiUnit,
                                   FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Convenience accessor for the colony to work in.
     *
     * @return The <code>AIColony</code> to work in.
     */
    public AIColony getAIColony() {
        return getAIMain().getAIColony(colony);
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
            : (!aiUnit.getUnit().isPerson()) ? Mission.UNITNOTAPERSON
            : ((reason = invalidTargetReason(loc, aiUnit.getUnit().getOwner()))
                != null) ? reason
            : null;
    }


    // Implement Mission
    //   Inherit dispose, getTransportDestination, isOneTime

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
    public Location getTarget() {
        return colony;
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
        return invalidReason(getAIUnit(), getTarget());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        String reason = invalidReason();
        if (reason != null) return lbFail(lb, false, reason);

        final Unit unit = getUnit();
        Unit.MoveType mt = travelToTarget(getTarget(),
            CostDeciders.avoidSettlementsAndBlockingUnits(), lb);
        switch (mt) {
        case MOVE: // Arrived
            break;

        case MOVE_HIGH_SEAS: case MOVE_NO_REPAIR:
        case MOVE_NO_MOVES: case MOVE_ILLEGAL:
            return lbWait(lb);

        case MOVE_NO_ACCESS_EMBARK: case MOVE_NO_TILE:
            return this;

        default:
            return lbMove(lb, mt);
        }

        lbAt(lb);
        if (unit.isInColony()) lb.add(", working");
        return lbWait(lb);
    }


    // Serialization

    private static final String COLONY_TAG = "colony";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(COLONY_TAG, colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        colony = xr.getAttribute(getGame(), COLONY_TAG,
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
     * @return "workInsideColonyMission".
     */
    public static String getXMLElementTagName() {
        return "workInsideColonyMission";
    }
}
