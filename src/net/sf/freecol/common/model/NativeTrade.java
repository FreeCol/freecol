/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.NativeTradeItem;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Container class for the information that persists while a
 * native trade session is underway.
 */
public class NativeTrade extends FreeColGameObject {

    private static final Logger logger = Logger.getLogger(NativeTrade.class.getName());

    public static final String TAG = "nativeTrade";

    /** A template to use as a magic cookie for aborted trades. */
    private static final StringTemplate abortTrade
        = StringTemplate.template("");

    /** The type of native trade command. */
    public static enum NativeTradeAction {
        // Requests from European trader
        OPEN(false, true),         // Start a new trade session
        CLOSE(true, true),         // End an existing session
        BUY(false, true),          // Buy goods
        SELL(false, true),         // Sell goods
        GIFT(false, true),         // Gift goods
        // Positive responses from native trader
        ACK_OPEN(false, false),    // Open accepted
        ACK_BUY(false, false),     // Purchase accepted
        ACK_SELL(false, false),    // Sale accepted
        ACK_GIFT(false, false),    // Gift accepted
        ACK_BUY_HAGGLE(false,false),    // Haggle accepted
        ACK_SELL_HAGGLE(false,false),   // Haggle accepted
        // Negative responses from native trader
        NAK_GOODS(false, false),   // Gift failed due to storage
        NAK_HAGGLE(true, false),   // Trade failed due to too much haggling
        NAK_HOSTILE(true, false),  // Natives are hostile
        NAK_NOSALE(true, false),   // Nothing to trade
        NAK_INVALID(true, false);  // Trade is completely invalid

        /** Does this action close the trade? */
        private final boolean closing;

        /** Should this action originate with a European player? */
        private final boolean fromEuropeans;


        /**
         * Create a new native trade action.
         *
         * @param closing If true this is an action that closes the session.
         * @param fromEuropeans True if a European action.
         */
        NativeTradeAction(boolean closing, boolean fromEuropeans) {
            this.closing = closing;
            this.fromEuropeans = fromEuropeans;
        }

        /**
         * Is this a closing action?
         *
         * @return True if a closing action.
         */
        public boolean isClosing() {
            return this.closing;
        }

        /**
         * Should this action have come from a European player?
         *
         * @return True if a European action.
         */
        public boolean isEuropean() {
            return this.fromEuropeans;
        }
    };

    /** The unit that is trading. */
    private Unit unit;

    /** The settlement to trade with. */
    private IndianSettlement is;

    /** How many times this trade has been tried. */
    private int count;

    /** True if no purchases made in this trade. */
    private boolean buy;
        
    /** True if no sales made in this trade. */
    private boolean sell;

    /** True if no gifts made in this trade. */
    private boolean gift;

    /** An item under consideration for a transaction. */
    private NativeTradeItem item;
    
    /**
     * The goods on the unit that are being offered for purchase by
     * the settlement.
     */
    private List<NativeTradeItem> unitToSettlement = new ArrayList<>();

    /**
     * The goods in the settlement that are being offered for purchase
     * by the unit.
     */
    private List<NativeTradeItem> settlementToUnit = new ArrayList<>();


    /**
     * Simple constructor, used in Game.newInstance.
     *
     * @param game The enclosing {@code Game}.
     * @param id The identifier (ignored).
     */
    public NativeTrade(Game game, String id) {
        super(game, id);
    }

    /**
     * Create a new trade session.
     *
     * @param unit The {@code Unit} that is trading.
     * @param is The {@code IndianSettlement} to trade with.
     */
    public NativeTrade(Unit unit, IndianSettlement is) {
        this(unit.getGame(), ""); // Identifier not needed

        this.unit = unit;
        this.is = is;
        this.count = 0;
        this.buy = this.sell = this.gift = true;
    }


