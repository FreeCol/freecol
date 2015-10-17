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

import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;


/**
 * Represents a <code>TileItem</code> item on a <code>Tile</code>.
 */
public abstract class TileItem extends FreeColGameObject
    implements Locatable, Named {

    /** The tile where the tile item is. */
    protected Tile tile;


    /**
     * Creates a new <code>TileItem</code>.
     *
     * @param game The enclosing <code>Game</code>.
     * @param tile The location of this <code>TileItem</code>.
     */
    public TileItem(Game game, Tile tile) {
        super(game);

        if (tile == null) {
            throw new IllegalArgumentException("Parameter 'tile' must not be 'null'.");
        }
        this.tile = tile;
    }

    /**
     * Creates a new <code>TileItem</code> from an XML stream.
     *
     * @param game The enclosing <code>Game</code>.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public TileItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(game, null);
    }

    /**
     * Initiates a new <code>TileItem</code> with the given
     * identifier.  The object should later be initialized by calling
     * either {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public TileItem(Game game, String id) {
        super(game, id);
    }


    // Routines to be implemented by subclasses.

    /**
     * Get the <code>ZIndex</code> value.
     *
     * @return The z-index.
     */
    public abstract int getZIndex();

    /**
     * Is a tile type compatible with this tile item?
     *
     * @param tileType The <code>TileType</code> to check.
     * @return True if the tile type is compatible.
     */
    public abstract boolean isTileTypeAllowed(TileType tileType);

    /**
     * Applies the production bonus for the given goods type and unit
     * type to the given potential production.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType The <code>UnitType</code> that is to work.
     * @param potential The base potential production.
     * @return The production with resource bonuses.
     */
    public abstract int applyBonus(GoodsType goodsType, UnitType unitType,
                                   int potential);

    /**
     * Does this tile item allow its enclosing tile to produce a given
     * goods type?
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType The optional <code>unitType</code> to produce with.
     * @return True if this tile item produces the goods.
     */
    public abstract boolean canProduce(GoodsType goodsType,
                                       UnitType unitType);

    /**
     * Gets the production modifiers for the given type of goods and unit.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType The optional <code>unitType</code> to produce them.
     * @return A list of the applicable modifiers.
     */
    public abstract List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                          UnitType unitType);

    /**
     * Is this a natural TileItem?
     *
     * @return True if this is a natural <code>TileItem</code>.
     */
    public abstract boolean isNatural();

    /**
     * Is this improvement complete?
     *
     * @return True if complete.
     */
    public abstract boolean isComplete();


    // Interface Locatable

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getLocation() {
        return tile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setLocation(Location newLocation) {
        if (newLocation == null || newLocation instanceof Tile) {
            tile = (Tile)newLocation;
            return true;
        }
        throw new IllegalArgumentException("newLocation is not a Tile");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInEurope() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getTile() {
        return tile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSpaceTaken() {
        return 0;
    }

    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String getNameKey();
}
