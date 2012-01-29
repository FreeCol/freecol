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

    private ResourceType type;
    private int quantity;

    /**
     * Creates a standard <code>Resource</code>-instance.
     *
     * This constructor asserts that the game, tile and type are valid.
     *
     * @param game The <code>Game</code> in which this object belongs.
     * @param tile The <code>Tile</code> on which this object sits.
     * @param type The <code>ResourceType</code> of this Resource.
     * @param quantity an <code>int</code> value
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
     * @param game The <code>Game</code> in which this object belongs.
     * @param tile The <code>Tile</code> on which this object sits.
     * @param type The <code>ResourceType</code> of this Resource.
     */
    public Resource(Game game, Tile tile, ResourceType type) {
        this(game, tile, type, type.getMaxValue());
    }


    public Resource(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Returns a textual representation of this object.
     * @return A <code>String</code> of either:
     * <ol>
     * <li>QUANTITY RESOURCETYPE (eg. 250 Minerals) if there is a limited quantity
     * <li>RESOURCETYPE (eg. Game) if it is an unlimited resource
     * </ol>
     */
    public String toString() {
        if (quantity > -1) {
            return Integer.toString(quantity) + " " + getType().getId();
        } else {
            return getType().getId();
        }
    }

    public String getNameKey() {
        return getType().getNameKey();
    }


    /**
     * Returns the <code>ResourceType</code> of this Resource.
     */
    public ResourceType getType() {
        return type;
    }

    /**
     * Returns the current quantity.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the current quantity.
     */
    public void setQuantity(int newQuantity) {
        quantity = newQuantity;
    }

    /**
     * Get the <code>ZIndex</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getZIndex() {
        return RESOURCE_ZINDEX;
    }

    /**
     * Returns the best GoodsType
     */
    public GoodsType getBestGoodsType() {
        return type.getBestGoodsType();
    }

    /**
     * Returns the bonus (checking available stock) for next turn.
     * @param goodsType The GoodsType to check
     * @param unitType an <code>UnitType</code> value
     * @param potential Potential of Tile + Improvements
     * @return an <code>int</code> value
     */
    public int getBonus(GoodsType goodsType, UnitType unitType, int potential) {
        Set<Modifier> productionBonus = type.getProductionModifier(goodsType, unitType);
        int bonusAmount = (int) FeatureContainer.applyModifierSet(potential, null, productionBonus) - potential;
        if (quantity > -1 && bonusAmount > quantity) {
            return potential + quantity;
        } else {
            return potential + bonusAmount;
        }
    }

    /**
     * Reduces the available quantity by the bonus output of <code>GoodsType</code>.
     * @param goodsType The GoodsType to check
     * @param unitType an <code>UnitType</code> value
     * @param potential Potential of Tile + Improvements
     * @return an <code>int</code> value
     */
    public int useQuantity(GoodsType goodsType, UnitType unitType, int potential) {
        // Return -1 here if not limited resource?
        return useQuantity(getBonus(goodsType, unitType, potential) - potential);
    }

    /**
     * Reduces the value <code>quantity</code>.
     * @param usedQuantity The quantity that was used up.
     * @return The final value of quantity.
     */
    public int useQuantity(int usedQuantity) {
        if (quantity >= usedQuantity) {
            quantity -= usedQuantity;
        } else if (quantity == -1) {
            logger.warning("useQuantity called for unlimited resource");
        } else {
            // Shouldn't generally happen.  Do something more drastic here?
            logger.severe("Insufficient quantity in " + this);
            quantity = 0;
        }
        return quantity;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTileTypeAllowed(TileType tileType) {
        return tileType.canHaveResourceType(getType());
    }

    /* TODO: what was this for? Named resources, such as Lost Crazy Russian Gold Mine?
    public void setName(String newName) {
        // do nothing
    }
    */


    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * <br>
     * <br>
     *
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("tile", getTile().getId());
        out.writeAttribute("type", getType().getId());
        out.writeAttribute("quantity", Integer.toString(quantity));

        // End element:
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        tile = getFreeColGameObject(in, "tile", Tile.class);
        type = getSpecification().getResourceType(in.getAttributeValue(null, "type"));
        quantity = Integer.parseInt(in.getAttributeValue(null, "quantity"));

        in.nextTag();
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
