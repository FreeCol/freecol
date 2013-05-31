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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class GoodsTradeItem extends TradeItem {
    
    /** The goods to change hands. */
    private Goods goods;

    /** The settlement where the trade is to take place. */
    private Settlement settlement;
        

    /**
     * Creates a new <code>GoodsTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param source The source <code>Player</code>.
     * @param destination The destination <code>Player</code>.
     * @param goods The <code>Goods</code> to trade.
     * @param settlement The <code>Settlement</code> to trade at.
     */
    public GoodsTradeItem(Game game, Player source, Player destination,
                          Goods goods, Settlement settlement) {
        super(game, "tradeItem.goods", source, destination);

        this.goods = goods;
        this.settlement = settlement;
    }

    /**
     * Creates a new <code>GoodsTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param in The <code>XMLStreamReader</code> to read from.
     */
    public GoodsTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }


    /**
     * Get the <code>Settlement</code> value.
     *
     * @return The <code>Settlement</code>.
     */
    public final Settlement getSettlement() {
        return settlement;
    }

    /**
     * Set the <code>Settlement</code> value.
     *
     * @param newSettlement The new <code>Settlement</code> value.
     */
    public final void setSettlement(final Settlement newSettlement) {
        this.settlement = newSettlement;
    }

    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        if (!(goods.getLocation() instanceof Unit)) return false;

        Unit unit = (Unit) goods.getLocation();
        if (unit.getOwner() != getSource()) return false;

        return settlement != null && settlement.getOwner() == getDestination();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isUnique() {
        return false;
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


    // Serialization

    private static final String SETTLEMENT_TAG = "settlement";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, SETTLEMENT_TAG, settlement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        goods.toXML(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        settlement = getAttribute(in, SETTLEMENT_TAG, getGame(),
                                  Settlement.class, (Settlement)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear containers
        goods = null;

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Game game = settlement.getGame();
        final String tag = in.getLocalName();

        if (Goods.getXMLElementTagName().equals(tag)) {
            goods = new Goods(game, in);

        } else {
            super.readChild(in);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goodsTradeItem".
     */
    public static String getXMLElementTagName() {
        return "goodsTradeItem";
    }
}
