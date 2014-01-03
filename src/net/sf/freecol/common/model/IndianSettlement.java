/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.util.Utils;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Tension.Level;


/**
 * Represents an Indian settlement.
 */
public class IndianSettlement extends Settlement {

    private static final Logger logger = Logger.getLogger(IndianSettlement.class.getName());

    /** The level of contact between a player and this settlement. */
    public static enum ContactLevel {
        UNCONTACTED,     // Nothing known other than location?
        CONTACTED,       // Name, wanted-goods now visible
        VISITED,         // Skill now known
        SCOUTED          // Scouting bonus consumed
    };

    // When choosing what goods to buy, sort goods types descending by price.
    private final Comparator<GoodsType> wantedGoodsComparator
        = new Comparator<GoodsType>() {
            public int compare(GoodsType goodsType1, GoodsType goodsType2) {
                return (getNormalGoodsPriceToBuy(goodsType2,
                        GoodsContainer.CARGO_SIZE)
                    - getNormalGoodsPriceToBuy(goodsType1,
                        GoodsContainer.CARGO_SIZE));
            }
        };

    // When choosing what goods to sell, sort goods with new world
    // goods first, then by price, then amount.
    private final Comparator<Goods> exportGoodsComparator
        = new Comparator<Goods>() {
            public int compare(Goods goods1, Goods goods2) {
                int cmp;
                GoodsType t1 = goods1.getType();
                GoodsType t2 = goods2.getType();
                cmp = (((t2.isNewWorldGoodsType()) ? 1 : 0)
                    - ((t1.isNewWorldGoodsType()) ? 1 : 0));
                if (cmp == 0) {
                    int a1 = Math.min(goods2.getAmount(),
                        GoodsContainer.CARGO_SIZE);
                    int a2 = Math.min(goods1.getAmount(),
                        GoodsContainer.CARGO_SIZE);
                    cmp = getPriceToSell(t2, a2) - getPriceToSell(t1, a1);
                    if (cmp == 0) {
                        cmp = a2 - a1;
                    }
                }
                return cmp;
            }
        };

    /** The maximum number of wanted goods. */
    public static final int WANTED_GOODS_COUNT = 3;

    /** Radius of native tales map reveal. */
    public static final int TALES_RADIUS = 6;

    /** Do not sell less than this amount of goods. */
    public static final int TRADE_MINIMUM_SIZE = 20;

    /** Do not buy goods when the price is this low. */
    public static final int TRADE_MINIMUM_PRICE = 3;

    public static final int GOODS_BASE_PRICE = 12;

    /** The amount of goods a brave can produce a single turn. */
    //private static final int WORK_AMOUNT = 5;

    /**
     * The amount of raw material that should be available before
     * producing manufactured goods.
     */
    public static final int KEEP_RAW_MATERIAL = 50;

    /**
     * Generate gifts from goods that exceed KEEP_RAW_MATERIAL +
     * GIFT_THRESHOLD.
     */
    public static final int GIFT_THRESHOLD = 25;

    /** The minimum gift amount. */
    public static final int GIFT_MINIMUM = 10;

    /** The maximum gift amount. */
    public static final int GIFT_MAXIMUM = 80;

    /**
     * This is the skill that can be learned by Europeans at this
     * settlement.  At the server side its value will be null when the
     * skill has already been taught to a European.  At the client
     * side the value null is also possible in case the player hasn't
     * checked out the settlement yet.
     */
    protected UnitType learnableSkill = null;

    /** The goods this settlement wants. */
    protected GoodsType[] wantedGoods = new GoodsType[] { null, null, null };

    /**
     * A map that tells if a player has spoken to the chief of this settlement.
     *
     * At the client side, only the information regarding the player
     * on that client should be included.
     */
    protected final java.util.Map<Player, ContactLevel> contactLevels
        = new HashMap<Player, ContactLevel>();

    /** Units that belong to this settlement. */
    protected List<Unit> ownedUnits = new ArrayList<Unit>();

    /** The missionary at this settlement. */
    protected Unit missionary = null;

    /** Used for monitoring the progress towards creating a convert. */
    protected int convertProgress = 0;

    /** The number of the turn during which the last tribute was paid. */
    protected int lastTribute = 0;

    /** The most hated nation. */
    protected Player mostHated = null;

    /**
     * Stores the alarm levels. <b>Only used by AI.</b>
     * "Alarm" means: Tension with respect to a player from an
     *      IndianSettlement.
     * Alarm is overloaded with the concept of "contact".  If a settlement
     * has never been contacted by a player, alarm.get(player) will be null.
     * Acts causing contact initialize this variable.
     */
    protected final java.util.Map<Player, Tension> alarm
        = new HashMap<Player, Tension>();


    /**
     * Constructor for ServerIndianSettlement.
     *
     * @param game The enclosing <code>Game</code>.
     * @param owner The <code>Player</code> owning this settlement.
     * @param name The name for this settlement.
     * @param tile The containing <code>Tile</code>.
     */
    protected IndianSettlement(Game game, Player owner, String name,
                               Tile tile) {
        super(game, owner, name, tile);
    }

