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
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for wandering in random directions.
 */
public class UnitWanderMission extends Mission {

    private static final Logger logger = Logger.getLogger(UnitWanderMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI wanderer";


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public UnitWanderMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit, null);
    }

    /**
     * Creates a new <code>UnitWanderMission</code> and reads the
     * given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public UnitWanderMission(AIMain aiMain, AIUnit aiUnit,
                             FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    // Implement Mission
    //   Inherit dispose, getBaseTransportPriority, getTransportDestination

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTarget() {
        return null;
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
        return null;
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

        // Just move in random directions.
        moveRandomlyTurn(tag);
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
     * @return "unitWanderMission".
     */
    public static String getXMLElementTagName() {
        return "unitWanderMission";
    }
}
