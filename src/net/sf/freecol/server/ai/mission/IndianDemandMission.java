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
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for demanding goods from a specified player.
 */
public class IndianDemandMission extends Mission {

    private static final Logger logger = Logger.getLogger(IndianDemandMission.class.getName());

    private static final String tag = "AI native demander";

    /** The <code>Colony</code> receiving the demand. */
    private Colony target;

    /** Whether this mission has been completed or not. */
    private boolean completed;

    /** Whether the demand has been made or not. */
    private boolean demanded;


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
            throw new IllegalArgumentException("Only an indian which can carry goods can be given the mission: IndianBringGiftMission");
        }
        uninitialized = false;
    }

    /**
     * Creates a new <code>IndianDemandMission</code> and reads the given
     * element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public IndianDemandMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Performs the mission.
     */
    public void doMission() {
        if (!isValid()) return;
        Unit unit = getUnit();

        if (hasTribute()) {
            if (unit.getTile() != unit.getIndianSettlement().getTile()) {
                // Move to the owning settlement:
                Direction r = moveTowards(unit.getIndianSettlement().getTile());
                if (r == null || !moveButDontAttack(r)) return;
            } else {
                // Unload the goods
                GoodsContainer container = unit.getGoodsContainer();
                IndianSettlement is = unit.getIndianSettlement();
                for (Goods goods : container.getCompactGoods()) {
                    Goods tribute = container.removeGoods(goods.getType());
                    is.addGoods(tribute);
                }
                logger.finest(tag + " completed unloading tribute at "
                    + is.getName() + ": " + unit);
                completed = true;
            }
        } else {
            // Move to the target's colony and demand
            moveTowards(target.getTile());
            if (unit.getTile().isAdjacent(target.getTile())
                && unit.getMovesLeft() > 0) {
                if (demanded) return; // doMission can be called multiple times
                // We have arrived.
                Player enemy = target.getOwner();
                Goods goods = selectGoods(target);
                int gold = 0;
                int oldGoods = (goods == null) ? 0
                    : unit.getGoodsContainer().getGoodsCount(goods.getType());
                final int oldGold = unit.getOwner().getGold();
                if (goods == null) {
                    if (!enemy.checkGold(1)) {
                        completed = true;
                        return;
                    }
                    gold = enemy.getGold() / 20;
                    if (gold == 0) gold = enemy.getGold();
                }
                demanded = true;
                AIUnit au = getAIUnit();
                boolean accepted = AIMessage.askIndianDemand(au, target,
                                                             goods, gold);
                // Drop the mission if there is no tribute to return
                // to the home settlement.
                Mission mission = au.getMission();
                if (mission instanceof IndianDemandMission
                    && !((IndianDemandMission)mission).hasTribute()) {
                    au.abortMission("completed demand");
                }
                if (accepted) {
                    logger.finest(tag + " demand "
                        + " accepted at " + target.getName()
                        + " tribute: " + ((goods != null) ? goods.toString()
                            : (Integer.toString(gold) + " gold"))
                        + ": " + unit);
                } else { // If the demand was rejected and not content, attack.
                    int unitTension = (unit.getIndianSettlement() == null) ? 0
                        : unit.getIndianSettlement().getAlarm(enemy).getValue();
                    int tension = Math.max(unitTension,
                        unit.getOwner().getTension(enemy).getValue());
                    if (tension >= Tension.Level.CONTENT.getLimit()) {
                        Direction d = unit.getTile()
                            .getDirection(target.getTile());
                        if (d != null) AIMessage.askAttack(au, d);
                    }
                    logger.finest(tag + " demand "
                        + " refused at " + target.getName()
                        + ": " + unit);
                }
                return;
            }
        }

        // Walk in a random direction if we have any moves left:
        moveRandomly(tag, null);
    }

    /**
     * Selects the most desirable goods from the colony.
     *
     * @param target The colony.
     * @return The goods to demand.
     */
    public Goods selectGoods(Colony target) {
        Tension.Level tension = getUnit().getOwner().getTension(target.getOwner()).getLevel();
        int dx = getSpecification().getIntegerOption("model.option.nativeDemands")
            .getValue() + 1;
        GoodsType food = getSpecification().getPrimaryFoodType();
        Goods goods = null;
        GoodsContainer warehouse = target.getGoodsContainer();
        if (tension.compareTo(Tension.Level.CONTENT) <= 0 &&
            warehouse.getGoodsCount(food) >= GoodsContainer.CARGO_SIZE) {
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
            for (GoodsType preferred : getSpecification().getGoodsTypeList()) {
                if (preferred.isMilitaryGoods()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, capAmount(amount, dx));
                    }
                }
            }
            // storable building materials (what do the natives need tools for?)
            for (GoodsType preferred : getSpecification().getGoodsTypeList()) {
                if (preferred.isBuildingMaterial() && preferred.isStorable()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, capAmount(amount, dx));
                    }
                }
            }
            // trade goods
            for (GoodsType preferred : getSpecification().getGoodsTypeList()) {
                if (preferred.isTradeGoods()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, capAmount(amount, dx));
                    }
                }
            }
            // refined goods
            for (GoodsType preferred : getSpecification().getGoodsTypeList()) {
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
        finalAmount = Math.min(finalAmount, GoodsContainer.CARGO_SIZE);
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
     * @return True if this mission is still valid.
     */
    public boolean isValid() {
        return super.isValid() && !completed
            && getUnit().getIndianSettlement() != null
            && (hasTribute()
                || (target != null && !target.isDisposed()
                    && target.getTile().getColony() == target
                    && getUnit().getOwner().getTension(target.getOwner())
                        .getLevel().compareTo(Tension.Level.HAPPY) <= 0));
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

        out.writeAttribute("demanded", Boolean.toString(demanded));
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
        completed = Boolean.valueOf(str).booleanValue();

        str = in.getAttributeValue(null, "demanded");
        demanded = Boolean.valueOf(str).booleanValue();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "indianDemandMission".
     */
    public static String getXMLElementTagName() {
        return "indianDemandMission";
    }
}
