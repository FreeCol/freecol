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
        super(game, "tradeItem.gold", source, destination);
        this.gold = gold;
    }

    /**
     * Creates a new <code>GoldTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param in The <code>XMLStreamReader</code> to read from.
     * @exception XMLStreamException if there is an error reading the stream.
     */
    public GoldTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }


    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return gold >= 0 && getSource().checkGold(gold);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUnique() {
        return true;
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


    // Serialization

    private static final String GOLD_TAG = "gold";

    /**
     * {@inheritDoc}
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, GOLD_TAG, gold);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        gold = getAttribute(in, GOLD_TAG, UNDEFINED);
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goldTradeItem".
     */
    public static String getXMLElementTagName() {
        return "goldTradeItem";
    }
}
