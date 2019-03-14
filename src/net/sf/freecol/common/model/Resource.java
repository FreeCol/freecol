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
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.Map.Layer;
import net.sf.freecol.common.util.LogBuilder;


/**
 * Represents a production resource, such as prime tobacco, or an ore
 * vein, located on a Tile. A resource may be exhausted. In the
 * original game, only resources that produced silver were exhausted.
 */
public class Resource extends TileItem {

    private static final Logger logger = Logger.getLogger(Resource.class.getName());

    public static final String TAG = "resource";

    /** Some resources are unlimited. */
    private static final int UNLIMITED = -1;


    /** The type of resource. */
    private ResourceType type;

    /** The amount of the resource present. */
    private int quantity;


    /**
     * Creates a standard {@code Resource}-instance.
     *
     * This constructor asserts that the game, tile and type are valid.
     *
     * @param game The enclosing {@code Game}.
     * @param tile The {@code Tile} on which this object sits.
     * @param type The {@code ResourceType} of this Resource.
     * @param quantity The quantity of resource.
     */
    public Resource(Game game, Tile tile, ResourceType type, int quantity) {
        super(game, tile);

        if (type == null) {
            throw new RuntimeException("Type must not be null: " + this);
        }
        this.type = type;
        this.quantity = quantity;
    }

    /**
     * Creates a standard {@code Resource}-instance.
     *
     * This constructor asserts that the game, tile and type are valid.
     *
     * @param game The enclosing {@code Game}.
     * @param tile The {@code Tile} on which this object sits.
     * @param type The {@code ResourceType} of this Resource.
     */
    public Resource(Game game, Tile tile, ResourceType type) {
        this(game, tile, type, type.getMaxValue());
    }

    /**
     * Creates new {@code Resource}.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public Resource(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the type of this resource.
     *
     * @return The resource type.
     */
    public ResourceType getType() {
        return type;
    }

    /**
     * Is this an unlimited resource?
     *
     * @return True if this is an unlimited resource.
     */
    public boolean isUnlimited() {
        return quantity == UNLIMITED;
    }

    /**
     * Get the resource quantity.
     *
     * @return The resource quantity.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Set the resource quantity.
     *
     * @param newQuantity The new resource quantity.
     */
    public void setQuantity(int newQuantity) {
        quantity = newQuantity;
    }

    /**
     * Get the best goods type to produce here.
     *
     * @return The best goods type.
     */
    public GoodsType getBestGoodsType() {
        return type.getBestGoodsType();
    }

    /**
     * Reduce the available quantity by the bonus output of
     * {@code GoodsType}.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @param unitType The producing {@code UnitType}.
     * @param potential The base potential of the tile.
     * @return The new quantity of resource.
     */
    public int useQuantity(GoodsType goodsType, UnitType unitType,
                           int potential) {
        // Return UNLIMITED here if not limited resource?
        return useQuantity(applyBonus(goodsType, unitType, potential)
            - potential);
    }

    /**
     * Reduces the available quantity.
     *
     * @param usedQuantity The quantity that was used up.
     * @return The final value of quantity.
     */
    private int useQuantity(int usedQuantity) {
        if (quantity == UNLIMITED) {
            ; // No change
        } else if (quantity >= usedQuantity) {
            quantity -= usedQuantity;
        } else {
            // Shouldn't generally happen.  Do something more drastic here?
            logger.severe("Insufficient quantity in " + this);
            quantity = 0;
        }
        return quantity;
    }


    // Implement Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return getType().getNameKey();
    }


    // Interface TileItem

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getZIndex() {
        return Tile.RESOURCE_ZINDEX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTileTypeAllowed(TileType tileType) {
        return tileType.canHaveResourceType(getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int applyBonus(GoodsType goodsType, UnitType unitType,
                          int potential) {
        int amount = -potential + (int)applyModifiers(potential, null,
            type.getModifiers(goodsType.getId(), unitType));
        return potential
            + ((quantity == UNLIMITED || quantity > amount) ? amount
                : quantity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canProduce(GoodsType goodsType, UnitType unitType) {
        if (goodsType == null) return false;
        // The presence of a resource can give a tile the ability to
        // produce a goods type.
        return (int)applyModifiers(0f, getGame().getTurn(),
            getProductionModifiers(goodsType, unitType)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Modifier> getProductionModifiers(GoodsType goodsType,
                                                   UnitType unitType) {
        return (goodsType == null) ? Stream.<Modifier>empty()
            : getType().getModifiers(goodsType.getId(), unitType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNatural() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isComplete() {
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Layer getLayer() {
        return Layer.RESOURCES;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        if (type == null) {
            lb.add("\n  Resource without type: ", getId());
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
        Resource o = copyInCast(other, Resource.class);
        if (o == null || !super.copyIn(o)) return false;
        this.type = o.getType();
        this.quantity = o.getQuantity();
        return true;
    }


    // Serialization

    private static final String QUANTITY_TAG = "quantity";
    private static final String TILE_TAG = "tile";
    private static final String TYPE_TAG = "type";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(TILE_TAG, getTile());

        xw.writeAttribute(TYPE_TAG, getType());

        xw.writeAttribute(QUANTITY_TAG, quantity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(xr);

        tile = xr.findFreeColGameObject(getGame(), TILE_TAG,
                                        Tile.class, (Tile)null, true);

        type = xr.getType(spec, TYPE_TAG,
                          ResourceType.class, (ResourceType)null);

        quantity = xr.getAttribute(QUANTITY_TAG, 0);
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
    public String toString() {
        return (quantity == UNLIMITED) ? getType().getId()
            : Integer.toString(quantity) + " " + getType().getId();
    }
}
