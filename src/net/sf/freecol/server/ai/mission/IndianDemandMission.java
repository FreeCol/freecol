package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;

/**
 * Mission for demanding goods from a specified player.
 */
public class IndianDemandMission extends Mission {
    private static final Logger logger = Logger.getLogger(IndianDemandMission.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    /** The <code>Colony</code> receiving the gift. */
    private Colony target;

    /** Whether this mission has been completed or not. */
    private boolean completed;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The <code>Colony</code> receiving the gift.
     */
    public IndianDemandMission(AIMain aiMain, AIUnit aiUnit, Colony target) {
        super(aiMain, aiUnit);

        this.target = target;

        if (getUnit().getType() != Unit.BRAVE) {
            logger.warning("Only an indian brave can be given the mission: IndianDemandMission");
            throw new IllegalArgumentException("Only an indian brave can be given the mission: IndianDemandMission");
        }
    }

    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public IndianDemandMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>IndianDemandMission</code> and reads the given
     * element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see AIObject#readFromXML
     */
    public IndianDemandMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
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
            return;
        }

        if (!hasGift()) {
            if (getUnit().getTile() != getUnit().getIndianSettlement().getTile()) {
                // Move to the owning settlement:
                int r = moveTowards(connection, getUnit().getIndianSettlement().getTile());
                if (r >= 0 && getUnit().getMoveType(r) == Unit.MOVE) {
                    move(connection, r);
                }
            } else {
                // Load the goods:
                ArrayList<Goods> goodsList = new ArrayList<Goods>();
                GoodsContainer gc = getUnit().getIndianSettlement().getGoodsContainer();
                for (int i = 1; i <= 4; i++) {
                    if (gc.getGoodsCount(i) >= IndianSettlement.KEEP_RAW_MATERIAL + 25) {
                        goodsList.add(new Goods(getGame(), getUnit().getIndianSettlement(), i,
                                getRandom().nextInt(15) + 10));
                    }
                }

                if (goodsList.size() > 0) {
                    Goods goods = goodsList.get(getRandom().nextInt(goodsList.size()));
                    goods.setLocation(getUnit());
                }
            }
        } else {
            // Move to the target's colony and deliver
            Unit unit = getUnit();
            int r = moveTowards(connection, target.getTile());
            if (r >= 0 && getGame().getMap().getNeighbourOrNull(r, unit.getTile()) == target.getTile()
                    && unit.getMovesLeft() > 0) {
                // We have arrived.
                Element demandElement = Message.createNewRootElement("indianDemand");
                demandElement.setAttribute("unit", unit.getID());
                demandElement.setAttribute("colony", target.getID());

                Player enemy = target.getOwner();
                Goods goods = selectGoods(target);
                if (goods == null) {
                    demandElement.setAttribute("gold", String.valueOf(enemy.getGold() / 20));
                } else {
                    demandElement.appendChild(goods.toXMLElement(null, demandElement.getOwnerDocument()));
                }
                if (!unit.isVisibleTo(enemy)) {
                    demandElement.appendChild(unit.toXMLElement(enemy, demandElement.getOwnerDocument()));
                }

                Element reply;
                try {
                    reply = connection.ask(demandElement);
                } catch (IOException e) {
                    logger.warning("Could not send \"demand\"-message!");
                    completed = true;
                    return;
                }

                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                int tension = 0;
                int unitTension = unit.getOwner().getTension(enemy).getValue();
                if (unit.getIndianSettlement() != null) {
                    unitTension += unit.getIndianSettlement().getOwner().getTension(enemy).getValue();
                }
                if (accepted) {
                    // TODO: if very happy, the brave should convert
                    tension = -(5 - enemy.getDifficulty()) * 50;
                    unit.getOwner().modifyTension(enemy, tension);
                    if (unitTension <= Tension.TENSION_HAPPY && (goods == null || goods.getType() == Goods.FOOD)) {
                        Element deliverGiftElement = Message.createNewRootElement("deliverGift");
                        deliverGiftElement.setAttribute("unit", getUnit().getID());
                        deliverGiftElement.setAttribute("settlement", target.getID());
                        deliverGiftElement.appendChild(getUnit().getGoodsIterator().next().toXMLElement(null,
                                deliverGiftElement.getOwnerDocument()));

                        try {
                            connection.sendAndWait(deliverGiftElement);
                        } catch (IOException e) {
                            logger.warning("Could not send \"deliverGift\"-message!");
                        }
                    }
                } else {
                    tension = (enemy.getDifficulty() + 1) * 50;
                    unit.getOwner().modifyTension(enemy, tension);
                    if (unitTension >= Tension.TENSION_CONTENT) {
                        // if we didn't get what we wanted, attack
                        Element element = Message.createNewRootElement("attack");
                        element.setAttribute("unit", unit.getID());
                        element.setAttribute("direction", Integer.toString(r));

                        try {
                            connection.ask(element);
                        } catch (IOException e) {
                            logger.warning("Could not send message!");
                        }
                    }
                }
                completed = true;
            }
        }

