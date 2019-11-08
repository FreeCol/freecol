/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
 * A trade item consisting of a unit.
 */
public class UnitTradeItem extends TradeItem {
    
    public static final String TAG = "unitTradeItem";

    /** The unit to change hands. */
    private Unit unit;


    /**
     * Creates a new {@code UnitTradeItem} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param source The source {@code Player}.
     * @param destination The destination {@code Player}.
     * @param unit The {@code Unit} to trade.
     */
    public UnitTradeItem(Game game, Player source, Player destination,
                         Unit unit) {
        super(game, Messages.nameKey("model.tradeItem.unit"),
              source, destination);

        this.unit = unit;
    }

    /**
     * Creates a new {@code UnitTradeItem} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public UnitTradeItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(game, xr);
    }


    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return unit.getOwner() == getSource()
            && unit.getType().isAvailableTo(getDestination());
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
        return unit.getLabel();
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

    /**
     * {@inheritDoc}
     */
    public int evaluateFor(Player player) {
        final Unit unit = getUnit();
        return (!isValid()) ? INVALID_TRADE_ITEM
            : (getSource() == player)
            ? ((player.isAI() && player.getUnitCount() < 10)
                ? TradeItem.INVALID_TRADE_ITEM
                : -unit.evaluateFor(player))
            : unit.evaluateFor(player);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        UnitTradeItem o = copyInCast(other, UnitTradeItem.class);
        if (o == null || !super.copyIn(o)) return false;
        this.unit = getGame().updateRef(o.getUnit());
        return true;
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
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof UnitTradeItem) {
            return Utils.equals(this.unit, ((UnitTradeItem)other).unit)
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
        return 37 * hash + Utils.hashCode(this.unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append('[').append(getId())
            .append(' ').append(unit.getId()).append(']');
        return sb.toString();
    }
}
