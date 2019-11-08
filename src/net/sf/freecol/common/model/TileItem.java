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

import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.Map.Layer;
import net.sf.freecol.common.util.LogBuilder;


/**
 * Represents a {@code TileItem} item on a {@code Tile}.
 */
public abstract class TileItem extends FreeColGameObject
    implements Locatable, Named {

    private static final Logger logger = Logger.getLogger(TileItem.class.getName());

    /** The tile where the tile item is. */
    protected Tile tile;


    /**
     * Creates a new {@code TileItem}.
     *
     * @param game The enclosing {@code Game}.
     * @param tile The location of this {@code TileItem}.
     */
    protected TileItem(Game game, Tile tile) {
        super(game);

        if (tile == null) {
            throw new RuntimeException("Tile must not be null: " + this);
        }
        this.tile = tile;
    }

    /**
     * Creates a new {@code TileItem} from an XML stream.
     *
     * @param game The enclosing {@code Game}.
     * @param xr The input stream containing the XML.
     */
    public TileItem(Game game, FreeColXMLReader xr) {
        super(game, null);
    }

    /**
     * Initiates a new {@code TileItem} with the given
     * identifier.  The object should later be initialized by calling
     * either {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public TileItem(Game game, String id) {
        super(game, id);
    }


    // Routines to be implemented by subclasses.

    /**
     * Get the {@code ZIndex} value.
     *
     * @return The z-index.
     */
    public abstract int getZIndex();

    /**
     * Is a tile type compatible with this tile item?
     *
     * @param tileType The {@code TileType} to check.
     * @return True if the tile type is compatible.
     */
    public abstract boolean isTileTypeAllowed(TileType tileType);

    /**
     * Applies the production bonus for the given goods type and unit
     * type to the given potential production.
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType The {@code UnitType} that is to work.
     * @param potential The base potential production.
     * @return The production with resource bonuses.
     */
    public abstract int applyBonus(GoodsType goodsType, UnitType unitType,
                                   int potential);

    /**
     * Does this tile item allow its enclosing tile to produce a given
     * goods type?
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType The optional {@code unitType} to produce with.
     * @return True if this tile item produces the goods.
     */
    public abstract boolean canProduce(GoodsType goodsType,
                                       UnitType unitType);

    /**
     * Gets the production modifiers for the given type of goods and unit.
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType The optional {@code unitType} to produce them.
     * @return A stream of the applicable modifiers.
     */
    public abstract Stream<Modifier> getProductionModifiers(GoodsType goodsType,
                                                            UnitType unitType);

    /**
     * Is this a natural TileItem?
     *
     * @return True if this is a natural {@code TileItem}.
     */
    public abstract boolean isNatural();

    /**
     * Is this improvement complete?
     *
     * @return True if complete.
     */
    public abstract boolean isComplete();

    /**
     * Get the layer associated with this tile item.
     *
     * @return The layer.
     */
    public abstract Layer getLayer();


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
        throw new RuntimeException("newLocation is not a Tile: "
            + newLocation);
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


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        if (this.tile == null) {
            lb.add("\n  Tile item with no tile: ", this.getId());
            result = result.fail();
        }
        return result;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        TileItem o = copyInCast(other, TileItem.class);
        if (o == null || !super.copyIn(o)) return false;
        this.tile = getGame().updateRef(o.getTile());
        return true;
    }
}
