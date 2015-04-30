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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.util.Utils;


/**
 * One of the items a DiplomaticTrade consists of.
 */
public abstract class TradeItem extends FreeColObject {

    /** The game this TradeItem belongs to. */
    protected final Game game;

    /** The player who is to provide this item. */
    private Player source;

    /** The player who is to receive this item. */
    private Player destination;


    /**
     * Creates a new <code>TradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     * @param source The source <code>Player</code>.
     * @param destination The destination <code>Player</code>.
     */
    public TradeItem(Game game, String id, Player source, Player destination) {
        setId(id);
        this.game = game;
        this.source = source;
        this.destination = destination;
    }

    /**
     * Creates a new <code>TradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public TradeItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
        this.game = game;

        readFromXML(xr);
    }


    /**
     * Gets the game.  The subclasses need this.
     *
     * @return The game.
     */
    protected final Game getGame() {
        return game;
    }

    /**
     * Get the source player.
     *
     * @return The source <code>Player</code>.
     */
    public final Player getSource() {
        return source;
    }

    /**
     * Set the source player.
     *
     * @param newSource The new source <code>Player</code>.
     */
    public final void setSource(final Player newSource) {
        this.source = newSource;
    }

    /**
     * Get the destination player.
     *
     * @return The destination <code>Player</code>.
     */
    public final Player getDestination() {
        return destination;
    }

    /**
     * Set the destination player.
     *
     * @param newDestination The new destination <code>Player</code>.
     */
    public final void setDestination(final Player newDestination) {
        this.destination = newDestination;
    }

    /**
     * Get the other player for this trade item.
     *
     * @param player The <code>Player</code> we do not want.
     * @return The <code>Player</code> we want.
     */
    public final Player getOther(Player player) {
        return (player == source) ? destination : source;
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
     * @return A <code>StringTemplate</code> describing this item.
     */
    public abstract StringTemplate getLabel();

    /**
     * Get the colony to trade.
     *
     * @param game A <code>Game</code> to look for the colony in.
     * @return The <code>Colony</code> to trade.
     */
    public Colony getColony(Game game) { return null; }

    /**
     * Get the goods to trade.
     *
     * @return The <code>Goods</code> to trade.
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
     * @param gold The new gold to trade.
     */
    public void setGold(int gold) {}

    /**
     * Get the victim player to incite war against.
     *
     * @return The <code>Player</code> to trade.
     */
    public Player getVictim() { return null; }

    /**
     * Get the stance to trade.
     *
     * @return The <code>Stance</code> to trade.
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
     * @return The <code>Unit</code> to trade.
     */
    public Unit getUnit() { return null; }

    /**
     * Set the unit to trade.
     *
     * @param unit The new <code>Unit</code> to trade.
     */
    public void setUnit(Unit unit) {}

    /**
     * Evaluate this trade item for a given player.
     *
     * @param player The <code>Player</code> to evaluate for.
     * @return A value for the player, MIN_VALUE for invalid.
     */
    public abstract int evaluateFor(Player player);


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof TradeItem) {
            return Utils.equals(this.source, ((TradeItem)other).source)
                && Utils.equals(this.destination, ((TradeItem)other).destination)
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
        hash = 37 * hash + Utils.hashCode(source);
        return 37 * hash + Utils.hashCode(destination);
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

        xw.writeAttribute(SOURCE_TAG, source);

        xw.writeAttribute(DESTINATION_TAG, destination);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        source = xr.getAttribute(game, SOURCE_TAG,
                                 Player.class, (Player)null);

        destination = xr.getAttribute(game, DESTINATION_TAG,
                                      Player.class, (Player)null);
    }
}
