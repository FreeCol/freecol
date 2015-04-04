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
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.WorkerWish;


/**
 * Mission for realizing a <code>Wish</code>.
 */
public class WishRealizationMission extends Mission {

    private static final Logger logger = Logger.getLogger(WishRealizationMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI wisher";

    /** The wish to be realized. */
    private Wish wish;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param wish The <code>Wish</code> which will be realized by
     *     the unit and this mission.
     */
    public WishRealizationMission(AIMain aiMain, AIUnit aiUnit, Wish wish) {
        super(aiMain, aiUnit, wish.getDestination());

        this.wish = wish;
        wish.setTransportable(aiUnit);
    }

    /**
     * Creates a new <code>WishRealizationMission</code> and reads the
     * given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public WishRealizationMission(AIMain aiMain, AIUnit aiUnit,
                                  FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Get the wish handled by this mission.
     *
     * @return The mission <code>Wish</code>.
     */
    public Wish getWish() {
        return this.wish;
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
            : ((reason = invalidTargetReason(loc,
                        aiUnit.getUnit().getOwner())) != null) ? reason
            : null;
    }


    // Implement Mission
    //   Inherit getTransportDestination, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        if (this.wish != null) {
            this.wish.setTransportable(null);
            this.wish = null;
        }
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseTransportPriority() {
        return NORMAL_TRANSPORT_PRIORITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTarget() {
        return (this.wish == null) ? null : this.wish.getDestination();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTarget(Location target) {
        // Ignored, target is set by wish
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        return getTarget();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        return (this.wish == null) ? "wish-null"
            : invalidReason(getAIUnit(), getTarget());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        String reason = invalidReason();
        if (reason != null) return lbFail(lb, false, reason);

        // Move towards the target.
        final Unit unit = getUnit();
        Location target = getTarget();
        Unit.MoveType mt = travelToTarget(target,
            CostDeciders.avoidSettlementsAndBlockingUnits(), lb);
        switch (mt) {
        case MOVE: // Arrived
            break;

        case MOVE_HIGH_SEAS: case MOVE_NO_MOVES:
        case MOVE_NO_REPAIR: case MOVE_ILLEGAL:
            return lbWait(lb);

        case MOVE_NO_ACCESS_EMBARK: case MOVE_NO_TILE:
            return this;

        default:
            return lbMove(lb, mt);
        }

        lbAt(lb);
        if (target instanceof Colony) {
            final AIMain aiMain = getAIMain();
            final Colony colony = (Colony)target;
            final AIUnit aiUnit = getAIUnit();
            final AIColony aiColony = aiMain.getAIColony(colony);
            final EuropeanAIPlayer owner = getEuropeanAIPlayer();
            aiColony.completeWish(wish, unit.toShortString(), lb);
            // Replace the mission, with a defensive one if this is a
            // military unit or a simple working one if not.
            if (unit.getType().isOffensive()) {
                if (owner.getDefendSettlementMission(aiUnit, colony) != null) {
                    lbDone(lb, true, "ready to defend, ", colony);
                } else {
                    lbFail(lb, true, "unable to defend");
                }
            } else {                
                aiColony.requestRearrange();
                if (owner.getWorkInsideColonyMission(aiUnit, aiColony)!=null) {
                    lbDone(lb, true, "ready to work ", colony);
                } else {
                    lbFail(lb, true, "unable to work");
                }
            }
        } else {
            lbFail(lb, true, "broken wish ", wish);
        }

        this.wish = null; // completeWish disposes
        return lbDrop(lb);
    }


    // Serialization

    private static final String WISH_TAG = "wish";
    // @compat 0.10.3
    private static final String OLD_GOODS_WISH_TAG = "GoodsWish";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(WISH_TAG, wish);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();

        final String wid = xr.getAttribute(WISH_TAG, (String)null);
        wish = xr.getAttribute(aiMain, WISH_TAG, Wish.class, (Wish)null);
        if (wish == null) {
            if (wid.startsWith(GoodsWish.getXMLElementTagName())
                // @compat 0.10.3
                || wid.startsWith(OLD_GOODS_WISH_TAG)
                // end @compat
                ) {
                wish = new GoodsWish(aiMain, wid);

            } else if (wid.startsWith(WorkerWish.getXMLElementTagName())) {
                wish = new WorkerWish(aiMain, wid);

            } else {
                throw new XMLStreamException("Unknown wish tag: " + wid);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return The <code>String</code> "wishRealizationMission".
     */
    public static String getXMLElementTagName() {
        return "wishRealizationMission";
    }
}
