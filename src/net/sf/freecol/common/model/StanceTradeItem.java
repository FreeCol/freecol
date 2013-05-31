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

import net.sf.freecol.common.model.Player.Stance;


public class StanceTradeItem extends TradeItem {
    
    /** The stance between source and destination. */
    private Stance stance;

        
    /**
     * Creates a new <code>StanceTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param source The source <code>Player</code>.
     * @param destination The destination <code>Player</code>.
     * @param stance The <code>Stance</code> to trade.
     */
    public StanceTradeItem(Game game, Player source, Player destination,
                           Stance stance) {
        super(game, "tradeItem.stance", source, destination);

        this.stance = stance;
    }

    /**
     * Creates a new <code>StanceTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param in A <code>XMLStreamReader</code> to read from.
     * @exception XMLStreamException if there is an error reading the stream.
     */
    public StanceTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }


    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return stance != null;
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


    // Serialization

    private static final String STANCE_TAG = "stance";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, STANCE_TAG, stance);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        stance = getAttribute(in, STANCE_TAG, Stance.class, (Stance)null);
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
