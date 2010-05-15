/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
    
    /**
     * The goods to change hands.
     */
    private Goods goods;

    /**
     * The settlement where the trade is to take place.
     */
    private Settlement settlement;
        
    /**
     * Creates a new <code>GoodsTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param goods a <code>Goods</code> value
     * @param settlement a <code>Settlement</code> value
     */
    public GoodsTradeItem(Game game, Player source, Player destination, Goods goods, Settlement settlement) {
        super(game, "tradeItem.goods", source, destination);
        this.goods = goods;
        this.settlement = settlement;
    }

    /**
     * Creates a new <code>GoodsTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public GoodsTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Get the <code>Settlement</code> value.
     *
     * @return a <code>Settlement</code> value
     */
    public final Settlement getSettlement() {
        return settlement;
    }

    /**
     * Set the <code>Settlement</code> value.
     *
     * @param newSettlement The new Settlement value.
     */
    public final void setSettlement(final Settlement newSettlement) {
        this.settlement = newSettlement;
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        if (!(goods.getLocation() instanceof Unit)) {
            return false;
        }
        Unit unit = (Unit) goods.getLocation();
        if (unit.getOwner() != getSource()) {
            return false;
        }
        if (settlement != null && settlement.getOwner() == getDestination()) {
            return true;
        } else {
            return false;
        }

    }
    
    /**
     * Returns whether this TradeItem must be unique. This is true for
     * the StanceTradeItem and the GoldTradeItem, and false for all
     * others.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isUnique() {
        return false;
    }

    /**
     * Make the trade.
     */
    public void makeTrade() {
        Location where = goods.getLocation();
        where.remove(goods);
        settlement.add(goods);
    }


    /**
     * Get the goods to trade.
     *
     * @return The goods to trade.
     */
    @Override
    public Goods getGoods() {
        return goods;
    }

    /**
     * Set the goods to trade.
     *
     * @param goods The new <code>Goods</code> to trade.
     */
    @Override
    public void setGoods(Goods goods) {
        this.goods = goods;
    }


    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        super.readFromXMLImpl(in);
        this.settlement = (Settlement) game.getFreeColGameObject(in.getAttributeValue(null, "settlement"));
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Goods.getXMLElementTagName())) {
                this.goods = new Goods(game, in);
            }
        }
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *  
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        super.toXMLImpl(out);
        out.writeAttribute("settlement", settlement.getId());
        this.goods.toXML(out);
        out.writeEndElement();
    }
    
    /**
     * Gets the tag name of the root element representing this object.
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "goodsTradeItem";
    }

}


