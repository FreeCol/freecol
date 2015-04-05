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
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.RandomUtils.*;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for wandering around, attacking targets owned by a player we 
 * do not like.
 */
public class UnitWanderHostileMission extends Mission {

    private static final Logger logger = Logger.getLogger(UnitWanderHostileMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI hostile-wanderer";


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public UnitWanderHostileMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit, null);
    }

    /**
     * Creates a new <code>UnitWanderHostileMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public UnitWanderHostileMission(AIMain aiMain, AIUnit aiUnit,
                                    FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Why would this mission be invalid with the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        return (!unit.isOffensiveUnit()) ? Mission.UNITNOTOFFENSIVE
            : (!unit.hasTile()) ? Mission.UNITNOTONMAP
            : null;
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        String reason;
        return ((reason = Mission.invalidReason(aiUnit)) != null) ? reason
            : ((reason = invalidMissionReason(aiUnit)) != null) ? reason
            : null;
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit,
        @SuppressWarnings("unused") Location loc) {
        String reason;
        return ((reason = invalidAIUnitReason(aiUnit)) != null) ? reason
            : ((reason = invalidMissionReason(aiUnit)) != null) ? reason
            : null;
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
        return invalidReason(getAIUnit(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        String reason = invalidReason();
        if (reason != null) return lbFail(lb, false, reason);

        // Make random moves in a reasonably consistent direction,
        // checking for a target along the way.
        final Unit unit = getUnit();
        final AIUnit aiUnit = getAIUnit();
        int check = 0, checkTurns = randomInt(logger, "Hostile",
                                              getAIRandom(), 4);
        Direction d = Direction.getRandomDirection(tag, logger, getAIRandom());
        while (unit.getMovesLeft() > 0) {
            // Every checkTurns, look for a target of opportunity.
            if (check == 0) {
                Mission m = getAIPlayer().getSeekAndDestroyMission(aiUnit, 1);
                if (m != null) {
                    return lbDone(lb, true, "found target ", m.getTarget());
                }
                check = checkTurns;
            } else check--;

            if ((d = moveRandomly(tag, d)) == null) break;
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
     * @return "unitWanderHostileMission".
     */
    public static String getXMLElementTagName() {
        return "unitWanderHostileMission";
    }
}
