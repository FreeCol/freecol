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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.WorkerWish;

import org.w3c.dom.Element;


/**
 * Mission for realizing a <code>Wish</code>.
 */
public class WishRealizationMission extends Mission {

    private static final Logger logger = Logger.getLogger(WishRealizationMission.class.getName());

    /** The wish to be realized. */
    private Wish wish;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     * @param wish The <code>Wish</code> which will be realized by
     *        the unit and this mission.
     */
    public WishRealizationMission(AIMain aiMain, AIUnit aiUnit, Wish wish) {
        super(aiMain, aiUnit);

        this.wish = wish;
        logger.finest("AI wish unit starting"
            + " with destination " + wish.getDestination()
            + ": " + aiUnit.getUnit());
        uninitialized = false;
    }

    /**
     * Creates a new <code>WishRealizationMission</code> and reads the
     * given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public WishRealizationMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Disposes this <code>Mission</code>.
     */
    public void dispose() {
        if (wish != null) {
            wish.setTransportable(null);
            wish = null;
        }
        super.dispose();
    }


    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        Tile tile = (wish == null || wish.getDestination() == null) ? null
            : wish.getDestination().getTile();
        return (shouldTakeTransportToTile(tile)) ? tile : null;
    }

    // Mission interface

    /**
     * Checks if this mission is still valid to perform.
     *
     * @return True if this mission is still valid to perform.
     */
    public boolean isValid() {
        Location loc;
        return super.isValid()
            && wish != null
            && (loc = wish.getDestination()) != null
            && !((FreeColGameObject)loc).isDisposed()
            && !(loc instanceof Ownable
                && ((Ownable)loc).getOwner() != getUnit().getOwner());
    }

    /**
     * Performs this mission.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed() || !isValid()) {
            logger.warning("AI wish broken: " + unit);
            return;
        }

        // Move towards the target.
        if (travelToTarget("AI wish unit", wish.getDestination())
            != Unit.MoveType.MOVE) return;

        if (wish.getDestination() instanceof Colony) {
            final Colony colony = (Colony)wish.getDestination();
            final AIUnit aiUnit = getAIUnit();
            final AIColony aiColony = getAIMain().getAIColony(colony);
            aiColony.completeWish(wish, "mission(" + unit + ")");
            logger.finest("AI wish completed at " + colony
                + ": " + unit);
            // Replace the mission, with a defensive one if this is a
            // military unit or a simple working one if not.  Beware
            // that setMission() will dispose of this mission which is
            // why this is done last.
            if (unit.getType().getOffence() > UnitType.DEFAULT_OFFENCE) {
                aiUnit.setMission(new DefendSettlementMission(getAIMain(),
                                  aiUnit, colony));
            } else {                
                aiColony.requestRearrange();
                aiUnit.setMission(new WorkInsideColonyMission(getAIMain(),
                                  aiUnit, aiColony));
            }
        } else {
            logger.warning("AI wish unknown destination type " + wish
                + ": " + unit);
            wish.dispose();
            wish = null;
        }
    }

    // Serialization

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        if (wish.shouldBeStored()) {
            toXML(out, getXMLElementTagName());
        }
    }

    /**
     * {@inherit-doc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("wish", wish.getId());
    }

    /**
     * {@inherit-doc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        final String wid = in.getAttributeValue(null, "wish");
        wish = (Wish)getAIMain().getAIObject(wid);

        if (wish == null) {
            if (wid.startsWith(GoodsWish.getXMLElementTagName())
                // @compat 0.10.3
                || wid.startsWith("GoodsWish")
                // end compatibility code
                ) {
                wish = new GoodsWish(getAIMain(), wid);
            } else if (wid.startsWith(WorkerWish.getXMLElementTagName())) {
                wish = new WorkerWish(getAIMain(), wid);
            } else {
                logger.warning("Unknown type of Wish.");
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return The <code>String</code> "wishRealizationMission".
     */
    public static String getXMLElementTagName() {
        return "wishRealizationMission";
    }
}
