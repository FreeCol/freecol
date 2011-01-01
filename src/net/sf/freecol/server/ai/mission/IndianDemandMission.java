/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.LoadCargoMessage;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
 * Mission for demanding goods from a specified player.
 */
public class IndianDemandMission extends Mission {

    private static final Logger logger = Logger.getLogger(IndianDemandMission.class.getName());

    /** The <code>Colony</code> receiving the demand. */
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

        if (hasTribute()) {
            if (getUnit().getTile() != getUnit().getIndianSettlement().getTile()) {
                // Move to the owning settlement:
                Direction r = moveTowards(connection, getUnit().getIndianSettlement().getTile());
                moveButDontAttack(connection, r);
            } else {
                // Unload the goods
                GoodsContainer container = getUnit().getGoodsContainer();
                IndianSettlement is = getUnit().getIndianSettlement();
                for (Goods goods : container.getCompactGoods()) {
                    Goods tribute = container.removeGoods(goods.getType());
                    is.addGoods(tribute);
                }
                logger.info("IndianDemand for " + getUnit().getId()
                            + " complete, tribute unloaded at " + is.getName());
                completed = true;
            }
        } else {
            // Move to the target's colony and demand
            Unit unit = getUnit();
            Direction r = moveTowards(connection, target.getTile());
            if (r != null &&
                unit.getTile().getNeighbourOrNull(r) == target.getTile()
                && unit.getMovesLeft() > 0) {
                // We have arrived.
                Player enemy = target.getOwner();
                Goods goods = selectGoods(target);
                int gold = 0;
                int oldGoods = (goods == null) ? 0
                    : unit.getGoodsContainer().getGoodsCount(goods.getType());
                int oldGold = unit.getOwner().getGold();
                if (goods == null) {
                    if (enemy.getGold() <= 0) {
                        completed = true;
                        return;
                    }
                    gold = enemy.getGold() / 20;
                }
                AIMessage.askIndianDemand(getAIUnit(), target, goods, gold);

                int unitTension = (unit.getIndianSettlement() == null) ? 0
                    : unit.getIndianSettlement().getAlarm(enemy).getValue();
                unitTension = Math.max(unitTension,
                        unit.getOwner().getTension(enemy).getValue());
                boolean accepted = (goods != null
                    && unit.getGoodsContainer().getGoodsCount(goods.getType())
                                    > oldGoods)
                    || (gold > 0 && unit.getOwner().getGold() > oldGold);
                if (accepted) {
                    String tribute = (goods != null) ? goods.toString()
                        : (Integer.toString(gold) + " gold");
                    logger.info("IndianDemand for " + getUnit().getId()
                                + " accepted, tribute " + tribute);
                } else {
                    logger.info("IndianDemand for " + getUnit().getId()
                                + " complete/refused at " + target.getName());
                    // If not content and we didn't get what we
                    // wanted then attack.
                    if (unitTension >= Tension.Level.CONTENT.getLimit()) {
                        AIMessage.askAttack(getAIUnit(), r);
                    }
                    completed = true;
                }
            }
        }

