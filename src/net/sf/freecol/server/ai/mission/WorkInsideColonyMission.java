/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
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

    /** The AI colony to work inside. */
    private AIColony aiColony;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     * @param aiColony The <code>AIColony</code> the unit should be
     *        working in.
     */
    public WorkInsideColonyMission(AIMain aiMain, AIUnit aiUnit,
                                   AIColony aiColony) {
        super(aiMain, aiUnit);

        this.aiColony = aiColony;
        uninitialized = false;
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
                                   FreeColXMLReader xr)
        throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Convenience accessor for the colony to work in.
     *
     * @return The <code>AIColony</code> to work in.
     */
    public AIColony getAIColony() {
        return aiColony;
    }


    // Fake Transportable interface

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        return (getUnit().shouldTakeTransportTo(getTarget())) ? getTarget()
            : null;
    }


    // Mission interface

    /**
     * {@inheritDoc}
     */
    public Location getTarget() {
        return (aiColony == null || aiColony.getColony() == null) ? null
            : aiColony.getColony().getTile();
    }

    /**
     * {@inheritDoc}
     */
    public void setTarget(Location target) {
        throw new IllegalStateException("Target is fixed.");
    }

    /**
     * {@inheritDoc}
     */
    public Location findTarget() {
        throw new IllegalStateException("Target is fixed.");
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

    // Omitted invalidReason(AIUnit), not needed.

    /**
     * {@inheritDoc}
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), getTarget());
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * {@inheritDoc}
     */
    public void doMission() {
        String reason = invalidReason();
        if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        travelToTarget(tag, getTarget(),
                       CostDeciders.avoidSettlementsAndBlockingUnits());
    }


    // Serialization

    private static final String COLONY_TAG = "colony";


    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        if (isValid()) {
            super.toXML(xw, getXMLElementTagName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(COLONY_TAG, aiColony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        aiColony = getAttribute(xr, COLONY_TAG, AIColony.class, (AIColony)null);
    }

    /**
     * {@inheritDoc}
     */
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
