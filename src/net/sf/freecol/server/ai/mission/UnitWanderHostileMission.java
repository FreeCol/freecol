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
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
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


    /**
     * Looks for a target of opportunity, move to it and attack.
     * Pretend to be a UnitSeekAndDestroyMission which has the targeting
     * code.
     *
     * @param aiUnit The <code>AIUnit</code> that attacks.
     * @return True if the move is completed by this action.
     */
    private boolean seekAndAttack(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        if (!unit.isOffensiveUnit()) return false;
        PathNode path = Mission.findTargetPath(aiUnit, 1,
                                               UnitSeekAndDestroyMission.class);
        Location target = UnitSeekAndDestroyMission.extractTarget(aiUnit, path);
        if (target == null) return false;
        Unit.MoveType mt = travelToTarget(tag, target);
        switch (mt) {
        case MOVE_NO_MOVES:
            logger.finest(tag + " en route to " + target + ": " + unit);
            break;
        case ATTACK_UNIT: case ATTACK_SETTLEMENT:
            Tile unitTile = unit.getTile();
            Settlement settlement = unitTile.getSettlement();
            if (settlement != null && settlement.getUnitCount() < 2) {
                // Do not risk attacking out of a settlement that
                // might collapse.  Defend instead.
                aiUnit.setMission(new DefendSettlementMission(getAIMain(),
                        aiUnit, settlement));
                return true;
            }
            Direction dirn = unitTile.getDirection(target.getTile());
            if (dirn == null) {
                throw new IllegalStateException("No direction");
            }
            logger.finest(tag + " attacking " + target + ": " + unit);
            AIMessage.askAttack(aiUnit, dirn);
            break;
        default:
            logger.finest(tag + " unexpected move type: " + mt
                + ": " + unit);
            break;
        }
        return true;
    }

    /**
     * Performs the mission. This is done by searching for hostile units
     * that are located within one tile and attacking them. If no such units
     * are found, then wander in a random direction.
     */
    public void doMission() {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed()) {
            logger.warning(tag + " broken: " + unit);
            return;
        } else if (unit.getTile() == null) {
            logger.warning(tag + " not on the map: " + unit);
            return;
        }

        // Make random moves in a reasonably consistent direction,
        // checking for a target along the way.
        final AIUnit aiUnit = getAIUnit();
        Direction d = Direction.getRandomDirection(tag, getAIRandom());
        boolean moved = false;
        while (unit.getMovesLeft() > 0) {
            if (seekAndAttack(aiUnit)) return;
            if ((d = moveRandomly(tag, d)) == null) break;
            moved = true;
        }
        if (moved) {
            logger.finest(tag + " moved to " + unit.getTile() + ": " + unit);
        } else {
            logger.finest(tag + " failed to move: " + unit);
        }
    }

    /**
     * Returns true if this Mission should only be carried out once.
     *
     * @return true
     */
    public boolean isOneTime() {
        return true;
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
