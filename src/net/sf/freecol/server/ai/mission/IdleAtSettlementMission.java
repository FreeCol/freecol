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
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
 * Mission for idling in a settlement.
 */
public class IdleAtSettlementMission extends Mission {

    private static final Logger logger = Logger.getLogger(IdleAtSettlementMission.class.getName());


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
     * Should this Mission only be carried out once?
     *
     * @return True.
     */
    public boolean isOneTime() {
        return true;
    }

    /**
     * Performs the mission. This is done by moving in a random direction
     * until the move points are zero or the unit gets stuck.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed()) {
            logger.warning("AI idler broken: " + unit);
            return;
        }

        // Wait if in Europe.
        if (unit.getTile() == null) return;

        // If our tile contains a settlement, idle.  No log, this is normal.
        if (unit.getTile().getSettlement() != null) return;

        PathNode path = unit.findOurNearestOtherSettlement();
        if (path != null) {
            travelToTarget("AI idler", path.getLastNode().getTile());
        } else { // Just make a random move if no target can be found.
            moveRandomly();
            logger.finest("AI idler wandered randomly: " + unit);
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
