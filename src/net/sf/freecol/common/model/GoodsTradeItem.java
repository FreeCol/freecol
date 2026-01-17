/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import java.util.Objects;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * A trade item consisting of some goods.
 */
public class GoodsTradeItem extends TradeItem {
    
    public static final String TAG = "goodsTradeItem";

    /** The goods to change hands. */
    protected Goods goods;

    
    /**
     * Creates a new {@code GoodsTradeItem} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param source The source {@code Player}.
     * @param destination The destination {@code Player}.
     * @param goods The {@code Goods} to trade.
     */
    public GoodsTradeItem(Game game, Player source, Player destination,
                          Goods goods) {
        super(game, Messages.nameKey("model.tradeItem.goods"),
              source, destination);

        this.goods = goods;
    }

    /**
     * Creates a new {@code GoodsTradeItem} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is an error reading the stream.
     */
    public GoodsTradeItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(game, xr);
    }


    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return goods != null && goods.getType() != null
            && goods.getAmount() > 0
            && (goods.getLocation() instanceof Ownable)
            && getSource().owns((Ownable)goods.getLocation());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnique() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLabel() {
        return goods.getLabel(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Goods getGoods() {
        return goods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGoods(Goods goods) {
        this.goods = goods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int evaluateFor(Player player) {
        // Guard Clause: If the item isn't valid, don't bother with math.
        if (!isValid()) {
            return INVALID_TRADE_ITEM;
        }

        final Market market = player.getMarket();
        final Goods goods = getGoods();

        // Scenario: The player is the one giving the goods (Source).
        if (getSource() == player) {
            return (market == null) 
                ? -2 * goods.getAmount() 
                : market.getBidPrice(goods.getType(), goods.getAmount());
        }

        // Scenario: The player is receiving the goods (Destination).
        if (market == null) {
            return 2 * goods.getAmount();
        }

        // Apply tax logic for receiving goods.
        double salePrice = market.getSalePrice(goods.getType(), goods.getAmount());
        double taxFactor = 1.0 - player.getTax() / 100.0;
        
        return (int) Math.round(salePrice * taxFactor);
    }

    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        goods.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        goods = null;

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (Goods.TAG.equals(tag)) {
            goods = new Goods(game, xr);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        GoodsTradeItem o = copyInCast(other, GoodsTradeItem.class);
        if (o == null || !super.copyIn(o)) return false;
        
        Goods g = o.getGoods();
        if (g == null) {
            this.goods = null;
        } else if (this.goods == null) {
            this.goods = g;
        } else {
            return this.goods.copyIn(g);
        }
        return true;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        GoodsTradeItem other = (GoodsTradeItem) o;
        return Objects.equals(this.goods, other.goods)
            && super.equals(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.goods);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "[id=" + getId()
            + ", goods=" + ((goods == null) ? "null" 
                : (goods.getAmount() + " " + Messages.getName(goods)))
            + "]";
    }
}
