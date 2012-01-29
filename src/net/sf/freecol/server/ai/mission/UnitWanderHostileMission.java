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

import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
 * Mission for attacking any unit owned by a player we do not like that
 * is within a radius of 1 tile. If no such unit can be found; just
 * wander around.
 */
public class UnitWanderHostileMission extends Mission {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(UnitWanderHostileMission.class.getName());

    /**
    * Creates a mission for the given <code>AIUnit</code>.
    *
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public UnitWanderHostileMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }


    /**
     * Loads a mission from the given element.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public UnitWanderHostileMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>UnitWanderHostileMission</code> and reads the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public UnitWanderHostileMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }


    /**
    * Performs the mission. This is done by searching for hostile units
    * that are located within one tile and attacking them. If no such units
    * are found, then wander in a random direction.
    *
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Unit unit = getUnit();
        if (!(unit.getLocation() instanceof Tile)) {
            return;
        }

        PathNode pathToTarget = null;
        if (unit.isOffensiveUnit()) {
            pathToTarget = findTarget(5);
        }

        if (pathToTarget != null) {
            Direction direction = moveTowards(pathToTarget);
            if (direction != null && unit.getMoveType(direction).isAttack()) {
                if (unit.getTile().getSettlement() != null
                    && unit.getTile().getSettlement().getUnitCount() < 2) {
                    // Do not risk attacking out of a settlement that
                    // might collapse.
                } else {
                    AIMessage.askAttack(getAIUnit(), direction);
                }
            }
        } else {
            // Just make a random move if no target can be found.
            moveRandomly();
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
