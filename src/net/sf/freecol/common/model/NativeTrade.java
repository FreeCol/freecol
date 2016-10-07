/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
        NAK_INVALID(true, false);  // Trade is completely invalid

        /** Does this action close the trade? */
        private final boolean closing;

        /** Should this action originate with a European player? */
        private final boolean fromEuropeans;


        /**
         * Create a new native trade action.
         *
         * @param closing If true this is an action that closes the session.
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
     * Simple constructor, used in FreeColGameObject.newInstance.
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


    private boolean atWar() {
        return this.is.getOwner().atWarWith(this.unit.getOwner());
    }

    public String getKey() {
        return getKey(this.unit, this.is);
    }
    
    public static String getKey(Unit unit, IndianSettlement is) {
        return unit.getId() + "-" + is.getId();
    }
    
    public Unit getUnit() {
        return this.unit;
    }

    public IndianSettlement getIndianSettlement() {
        return this.is;
    }

    public boolean getBuy() {
        return this.buy;
    }

    public boolean canBuy() {
        return getBuy() && !atWar() && this.unit.getSpaceLeft() > 0;
    }
    
    public void setBuy(boolean buy) {
        this.buy = buy;
    }

    public boolean getSell() {
        return this.sell;
    }

    public boolean canSell() {
        return getSell() && !atWar() && this.unit.hasGoodsCargo();
    }
    
    public void setSell(boolean sell) {
        this.sell = sell;
    }
    
    public boolean getGift() {
        return this.gift;
    }

    public boolean canGift() {
        return getGift() && this.unit.hasGoodsCargo();
    }
    
    public void setGift(boolean gift) {
        this.gift = gift;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean getDone() {
        return this.count < 0
            || (!canBuy() && !canSell() && !canGift());
    }

    public void setDone() {
        this.count = -1;
    }

    public boolean hasNotTraded() {
        return getBuy() && getSell() && getGift();
    }

    public List<NativeTradeItem> getUnitToSettlement() {
        return this.unitToSettlement;
    }

    public List<NativeTradeItem> getSettlementToUnit() {
        return this.settlementToUnit;
    }

    public void addToUnit(NativeTradeItem nti) {
        this.unitToSettlement.add(nti);
    }

    public void removeFromUnit(NativeTradeItem nti) {
        removeInPlace(this.unitToSettlement, nti.goodsMatcher());
    }
    
    public boolean isCompatible(final NativeTrade nt) {
        return this.getKey().equals(nt.getKey());
    }

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

    public void mergeFrom(final NativeTrade nt) {
        if (isCompatible(nt)) {
            this.unitToSettlement.clear();
            this.unitToSettlement.addAll(nt.getUnitToSettlement());
            this.settlementToUnit.clear();
            this.settlementToUnit.addAll(nt.getSettlementToUnit());
            this.item = nt.getItem();
        }
    }

    public NativeTradeItem getItem() {
        return this.item;
    }

    public void setItem(NativeTradeItem nti) {
        this.item = nti;
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
                if (NativeTradeItem.getTagName().equals(tag)) {
                    NativeTradeItem nti = new NativeTradeItem(game, xr);
                    if (nti != null) this.settlementToUnit.add(nti);
                } else {
                    logger.warning("SettlementToUnit-item expected, not: " + tag);
                }
            }

        } else if (UNIT_TO_SETTLEMENT_TAG.equals(tag)) {
            while (xr.moreTags()) {
                tag = xr.getLocalName();
                if (NativeTradeItem.getTagName().equals(tag)) {
                    NativeTradeItem nti = new NativeTradeItem(game, xr);
                    if (nti != null) this.unitToSettlement.add(nti);
                } else {
                    logger.warning("UnitToSettlement-item expected, not: " + tag);
                }
            }

        } else if (NativeTradeItem.getTagName().equals(tag)) {
            this.item = new NativeTradeItem(game, xr);

        } else {
            super.readChild(xr);
        }
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        NativeTradeItem item = getItem();
        sb.append('[').append(getTagName())
            .append(' ').append(getUnit().getId())
            .append(' ').append(getIndianSettlement().getId())
            .append(" buy=").append(getBuy())
            .append(" sell=").append(getSell())
            .append(" gift=").append(getGift())
            .append(" count=").append(getCount())
            .append(" item=").append((item == null) ? "null" : item.toString())
            .append(" unitToSettlement[");
        for (NativeTradeItem nti : this.unitToSettlement) {
            sb.append(' ').append(nti.toString());
        }
        sb.append("] settlementToUnit[");
        for (NativeTradeItem nti : this.settlementToUnit) {
            sb.append(' ').append(nti.toString());
        }
        return sb.append(" ]]").toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getTagName(); }

    /**
     * Gets the tag name of the object.
     *
     * @return "nativeTrade".
     */
    public static String getTagName() {
        return "nativeTrade";
    }
}
