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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Tension.Level;


/**
 * Represents an Indian settlement.
 */
public class IndianSettlement extends Settlement {

    private static final Logger logger = Logger.getLogger(IndianSettlement.class.getName());

    public static final int TALES_RADIUS = 6;

    public static final String OWNED_UNITS_TAG_NAME = "ownedUnits";
    public static final String IS_VISITED_TAG_NAME = "isVisited";
    public static final String ALARM_TAG_NAME = "alarm";
    public static final String MISSIONARY_TAG_NAME = "missionary";
    public static final String WANTED_GOODS_TAG_NAME = "wantedGoods";

    // Do not sell less than this amount of goods.
    public static final int TRADE_MINIMUM_SIZE = 20;

    // Do not buy goods when the price is this low.
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
    protected GoodsType[] wantedGoods = new GoodsType[] {null, null, null};

    /**
     * A map that tells if a player has spoken to the chief of this settlement.
     *
     * At the client side, only the information regarding the player
     * on that client should be included.
     */
    protected Set<Player> spokenTo = new HashSet<Player>();

    /** Units that belong to this settlement. */
    protected ArrayList<Unit> ownedUnits = new ArrayList<Unit>();

    /** The missionary at this settlement. */
    protected Unit missionary = null;

    /** Used for monitoring the progress towards creating a convert. */
    protected int convertProgress = 0;

    /** The number of the turn during which the last tribute was paid. */
    protected int lastTribute = 0;

    /**
     * Stores the alarm levels. <b>Only used by AI.</b>
     * "Alarm" means: Tension with respect to a player from an
     *      IndianSettlement.
     * Alarm is overloaded with the concept of "contact".  If a settlement
     * has never been contacted by a player, alarm.get(player) will be null.
     * Acts causing contact initialize this variable.
     */
    private java.util.Map<Player, Tension> alarm
        = new HashMap<Player, Tension>();

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


    /**
     * Constructor for ServerIndianSettlement.
     */
    protected IndianSettlement() {
        // empty constructor
    }

    /**
     * Constructor for ServerIndianSettlement.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param owner The <code>Player</code> owning this settlement.
     * @param name The name for this settlement.
     * @param tile The location of the <code>IndianSettlement</code>.
     */
    protected IndianSettlement(Game game, Player owner, String name,
                               Tile tile) {
        super(game, owner, name, tile);
    }


