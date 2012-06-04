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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for bringing a gift to a specified player.
 *
 * The mission has three different tasks to perform:
 * <ol>
 * <li>Get the gift (goods) from the {@link IndianSettlement} that owns the
 * unit.
 * <li>Transport this gift to the given {@link Colony}.
 * <li>Complete the mission by delivering the gift.
 * </ol>
 */
public class IndianBringGiftMission extends Mission {

    private static final Logger logger = Logger.getLogger(IndianBringGiftMission.class.getName());

    private static final String tag = "AI native gifter";

    /** The <code>Colony</code> receiving the gift. */
    private Colony target;

    /** Decides whether this mission has been completed or not. */
    private boolean completed;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The <code>Colony</code> receiving the gift.
     */
    public IndianBringGiftMission(AIMain aiMain, AIUnit aiUnit, Colony target) {
        super(aiMain, aiUnit);

        this.target = target;
        this.completed = false;

        if (!getUnit().getOwner().isIndian() || !getUnit().canCarryGoods()) {
            throw new IllegalArgumentException("Only an indian which can carry goods can be given the mission: IndianBringGiftMission");
        }
        uninitialized = false;
    }

    /**
     * Creates a new <code>IndianBringGiftMission</code> and reads the given
     * element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public IndianBringGiftMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    // Mission interface

    /**
     * Gets the mission target.
     *
     * @return The target <code>Colony</code>.
     */
    public Location getTarget() {
        return target;
    }

    /**
     * Why would an IndianBringGiftMission be invalid with the given unit?
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidGiftReason(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        return (unit.getIndianSettlement() == null) ? "home-destroyed"
            : (unit.getTile() == null) ? Mission.UNITNOTONMAP
            : null;
    }

    /**
     * Why would an IndianBringGiftMission be invalid with the given
     * unit and colony.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param colony The <code>Colony</code> to test.
     * @return A reason why the mission would be invalid with the unit
     *     and colony or null if none found.
     */
    private static String invalidGiftColonyReason(AIUnit aiUnit,
                                                  Colony colony) {
        final Unit unit = aiUnit.getUnit();
        final Player owner = unit.getOwner();
        Player targetPlayer = colony.getOwner();
        switch (owner.getStance(targetPlayer)) {
        case UNCONTACTED: case WAR: case CEASE_FIRE:
            return "bad-stance";
        case PEACE: case ALLIANCE:
            Tension tension = unit.getIndianSettlement()
                .getAlarm(targetPlayer);
            if (tension != null && tension.getLevel()
                .compareTo(Tension.Level.HAPPY) > 0) return "unhappy";
        }
        return null;
    }

    /**
     * Why is this mission invalid?
     *
     * @return A reason for mission invalidity, or null if none found.
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), target);
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        String reason;
        return ((reason = Mission.invalidReason(aiUnit)) != null) ? reason
            : ((reason = invalidGiftReason(aiUnit)) != null) ? reason
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
            : ((reason = invalidGiftReason(aiUnit)) != null) ? reason
            : (loc instanceof Colony)
            ? (((reason = invalidTargetReason(loc, null)) != null) ? reason
                : ((reason = invalidGiftColonyReason(aiUnit, (Colony)loc))
                    != null) ? reason : null)
            : Mission.TARGETINVALID;
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * Performs the mission.
     */
    public void doMission() {
        final Unit unit = getUnit();
        String reason = invalidReason();
        if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        if (!hasGift()) {
            if (getUnit().getTile() != getUnit().getIndianSettlement().getTile()) {
                // Move to the owning settlement:
                Direction r = moveTowards(getUnit().getIndianSettlement().getTile());
                if (r == null || !moveButDontAttack(r)) return;
            } else {
                IndianSettlement is = getUnit().getIndianSettlement();
                // Load the goods:
                List<Goods> goodsList = new ArrayList<Goods>();
                GoodsContainer gc = is.getGoodsContainer();
                for (GoodsType goodsType : getSpecification().getNewWorldGoodsTypeList()) {
                    if (gc.getGoodsCount(goodsType) >= IndianSettlement.KEEP_RAW_MATERIAL + 25) {
                        Goods goods = new Goods(getGame(), is, goodsType,
                            Utils.randomInt(logger, "Gift amount",
                                getAIRandom(), 15) + 10);
                        goodsList.add(goods);
                    }
                }

                if (goodsList.size() == 0) {
                    completed = true;
                } else {
                    Goods goods = goodsList.get(Utils.randomInt(logger,
                            "Gift amount", getAIRandom(), goodsList.size()));
                    AIMessage.askLoadCargo(getAIUnit(), goods);
                }
            }
        } else {
            // Move to the target's colony and deliver
            Direction r = moveTowards(target.getTile());
            if (r != null
                && getUnit().getTile().getNeighbourOrNull(r) == target.getTile()) {
                // We have arrived.
                if (AIMessage.askGetTransaction(getAIUnit(), target)
                    && AIMessage.askDeliverGift(getAIUnit(), target,
                                                getUnit().getGoodsIterator().next())) {
                    AIMessage.askCloseTransaction(getAIUnit(), target);
                    logger.finest(tag + " completed at " + target.getName()
                        + ": " + this);
                } else {
                    logger.warning(tag + " failed at " + target.getName()
                        + ": " + this);
                }
                completed = true;
            }
        }

        // Walk in a random direction if we have any moves left:
        moveRandomly(tag, null);
    }

    /**
     * Checks if the unit is carrying a gift (goods).
     *
     * @return <i>true</i> if <code>getUnit().getSpaceLeft() == 0</code> and
     *         false otherwise.
     */
    private boolean hasGift() {
        return (getUnit().getSpaceLeft() == 0);
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
        toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        if (target != null) {
            out.writeAttribute("target", target.getId());
        }

        out.writeAttribute("completed", Boolean.toString(completed));
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *             from the stream.
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        String str = in.getAttributeValue(null, "target");
        target = getGame().getFreeColGameObject(str, Colony.class);

        str = in.getAttributeValue(null, "completed");
        // @compat 0.9.x
        if (str == null) str = in.getAttributeValue(null, "giftDelivered");
        // end compatibility code
        completed = Boolean.valueOf(str).booleanValue();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "indianBringGiftMission".
     */
    public static String getXMLElementTagName() {
        return "indianBringGiftMission";
    }
}
