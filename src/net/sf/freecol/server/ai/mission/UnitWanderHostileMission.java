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

import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for wandering around, attacking targets owned by a player we 
 * do not like.
 */
public class UnitWanderHostileMission extends Mission {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(UnitWanderHostileMission.class.getName());

    private static final String tag = "AI hostile-wanderer";


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public UnitWanderHostileMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);

        uninitialized = false;
    }

    /**
     * Creates a new <code>UnitWanderHostileMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public UnitWanderHostileMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    // Mission interface

    /**
     * Gets the mission target.
     *
     * @return Null.
     */
    public Location getTarget() {
        return null;
    }

    /**
     * Why would a UnitWanderHostileMission be invalid with the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidHostileReason(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        return (!unit.isOffensiveUnit()) ? Mission.UNITNOTOFFENSIVE
            : (unit.getTile() == null) ? Mission.UNITNOTONMAP
            : null;
    }

    /**
     * Why is this mission invalid?
     *
     * @return A reason for mission invalidity, or null if none found.
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), null);
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
            : ((reason = invalidHostileReason(aiUnit)) != null) ? reason
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
            : ((reason = invalidHostileReason(aiUnit)) != null) ? reason
            : null;
    }

    /**
     * Should this mission only be carried out once?
     *
     * @return True.
     */
    @Override
    public boolean isOneTime() {
        return true;
    }

    /**
     * Performs the mission.  Search for hostile targets that are
     * located within one move and attack them, otherwise wander in a
     * random direction.
     */
    public void doMission() {
        final Unit unit = getUnit();
        String reason = invalidReason();
        if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        // Make random moves in a reasonably consistent direction,
        // checking for a target along the way.
        final AIMain aiMain = getAIMain();
        final AIUnit aiUnit = getAIUnit();
        int check = 0, checkTurns = Utils.randomInt(logger, "Hostile",
                                                    getAIRandom(), 4);
        Direction d = Direction.getRandomDirection(tag, getAIRandom());
        boolean moved = false;
        Mission m;
        while (unit.getMovesLeft() > 0) {
            // Every checkTurns, look for a target of opportunity.
            if (check == 0) {
                Location loc = UnitSeekAndDestroyMission.findTarget(aiUnit, 1);
                if (loc != null) {
                    m = new UnitSeekAndDestroyMission(aiMain, aiUnit, loc);
                    aiUnit.setMission(m);
                    m.doMission();
                    return;
                }
                check = checkTurns;
            } else check--;

            if ((d = moveRandomly(tag, d)) == null) break;
            moved = true;
        }
        if (moved) {
            logger.finest(tag + " moved to " + unit.getLocation()
                + ": " + this);
        } else {
            logger.finest(tag + " failed to move: " + this);
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
        toXML(out, getXMLElementTagName());
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "unitWanderHostileMission".
     */
    public static String getXMLElementTagName() {
        return "unitWanderHostileMission";
    }
}
