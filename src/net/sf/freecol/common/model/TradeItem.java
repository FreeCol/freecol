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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.util.Utils;


/**
 * One of the items a DiplomaticTrade consists of.
 */
public abstract class TradeItem extends FreeColGameObject {

    /** Flag for validity tests. */
    public static final int INVALID_TRADE_ITEM = Integer.MIN_VALUE;
    
    /** The player who is to provide this item. */
    private Player source;

    /** The player who is to receive this item. */
    private Player destination;


    /**
     * Creates a new {@code TradeItem} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     * @param source The source {@code Player}.
     * @param destination The destination {@code Player}.
     */
    protected TradeItem(Game game, String id, Player source,
                        Player destination) {
        super(game, id);

        this.source = source;
        this.destination = destination;
    }

    /**
     * Creates a new {@code TradeItem} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected TradeItem(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(game, "");

        readFromXML(xr);
    }


    /**
     * Get the source player.
     *
     * @return The source {@code Player}.
     */
    public final Player getSource() {
        return this.source;
    }

    /**
     * Set the source player.
     *
     * @param newSource The new source {@code Player}.
     */
    public final void setSource(final Player newSource) {
        this.source = newSource;
    }

    /**
     * Get the destination player.
     *
     * @return The destination {@code Player}.
     */
    public final Player getDestination() {
        return this.destination;
    }

    /**
     * Set the destination player.
     *
     * @param newDestination The new destination {@code Player}.
     */
    public final void setDestination(final Player newDestination) {
        this.destination = newDestination;
    }

    /**
     * Get the other player for this trade item.
     *
     * @param player The {@code Player} we do not want.
     * @return The {@code Player} we want.
     */
    public final Player getOther(Player player) {
        return (player == this.source) ? this.destination : this.source;
    }


    // The following routines must be supplied/overridden by the subclasses.

    /**
     * Is this trade item valid?  That is, is the request well formed.
     *
     * @return True if the item is valid.
     */
    public abstract boolean isValid();

    /**
     * Is this trade item unique?
     * This is true for the StanceTradeItem and the GoldTradeItem,
     * and false for all others.
     *
     * @return True if the item is unique.
     */
    public abstract boolean isUnique();

    /**
     * Get a label for this item.
     *
     * @return A {@code StringTemplate} describing this item.
     */
    public abstract StringTemplate getLabel();

    /**
     * Get the colony to trade.
     *
     * @param game A {@code Game} to look for the colony in.
     * @return The {@code Colony} to trade.
     */
    public Colony getColony(Game game) { return null; }

    /**
     * Get the goods to trade.
     *
     * @return The {@code Goods} to trade.
     */
    public Goods getGoods() { return null; }

    /**
     * Set the goods to trade.
     *
     * @param goods The new {@code Goods} to trade.
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
     * @param gold The new gold to trade.
     */
    public void setGold(int gold) {}

    /**
     * Get the victim player to incite war against.
     *
     * @return The {@code Player} to trade.
     */
    public Player getVictim() { return null; }

    /**
     * Get the stance to trade.
     *
     * @return The {@code Stance} to trade.
     */
    public Stance getStance() { return null; }

    /**
     * Set the stance to trade.
     *
     * @param stance The new {@code Stance} to trade.
     */
    public void setStance(Stance stance) {}

    /**
     * Get the unit to trade.
     *
     * @return The {@code Unit} to trade.
     */
    public Unit getUnit() { return null; }

    /**
     * Set the unit to trade.
     *
     * @param unit The new {@code Unit} to trade.
     */
    public void setUnit(Unit unit) {}

    /**
     * Evaluate this trade item for a given player.
     *
     * @param player The {@code Player} to evaluate for.
     * @return A value for the player, INVALID_TRADE_ITEM for invalid.
     */
    public abstract int evaluateFor(Player player);


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInternable() {
        return false;
    }

    
    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        TradeItem o = copyInCast(other, TradeItem.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        this.source = game.updateRef(o.getSource());
        this.destination = game.updateRef(o.getDestination());
        return true;
    }


    // Serialization

    private static final String DESTINATION_TAG = "destination";
    private static final String SOURCE_TAG = "source";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(SOURCE_TAG, this.source);

        xw.writeAttribute(DESTINATION_TAG, this.destination);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.source = xr.getAttribute(getGame(), SOURCE_TAG,
                                      Player.class, (Player)null);

        this.destination = xr.getAttribute(getGame(), DESTINATION_TAG,
                                           Player.class, (Player)null);
    }

    // getXMLTagName left to subclasses


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TradeItem)) return false;
        TradeItem other = (TradeItem)o;
        return Utils.equals(this.source, other.source)
            && Utils.equals(this.destination, other.destination)
            && super.equals(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 37 * hash + Utils.hashCode(this.source);
        return 37 * hash + Utils.hashCode(this.destination);
    }
}
