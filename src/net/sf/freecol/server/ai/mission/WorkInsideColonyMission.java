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

import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
 * Mission for working inside an AI colony.
 */
public class WorkInsideColonyMission extends Mission {

    private static final Logger logger = Logger.getLogger(WorkInsideColonyMission.class.getName());

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
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public WorkInsideColonyMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
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
     * Gets the destination for units with this mission.
     *
     * @return Usually the colony tile unless the unit is there
     *         already or can get there itself.
     */
    public Tile getTransportDestination() {
        final Tile colonyTile = aiColony.getColony().getTile();
        return (shouldTakeTransportToTile(colonyTile))
            ? colonyTile
            : null;
    }

    // Mission interface

    /**
     * Checks if this mission is still valid to perform.
     *
     * @return True if this mission is still valid to perform.
     */
    public boolean isValid() {
        return super.isValid()
            && getUnit().isPerson()
            && aiColony != null
            && aiColony.getColony() != null
            && !aiColony.getColony().isDisposed()
            && aiColony.getColony().getOwner() == getUnit().getOwner();
    }

    /**
     * Performs this mission.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed() || !isValid()) {
            logger.finest("AI worker broken: " + unit);
            return;
        }

        travelToTarget("AI worker", aiColony.getColony().getTile());
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
        if (isValid()) {
            toXML(out, getXMLElementTagName());
        }
    }

    /**
     * {@inherit-doc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("colony", aiColony.getId());
    }

    /**
     * {@inherit-doc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        aiColony = (AIColony) getAIMain()
            .getAIObject(in.getAttributeValue(null, "colony"));
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "workInsideColonyMission".
     */
    public static String getXMLElementTagName() {
        return "workInsideColonyMission";
    }
}
