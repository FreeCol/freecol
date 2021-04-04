/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.util.Utils;


/**
 * A trade item consisting of a change of stance.
 */
public class StanceTradeItem extends TradeItem {
    
    public static final String TAG = "stanceTradeItem";

    /** The stance between source and destination. */
    private Stance stance;

    
    /**
     * Creates a new {@code StanceTradeItem} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param source The source {@code Player}.
     * @param destination The destination {@code Player}.
     * @param stance The {@code Stance} to trade.
     */
    public StanceTradeItem(Game game, Player source, Player destination,
                           Stance stance) {
        super(game, Messages.nameKey("model.tradeItem.stance"),
              source, destination);

        this.stance = stance;
    }

    /**
     * Creates a new {@code StanceTradeItem} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param xr A {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is an error reading the stream.
     */
    public StanceTradeItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(game, xr);
    }


    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return stance != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnique() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLabel() {
        return StringTemplate.key(stance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stance getStance() {
        return stance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStance(Stance stance) {
        this.stance = stance;
    }

    /**
     * {@inheritDoc}
     */
    public int evaluateFor(Player player) {
        final Stance stance = getStance();
        final double ratio = player.getStrengthRatio(getOther(player), false);
        int value = (int)Math.round(100 * ratio);
        switch (stance) {
        case WAR:
            if (ratio < 0.33) value = INVALID_TRADE_ITEM;
            else if (ratio < 0.5) value = -value;
            break;
        case PEACE: case CEASE_FIRE: case ALLIANCE:
            if (ratio > 0.66) value = INVALID_TRADE_ITEM;
            else if (ratio > 0.5) value = -value;
            else if (ratio < 0.33) value = 1000;
            break;
        case UNCONTACTED: default:
            value = INVALID_TRADE_ITEM;
            break;
        }
        return value;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        StanceTradeItem o = copyInCast(other, StanceTradeItem.class);
        if (o == null || !super.copyIn(o)) return false;
        this.stance = o.getStance();
        return true;
    }


    // Serialization

    private static final String STANCE_TAG = "stance";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(STANCE_TAG, stance);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        stance = xr.getAttribute(STANCE_TAG, Stance.class, (Stance)null);
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
    public boolean equals(Object o) {
        if (o instanceof StanceTradeItem) {
            StanceTradeItem other = (StanceTradeItem)o;
            return this.stance == other.stance
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
        return 37 * hash + Utils.hashCode(this.stance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append('[').append(getId())
            .append(' ').append(stance).append(']');
        return sb.toString();
    }
}
