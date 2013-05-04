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

import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Represents a locatable goods of a specified type and quantity.
 */
public class Resource extends TileItem {

    private static Logger logger = Logger.getLogger(Resource.class.getName());

    private static final int UNLIMITED = -1;

    /** The type of resource. */
    private ResourceType type;

    /** The amount of the resource present. */
    private int quantity;


    /**
     * Creates a standard <code>Resource</code>-instance.
     *
     * This constructor asserts that the game, tile and type are valid.
     *
     * @param game The enclosing <code>Game</code>.
     * @param tile The <code>Tile</code> on which this object sits.
     * @param type The <code>ResourceType</code> of this Resource.
     * @param quantity The quantity of resource.
     */
    public Resource(Game game, Tile tile, ResourceType type, int quantity) {
        super(game, tile);

        if (type == null) {
            throw new IllegalArgumentException("Parameter 'type' must not be 'null'.");
        }
        this.type = type;
        this.quantity = quantity;
    }

    /**
     * Creates a standard <code>Resource</code>-instance.
     *
     * This constructor asserts that the game, tile and type are valid.
     *
     * @param game The enclosing <code>Game</code>.
     * @param tile The <code>Tile</code> on which this object sits.
     * @param type The <code>ResourceType</code> of this Resource.
     */
    public Resource(Game game, Tile tile, ResourceType type) {
        this(game, tile, type, type.getMaxValue());
    }

    /**
     * Creates new <code>Resource</code>.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Resource(Game game, String id) {
        super(game, id);
    }


    /**
     * Get a name key for this resource.
     *
     * @return The name key.
     */
    public String getNameKey() {
        return getType().getNameKey();
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
     * <code>GoodsType</code>.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @param unitType The producing <code>UnitType</code>.
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
    public int useQuantity(int usedQuantity) {
        if (quantity >= usedQuantity) {
            quantity -= usedQuantity;
        } else if (quantity == UNLIMITED) {
            logger.warning("useQuantity called for unlimited resource");
        } else {
            // Shouldn't generally happen.  Do something more drastic here?
            logger.severe("Insufficient quantity in " + this);
            quantity = 0;
        }
        return quantity;
    }


    // Interface TileItem

    /**
     * {@inheritDoc}
     */
    public final int getZIndex() {
        return RESOURCE_ZINDEX;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTileTypeAllowed(TileType tileType) {
        return tileType.canHaveResourceType(getType());
    }

    /**
     * {@inheritDoc}
     */
    public int applyBonus(GoodsType goodsType, UnitType unitType,
                          int potential) {
        Set<Modifier> bonus = type.getModifierSet(goodsType.getId(), unitType);
        int amount = (int)FeatureContainer.applyModifierSet(potential, null,
                                                            bonus) - potential;
        return potential
            + ((quantity == UNLIMITED || quantity > amount) ? amount
                : quantity);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNatural() {
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
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll,
                             boolean toSavedGame) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName(), player, showAll, toSavedGame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out, Player player,
                                   boolean showAll,
                                   boolean toSavedGame) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, TILE_TAG, getTile());

        writeAttribute(out, TYPE_TAG, getType());

        writeAttribute(out, QUANTITY_TAG, quantity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(in);

        tile = makeFreeColGameObject(in, TILE_TAG, Tile.class);

        type = spec.getType(in, TYPE_TAG,
                            ResourceType.class, (ResourceType)null);

        quantity = getAttribute(in, QUANTITY_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (quantity == UNLIMITED) ? getType().getId()
            : Integer.toString(quantity) + " " + getType().getId();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "resource".
     */
    public static String getXMLElementTagName() {
        return "resource";
    }
}