    /**
     * Check if the trade participants are at war.
     *
     * @return True if the traders are at war.
     */
    private boolean atWar() {
        return this.is.getOwner().atWarWith(this.unit.getOwner());
    }

    /**
     * Get a key for this transaction.
     *
     * @return A suitable key.
     */
    public String getKey() {
        return getNativeTradeKey(this.unit, this.is);
    }

    /**
     * Make a transaction key for a native trade.
     *
     * @param unit The {@code Unit} that is trading.
     * @param is The {@code IndianSettlement} that is trading.
     * @return A suitable key.
     */
    public static String getNativeTradeKey(Unit unit, IndianSettlement is) {
        return unit.getId() + "-" + is.getId();
    }

    /**
     * Get the unit that is trading.
     *
     * @return The {@code Unit} that started the trade.
     */
    public Unit getUnit() {
        return this.unit;
    }

    /**
     * Get the settlement that is trading.
     *
     * @return The {@code IndianSettlement} that is traded with.
     */
    public IndianSettlement getIndianSettlement() {
        return this.is;
    }

    /**
     * Is purchasing available in this transaction?
     *
     * @return True if no blocking purchase has been made.
     */
    public boolean getBuy() {
        return this.buy;
    }

    /**
     * Can the unit owner buy more items in this session at present?
     *
     * @return True if not blocked, at peace, there are purchases to
     *     be made and space available.
     */
    public boolean canBuy() {
        return getBuy() && !atWar() && this.unit.getSpaceLeft() > 0
            && any(getSettlementToUnit(), NativeTradeItem::priceIsValid);
    }

    /**
     * Set the purchase state.
     *
     * @param buy The new purchase state.
     */
    public void setBuy(boolean buy) {
        this.buy = buy;
    }

    /**
     * Is selling available in this transaction?
     *
     * @return True if no blocking sale has been made.
     */
    public boolean getSell() {
        return this.sell;
    }

    /**
     * Can the unit owner buy more items in this session at present?
     *
     * @return True if not blocked, at peace, and there are sales to be made.
     */
    public boolean canSell() {
        return getSell() && !atWar()
            && any(getUnitToSettlement(), NativeTradeItem::priceIsValid);
    }
    
    /**
     * Set the sale state.
     *
     * @param sell The new sale state.
     */
    public void setSell(boolean sell) {
        this.sell = sell;
    }
    
    /**
     * Is giving available in this transaction?
     *
     * @return True if no blocking gift has been made.
     */
    public boolean getGift() {
        return this.gift;
    }

    /**
     * Can the unit owner give more items in this session at present?
     *
     * @return True if not blocked, and the unit has gifts to give.
     */
    public boolean canGift() {
        return getGift() && this.unit.hasGoodsCargo();
    }
    
    /**
     * Set the gift state.
     *
     * @param gift The new gift state.
     */
    public void setGift(boolean gift) {
        this.gift = gift;
    }

    /**
     * Have no trades been performed in this transaction?
     *
     * @return True if no blocking purchase, sale or gift has occurred.
     */
    public boolean hasNotTraded() {
        return getBuy() && getSell() && getGift();
    }

    /**
     * Get the transaction count.
     *
     * @return The transaction count.
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Set the transaction count.
     *
     * @param count The new transaction count.
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Is this transaction complete?
     *
     * @return True if the transaction is over.
     */
    public boolean getDone() {
        return this.count < 0
            || (!canBuy() && !canSell() && !canGift());
    }

    /**
     * Set this transaction as complete.
     */
    public void setDone() {
        this.count = -1;
    }

    /**
     * Get the item being traded.
     *
     * @return The current {@code NativeTradeItem}.
     */
    public NativeTradeItem getItem() {
        return this.item;
    }

    /**
     * Set the item being traded.
     *
     * @param nti The new {@code NativeTradeItem}.
     */
    public void setItem(NativeTradeItem nti) {
        this.item = nti;
    }

