/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;


/**
 * A trade item consisting of some goods.
 */
public class GoodsTradeItem extends TradeItem {
    
    /** The goods to change hands. */
    private Goods goods;


    /**
     * Creates a new <code>GoodsTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param source The source <code>Player</code>.
     * @param destination The destination <code>Player</code>.
     * @param goods The <code>Goods</code> to trade.
     */
    public GoodsTradeItem(Game game, Player source, Player destination,
                          Goods goods) {
        super(game, Messages.nameKey("model.tradeItem.goods"),
              source, destination);

        this.goods = goods;
    }

    /**
     * Creates a new <code>GoodsTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param xr The <code>FreeColXMLReader</code> to read from.
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
    public int evaluateFor(Player player) {
        final Market market = player.getMarket();
        final Goods goods = getGoods();
        int value;
        if (!isValid()) {
            return Integer.MIN_VALUE;
        } else if (market == null) {
            value = 2 * goods.getAmount();
            if (getSource() == player) value = -value;
        } else {
            if (getSource() == player) {
                value = -market.getBidPrice(goods.getType(), goods.getAmount());
            } else {
                value = market.getSalePrice(goods.getType(), goods.getAmount());
                value = (int)Math.round(value
                    * (1.0 - player.getTax() / 100.0));
            }
        }
        return value;
    }

    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof GoodsTradeItem) {
            return Utils.equals(this.goods, ((GoodsTradeItem)other).goods)
                && super.equals(other);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return 37 * hash + Utils.hashCode(this.goods);
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

        if (Goods.getXMLElementTagName().equals(tag)) {
            goods = new Goods(game, xr);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append("[").append(getId())
            .append(" ").append(goods.getAmount()).append(" ")
            .append(Messages.getName(goods)).append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goodsTradeItem".
     */
    public static String getXMLElementTagName() {
        return "goodsTradeItem";
    }
}