        // Walk in a random direction if we have any moves left:
        Tile thisTile = getUnit().getTile();
        Unit unit = getUnit();
        while (unit.getMovesLeft() > 0) {
            int direction = (int) (Math.random() * 8);
            int j;
            for (j = 8; j > 0
                    && ((unit.getGame().getMap().getNeighbourOrNull(direction, thisTile) == null) || (unit
                            .getMoveType(direction) != Unit.MOVE)); j--) {
                direction = (int) (Math.random() * 8);
            }
            if (j == 0)
                break;
            thisTile = unit.getGame().getMap().getNeighbourOrNull(direction, thisTile);

            Element moveElement = Message.createNewRootElement("move");
            moveElement.setAttribute("unit", unit.getID());
            moveElement.setAttribute("direction", Integer.toString(direction));

            try {
                connection.sendAndWait(moveElement);
            } catch (IOException e) {
                logger.warning("Could not send \"move\"-message!");
            }
        }
    }

    /**
     * Selects the most desirable goods from the colony.
     * 
     * @param target The colony.
     * @return The goods to demand.
     */
    public Goods selectGoods(Colony target) {
        int tension = getUnit().getOwner().getTension(target.getOwner()).getLevel();
        int dx = target.getOwner().getDifficulty() + 1;
        Goods goods = null;
        GoodsContainer warehouse = target.getGoodsContainer();
        if (tension <= Tension.CONTENT && warehouse.getGoodsCount(Goods.FOOD) >= 100) {
            goods = new Goods(getGame(), target, Goods.FOOD, (warehouse.getGoodsCount(Goods.FOOD) * dx) / 6);
        } else if (tension <= Tension.DISPLEASED) {
            Market market = getGame().getMarket();
            int value = 0;
            Iterator iterator = warehouse.getCompactGoodsIterator();
            while (iterator.hasNext()) {
                Goods currentGoods = (Goods) iterator.next();
                int goodsValue = market.getSalePrice(currentGoods);
                if (currentGoods.getType() == Goods.FOOD || currentGoods.getType() == Goods.HORSES
                        || currentGoods.getType() == Goods.MUSKETS) {
                    continue;
                } else if (goodsValue > value) {
                    value = goodsValue;
                    goods = currentGoods;
                }
            }
            if (goods != null) {
                goods.setAmount(Math.max((goods.getAmount() * dx) / 6, 1));
            }
        } else {
            int[] preferred = new int[] { Goods.MUSKETS, Goods.HORSES, Goods.TOOLS, Goods.TRADE_GOODS, Goods.RUM,
                    Goods.CLOTH, Goods.COATS, Goods.CIGARS };
            for (int i = 0; i < preferred.length; i++) {
                int amount = warehouse.getGoodsCount(preferred[i]);
                if (amount > 0) {
                    goods = new Goods(getGame(), target, preferred[i], Math.max((amount * dx) / 6, 1));
                    break;
                }
            }
        }
        return goods;
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
     * This mission will be invalidated when the demand has been delivered.
     * 
     * @return <code>true</code> if this mission is still valid.
     */
    public boolean isValid() {
        // The last check is to ensure that the colony have not been burned to
        // the ground.
        return (!completed && target != null && !target.isDisposed() && target.getTile().getColony() == target);
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

        out.writeAttribute("unit", getUnit().getID());
        out.writeAttribute("target", target.getID());
        out.writeAttribute("completed", Boolean.toString(completed));

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
        completed = Boolean.valueOf(in.getAttributeValue(null, "completed")).booleanValue();

        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return The <code>String</code> "indianDemandMission".
     */
    public static String getXMLElementTagName() {
        return "indianDemandMission";
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
            return "[" + target.getName() + "] " + getUnit().getGoodsIterator().next().getName();
        }
    }
}