    /**
     * Get the list of items the unit is able to offer the settlement.
     *
     * Note: some of these items might be currently invalid.
     *
     * @return A list of {@code NativeTradeItem}s the unit might sell.
     */
    public List<NativeTradeItem> getUnitToSettlement() {
        return this.unitToSettlement;
    }

    /**
     * Get the list of items the settlement is able to offer the unit.
     *
     * Note: some of these items might be currently invalid.
     *
     * @return A list of {@code NativeTradeItem}s the unit might buy.
     */
    public List<NativeTradeItem> getSettlementToUnit() {
        return this.settlementToUnit;
    }

    /**
     * Add an item to the unit list of items.
     *
     * @param nti The {@code NativeTradeItem} to add.
     */
    public void addToUnit(NativeTradeItem nti) {
        this.unitToSettlement.add(nti);
    }

    /**
     * Remove an item from the unit list of items.
     *
     * @param nti The {@code NativeTradeItem} to remove.
     */
    public void removeFromUnit(NativeTradeItem nti) {
        removeInPlace(this.unitToSettlement, nti.goodsMatcher());
    }

    /**
     * Is another native trade compatible with this one?
     *
     * @param nt The other {@code NativeTrade}.
     * @return True if the other trade is compatible.
     */
    public boolean isCompatible(final NativeTrade nt) {
        return this.getKey().equals(nt.getKey());
    }

    /**
     * Raw initialization of the unit and settlement list.
     * Does not do pricing!
     */
    public void initialize() {
        final Player unitPlayer = this.unit.getOwner();
        final Player settlementPlayer = this.is.getOwner();
        final Game game = this.unit.getGame();
        for (Goods g : this.unit.getGoodsList()) {
            this.unitToSettlement.add(new NativeTradeItem(game,
                    unitPlayer, settlementPlayer, g));
        }
        for (Goods g : this.is.getSellGoods(this.unit)) {
            this.settlementToUnit.add(new NativeTradeItem(game,
                    settlementPlayer, unitPlayer, g));
        }
    }

    /**
     * Merge another compatible native trade into this one.
     *
     * @param nt The {@code NativeTrade} to merge.
     */
    public void mergeFrom(final NativeTrade nt) {
        if (isCompatible(nt) && !this.equals(nt)) {
            this.unitToSettlement.clear();
            this.unitToSettlement.addAll(nt.getUnitToSettlement());
            this.settlementToUnit.clear();
            this.settlementToUnit.addAll(nt.getSettlementToUnit());
            this.item = nt.getItem();
        }
    }

    /**
     * Limit the number of items offered by the settlement.
     *
     * Used in the client to implement a Col1 restriction.
     *
     * @param n The number of items to offer.
     */
    public void limitSettlementToUnit(int n) {
        List<NativeTradeItem> best = transform(this.settlementToUnit,
            NativeTradeItem::priceIsValid,
            Function.<NativeTradeItem>identity(),
            NativeTradeItem.descendingPriceComparator);
        if (best.size() <= n) return;
        for (NativeTradeItem nti : best.subList(n, best.size())) {
            nti.setPrice(NativeTradeItem.PRICE_INVALID);
        }
    }
            
    /**
     * Choose the next available upward haggling price.
     *
     * @param price The initial price.
     * @return The new upward haggled price.
     */
    public static int haggleUp(int price) {
        return (price * 11) / 10;
    }

