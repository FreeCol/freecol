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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class GoldTradeItem extends TradeItem {
    
    /**
     * The amount of gold to change hands.
     */
    private int gold;
        
    /**
     * Creates a new <code>GoldTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param gold an <code>int</code> value
     */
    public GoldTradeItem(Game game, Player source, Player destination, int gold) {
        super(game, "tradeItem.gold", source, destination);
        this.gold = gold;
    }

    /**
     * Creates a new <code>GoldTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public GoldTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        return ((gold >= 0) && (getSource().getGold() >= gold));
    }

    /**
     * Returns whether this TradeItem must be unique. This is true for
     * the StanceTradeItem and the GoldTradeItem, and false for all
     * others.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isUnique() {
        return true;
    }

    /**
     * Get the gold to trade.
     *
     * @return The gold to trade.
     */
    @Override
    public int getGold() {
        return gold;
    }

    /**
     * Set the gold to trade.
     *
     * @param gold The new gold value.
     */
    @Override
    public void setGold(int gold) {
        this.gold = gold;
    }


    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        super.readFromXMLImpl(in);
        this.gold = Integer.parseInt(in.getAttributeValue(null, "gold"));
        in.nextTag();
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
        out.writeAttribute("gold", Integer.toString(this.gold));
        out.writeEndElement();
    }
    
    /**
     * Gets the tag name of the root element representing this object.
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "goldTradeItem";
    }

}
