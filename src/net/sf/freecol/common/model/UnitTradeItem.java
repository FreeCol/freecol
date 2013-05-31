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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class UnitTradeItem extends TradeItem {
    
    /** The unit to change hands. */
    private Unit unit;

        
    /**
     * Creates a new <code>UnitTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param source The source <code>Player</code>.
     * @param destination The destination <code>Player</code>.
     * @param unit The <code>Unit</code> to trade.
     */
    public UnitTradeItem(Game game, Player source, Player destination,
                         Unit unit) {
        super(game, "tradeItem.unit", source, destination);

        this.unit = unit;
    }

    /**
     * Creates a new <code>UnitTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param in The <code>XMLStreamReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public UnitTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }


    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return unit.getOwner() == getSource()
            && unit.getType().isAvailableTo(getDestination());
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
    public Unit getUnit() {
        return unit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUnit(Unit unit) {
        this.unit = unit;
    }


    // Serialization

    private static final String UNIT_TAG = "unit";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, UNIT_TAG, unit);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        unit = getAttribute(in, UNIT_TAG, getGame(), Unit.class, (Unit)null);
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