        // Walk in a random direction if we have any moves left:
        moveRandomly(connection);
    }

    /**
     * Selects the most desirable goods from the colony.
     * 
     * @param target The colony.
     * @return The goods to demand.
     */
    public Goods selectGoods(Colony target) {
        Tension.Level tension = getUnit().getOwner().getTension(target.getOwner()).getLevel();
        int dx = getAIMain().getGame().getSpecification().getIntegerOption("model.option.nativeDemands")
            .getValue() + 1;
        GoodsType food = getAIMain().getGame().getSpecification().getPrimaryFoodType();
        Goods goods = null;
        GoodsContainer warehouse = target.getGoodsContainer();
        if (tension.compareTo(Tension.Level.CONTENT) <= 0 &&
            warehouse.getGoodsCount(food) >= 100) {
            int amount = (warehouse.getGoodsCount(food) * dx) / 6;
            if (amount > 0) {
                return new Goods(getGame(), target, food, capAmount(amount, dx));
            }
        } else if (tension.compareTo(Tension.Level.DISPLEASED) <= 0) {
            Market market = target.getOwner().getMarket();
            int value = 0;
            List<Goods> warehouseGoods = warehouse.getCompactGoods();
            for (Goods currentGoods : warehouseGoods) {
                int goodsValue = market.getSalePrice(currentGoods);
                if (currentGoods.getType().isFoodType() ||
                    currentGoods.getType().isMilitaryGoods()) {
                    continue;
                } else if (goodsValue > value) {
                    value = goodsValue;
                    goods = currentGoods;
                }
            }
            if (goods != null) {
                goods.setAmount(capAmount(goods.getAmount(), dx));
                return goods;
            }
        } else {
            // military goods
            for (GoodsType preferred : getAIMain().getGame().getSpecification().getGoodsTypeList()) {
                if (preferred.isMilitaryGoods()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, capAmount(amount, dx));
                    }
                }
            }
            // storable building materials (what do the natives need tools for?)
            for (GoodsType preferred : getAIMain().getGame().getSpecification().getGoodsTypeList()) {
                if (preferred.isBuildingMaterial() && preferred.isStorable()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, capAmount(amount, dx));
                    }
                }
            }
            // trade goods
            for (GoodsType preferred : getAIMain().getGame().getSpecification().getGoodsTypeList()) {
                if (preferred.isTradeGoods()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, capAmount(amount, dx));
                    }
                }
            }
            // refined goods
            for (GoodsType preferred : getAIMain().getGame().getSpecification().getGoodsTypeList()) {
                if (preferred.isRefined() && preferred.isStorable()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, capAmount(amount, dx));
                    }
                }
            }
        }

        // haven't found what we want
        Market market = target.getOwner().getMarket();
        int value = 0;
        List<Goods> warehouseGoods = warehouse.getCompactGoods();
        for (Goods currentGoods : warehouseGoods) {
            int goodsValue = market.getSalePrice(currentGoods);
            if (goodsValue > value) {
                value = goodsValue;
                goods = currentGoods;
            }
        }
        if (goods != null) {
            goods.setAmount(capAmount(goods.getAmount(), dx));
        }
        return goods;
    }

    private int capAmount(int amount, int difficulty) {
        int finalAmount = Math.max((amount * difficulty) / 6, 1);
        // natives can only carry one load of goods
        finalAmount = Math.min(finalAmount, 100);
        return finalAmount;
    }

    /**
     * Checks if the unit is carrying a gift (goods).
     * 
     * @return <i>true</i> if <code>getUnit().getSpaceLeft() == 0</code> and
     *         false otherwise.
     */
    private boolean hasTribute() {
        return (getUnit().getSpaceLeft() == 0);
    }

    /**
     * Checks if this mission is still valid to perform.
     * This mission will be invalidated when complete, if the home settlement
     * is gone, the target is gone, or tension reduces to happy.
     * 
     * @return <code>true</code> if this mission is still valid.
     */
    public boolean isValid() {
        return !completed && getUnit().getIndianSettlement() != null
            && (hasTribute()
                || (target != null && !target.isDisposed()
                    && target.getTile().getColony() == target
                    && getUnit().getOwner().getTension(target.getOwner())
                        .getLevel().compareTo(Tension.Level.HAPPY) <= 0));
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
        if (target != null) {
            out.writeAttribute("target", target.getId());
        }
        out.writeAttribute("completed", Boolean.toString(completed));
        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *             from the stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        String unitString = in.getAttributeValue(null, "unit");
        setAIUnit((AIUnit) getAIMain().getAIObject(unitString));
        String targetString = in.getAttributeValue(null, "target");
        target = (targetString == null) ? null
            : (Colony) getGame().getFreeColGameObject(targetString);
        String completedString = in.getAttributeValue(null, "completed");
        completed = Boolean.valueOf(completedString).booleanValue();
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
        if (getUnit().getIndianSettlement() == null) {
            return "invalid";
        }
        final String targetName = (target != null) ? target.getName() : "null";
        if (!hasTribute()) {
            return "[" + targetName + "] Getting tribute: "
                + getUnit().getIndianSettlement().getTile().getPosition();
        } else {
            return "[" + targetName + "] "
                + getUnit().getGoodsIterator().next().getNameKey();
        }
    }
}