    /**
     * Creates a new <code>IndianSettlement</code> with the given
     * identifier.  The object should later be initialized by calling either
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The object identifier.
     */
    public IndianSettlement(Game game, String id) {
        super(game, id);
    }


    /**
     * Adds the given <code>Unit</code> to the list of units that
     * belongs to this <code>IndianSettlement</code>.
     *
     * @param unit The <code>Unit</code> to be added.
     */
    public void addOwnedUnit(Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Parameter 'unit' must not be 'null'.");
        }

        if (!ownedUnits.contains(unit)) {
            ownedUnits.add(unit);
        }
    }

    /**
     * Gets a list of the units native to this settlement.
     *
     * @return The list of units native to this settlement.
     */
    public List<Unit> getOwnedUnits() {
        return new ArrayList<Unit>(ownedUnits);
    }

    /**
     * Gets an iterator over all the units this
     * <code>IndianSettlement</code> is owning.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getOwnedUnitsIterator() {
        return ownedUnits.iterator();
    }

    /**
     * Removes the given <code>Unit</code> to the list of units that
     * belongs to this <code>IndianSettlement</code>. Returns true if
     * the Unit was removed.
     *
     * @param unit The <code>Unit</code> to be removed from the
     *       list of the units this <code>IndianSettlement</code>
     *       owns.
     * @return a <code>boolean</code> value
     */
    public boolean removeOwnedUnit(Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Parameter 'unit' must not be 'null'.");
        }
        return ownedUnits.remove(unit);
    }

    /**
     * Get the year of the last tribute.
     *
     * @return The year of the last tribute.
     */
    public int getLastTribute() {
        return lastTribute;
    }

    /**
     * Set the year of the last tribute.
     *
     * @param lastTribute The new last tribute year.
     */
    public void setLastTribute(int lastTribute) {
        this.lastTribute = lastTribute;
    }

    /**
     * Gets the skill that can be learned at this settlement.
     *
     * @return The skill that can be learned at this settlement.
     */
    public UnitType getLearnableSkill() {
        return learnableSkill;
    }

    /**
     * Sets the learnable skill for this Indian settlement.
     *
     * @param skill The new learnable skill for this Indian settlement.
     */
    public void setLearnableSkill(UnitType skill) {
        learnableSkill = skill;
    }

    /**
     * Gets the missionary from this settlement.
     *
     * @return The missionary at this settlement, or null if there is none.
     */
    public Unit getMissionary() {
        return missionary;
    }

    /**
     * Does this settlement have a missionary?
     *
     * @return True if there is a missionary at this settlement.
     */
    public boolean hasMissionary() {
        return missionary != null;
    }

    /**
     * Does this settlement have a missionary from the given player?
     *
     * @param player The <code>Player</code> to test.
     * @return True if there is a suitable missionary present.
     */
    public boolean hasMissionary(Player player) {
        return missionary != null && player != null && player.owns(missionary);
    }

    /**
     * Sets the missionary for this settlement.
     *
     * -vis: This routine has visibility implications when enhanced
     * missionaries are enabled.
     * -til: This changes the tile appearance.
     *
     * @param missionary The missionary for this settlement.
     */
    public void setMissionary(Unit missionary) {
        this.missionary = missionary;
    }

    /**
     * Gets the convert progress status for this settlement.
     *
     * @return The convert progress status.
     */
    public int getConvertProgress() {
        return convertProgress;
    }

    /**
     * Sets the convert progress status for this settlement.
     *
     * @param progress The new convert progress status.
     */
    public void setConvertProgress(int progress) {
        convertProgress = progress;
    }

    /**
     * Gets the goods wanted by this settlement.
     *
     * @return The wanted goods list.
     */
    public GoodsType[] getWantedGoods() {
        return wantedGoods;
    }

    /**
     * Sets the goods wanted by this Settlement.
     *
     * @param wantedGoods a <code>GoodsType</code> value
     */
    public void setWantedGoods(GoodsType[] wantedGoods) {
        this.wantedGoods = wantedGoods;
    }

    /**
     * Sets the goods wanted by this settlement.
     *
     * @param index Which of the (usually 3) goods to set.
     * @param type The <code>GoodsType</code> wanted.
     */
    public void setWantedGoods(int index, GoodsType type) {
        if (0 <= index && index < wantedGoods.length) {
            wantedGoods[index] = type;
        }
    }

    /**
     * Gets the most hated nation of this settlement.
     *
     * @return The most hated nation.
     */
    public Player getMostHated() {
        return mostHated;
    }

    /**
     * Sets the most hated nation of this settlement.
     *
     * -til: Changes the tile appearance.
     *
     * @param mostHated The new most hated nation.
     */
    public void setMostHated(Player mostHated) {
        this.mostHated = mostHated;
    }

    /**
     * Gets the contact level between this settlement and a player.
     *
     * @param player The <code>Player</code> to check.
     * @return The contact level.
     */
    public ContactLevel getContactLevel(Player player) {
        ContactLevel cl = contactLevels.get(player);
        return (cl == null) ? ContactLevel.UNCONTACTED : cl;
    }

    /**
     * Has a player contacted this settlement?
     *
     * @param player The <code>Player</code> to check.
     * @return True if the player has contacted this settlement.
     */
    public boolean hasContacted(Player player) {
        return getContactLevel(player) != ContactLevel.UNCONTACTED;
    }

    /**
     * Make contact with this settlement (if it has not been
     * previously contacted).  Initialize tension level to the general
     * level with respect to the contacting player--- effectively the
     * average reputation of this player with the overall tribe.
     *
     * @param player The <code>Player</code> making contact.
     * @return True if this was indeed the first contact between settlement
     *     and player.
     */
    public boolean setContacted(Player player) {
        if (!hasContacted(player)) {
            contactLevels.put(player, ContactLevel.CONTACTED);
            initializeAlarm(player);
            return true;
        }
        return false;
    }

    /**
     * Has a player visited this settlement?
     *
     * @param player The <code>Player</code> to check.
     * @return True if the player has contacted this settlement.
     */
    public boolean hasVisited(Player player) {
        return getContactLevel(player).ordinal()
            >= ContactLevel.VISITED.ordinal();
    }

    /**
     * Sets the contact level of this settlement to indicate
     * that a European player has visited the settlement.
     *
     * @param player The visiting <code>Player</code>.
     * @return True if this was the first time the settlement was visited
     *     by the player.
     */
    public boolean setVisited(Player player) {
        if (!hasVisited(player)) {
            if (!hasContacted(player)) initializeAlarm(player);
            contactLevels.put(player, ContactLevel.VISITED);
            return true;
        }
        return false;
    }

    /**
     * Has a player has spoken with the chief of this settlement.
     *
     * @param player The <code>Player</code> to check.
     * @return True if the player has visited this settlement to speak
     *     with the chief.
     */
    public boolean hasScouted(Player player) {
        return getContactLevel(player) == ContactLevel.SCOUTED;
    }

    /**
     * Sets the contact level of this settlement to indicate
     * that a European player has had a chat with the chief.
     *
     * @param player The visiting <code>Player</code>.
     * @return True if this was the first time the settlement was scouted
     *     by the player.
     */
    public boolean setScouted(Player player) {
        if (!hasScouted(player)) {
            if (!hasContacted(player)) initializeAlarm(player);
            contactLevels.put(player, ContactLevel.SCOUTED);
            return true;
        }
        return false;
    }

    /**
     * Has any European player spoken with the chief of this settlement.
     *
     * @return True if any European player has spoken with the chief.
     */
    public boolean hasAnyScouted() {
        for (Player p : contactLevels.keySet()) {
            if (hasScouted(p)) return true;
        }
        return false;
    }

    /**
     * Is this settlement worth scouting?
     * That is, has it been contacted, but not scouted already, or
     * visited when the "Chief contact" option is set.
     *
     * @param player The <code>Player</code> contemplating scouting.
     * @return Whether it might be worth the player scouting this settlement.
     */
    public boolean worthScouting(Player player) {
        ContactLevel cl = getContactLevel(player);
        switch (cl) {
        case CONTACTED:
            return true;
        case VISITED:
            return !getSpecification()
                .getBoolean(GameOptions.SETTLEMENT_ACTIONS_CONTACT_CHIEF);
        case UNCONTACTED: case SCOUTED: default:
            break;
        }
        return false;
    }

    /**
     * Gets the alarm level towards the given player.
     *
     * @param player The <code>Player</code> to get the alarm level for.
     * @return The current alarm level or null if the settlement has not
     *     encoutered the player.
     */
    public Tension getAlarm(Player player) {
        return alarm.get(player);
    }

    /**
     * Sets alarm towards the given player.
     *
     * -til: Might change tile appearance through most hated state
     *
     * @param player The <code>Player</code> to set the alarm level for.
     * @param newAlarm The new alarm value.
     */
    public void setAlarm(Player player, Tension newAlarm) {
        alarm.put(player, newAlarm);
    }

    /**
     * Initialize the alarm at this settlement with respect to a
     * player with the current national tension.
     *
     * @param player The <code>Player</code> to set the alarm level for.
     */
    protected void initializeAlarm(Player player) {
        Tension tension = owner.getTension(player);
        setAlarm(player, new Tension((tension == null) ? 0
                : tension.getValue()));
    }

    /**
     * Gets a messageId for a short alarm message associated with the
     * alarm level of this player.
     *
     * @param player The other <code>Player</code>.
     * @return The alarm messageId.
     */
    public String getShortAlarmLevelMessageId(Player player) {
        return (!player.hasContacted(owner)) ? "tension.wary"
            : (hasContacted(player)) ? getAlarm(player).getKey()
            : "tension.unknown";
    }

    /**
     * Is a unit permitted to make contact with this settlement?
     * The unit must be from a nation that has already made contact,
     * or in the first instance, must be arriving by land, with the
     * exception of trading ships.
     *
     * @param unit The <code>Unit</code> that proposes to contact this
     *             settlement.
     * @return True if the settlement accepts such contact.
     */
    public boolean allowContact(Unit unit) {
        return unit.getOwner().hasContacted(owner)
            || !unit.isNaval()
            || unit.hasGoodsCargo();
    }

    /**
     * Gets the amount of gold this <code>IndianSettlment</code>
     * is willing to pay for the given <code>Goods</code>.
     *
     * It is only meaningful to call this method from the
     * server, since the settlement's {@link GoodsContainer}
     * is hidden from the clients.
     *
     * @param goods The <code>Goods</code> to price.
     * @return The price.
     */
    public int getPriceToBuy(Goods goods) {
        return getPriceToBuy(goods.getType(), goods.getAmount());
    }

    /**
     * Gets the amount of gold this <code>IndianSettlment</code>
     * is willing to pay for the given <code>Goods</code>.
     *
     * It is only meaningful to call this method from the server,
     * since the settlement's {@link GoodsContainer} is hidden from
     * the clients.  The AI uses it though so it stays here for now.
     * Note that it takes no account of whether the native player
     * actually has the gold.
     *
     * TODO: this is rancid with magic numbers.
     * TODO: the hardwired goods/equipment types are a wart.
     *
     * @param type The type of <code>Goods</code> to price.
     * @param amount The amount of <code>Goods</code> to price.
     * @return The price.
     */
    public int getPriceToBuy(GoodsType type, int amount) {
        if (amount > GoodsContainer.CARGO_SIZE) {
            throw new IllegalArgumentException("Amount > "
                + GoodsContainer.CARGO_SIZE);
        }

        int price = 0;
        if (type.isMilitaryGoods()) {
            // Might return zero if a surplus is present
            price = getMilitaryGoodsPriceToBuy(type, amount);
        }
        if (price == 0) {
            price = getNormalGoodsPriceToBuy(type, amount);
        }

        // Apply wanted bonus
        final int wantedBase = 100; // Granularity for wanted bonus
        final int wantedBonus // Premium paid for wanted goods types
            = (type == wantedGoods[0]) ? 150
            : (type == wantedGoods[1]) ? 125
            : (type == wantedGoods[2]) ? 110
            : 100;
        // Do not simplify with *=, we want the integer truncation.
        price = wantedBonus * price / wantedBase;

        logger.finest("Full price(" + amount + " " + type + ")"
                      + " -> " + price);
        return price;
    }

    /**
     * Price some goods according to the amount present in the settlement.
     *
     * @param type The type of goods for sale.
     * @param amount The amount of goods for sale.
     * @return A price for the goods.
     */
    private int getNormalGoodsPriceToBuy(GoodsType type, int amount) {
        final int tradeGoodsAdd = 20; // Fake additional trade goods present
        final int capacity = getGoodsCapacity();
        int current = getGoodsCount(type);

        // Increase effective stock if its raw material is produced here.
        GoodsType rawType = type.getInputType();
        if (rawType != null) {
            int rawProduction = getMaximumProduction(rawType);
            int add = (rawProduction < 5) ? 10 * rawProduction
                : (rawProduction < 10) ? 5 * rawProduction + 25
                : (rawProduction < 20) ? 2 * rawProduction + 55
                : 100;
            // Decrease bonus in proportion to current stock, up to capacity.
            add = add * Math.max(0, capacity - current) / capacity;
            current += add;
        } else if (type.isTradeGoods()) {
            // Small artificial increase of the trade goods stored.
            current += tradeGoodsAdd;
        }

        // Only interested in the amount of goods that keeps the
        // total under the threshold.
        int retain = Math.min(getWantedGoodsAmount(type), capacity);
        int valued = (retain <= current) ? 0
            : Math.min(amount, retain - current);

        // Unit price then is maximum price plus the bonus for the
        // settlement type, reduced by the proportion of goods present.
        int unitPrice = (GOODS_BASE_PRICE + getType().getTradeBonus())
            * Math.max(0, capacity - current) / capacity;

        // But farmed goods are always less interesting.
        // and small settlements are not interested in building.
        if (type.isFarmed() || type.isRawBuildingMaterial()) unitPrice /= 2;

        // Only pay for the portion that is valued.
        int price = (unitPrice < 0) ? 0 : valued * unitPrice;
        //logger.finest("Normal price(" + amount + " " + type + ")"
        //              + " valued=" + valued
        //              + " current=" + getGoodsCount(type)
        //              + " + " + (current - getGoodsCount(type))
        //              + " unitPrice=" + unitPrice
        //              + " -> " + price);
        return price;
    }

    /**
     * Calculates how much of the given goods type this settlement
     * wants and should retain.
     *
     * @param type The <code>GoodsType</code>.
     * @return The amount of goods wanted.
     */
    protected int getWantedGoodsAmount(GoodsType type) {
        final Specification spec = getSpecification();

        if (type.isMilitaryGoods()) {
            // Retain enough goods to fully arm.
            int need = 0;
            int toArm = 0;
            if (type == spec.getGoodsType("model.goods.muskets")) {
                for (Unit u : ownedUnits) if (!u.isArmed()) need++;
                toArm = spec.getEquipmentType("model.equipment.indian.muskets")
                    .getRequiredAmountOf(type);
            } else if (type == spec.getGoodsType("model.goods.horses")) {
                for (Unit u : ownedUnits) if (!u.isMounted()) need++;
                toArm = spec.getEquipmentType("model.equipment.indian.horses")
                    .getRequiredAmountOf(type);
            }
            return need * toArm;
        }

        int consumption = getConsumptionOf(type);
        if (type == spec.getPrimaryFoodType()) {
            // Food is perishable, do not try to retain that much
            return Math.max(40, consumption * 3);
        }
        if (type.isTradeGoods() || type.isNewWorldLuxuryType()
            || type.isRefined()) {
            // Aim for 10 years supply, resupply is doubtful
            return Math.max(80, consumption * 20);
        }
        // Just keep some around
        return 2 * getUnitCount();
    }

    /**
     * Price some goods that have military value to the settlement.
     *
     * @param type The type of goods for sale.
     * @param amount The amount of goods for sale.
     * @return A price for the goods.
     */
    private int getMilitaryGoodsPriceToBuy(GoodsType type, int amount) {
        final int full = GOODS_BASE_PRICE + getType().getTradeBonus();
        int required = getWantedGoodsAmount(type);
        if (required == 0) return 0; // Do not pay military price

        // If the settlement can use more than half of the goods on offer,
        // then pay top dollar for the lot.  Otherwise only pay the premium
        // price for the part they need and refer the remaining amount to
        // the normal goods pricing.
        int valued = Math.max(0, required - getGoodsCount(type));
        int price = (valued > amount / 2) ? full * amount
            : valued * full + getNormalGoodsPriceToBuy(type, amount - valued);
        logger.finest("Military price(" + amount + " " + type + ")"
                      + " valued=" + valued
                      + " -> " + price);
        return price;
    }

    /**
     * Gets the amount of gold this <code>IndianSettlment</code>
     * is willing to sell the given <code>Goods</code> for.
     *
     * It is only meaningful to call this method from the
     * server, since the settlement's {@link GoodsContainer}
     * is hidden from the clients.
     *
     * @param goods The <code>Goods</code> to price.
     * @return The price.
     */
    public int getPriceToSell(Goods goods) {
        return getPriceToSell(goods.getType(), goods.getAmount());
    }

    /**
     * Gets the amount of gold this <code>IndianSettlment</code>
     * is willing to sell the given <code>Goods</code> for.
     *
     * It is only meaningful to call this method from the
     * server, since the settlement's {@link GoodsContainer}
     * is hidden from the clients.
     *
     * @param type The type of <code>Goods</code> to price.
     * @param amount The amount of <code>Goods</code> to price.
     * @return The price.
     */
    public int getPriceToSell(GoodsType type, int amount) {
        if (amount > GoodsContainer.CARGO_SIZE) {
            throw new IllegalArgumentException("Amount > "
                + GoodsContainer.CARGO_SIZE);
        }
        final int full = GOODS_BASE_PRICE + getType().getTradeBonus();

        // Base price is purchase price plus delta.
        // - military goods at double value
        // - trade goods at +50%
        int price = amount + Math.max(0, 11 * getPriceToBuy(type, amount) / 10);
        if (type.isMilitaryGoods()) {
            price = Math.max(price, amount * full * 2);
        } else if (type.isTradeGoods()) {
            price = Math.max(price, 150 * amount * full / 100);
        }
        return price;
    }

    /**
     * Will this settlement sell a type of goods.
     * Placeholder until we have a spec-configured blacklist.
     *
     * @param type The <code>GoodsType</code> to consider.
     * @return True if the settlement would sell the goods.
     */
    public boolean willSell(GoodsType type) {
        return !type.isTradeGoods();
    }

    /**
     * Gets the goods this settlement is willing to sell.
     *
     * @param limit The maximum number of goods required.
     * @param unit The <code>Unit</code> that is trading.
     * @return A list of goods to sell.
     */
    public List<Goods> getSellGoods(int limit, Unit unit) {
        List<Goods> result = new ArrayList<Goods>();
        List<Goods> settlementGoods = getCompactGoods();
        Collections.sort(settlementGoods, exportGoodsComparator);

        int count = 0;
        for (Goods goods : settlementGoods) {
            if (!willSell(goods.getType())) continue;
            int amount = goods.getAmount();
            int retain = getWantedGoodsAmount(goods.getType());
            if (retain >= amount) continue;
            amount -= retain;
            if (amount > GoodsContainer.CARGO_SIZE) {
                amount = GoodsContainer.CARGO_SIZE;
            }
            if (unit != null) {
                amount = Math.round(FeatureContainer
                    .applyModifierSet((float) amount, getGame().getTurn(),
                        unit.getModifierSet(Modifier.TRADE_VOLUME_PENALTY)));
            }
            if (amount < TRADE_MINIMUM_SIZE) continue;
            result.add(new Goods(getGame(), this, goods.getType(), amount));
            count++;
            if (count >= limit) break;
        }
        return result;
    }


    /**
     * Allows spread of horses and arms between settlements
     *
     * @param settlement The other <code>IndianSettlement</code> to trade with.
     */
    public void tradeGoodsWithSettlement(IndianSettlement settlement) {
        GoodsType armsType = getSpecification().getGoodsType("model.goods.muskets");
        GoodsType horsesType = getSpecification().getGoodsType("model.goods.horses");
        List<GoodsType> goodsToTrade = new ArrayList<GoodsType>();
        goodsToTrade.add(armsType);
        goodsToTrade.add(horsesType);

        for(GoodsType goods : goodsToTrade){
            int goodsInStock = getGoodsCount(goods);
            if(goodsInStock <= 50){
                continue;
            }
            int goodsTraded = goodsInStock / 2;
            settlement.addGoods(goods, goodsTraded);
            removeGoods(goods, goodsTraded);
        }
    }

    /**
     * Gets the maximum possible production of the given type of goods.
     * @param goodsType The type of goods to check.
     * @return The maximum amount, of the given type of goods, that can
     *         be produced in one turn.
     */
    public int getMaximumProduction(GoodsType goodsType) {
        int amount = 0;
        for (Tile workTile: getTile().getSurroundingTiles(getRadius())) {
            if (workTile.getOwningSettlement() == null || workTile.getOwningSettlement() == this) {
                // TODO: make unitType brave
                amount += workTile.potential(goodsType, null);
            }
        }

        return amount;
    }


    /**
     * Updates the goods wanted by this settlement.
     *
     * It is only meaningful to call this method from the
     * server, since the settlement's {@link GoodsContainer}
     * is hidden from the clients.
     */
    public void updateWantedGoods() {
        List<GoodsType> goodsTypes = new ArrayList<GoodsType>(getSpecification().getGoodsTypeList());
        Collections.sort(goodsTypes, wantedGoodsComparator);
        int wantedIndex = 0;
        for (GoodsType goodsType : goodsTypes) {
            // The natives do not trade military or non-storable goods.
            if (goodsType.isMilitaryGoods() || !goodsType.isStorable())
                continue;
            if (getNormalGoodsPriceToBuy(goodsType, GoodsContainer.CARGO_SIZE)
                <= GoodsContainer.CARGO_SIZE * TRADE_MINIMUM_PRICE
                || wantedIndex >= wantedGoods.length) break;
            wantedGoods[wantedIndex] = goodsType;
            wantedIndex++;
        }
        for (; wantedIndex < wantedGoods.length; wantedIndex++) {
            wantedGoods[wantedIndex] = null;
        }
    }

    /**
     * Chooses a type of goods for some of the natives in a settlement
     * to manufacture.
     * Simple rule: choose the refined goods that is the greatest shortage
     * for which there is a surplus of the raw material.
     *
     * @return A <code>GoodsType</code> to manufacture, or null if
     *      none suitable.
     */
    private GoodsType goodsToMake() {
        GoodsType wantGoods = null;
        int diff, wantAmount = -1;
        for (GoodsType g : getSpecification().getGoodsTypeList()) {
            GoodsType produced;
            if (g.isRawMaterial()
                && (produced = g.getOutputType()) != null
                && produced.isStorable()
                && getGoodsCount(g) > getWantedGoodsAmount(g)
                && (diff = getWantedGoodsAmount(produced)
                    - getGoodsCount(produced)) > wantAmount) {
                wantGoods = produced;
                wantAmount = diff;
            }
        }
        return wantGoods;
    }

    /**
     * Gets a random goods gift from this settlement.
     *
     * @param random A pseudo random number source.
     * @return A random goods gift, or null if none found.
     */
    public Goods getRandomGift(Random random) {
        List<Goods> goodsList = new ArrayList<Goods>();
        for (GoodsType type : getSpecification().getNewWorldGoodsTypeList()) {
            int n = getGoodsCount(type) - KEEP_RAW_MATERIAL;
            if (n >= GIFT_THRESHOLD) {
                n -= GIFT_MINIMUM;
                Goods goods = new Goods(getGame(), this, type,
                    Math.min(Utils.randomInt(logger, "Gift amount", random, n)
                        + GIFT_MINIMUM, GIFT_MAXIMUM));
                goodsList.add(goods);
            }

        }
        return (goodsList.isEmpty()) ? null
            : Utils.getRandomMember(logger, "Gift type", goodsList, random);
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FreeColGameObject> disposeList() {
        // Orphan the units whose home settlement this is.
        while (!ownedUnits.isEmpty()) {
            ownedUnits.remove(0).setHomeIndianSettlement(null);
        }
        return super.disposeList();
    }


    // Interface Location (from Settlement via GoodsLocation via UnitLocation)
    // Inherits
    //   FreeColObject.getId()
    //   Settlement.getTile
    //   Settlement.getLocationName
    //   GoodsLocation.remove
    //   GoodsLocation.contains
    //   UnitLocation.canAdd
    //   UnitLocation.getUnitCount
    //   UnitLocation.getUnitList
    //   Settlement.getSettlement

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationNameFor(Player player) {
        return (hasContacted(player)) ? StringTemplate.name(getName())
            : StringTemplate.label("indianSettlement.nameUnknown");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Locatable locatable) {
        boolean result = super.add(locatable);
        if (result && locatable instanceof Unit) {
            Unit indian = (Unit)locatable;
            if (indian.getHomeIndianSettlement() == null) {
                // Adopt homeless Indians
                indian.setHomeIndianSettlement(this);
            }
        }
        return result;
    }


    // UnitLocation
    // Inherits
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    //   Settlement.getNoAddReason
    //   UnitLocation.getUnitCapacity


    // GoodsLocation
    // Inherits
    //   GoodsLocation.addGoods
    //   GoodsLocation.removeGoods

    /**
     * {@inheritDoc}
     */
    public int getGoodsCapacity() {
        return getType().getWarehouseCapacity();
    }


    // Settlement

    /**
     * {@inheritDoc}
     */
    public String getImageKey() {
        return getOwner().getNationId()
            + (isCapital() ? ".capital" : ".settlement")
            + ((hasMissionary()) ? "" : ".mission")
            + ".image";
    }

    /**
     * {@inheritDoc}
     */
    public Unit getDefendingUnit(Unit attacker) {
        Unit defender = null;
        float defencePower = -1.0f;
        for (Unit nextUnit : getUnitList()) {
            float unitPower = attacker.getGame().getCombatModel()
                .getDefencePower(attacker, nextUnit);
            if (Unit.betterDefender(defender, defencePower,
                    nextUnit, unitPower)) {
                defender = nextUnit;
                defencePower = unitPower;
            }
        }
        return defender;
    }

    /**
     * {@inheritDoc}
     */
    public float getDefenceRatio() {
        return getUnitCount() * 2.0f / (getType().getMinimumSize()
            + getType().getMaximumSize());
    }

    /**
     * {@inheritDoc}
     */
    public RandomRange getPlunderRange(Unit attacker) {
        return getType().getPlunderRange(attacker);
    }

    /**
     * {@inheritDoc}
     */
    public int getSoL() {
        // Native settlements do not generate SoL.
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getUpkeep() {
        // Native settlements do not require upkeep.
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalProductionOf(GoodsType type) {
        if (type.isRefined()) {
            if (type != goodsToMake()) return 0;
            // Pretend 1/3 of the units present make the item with
            // basic production of 3.
            return getUnitCount();
        }

        int potential = 0;
        int tiles = 1; // Always include center tile

        for (Tile workTile : getOwnedTiles()) {
            if (workTile != getTile() && !workTile.isOccupied()) {
                // TODO: make unitType brave
                potential += workTile.potential(type, null);
                tiles++;
            }
        }

        // When a native settlement has more tiles than units, pretend
        // that they produce from their entire area at reduced
        // efficiency.
        if (tiles > getUnitCount()) {
            potential *= (float) getUnitCount() / tiles;
        }

        // But always add full potential of the center tile.
        potential += getTile().potential(type, null);

        return potential;
    }

    /**
     * {@inheritDoc}
     */
    public StringTemplate getAlarmLevelMessage(Player player) {
        Tension alarm = (hasContacted(player)) ? getAlarm(player)
            : new Tension(Tension.TENSION_MIN);
        return StringTemplate.template("indianSettlement." + alarm.getKey())
            .addStringTemplate("%nation%", getOwner().getNationName());
    }


    // Serialization

    private static final String ALARM_TAG = "alarm";
    private static final String CONTACT_LEVEL_TAG = "contactLevel";
    private static final String CONVERT_PROGRESS_TAG = "convertProgress";
    private static final String IS_VISITED_TAG = "isVisited";
    private static final String LAST_TRIBUTE_TAG = "lastTribute";
    private static final String LEVEL_TAG = "level";
    private static final String MISSIONARY_TAG = "missionary";
    private static final String MOST_HATED_TAG = "mostHated";
    private static final String NAME_TAG = "name";
    private static final String OWNED_UNITS_TAG = "ownedUnits";
    private static final String PLAYER_TAG = "player";
    // Public for now while 0.10.7 backward compatibility code in Tile
    // for PlayerExploredTile needs to check these.
    public static final String LEARNABLE_SKILL_TAG = "learnableSkill";
    public static final String WANTED_GOODS_TAG = "wantedGoods";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        final Player client = xw.getClientPlayer();
        final Player hated = getMostHated();

        if (getName() != null) { // Delegated from Settlement
            xw.writeAttribute(NAME_TAG, getName());
        }

        if (xw.validFor(getOwner())) {

            xw.writeAttribute(LAST_TRIBUTE_TAG, lastTribute);

            xw.writeAttribute(CONVERT_PROGRESS_TAG, convertProgress);
        }

        if (learnableSkill != null) {
            xw.writeAttribute(LEARNABLE_SKILL_TAG, learnableSkill);
        }

        for (int i = 0; i < wantedGoods.length; i++) {
            if (wantedGoods[i] != null) {
                xw.writeAttribute(WANTED_GOODS_TAG + i, wantedGoods[i]);
            }
        }

        if (hated != null) xw.writeAttribute(MOST_HATED_TAG, hated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (missionary != null) {
            xw.writeStartElement(MISSIONARY_TAG);
            
            missionary.toXML(xw);

            xw.writeEndElement();
        }

        if (xw.validFor(getOwner())) {

            for (Player p : getSortedCopy(contactLevels.keySet())) {
                xw.writeStartElement(CONTACT_LEVEL_TAG);

                xw.writeAttribute(LEVEL_TAG, contactLevels.get(p));

                xw.writeAttribute(PLAYER_TAG, p);

                xw.writeEndElement();
            }

            for (Player p : getSortedCopy(alarm.keySet())) {
                xw.writeStartElement(ALARM_TAG);

                xw.writeAttribute(PLAYER_TAG, p);

                xw.writeAttribute(VALUE_TAG, alarm.get(p).getValue());

                xw.writeEndElement();
            }

            for (Unit unit : getSortedCopy(ownedUnits)) {
                xw.writeStartElement(OWNED_UNITS_TAG);

                xw.writeAttribute(ID_ATTRIBUTE_TAG, unit);

                xw.writeEndElement();
            }

        } else {
            Player client = xw.getClientPlayer();

            ContactLevel cl = contactLevels.get(client);
            if (cl != null) {
                xw.writeStartElement(CONTACT_LEVEL_TAG);

                xw.writeAttribute(LEVEL_TAG, cl);

                xw.writeAttribute(PLAYER_TAG, client);

                xw.writeEndElement();
            }

            Tension alarm = getAlarm(client);
            if (alarm != null) {
                xw.writeStartElement(ALARM_TAG);

                xw.writeAttribute(PLAYER_TAG, client);

                xw.writeAttribute(VALUE_TAG, alarm.getValue());

                xw.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        lastTribute = xr.getAttribute(LAST_TRIBUTE_TAG, 0);

        convertProgress = xr.getAttribute(CONVERT_PROGRESS_TAG, 0);

        learnableSkill = xr.getType(spec, LEARNABLE_SKILL_TAG,
                                    UnitType.class, (UnitType)null);

        mostHated = xr.findFreeColGameObject(getGame(), MOST_HATED_TAG,
                                             Player.class, (Player)null, false);

        for (int i = 0; i < wantedGoods.length; i++) {
            wantedGoods[i] = xr.getType(spec, WANTED_GOODS_TAG + i,
                                        GoodsType.class, (GoodsType)null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        contactLevels.clear();
        alarm.clear();
        missionary = null;
        ownedUnits.clear();

        super.readChildren(xr);

        // @compat 0.10.1
        for (Unit u : getUnitList()) {
            if (u.getLocation() != this) {
                u.setLocationNoUpdate(this);
                logger.warning("Fixing unit location"
                    + " from " + u.getLocation()
                    + " to " + this.getId());
            }
        }
        // end @compat
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (ALARM_TAG.equals(tag)) {
            Player player = xr.findFreeColGameObject(game, PLAYER_TAG,
                Player.class, (Player)null, true);
            // @compat 0.10.5
            if (getName() != null) {
                // Alarm used to imply contact, but only set contacted if
                // we also have a valid name for the settlement.
                setContacted(player);
            }
            // end @compat
            alarm.put(player, new Tension(xr.getAttribute(VALUE_TAG, 0)));
            xr.closeTag(ALARM_TAG);

        } else if (CONTACT_LEVEL_TAG.equals(tag)) {
            ContactLevel cl = xr.getAttribute(LEVEL_TAG,
                ContactLevel.class, ContactLevel.UNCONTACTED);
            Player player = xr.findFreeColGameObject(game, PLAYER_TAG,
                Player.class, (Player)null, true);
            contactLevels.put(player, cl);
            xr.closeTag(CONTACT_LEVEL_TAG);

        // @compat 0.10.5
        } else if (IS_VISITED_TAG.equals(tag)) {
            Player player = xr.findFreeColGameObject(game, PLAYER_TAG,
                Player.class, (Player)null, true);
            setScouted(player);
            xr.closeTag(IS_VISITED_TAG);
        // end @compat

        } else if (MISSIONARY_TAG.equals(tag)) {
            xr.nextTag();
            missionary = xr.readFreeColGameObject(game, Unit.class);
            missionary.setLocationNoUpdate(this);
            xr.closeTag(MISSIONARY_TAG);

        } else if (OWNED_UNITS_TAG.equals(tag)) {
            Unit unit = xr.makeFreeColGameObject(game, ID_ATTRIBUTE_TAG,
                                                 Unit.class, true);
            if (unit.getOwner() != null && !owner.owns(unit)) {
                logger.warning("Unit " + unit.getId()
                               + " does not belong to settlement " + getId());
            } else {
                addOwnedUnit(unit);
            }
            xr.closeTag(OWNED_UNITS_TAG);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(getName()).append(" at (").append(tile.getX())
            .append(",").append(tile.getY()).append(")");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "indianSettlement".
     */
    public static String getXMLElementTagName() {
        return "indianSettlement";
    }
}
