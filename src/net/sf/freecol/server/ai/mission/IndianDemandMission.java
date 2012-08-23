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
import net.sf.freecol.common.model.Location;
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

    /** The mission target, which is the colony to demand from. */
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
     * Checks if the unit is carrying a tribute (goods).
     *
     * @return True if the unit is carrying goods.
     */
    private boolean hasTribute() {
        return hasTribute(getAIUnit());
    }

    /**
     * Checks if a unit is carrying a tribute.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return True if the unit is carrying goods.
     */
    private static boolean hasTribute(AIUnit aiUnit) {
        return aiUnit.getUnit().getGoodsCount() > 0;
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
     * Why would this mission be invalid with the given unit?
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        return (reason != null)
            ? reason
            : (aiUnit.getUnit().getIndianSettlement() == null)
            ? "home-destroyed"
            : null;
    }

    /**
     * Why would an IndianDemandMission be invalid with the given
     * unit and colony.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param colony The <code>Colony</code> to test.
     * @return A reason why the mission would be invalid with the unit
     *     and colony or null if none found.
     */
    private static String invalidColonyReason(AIUnit aiUnit, Colony colony) {
        String reason = invalidTargetReason(colony);
        if (reason != null) return reason;
        final Unit unit = aiUnit.getUnit();
        final Player owner = unit.getOwner();
        Player targetPlayer = colony.getOwner();
        switch (owner.getStance(targetPlayer)) {
        case UNCONTACTED: case PEACE: case ALLIANCE:
            return "bad-stance";
        case WAR: case CEASE_FIRE:
            Tension tension = unit.getIndianSettlement()
                .getAlarm(targetPlayer);
            if (tension != null && tension.getLevel()
                .compareTo(Tension.Level.CONTENT) <= 0) return "happy";
            break;
        }
        return null;
    }

    /**
     * Why is this mission invalid?
     *
     * @return A reason for mission invalidity, or null if none found.
     */
    public String invalidReason() {
        return (completed) ? "completed"
            : invalidReason(getAIUnit(), target);
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        return invalidMissionReason(aiUnit);
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        String reason = invalidMissionReason(aiUnit);
        return (reason != null)
            ? reason
            : (loc instanceof Colony)
            ? invalidColonyReason(aiUnit, (Colony)loc)
            : Mission.TARGETINVALID;
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * Performs the mission.
     */
    public void doMission() {
        String reason = invalidReason();
        if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        final IndianSettlement is = unit.getIndianSettlement();
        while (!completed) {
            if (hasTribute()) {
                Unit.MoveType mt = travelToTarget(tag, is, null);
                switch (mt) {
                case MOVE_NO_MOVES:
                    return;
                case MOVE: // Arrived!
                    break;
                default:
                    logger.warning(tag + " unexpected delivery move type: " + mt
                        + ": " + this);
                    moveRandomly(tag, null);
                    return;
                }
                // Unload the goods
                GoodsContainer container = unit.getGoodsContainer();
                for (Goods goods : container.getCompactGoods()) {
                    Goods tribute = container.removeGoods(goods.getType());
                    is.addGoods(tribute);
                }
                logger.finest(tag + " completed unloading tribute at "
                    + is.getName() + ": " + this);
                completed = true;
                return;
            }

            // Move to the target's colony and demand
            Unit.MoveType mt = travelToTarget(tag, target, null);
            switch (mt) {
            case MOVE_NO_MOVES:
                return;
            case ATTACK_SETTLEMENT: // Arrived (do not really attack).
                break;
            default:
                logger.warning(tag + " unexpected demand move type: " + mt
                    + ": " + this);
                moveRandomly(tag, null);
                return;
            }

            Player enemy = target.getOwner();
            Goods goods = selectGoods(target);
            int gold = 0;
            int oldGoods = (goods == null) ? 0
                : unit.getGoodsContainer().getGoodsCount(goods.getType());
            if (goods == null) {
                if (!enemy.checkGold(1)) {
                    completed = true;
                    logger.finest(tag + " completed empty handed"
                        + " at " + target.getName() + ": " + this);
                    return;
                }
                gold = enemy.getGold() / 20;
                if (gold == 0) gold = enemy.getGold();
            }
            demanded = true;

            Direction d;
            boolean accepted
                = AIMessage.askIndianDemand(aiUnit, target, goods, gold);
            if (accepted && (gold > 0 || hasTribute())) {
                if (goods != null) {
                    logger.finest(tag + " accepted at " + target.getName()
                        + " tribute: " + goods.toString() + ": " + this);
                    continue; // Head for home
                } else {
                    logger.finest(tag + " completed at " + target.getName()
                        + " tribute: " + gold + " gold: " + this);
                }
            } else { // Consider attacking if not content.
                int unitTension = (is == null) ? 0
                    : is.getAlarm(enemy).getValue();
                int tension = Math.max(unitTension,
                    unit.getOwner().getTension(enemy).getValue());
                d = unit.getTile().getDirection(target.getTile());
                boolean attack = tension >= Tension.Level.CONTENT.getLimit()
                    && d != null;
                logger.finest(tag + " completed with refusal"
                    + " at " + target.getName()
                    + ((attack) ? "(attacking)" : "") + ": " + this);
                if (attack) {
                    AIMessage.askAttack(aiUnit, d);
                    return;
                }
            }
            // Consume any remaining moves.
            completed = true;
            moveRandomlyTurn(tag);
        }
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
