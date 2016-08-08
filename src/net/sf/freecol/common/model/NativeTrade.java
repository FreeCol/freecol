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
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Container class for the information that persists while a
 * native trade session is underway.
 */
public class NativeTrade extends FreeColGameObject {

    /** Container class for goods being traded. */
    public static class HaggleItem {
        public AbstractGoods goods;
        public int price;
        public int haggleCount;
    };
    
    /** A template to use as a magic cookie for aborted trades. */
    private static final StringTemplate abortTrade
        = StringTemplate.template("");

    /** The type of native trade command. */
    public static enum NativeTradeAction {
        UPDATE(false),   // Server update of a session, rest are client-sent
        OPEN(false),     // Start a new trade session
        CLOSE(true),     // End an existing session
        BUY(false),      // Buy goods
        SELL(false),     // Sell goods
        GIFT(false),     // Gift goods
        // Rejections
        INVALID(true),   // Trade is completely invalid
        HOSTILE(true),   // Natives are hostile
        HAGGLE(true);    // Trade failed due to too much haggling

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

    /** The action underway. */
    private NativeTradeAction action;
    
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
     * Simple constructor, used in FreeColGameObject.newInstance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The identifier (ignored).
     */
    public NativeTrade(Game game, String id) {
        super(game, id);
    }

    /**
     * Create a new trade session.
     *
     * @param action The <code>NativeTradeAction</code> to perform.
     * @param unit The <code>Unit</code> that is trading.
     * @param is The <code>IndianSettlement</code> to trade with.
     */
    public NativeTrade(NativeTradeAction action, Unit unit, IndianSettlement is) {
        this(unit.getGame(), ""); // Identifier not needed

        this.action = action;
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

    public NativeTradeAction getAction() {
        return this.action;
    }

    public NativeTrade setAction(NativeTradeAction action) {
        this.action = action;
        return this;
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

    public List<HaggleItem> getBuying() {
        return Collections.<HaggleItem>emptyList();
    }

    public List<HaggleItem> getSelling() {
        return Collections.<HaggleItem>emptyList();
    }

    
    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    public boolean isInternable() {
        return false;
    }


    // Serialization

    private static final String ACTION_TAG = "action";
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

        xw.writeAttribute(ACTION_TAG, this.action);

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
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        super.readAttributes(xr);

        this.action = xr.getAttribute(ACTION_TAG, NativeTradeAction.class,
                                      (NativeTradeAction)null);

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
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append('[').append(getTagName())
            .append(' ').append(getAction())
            .append(' ').append(getUnit().getId())
            .append(' ').append(getIndianSettlement().getId())
            .append(" buy=").append(getBuy())
            .append(" sell=").append(getSell())
            .append(" gift=").append(getGift())
            .append(" count=").append(getCount())
            .append(']');
        return sb.toString();
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
