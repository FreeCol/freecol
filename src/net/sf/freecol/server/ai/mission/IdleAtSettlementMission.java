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
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for idling in a settlement.
 */
public class IdleAtSettlementMission extends Mission {

    private static final Logger logger = Logger.getLogger(IdleAtSettlementMission.class.getName());

    private static final String tag = "AI idler";


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public IdleAtSettlementMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);

        uninitialized = false;
    }

    /**
     * Creates a new <code>IdleAtSettlementMission</code> and reads the
     * given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public IdleAtSettlementMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        final Unit unit = getUnit();
        if (unit.getTile() == null
            || unit.getTile().getSettlement() != null) return null;
        PathNode path = unit.findOurNearestOtherSettlement();
        Tile target = (path == null) ? null : path.getLastNode().getTile();
        return (shouldTakeTransportToTile(target)) ? target : null;
    }


    // Mission interface

    /**
     * Gets the mission target.
     *
     * @return Null.  There is no target.
     */
    public Location getTarget() {
        return null;
    }

    /**
     * Why is this mission invalid?
     *
     * @return A reason for mission invalidity, or null if none found.
     */
    public String invalidReason() {
        return invalidAIUnitReason(getAIUnit());
    }

    // No static invalidReason(AIUnit [,Location]) forms, as this is
    // a targetless fallback mission that is never invalid.
    // TODO: revise this, it could be invalid if there are no settlements?!?

    /**
     * Should this Mission only be carried out once?
     *
     * @return True.
     */
    @Override
    public boolean isOneTime() {
        return true;
    }

    /**
     * Performs the mission. This is done by moving in a random direction
     * until the move points are zero or the unit gets stuck.
     */
    public void doMission() {
        final Unit unit = getUnit();
        String reason = invalidReason();
        if (reason != null) {
            logger.warning(tag + " broken(" + reason + "): " + this);
            return;
        }

        // Wait if in Europe.
        if (unit.getTile() == null) return;

        // If our tile contains a settlement, idle.  No log, this is normal.
        if (unit.getTile().getSettlement() != null) return;

        PathNode path = unit.findOurNearestOtherSettlement();
        if (path != null) {
            travelToTarget(tag, path.getLastNode().getTile(), null);
        } else { // Just make a random moves if no target can be found.
            moveRandomlyTurn(tag);
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
     * @return "idleAtSettlementMission".
     */
    public static String getXMLElementTagName() {
        return "idleAtSettlementMission";
    }
}
