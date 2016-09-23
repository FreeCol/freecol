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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.NativeTradeItem;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Container class for the information that persists while a
 * native trade session is underway.
 */
public class NativeTrade extends FreeColGameObject {

    /** A template to use as a magic cookie for aborted trades. */
    private static final StringTemplate abortTrade
        = StringTemplate.template("");

    /** The type of native trade command. */
    public static enum NativeTradeAction {
        // Requests from European trader
        OPEN(false),          // Start a new trade session
        CLOSE(true),          // End an existing session
        BUY(false),           // Buy goods
        SELL(false),          // Sell goods
        GIFT(false),          // Gift goods
        // Positive responses from native trader
        ACK_OPEN(false),      // Open accepted
        // Negative responses from native trader
        NAK_INVALID(true),    // Trade is completely invalid
        NAK_HOSTILE(true),    // Natives are hostile
        NAK_HAGGLE(true);     // Trade failed due to too much haggling

        /** Does this action close the trade? */
        private final boolean closing;

        NativeTradeAction(boolean closing) {
            this.closing = closing;
        }

        public boolean isClosing() {
            return this.closing;
        }
    };

    /** Trading result types. */
    public static final int NO_TRADE_GOODS = 0,
                            NO_TRADE = -1,
                            NO_TRADE_HAGGLE = -2,
                            NO_TRADE_HOSTILE = -3;

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

    /**
     * The goods on the unit that are being offered for purchase by
     * the settlement.
     */
    private List<NativeTradeItem> buying = new ArrayList<>();

    /** The goods in the settlement that are being offered for sale. */
    private List<NativeTradeItem> selling = new ArrayList<>();
    

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
        return this.buy && !atWar() && this.unit.getSpaceLeft() > 0;
    }
    
    public void setBuy(boolean buy) {
        this.buy = buy;
    }
    
    public boolean getSell() {
        return this.sell;
    }

    public boolean canSell() {
        return this.sell && !atWar() && this.unit.hasGoodsCargo();
    }
    
    public void setSell(boolean sell) {
        this.sell = sell;
    }
    
    public boolean getGift() {
        return this.gift;
    }

    public boolean canGift() {
        return this.gift && this.unit.hasGoodsCargo();
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
        return this.buy && this.sell && this.gift;
    }

    public List<NativeTradeItem> getBuying() {
        return this.buying;
    }

    public List<NativeTradeItem> getSelling() {
        return this.selling;
    }

    public boolean isCompatible(final NativeTrade nt) {
        return this.getKey().equals(nt.getKey());
    }

    public void initializeBuying() {
        final Player source = this.unit.getOwner();
        final Player destination = this.is.getOwner();
        final Game game = this.unit.getGame();
        for (Goods g : unit.getGoodsList()) {
            this.buying.add(new NativeTradeItem(game, source, destination, g));
        }
    }

    public void initializeSelling() {
        final Player source = this.is.getOwner();
        final Player destination = this.unit.getOwner();
        final Game game = this.unit.getGame();
        for (Goods g : this.is.getSellGoods(this.unit)) {
            this.selling.add(new NativeTradeItem(game, source, destination, g));
        }
    }

    public void mergeFromNatives(final NativeTrade nt) {
        if (isCompatible(nt)) {
            this.buying.clear();
            this.buying.addAll(nt.buying);
            this.selling.clear();
            this.selling.addAll(nt.selling);
        }
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
    private static final String UNIT_TAG = "unit";


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

        for (NativeTradeItem nti : this.buying) nti.toXML(xw);

        for (NativeTradeItem nti : this.selling) nti.toXML(xw);
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
        this.buying.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();
        Game game = getGame();

        if (NativeTradeItem.getTagName().equals(tag)) {
            NativeTradeItem nti = new NativeTradeItem(game, xr);
            if (nti.getSource().isEuropean()) {
                this.buying.add(nti);
            } else {
                this.selling.add(nti);
            }

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
        sb.append('[').append(getTagName())
            .append(' ').append(this.unit.getId())
            .append(' ').append(this.is.getId())
            .append(" buy=").append(this.buy)
            .append(" sell=").append(this.sell)
            .append(" gift=").append(this.gift)
            .append(" count=").append(this.count)
            .append(" buying[");
        for (NativeTradeItem nti : this.buying) {
            sb.append(' ').append(nti.toString());
        }
        sb.append("] selling[");
        for (NativeTradeItem nti : this.selling) {
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
     * Gets the tag name of the root element representing this object.
     *
     * @return "nativeTrade".
     */
    public static String getTagName() {
        return "nativeTrade";
    }
}
