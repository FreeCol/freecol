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

import net.sf.freecol.common.model.Player.Stance;


/**
 * One of the items a DiplomaticTrade consists of.
 *
 */
public abstract class TradeItem extends FreeColObject {

    /**
     * The game this TradeItem belongs to.
     */
    protected Game game;

    /**
     *  The player who is to provide this item.
     */
    private Player source;

    /**
     * The player who is to receive this item.
     */
    private Player destination;

    /**
     * Creates a new <code>TradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id a <code>String</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     */
    public TradeItem(Game game, String id, Player source, Player destination) {
        this.game = game;
        setId(id);
        this.source = source;
        this.destination = destination;
    }

    /**
     * Creates a new <code>TradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public TradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        this.game = game;
    }

    /**
     * Get the <code>Source</code> value.
     *
     * @return a <code>Player</code> value
     */
    public final Player getSource() {
        return source;
    }

    /**
     * Set the <code>Source</code> value.
     *
     * @param newSource The new Source value.
     */
    public final void setSource(final Player newSource) {
        this.source = newSource;
    }

    /**
     * Get the <code>Destination</code> value.
     *
     * @return a <code>Player</code> value
     */
    public final Player getDestination() {
        return destination;
    }

    /**
     * Set the <code>Destination</code> value.
     *
     * @param newDestination The new Destination value.
     */
    public final void setDestination(final Player newDestination) {
        this.destination = newDestination;
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public abstract boolean isValid();

    /**
     * Returns whether this TradeItem must be unique. This is true for
     * the StanceTradeItem and the GoldTradeItem, and false for all
     * others.
     *
     * @return a <code>boolean</code> value
     */
    public abstract boolean isUnique();

    /**
     * Make the trade.
     */
    public abstract void makeTrade();

    /**
     * Get the colony to trade.
     *
     * @return The colony to trade.
     */
    public Colony getColony() { return null; }

    /**
     * Set the colony to trade.
     *
     * @param colony The new <code>Colony</code> to trade.
     */
    public void setColony(Colony colony) {}

    /**
     * Get the goods to trade.
     *
     * @return The goods to trade.
     */
    public Goods getGoods() { return null; }

    /**
     * Set the goods to trade.
     *
     * @param goods The new <code>Goods</code> to trade.
     */
    public void setGoods(Goods goods) {}

    /**
     * Get the gold to trade.
     *
     * @return The gold to trade.
     */
    public int getGold() { return 0; }

    /**
     * Set the gold to trade.
     *
     * @param gold The new gold value.
     */
    public void setGold(int gold) {}

    /**
     * Get the stance to trade.
     *
     * @return The stance to trade.
     */
    public Stance getStance() { return null; }

    /**
     * Set the stance to trade.
     *
     * @param stance The new <code>Stance</code> to trade.
     */
    public void setStance(Stance stance) {}

    /**
     * Get the unit to trade.
     *
     * @return The unit to trade.
     */
    public Unit getUnit() { return null; }

    /**
     * Set the unit to trade.
     *
     * @param unit The new <code>Unit</code> to trade.
     */
    public void setUnit(Unit unit) {}


    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        String sourceID = in.getAttributeValue(null, "source");
        this.source = (Player) game.getFreeColGameObject(sourceID);
        String destinationID = in.getAttributeValue(null, "destination");
        this.destination = (Player) game.getFreeColGameObject(destinationID);
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
        out.writeAttribute("ID", getId());
        out.writeAttribute("source", this.source.getId());
        out.writeAttribute("destination", this.destination.getId());
    }
    

}

