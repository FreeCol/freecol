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


/**
 * A trade item consisting of just some gold.
 */
public class GoldTradeItem extends TradeItem {
    
    /** The amount of gold to change hands. */
    private int gold;

        
    /**
     * Creates a new <code>GoldTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param source The source <code>Player</code>.
     * @param destination The destination <code>Player</code>.
     * @param gold The amount of gold.
     */
    public GoldTradeItem(Game game, Player source, Player destination,
                         int gold) {
        super(game, Messages.nameKey("model.tradeItem.gold"),
              source, destination);
        this.gold = gold;
    }

    /**
     * Creates a new <code>GoldTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is an error reading the stream.
     */
    public GoldTradeItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(game, xr);
    }


    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return gold >= 0 && getSource().checkGold(gold);
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
        return StringTemplate.template(Messages.descriptionKey("model.tradeItem.gold"))
            .addAmount("%amount%", gold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGold() {
        return gold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGold(int gold) {
        this.gold = gold;
    }

    /**
     * {@inheritDoc}
     */
    public int evaluateFor(Player player) {
        int gold = getGold();
        return (!isValid()) ? Integer.MIN_VALUE
            : (getSource() == player) ? -gold
            : gold;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof GoldTradeItem) {
            return this.gold == ((GoldTradeItem)other).gold
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
        return 37 * hash + this.gold;
    }


    // Serialization

    private static final String GOLD_TAG = "gold";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(GOLD_TAG, gold);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        gold = xr.getAttribute(GOLD_TAG, UNDEFINED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append("[").append(getId()).append(" ").append(gold).append("]");
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
     * @return "goldTradeItem".
     */
    public static String getXMLElementTagName() {
        return "goldTradeItem";
    }
}