    /**
     * Choose the next available downward haggling price.
     *
     * @param price The initial price.
     * @return The new downward haggled price.
     */
    public static int haggleDown(int price) {
        return (price * 9) / 10;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    public boolean isInternable() {
        return false;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        NativeTrade o = copyInCast(other, NativeTrade.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        this.unit = game.updateRef(o.getUnit());
        this.is = game.updateRef(o.getIndianSettlement());
        this.count = o.getCount();
        this.buy = o.getBuy();
        this.sell = o.getSell();
        this.gift = o.getGift();
        this.item = game.update(o.getItem(), false);
        this.unitToSettlement = game.update(o.getUnitToSettlement(), false);
        this.settlementToUnit = game.update(o.getSettlementToUnit(), false);
        return true;
    }


    // Serialization

    private static final String BUY_TAG = "buy";
    private static final String COUNT_TAG = "count";
    private static final String GIFT_TAG = "gift";
    private static final String SELL_TAG = "sell";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String SETTLEMENT_TO_UNIT_TAG = "settlementToUnit";
    private static final String UNIT_TAG = "unit";
    private static final String UNIT_TO_SETTLEMENT_TAG = "unitToSettlement";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(BUY_TAG, this.buy);
        
        xw.writeAttribute(COUNT_TAG, this.count);
        
        xw.writeAttribute(GIFT_TAG, this.gift);

        xw.writeAttribute(SELL_TAG, this.sell);

        xw.writeAttribute(SETTLEMENT_TAG, this.is);

        xw.writeAttribute(UNIT_TAG, this.unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        xw.writeStartElement(SETTLEMENT_TO_UNIT_TAG);

        for (NativeTradeItem nti : this.settlementToUnit) nti.toXML(xw);

        xw.writeEndElement();

        xw.writeStartElement(UNIT_TO_SETTLEMENT_TAG);

        for (NativeTradeItem nti : this.unitToSettlement) nti.toXML(xw);

        xw.writeEndElement();

        if (this.item != null) this.item.toXML(xw);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        super.readAttributes(xr);

        this.buy = xr.getAttribute(BUY_TAG, false);

        this.count = xr.getAttribute(COUNT_TAG, -1);
        
        this.gift = xr.getAttribute(GIFT_TAG, false);

        this.sell = xr.getAttribute(SELL_TAG, false);

        this.is = xr.getAttribute(game, SETTLEMENT_TAG,
            IndianSettlement.class, (IndianSettlement)null);

        this.unit = xr.getAttribute(game, UNIT_TAG, Unit.class, (Unit)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers
        this.unitToSettlement.clear();
        this.settlementToUnit.clear();
        this.item = null;

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        String tag = xr.getLocalName();
        Game game = getGame();

        if (SETTLEMENT_TO_UNIT_TAG.equals(tag)) {
            while (xr.moreTags()) {
                tag = xr.getLocalName();
                if (NativeTradeItem.TAG.equals(tag)) {
                    this.settlementToUnit.add(new NativeTradeItem(game, xr));
                } else {
                    logger.warning("SettlementToUnit-item expected, not: " + tag);
                }
            }

        } else if (UNIT_TO_SETTLEMENT_TAG.equals(tag)) {
            while (xr.moreTags()) {
                tag = xr.getLocalName();
                if (NativeTradeItem.TAG.equals(tag)) {
                    this.unitToSettlement.add(new NativeTradeItem(game, xr));
                } else {
                    logger.warning("UnitToSettlement-item expected, not: " + tag);
                }
            }

        } else if (NativeTradeItem.TAG.equals(tag)) {
            this.item = new NativeTradeItem(game, xr);

        } else {
            super.readChild(xr);
        }
    }
        
    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        NativeTradeItem item = getItem();
        sb.append('[').append(TAG)
            .append(' ').append(getUnit().getId())
            .append(' ').append(getIndianSettlement().getId())
            .append(" buy=").append(getBuy())
            .append(" sell=").append(getSell())
            .append(" gift=").append(getGift())
            .append(" count=").append(getCount())
            .append(" item=").append((item == null) ? "null" : item.toString())
            .append(" unitToSettlement[");
        for (NativeTradeItem nti : this.unitToSettlement) {
            sb.append(' ').append(nti);
        }
        sb.append("] settlementToUnit[");
        for (NativeTradeItem nti : this.settlementToUnit) {
            sb.append(' ').append(nti);
        }
        return sb.append(" ]]").toString();
    }
}
