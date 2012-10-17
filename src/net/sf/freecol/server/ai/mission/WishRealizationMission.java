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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.WorkerWish;


/**
 * Mission for realizing a <code>Wish</code>.
 */
public class WishRealizationMission extends Mission {

    private static final Logger logger = Logger.getLogger(WishRealizationMission.class.getName());

    private static final String tag = "AI wisher";

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
        logger.finest(tag + " starting with destination "
            + wish.getDestination() + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>WishRealizationMission</code> and reads the
     * given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public WishRealizationMission(AIMain aiMain, AIUnit aiUnit,
                                  XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, aiUnit);

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
     * {@inheritDoc}
     */
    public Location getTransportDestination() {
        Tile tile = (wish == null || wish.getDestination() == null) ? null
            : wish.getDestination().getTile();
        return (getUnit().shouldTakeTransportTo(tile)) ? tile : null;
    }


    // Mission interface

    /**
     * Gets the target of this mission.
     *
     * @return The target of this mission, or null if none.
     */
    public Location getTarget() {
        return (wish == null) ? null : wish.getDestination();
    }

    /**
     * Why is this mission invalid?
     *
     * @return A reason for mission invalidity, or null if none found.
     */
    public String invalidReason() {
        return (wish == null) ? "wish-null"
            : invalidReason(getAIUnit(), getTarget());
    }

    // Omitted invalidReason(AIUnit), not needed

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
            : ((reason = invalidTargetReason(loc,
                        aiUnit.getUnit().getOwner())) != null) ? reason
            : null;
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * Performs this mission.
     */
    public void doMission() {
        final Unit unit = getUnit();
        String reason = invalidReason();
        if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        // Move towards the target.
        Location target = getTarget();
        if (travelToTarget(tag, target,
                           CostDeciders.avoidSettlementsAndBlockingUnits())
            != Unit.MoveType.MOVE) return;

        if (target instanceof Colony) {
            final Colony colony = (Colony)target;
            final AIUnit aiUnit = getAIUnit();
            final AIColony aiColony = getAIMain().getAIColony(colony);
            aiColony.completeWish(wish, "mission(" + unit + ")");
            logger.finest(tag + " completed at " + colony + ": " + this);
            // Replace the mission, with a defensive one if this is a
            // military unit or a simple working one if not.  Beware
            // that setMission() will dispose of this mission which is
            // why this is done last.
            if (unit.getType().isOffensive()) {
                aiUnit.setMission(new DefendSettlementMission(getAIMain(),
                                  aiUnit, colony));
            } else {                
                aiColony.requestRearrange();
                aiUnit.setMission(new WorkInsideColonyMission(getAIMain(),
                                  aiUnit, aiColony));
            }
        } else {
            logger.warning(tag + " unknown destination type " + wish
                + ": " + this);
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
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("wish", wish.getId());
    }

    /**
     * {@inheritDoc}
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
