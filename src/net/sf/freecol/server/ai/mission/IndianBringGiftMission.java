/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.LoadCargoMessage;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;

/**
 * Mission for bringing a gift to a specified player.
 * 
 * <BR>
 * <BR>
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




    /** The <code>Colony</code> receiving the gift. */
    private Colony target;

    /** Decides whether this mission has been completed or not. */
    private boolean giftDelivered;


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
        this.giftDelivered = false;

        if (!getUnit().getOwner().isIndian() || !getUnit().canCarryGoods()) {
            logger.warning("Only an indian which can carry goods can be given the mission: IndianBringGiftMission");
            throw new IllegalArgumentException("Only an indian which can carry goods can be given the mission: IndianBringGiftMission");
        }
    }

    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public IndianBringGiftMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>IndianBringGiftMission</code> and reads the given
     * element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see AIObject#readFromXML
     */
    public IndianBringGiftMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }

    /**
     * Performs the mission.
     * 
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        
        if (!isValid()) {
            // the destination colony may have been destroyed
            return;
        }
        
        if (!hasGift()) {
            if (getUnit().getTile() != getUnit().getIndianSettlement().getTile()) {
                // Move to the owning settlement:
                Direction r = moveTowards(connection, getUnit().getIndianSettlement().getTile());
                moveButDontAttack(connection, r);
            } else {
                // Load the goods:
                List<Goods> goodsList = new ArrayList<Goods>();
                GoodsContainer gc = getUnit().getIndianSettlement().getGoodsContainer();
                for (GoodsType goodsType : FreeCol.getSpecification().getNewWorldGoodsTypeList()) {
                    if (gc.getGoodsCount(goodsType) >= IndianSettlement.KEEP_RAW_MATERIAL + 25) {
                        goodsList.add(new Goods(getGame(), getUnit().getIndianSettlement(),
                                                goodsType,
                                                getPseudoRandom().nextInt(15) + 10));
                    }
                }

                if (goodsList.size() > 0) {
                    Goods goods = goodsList.get(getPseudoRandom().nextInt(goodsList.size()));
                    LoadCargoMessage message = new LoadCargoMessage(goods, getUnit());
                    try {
                        connection.sendAndWait(message.toXMLElement());
                    } catch (IOException e) {
                        logger.warning("Could not send \"loadCargo\"-message!");
                    }
                }
            }
        } else {
            // Move to the target's colony and deliver
            Direction r = moveTowards(connection, target.getTile());
            if (r != null && 
                getUnit().getTile().getNeighbourOrNull(r) == target.getTile()) {
                // We have arrived.
                DeliverGiftMessage message = new DeliverGiftMessage(getUnit(), target, getUnit().getGoodsIterator().next());
                try {
                    connection.sendAndWait(message.toXMLElement());
                } catch (IOException e) {
                    logger.warning("Could not send \"deliverGift\"-message!");
                }

                giftDelivered = true;
                getUnit().getOwner().modifyTension(target.getOwner(), 1);
            }
        }

        // Walk in a random direction if we have any moves left:
        moveRandomly(connection);
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

    /**
     * Checks if this mission is still valid to perform.
     * 
     * <BR>
     * <BR>
     * 
     * This mission will be invalidated when the gift has been delivered. In
     * case of the stances {@link Stance#WAR WAR} or
     * {@link Stance#CEASE_FIRE CEASE_FIRE} towards the target player, the
     * mission would be invalidated as well.
     * 
     * @return <code>true</code> if this mission is still valid.
     */
    public boolean isValid() {
        return target != null && !target.isDisposed() && target.getTile().getColony() == target && !giftDelivered
            && isValidMission(getUnit().getOwner(), target.getOwner()) && getUnit().getIndianSettlement() != null;
    }

    /**
     * Checks if the player <code>owner</code> can bring a gift to the
     * <code>targetPlayer</code>.
     * 
     * @param owner The owner of this mission.
     * @param targetPlayer The target of the gift.
     * @return <code>true</code> if this mission is still valid to perform
     *         with regard to the tension towards the target player.
     */
    public static boolean isValidMission(Player owner, Player targetPlayer) {
        switch (owner.getStance(targetPlayer)) {
        case UNCONTACTED: case WAR: case CEASE_FIRE:
            break;
        case PEACE: case ALLIANCE:
            return owner.getTension(targetPlayer).getLevel() != null
                && owner.getTension(targetPlayer).getLevel().compareTo(Tension.Level.HAPPY) <= 0;
        }
        return false;
    }

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("unit", getUnit().getId());
        if (target!=null) {
            // the destination colony is still alive
            out.writeAttribute("target", target.getId());
        }
        out.writeAttribute("giftDelivered", Boolean.toString(giftDelivered));

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * 
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));

        target = (Colony) getGame().getFreeColGameObject(in.getAttributeValue(null, "target"));
        giftDelivered = Boolean.valueOf(in.getAttributeValue(null, "giftDelivered")).booleanValue();

        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return The <code>String</code> "indianBringGiftMission".
     */
    public static String getXMLElementTagName() {
        return "indianBringGiftMission";
    }

    /**
     * Gets debugging information about this mission. This string is a short
     * representation of this object's state.
     * 
     * @return The <code>String</code>: "[ColonyName] GIFT_TYPE" or
     *         "[ColonyName] Getting gift: (x, y)".
     */
    public String getDebuggingInfo() {
        if (!hasGift()) {
            return "[" + target.getName() + "] Getting gift: "
                    + getUnit().getIndianSettlement().getTile().getPosition();
        } else {
            return "[" + target.getName() + "] " + getUnit().getGoodsIterator().next().getNameKey();
        }
    }
}
