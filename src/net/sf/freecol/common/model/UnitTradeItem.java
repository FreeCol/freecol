/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * A trade item consisting of a unit.
 */
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
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public UnitTradeItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(game, xr);

        readFromXML(xr);
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
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(UNIT_TAG, unit);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        unit = xr.getAttribute(getGame(), UNIT_TAG, Unit.class, (Unit)null);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unitTradeItem".
     */
    public static String getXMLElementTagName() {
        return "unitTradeItem";
    }
}
