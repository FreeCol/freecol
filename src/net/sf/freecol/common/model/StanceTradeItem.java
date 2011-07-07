/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import net.sf.freecol.common.model.Player.Stance;


public class StanceTradeItem extends TradeItem {
    
    /**
     * The stance between source and destination.
     */
    private Stance stance;

        
    /**
     * Creates a new <code>StanceTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param stance an <code>Stance</code> value
     */
    public StanceTradeItem(Game game, Player source, Player destination,
                           Stance stance) {
        super(game, "tradeItem.stance", source, destination);
        this.stance = stance;
    }

    /**
     * Creates a new <code>StanceTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public StanceTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        return stance != null;
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
     * Get the stance to trade.
     *
     * @return The stance to trade.
     */
    @Override
    public Stance getStance() {
        return stance;
    }

    /**
     * Set the stance to trade.
     *
     * @param stance The new <code>Stance</code> to trade.
     */
    @Override
    public void setStance(Stance stance) {
        this.stance = stance;
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     * To be overridden by any object that uses
     * the toXML(XMLStreamWriter, String) call.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("stance", this.stance.toString());
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        super.readFromXMLImpl(in);
        this.stance = Enum.valueOf(Stance.class,
            in.getAttributeValue(null, "stance"));
        in.nextTag();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "stanceTradeItem".
     */
    public static String getXMLElementTagName() {
        return "stanceTradeItem";
    }
}
