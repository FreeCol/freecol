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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import org.w3c.dom.Element;


/**
 * Represents locatable goods of a specified type and amount. Use
 * AbstractGoods to represent abstract or potential goods that need
 * not be present in any particular location.
 *
 * @see AbstractGoods
 */
public class Goods extends AbstractGoods implements Locatable, Ownable {

    private static Logger logger = Logger.getLogger(Goods.class.getName());

    /** The game containing these goods. */
    private Game game;

    /**
     * Where the goods are.  This should always be non-null except for the
     * really special case of goods that are in the process of being looted
     * from a ship --- we can not use the ship as it is removed/disposed
     * at once while the looting is still being resolved.
     */
    private Location location;


    /**
     * Creates a standard <code>Goods</code>-instance given the place where
     * the goods is.
     *
     * This constructor only asserts that the game and
     * that the location (if given) can store goods. The goods will not
     * be added to the location (use Location.add for this).
     *
     * @param game The enclosing <code>Game</code>.
     * @param location The <code>Location</code> of the goods (may be null).
     * @param type The type of the goods.
     * @param amount The amount of the goods.
     *
     * @throws IllegalArgumentException if the location cannot store any goods.
     */
    public Goods(Game game, Location location, GoodsType type, int amount) {
        if (game == null) {
            throw new IllegalArgumentException("Null game.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Null type.");
        }
        if (location != null && location.getGoodsContainer() == null) {
            throw new IllegalArgumentException("Can not store goods at: "
                + location.toString());
        }

        this.game = game;
        setSpecification(game.getSpecification());
        setType(type);
        setAmount(amount);
        this.location = location;
    }

    /**
     * Creates a new <code>Goods</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if an error occurs
     */
    public Goods(Game game, FreeColXMLReader xr) throws XMLStreamException {
        this.game = game;
        setSpecification(game.getSpecification());
        readFromXML(xr);
    }

    /**
     * Creates a new <code>Goods</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param e an <code>Element</code> value
     */
    public Goods(Game game, Element e) {
        this.game = game;
        setSpecification(game.getSpecification());
        readFromXMLElement(e);
    }


    /**
     * Get the game containing these goods.
     *
     * @return The <code>Game</code> containing these <code>Goods</code>.
     */
    public Game getGame() {
        return game;
    }

    /**
     * Gets the name of this type of goods.
     *
     * @param sellable Whether these <code>Goods</code> can be sold.
     * @return A template for these <code>Goods</code>.
     */
    public StringTemplate getLabel(boolean sellable) {
        return StringTemplate.template((sellable) ? "model.goods.goodsAmount"
            : "model.goods.goodsBoycotted")
            .addAmount("%amount%", getAmount())
            .add("%goods%", getType().getNameKey());
    }

    /**
     * If the amount of goods is greater than the container can hold,
     * then this method adjusts the amount to the maximum amount possible.
     */
    public void adjustAmount() {
        if (location == null) return;
        GoodsContainer gc = location.getGoodsContainer();
        if (gc != null) {
            int maxAmount = gc.getGoodsCount(getType());
            if (getAmount() > maxAmount) setAmount(maxAmount);
        }
    }


    // Interface Locatable

    /**
     * {@inheritDoc}
     */
    public Location getLocation() {
        return location;
    }

    /**
     * {@inheritDoc}
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInEurope() {
        return (location instanceof Europe)
            || (location instanceof Unit && ((Unit)location).isInEurope());
    }

    /**
     * {@inheritDoc}
     */
    public Tile getTile() {
        return (location == null) ? null : location.getTile();
    }

    /**
     * {@inheritDoc}
     */
    public int getSpaceTaken() {
        return 1;
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    public Player getOwner() {
        return (location instanceof Ownable) ? ((Ownable)location).getOwner()
            : null;
    }

    /**
     * {@inheritDoc}
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Goods) {
            Goods g = (Goods)other;
            return location == g.location && super.equals(g);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int value = 19;
        value = 303 * value + ((location == null) ? 1
            : location.getId().hashCode());
        value = 303 * value + getType().hashCode();
        value = 303 * value + getAmount();
        return value;
    }


    // Serialization

    private static final String LOCATION_TAG = "location";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (location != null) {
            xw.writeAttribute(LOCATION_TAG, (FreeColGameObject)location);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        location = xr.makeLocationAttribute(LOCATION_TAG, game);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "goods";
    }
}