    /**
     * Initiates a new <code>IndianSettlement</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public IndianSettlement(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Initiates a new <code>IndianSettlement</code>
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public IndianSettlement(Game game, String id) {
        super(game, id);
    }


    /**
     * Dispose of this native settlement.
     *
     * @return A list of disposed objects.
     */
    @Override
    public List<FreeColGameObject> disposeList() {
        // Orphan the units whose home settlement this is.
        while (ownedUnits.size() > 0) {
            ownedUnits.remove(0).setIndianSettlement(null);
        }
        return super.disposeList();
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
     * @param newAlarm The new alarm value.
     */
    public void setAlarm(Player player, Tension newAlarm) {
        if (player != null && player != owner) alarm.put(player, newAlarm);
    }

    /**
     * Removes all alarm towards the given player.  Used the a player leaves
     * the game.
     *
     * @param player The <code>Player</code> to remove the alarm for.
     */
    public void removeAlarm(Player player) {
        if (player != null) alarm.remove(player);
    }

    /**
     * Change the alarm level of this settlement by a given amount.
     *
     * @param player The <code>Player</code> the alarm level changes wrt.
     * @param amount The amount to change the alarm by.
     * @return True if the <code>Tension.Level</code> of the
     *     settlement alarm changes as a result of this change.
     */
    protected boolean changeAlarm(Player player, int amount) {
        Tension alarm = getAlarm(player);
        Level oldLevel = alarm.getLevel();
        alarm.modify(amount);
        return oldLevel != alarm.getLevel();
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
            : (hasContactedSettlement(player)) ? getAlarm(player).getKey()
            : "indianSettlement.tensionUnknown";
    }

    /**
     * Gets a messageId for an alarm message associated with the
     * alarm level of this player.
     *
     * @param player The other <code>Player</code>.
     * @return The alarm messageId.
     */
    public String getAlarmLevelMessageId(Player player) {
        Tension alarm = (hasContactedSettlement(player)) ? getAlarm(player)
            : new Tension(Tension.TENSION_MIN);
        return "indianSettlement.alarm." + alarm.getKey();
    }

    /**
     * Has a player contacted this settlement?
     *
     * @param player The <code>Player</code> to check.
     * @return True if the player has contacted this settlement.
     */
    public boolean hasContactedSettlement(Player player) {
        return getAlarm(player) != null;
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
    public boolean makeContactSettlement(Player player) {
        if (!hasContactedSettlement(player)) {
            setAlarm(player, new Tension(owner.getTension(player).getValue()));
            return true;
         }
         return false;
     }

    /**
     * Returns true if a European player has spoken with the chief of
     * this settlement.
     *
     * @return True if a European player has spoken with the chief.
     */
    public boolean hasSpokenToChief() {
        Iterator<Player> playerIterator = spokenTo.iterator();
        while (playerIterator.hasNext()) {
            if (playerIterator.next().isEuropean()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if a the given player has spoken with the chief of
     * this settlement.
     *
     * @param player The <code>Player</code> to check.
     * @return True if the player has visited this settlement to speak
     *     with the chief.
     */
    public boolean hasSpokenToChief(Player player) {
        return spokenTo.contains(player);
    }

    /**
     * Sets the spoken-to status of this settlement to true, indicating
     * that a European player has had a chat with the chief.
     *
     * @param player The visiting <code>Player</code>.
     */
    public void setSpokenToChief(Player player) {
        if (!hasSpokenToChief(player)) {
            makeContactSettlement(player); // Just to be sure
            spokenTo.add(player);
        }
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
            || unit.getGoodsCount() > 0;
    }


    /**
     * Adds the given <code>Unit</code> to the list of units that belongs to this
     * <code>IndianSettlement</code>.
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
     * Returns the skill that can be learned at this settlement.
     * @return The skill that can be learned at this settlement.
     */
    public UnitType getLearnableSkill() {
        return learnableSkill;
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
     * Sets the missionary for this settlement.
     *
     * @param missionary The missionary for this settlement.
     */
    public void setMissionary(Unit missionary) {
        this.missionary = missionary;
    }

    /**
     * Changes the missionary for this settlement and updates other players.
     *
     * @param missionary The new missionary for this settlement.
     */
    public void changeMissionary(Unit missionary) {
        setMissionary(missionary);
        getTile().updatePlayerExploredTiles();
        if (missionary != null) {
            // Full update for the new missionary owner
            getTile().updatePlayerExploredTile(missionary.getOwner(), true);
        }
    }

    /**
     * Gets the missionary from this settlement if there is one and
     * it is owned by a specified player.
     *
     * @param player The player purported to own the missionary
     * @return The missionary from this settlement if there is one and
     *     it is owned by the specified player, otherwise null.
     */
    public Unit getMissionary(Player player) {
        return (missionary == null || missionary.getOwner() != player) ? null
            : missionary;
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


    public GoodsType[] getWantedGoods() {
        return wantedGoods;
    }

    public void setWantedGoods(int index, GoodsType type) {
        if (0 <= index && index < wantedGoods.length) {
            wantedGoods[index] = type;
        }
    }

    /**
     * Sets the learnable skill for this Indian settlement.
     * @param skill The new learnable skill for this Indian settlement.
     */
    public void setLearnableSkill(UnitType skill) {
        learnableSkill = skill;
    }


    /**
     * Adds a <code>Locatable</code> to this Location.
     *
     * @param locatable The <code>Locatable</code> to add to this Location.
     */
    public boolean add(Locatable locatable) {
        boolean result = super.add(locatable);
        if (result && locatable instanceof Unit) {
            Unit indian = (Unit)locatable;
            if (indian.getIndianSettlement() == null) {
                // Adopt homeless Indians
                indian.setIndianSettlement(this);
            }
        }
        return result;
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
        GoodsType rawType = type.getRawMaterial();
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
                    .getAmountRequiredOf(type);
            } else if (type == spec.getGoodsType("model.goods.horses")) {
                for (Unit u : ownedUnits) if (!u.isMounted()) need++;
                toArm = spec.getEquipmentType("model.equipment.indian.horses")
                    .getAmountRequiredOf(type);
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
                        unit.getModifierSet("model.modifier.tradeVolumePenalty")));
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
                && (produced = g.getProducedMaterial()) != null
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
        GoodsContainer gc = getGoodsContainer();
        for (GoodsType type : getSpecification().getNewWorldGoodsTypeList()) {
            int n = gc.getGoodsCount(type) - KEEP_RAW_MATERIAL;
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

    public boolean checkForNewMissionaryConvert() {

        /* Increase convert progress and generate convert if needed. */
        if (missionary != null && getGame().getViewOwner() == null) {
            int increment = 8;

            // Update increment if missionary is an expert.
            if (missionary.hasAbility("model.ability.expertMissionary")) {
                increment = 13;
            }

            // Increase increment if alarm level is high.
            increment += 2 * getAlarm(missionary.getOwner()).getValue() / 100;
            convertProgress += increment;

            if (convertProgress >= 100 && getUnitCount() > 2) {
                convertProgress = 0;
                return true;
            }
        }
        return false;
    }


    // Interface location

    // getId() inherited from FreeColGameObject
    // canAdd, getUnitCount, getUnitList inherited from UnitLocation
    // add, remove, contains inherited from GoodsLocation

    /**
     * {@inheritDoc}
     */
    @Override
    public final Colony getColony() {
        return null; // A native settlement can never be a colony.
    }

    // UnitLocation routines
    // getNoAddReason in Settlement is adequate

    // GoodsLocation routines

    /**
     * {@inheritDoc}
     */
    public int getGoodsCapacity() {
        return getType().getWarehouseCapacity();
    }


    // Settlement routines

    /**
     * {@inheritDoc}
     */
    public String getNameFor(Player player) {
        return (hasContactedSettlement(player)) ? getName()
            : "indianSettlement.nameUnknown";
    }

    /**
     * {@inheritDoc}
     */
    public String getImageKey() {
        return getOwner().getNationID()
            + (isCapital() ? ".capital" : ".settlement")
            + ((getMissionary() == null) ? "" : ".mission")
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
     * Native settlements do not generate SoL.
     *
     * @return 0.
     */
    public int getSoL() {
        return 0;
    }

    /**
     * Native settlements do not require upkeep.
     *
     * @return 0
     */
    public int getUpkeep() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean propagateAlarm(Player player, int addToAlarm) {
        if (hasContactedSettlement(player)) {
            return changeAlarm(player, addToAlarm);
        }
        return false;
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


    // Serialization

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        boolean full = showAll || toSavedGame || player == getOwner();
        PlayerExploredTile pet = (player == null) ? null
            : getTile().getPlayerExploredTile(player);

        if (toSavedGame && !showAll) {
            logger.warning("toSavedGame is true, but showAll is false");
        }

        // Start element:
        out.writeStartElement(getXMLElementTagName());
        super.writeAttributes(out);

        if (full) {
            out.writeAttribute("lastTribute", Integer.toString(lastTribute));
            out.writeAttribute("convertProgress", Integer.toString(convertProgress));
            writeAttribute(out, "learnableSkill", learnableSkill);
            for (int i = 0; i < wantedGoods.length; i++) {
                if (wantedGoods[i] != null) {
                    String tag = "wantedGoods" + Integer.toString(i);
                    out.writeAttribute(tag, wantedGoods[i].getId());
                }
            }
        } else if (pet != null) {
            writeAttribute(out, "learnableSkill", pet.getSkill());
            GoodsType[] wanted = pet.getWantedGoods();
            int i, j = 0;
            for (i = 0; i < wanted.length; i++) {
                if (wanted[i] != null) {
                    String tag = "wantedGoods" + Integer.toString(j);
                    out.writeAttribute(tag, wanted[i].getId());
                    j++;
                }
            }
        }

        writeChildren(out, player, showAll, toSavedGame);

        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    protected void writeChildren(XMLStreamWriter out, Player player,
                                 boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        PlayerExploredTile pet;

        if (showAll || toSavedGame || player == getOwner()) {
            for (Player p : spokenTo) {
                out.writeStartElement(IS_VISITED_TAG_NAME);
                out.writeAttribute("player", p.getId());
                out.writeEndElement();
            }
            for (Entry<Player, Tension> entry : alarm.entrySet()) {
                out.writeStartElement(ALARM_TAG_NAME);
                out.writeAttribute("player", entry.getKey().getId());
                out.writeAttribute(VALUE_TAG,
                    String.valueOf(entry.getValue().getValue()));
                out.writeEndElement();
            }
            if (missionary != null) {
                out.writeStartElement(MISSIONARY_TAG_NAME);
                missionary.toXML(out, player, showAll, toSavedGame);
                out.writeEndElement();
            }
            for (Unit unit : ownedUnits) {
                out.writeStartElement(OWNED_UNITS_TAG_NAME);
                out.writeAttribute(ID_ATTRIBUTE, unit.getId());
                out.writeEndElement();
            }
            super.writeChildren(out, player, showAll, toSavedGame);

        } else if ((pet = getTile().getPlayerExploredTile(player)) != null) {
            if (hasSpokenToChief(player)) {
                out.writeStartElement(IS_VISITED_TAG_NAME);
                out.writeAttribute("player", player.getId());
                out.writeEndElement();
            }
            if (getAlarm(player) != null) {
                out.writeStartElement(ALARM_TAG_NAME);
                out.writeAttribute("player", player.getId());
                out.writeAttribute(VALUE_TAG, String.valueOf(getAlarm(player).getValue()));
                out.writeEndElement();
            }
            if (pet.getMissionary() != null) {
                out.writeStartElement(MISSIONARY_TAG_NAME);
                pet.getMissionary().toXML(out, player, showAll, toSavedGame);
                out.writeEndElement();
            }
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        owner.addSettlement(this);
        ownedUnits.clear();

        for (int i = 0; i < wantedGoods.length; i++) {
            String tag = WANTED_GOODS_TAG_NAME + Integer.toString(i);
            String wantedGoodsId = getAttribute(in, tag, null);
            wantedGoods[i] = (wantedGoodsId == null) ? null
                : getSpecification().getGoodsType(wantedGoodsId);
        }

        convertProgress = getAttribute(in, "convertProgress", 0);
        lastTribute = getAttribute(in, "lastTribute", 0);
        learnableSkill = getSpecification().getType(in, "learnableSkill", UnitType.class, null);
    }

    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        spokenTo.clear();
        alarm = new HashMap<Player, Tension>();
        missionary = null;
        ownedUnits.clear();
        super.readChildren(in);
    }

    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        if (IS_VISITED_TAG_NAME.equals(in.getLocalName())) {
            Player player = getGame().getFreeColGameObject(in.getAttributeValue(null, "player"),
                                                           Player.class);
            spokenTo.add(player);
            in.nextTag(); // close tag is always generated.
        } else if (ALARM_TAG_NAME.equals(in.getLocalName())) {
            Player player = getGame().getFreeColGameObject(in.getAttributeValue(null, "player"),
                                                           Player.class);
            alarm.put(player, new Tension(getAttribute(in, VALUE_TAG, 0)));
            in.nextTag(); // close element
        } else if (WANTED_GOODS_TAG_NAME.equals(in.getLocalName())) {
            String[] wantedGoodsID = readFromArrayElement(WANTED_GOODS_TAG_NAME,
                                                          in, new String[0]);
            for (int i = 0; i < wantedGoods.length; i++) {
                String goodsId = (i < wantedGoodsID.length) ? wantedGoodsID[i]
                    : null;
                wantedGoods[i] = (goodsId == null || "".equals(goodsId)) ? null
                    : getSpecification().getGoodsType(goodsId);
            }
        } else if (MISSIONARY_TAG_NAME.equals(in.getLocalName())) {
            in.nextTag();
            missionary = updateFreeColGameObject(in, Unit.class);
            missionary.setLocationNoUpdate(this);
            in.nextTag();
        } else if (UNITS_TAG_NAME.equals(in.getLocalName())) {
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (in.getLocalName().equals(Unit.getXMLElementTagName())) {
                    Unit unit = updateFreeColGameObject(in, Unit.class);
                    // @compat 0.10.1
                    if (unit.getLocation() != this) {
                        logger.warning("fixing unit location");
                        unit.setLocation(this);
                    }
                    // end compatibility code
                    add(unit);
                }
            }
        } else if (OWNED_UNITS_TAG_NAME.equals(in.getLocalName())) {
            Unit unit = getFreeColGameObject(in, ID_ATTRIBUTE, Unit.class);
            if (unit.getOwner() != null && !owner.owns(unit)) {
                logger.warning("Error in savegame: unit " + unit.getId()
                               + " does not belong to settlement " + getId());
            } else {
                ownedUnits.add(unit);
                owner.setUnit(unit);
            }
            in.nextTag();
        } else {
            super.readChild(in);
        }
    }

    /**
     * Partial writer, so that "remove" messages can be brief.
     *
     * @param out The target stream.
     * @param fields The fields to write.
     * @throws XMLStreamException If there are problems writing the stream.
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields)
        throws XMLStreamException {
        toXMLPartialByClass(out, getClass(), fields);
    }

    /**
     * Partial reader, so that "remove" messages can be brief.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException If there are problems reading the stream.
     */
    @Override
    public void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(getName());
        s.append(" at (").append(tile.getX());
        s.append(",").append(tile.getY()).append(")");
        return s.toString();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "indianSettlement".
     */
    public static String getXMLElementTagName() {
        return "indianSettlement";
    }
}
