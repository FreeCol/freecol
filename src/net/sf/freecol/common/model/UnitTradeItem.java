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


public class UnitTradeItem extends TradeItem {
    
    /**
     * The unit to change hands.
     */
    private Unit unit;

        
    /**
     * Creates a new <code>UnitTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param unit an <code>Unit</code> value
     */
    public UnitTradeItem(Game game, Player source, Player destination, Unit unit) {
        super(game, "tradeItem.unit", source, destination);
        this.unit = unit;
    }

    /**
     * Creates a new <code>UnitTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public UnitTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        return unit.getOwner() == getSource()
            && unit.getType().isAvailableTo(getDestination());
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
     * Get the unit to trade.
     *
     * @return The unit to trade.
     */
    @Override
    public Unit getUnit() {
        return unit;
    }

    /**
     * Set the unit to trade.
     *
     * @param unit The new <code>Unit</code> to trade.
     */
    @Override
    public void setUnit(Unit unit) {
        this.unit = unit;
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
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("unit", this.unit.getId());
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
        String unitID = in.getAttributeValue(null, "unit");
        this.unit = (Unit) game.getFreeColGameObject(unitID);
        in.nextTag();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unitTradeItem".
     */
    public static String getXMLElementTagName() {
        return "unitTradeItem";
    }
}
