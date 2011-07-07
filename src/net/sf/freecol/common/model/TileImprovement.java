/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Map.Direction;

import org.w3c.dom.Element;


/**
 * Represents a tile improvement, such as a river or road.
 */
public class TileImprovement extends TileItem implements Named {

    private static Logger logger = Logger.getLogger(TileImprovement.class.getName());

    private TileImprovementType type;
    private int turnsToComplete;

    /**
     * Default is type.getMagnitude(), but this will override.
     */
    private int magnitude;

    /**
     * River magnitudes
     */
    public static final int NO_RIVER = 0;
    public static final int SMALL_RIVER = 1;
    public static final int LARGE_RIVER = 2;
    public static final int FJORD_RIVER = 3;


    /**
     * To store the style of multi-image TileImprovements (eg. rivers)
     * Rivers have 4 directions {NE=1, SE=3, SW=9, NW=27}, and 3 levels (see above)
     * @see Map
     * @see net.sf.freecol.server.generator.River
     */
    private int style;

    /**
     * Whether this is a virtual improvement granted by some structure
     * on the tile (a Colony, for example). Virtual improvements will
     * be removed along with the structure that granted them.
     */
    private boolean virtual;

    // ------------------------------------------------------------ constructor

    /**
     * Creates a standard <code>TileImprovement</code>-instance.
     *
     * This constructor asserts that the game, tile and type are valid.
     *
     * @param game The <code>Game</code> in which this object belongs.
     * @param tile The <code>Tile</code> on which this object sits.
     * @param type The <code>TileImprovementType</code> of this TileImprovement.
     */
    public TileImprovement(Game game, Tile tile, TileImprovementType type) {
        super(game, tile);
        if (type == null) {
            throw new IllegalArgumentException("Parameter 'type' must not be 'null'.");
        }
        this.type = type;
        if (!type.isNatural()) {
            this.turnsToComplete = tile.getType().getBasicWorkTurns() + type.getAddWorkTurns();
        }
        this.magnitude = type.getMagnitude();
    }

    public TileImprovement(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    public TileImprovement(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>TileImprovement</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public TileImprovement(Game game, String id) {
        super(game, id);
    }

    // ------------------------------------------------------------ retrieval methods

    public TileImprovementType getType() {
        return type;
    }

    public int getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(int magnitude) {
        this.magnitude = magnitude;
    }

    /**
     * Get the <code>Virtual</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isVirtual() {
        return virtual;
    }

    /**
     * Set the <code>Virtual</code> value.
     *
     * @param newVirtual The new Virtual value.
     */
    public final void setVirtual(final boolean newVirtual) {
        this.virtual = newVirtual;
    }

    /**
     * Is this <code>TileImprovement</code> a road?
     * @return a <code>boolean</code> value
     */
    public boolean isRoad() {
        return getType().getId().equals("model.improvement.road");
    }

    /**
     * Is this <code>TileImprovement</code> a river?
     * @return a <code>boolean</code> value
     */
    public boolean isRiver() {
        return getType().getId().equals("model.improvement.river");
    }

    public String getNameKey() {
        return getType().getNameKey();
    }

    /**
     * Returns a textual representation of this object.
     * @return A <code>String</code> of either:
     * <ol>
     * <li>NAME (#TURNS turns left) (eg. Road (2 turns left) ) if it is under construction
     * <li>NAME (eg. Road) if it is complete
     * </ol>
     */
    public String toString() {
        if (turnsToComplete > 0) {
            return getType().getId() + " (" + Integer.toString(turnsToComplete) + " turns left)";
        } else {
            return getType().getId();
        }
    }

    /**
     * @return the current turns to completion.
     */
    public int getTurnsToComplete() {
        return turnsToComplete;
    }

    /**
     * Update the turns required to complete the improvement.
     *
     * @param turns an <code>int</code> value
     */
    public void setTurnsToComplete(int turns) {
        turnsToComplete = turns;
    }

    /**
     * Get the <code>ZIndex</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getZIndex() {
        return type.getZIndex();
    }

    public boolean isComplete() {
        return turnsToComplete <= 0;
    }

    public EquipmentType getExpendedEquipmentType() {
        return type.getExpendedEquipmentType();
    }

    public int getExpendedAmount() {
        return type.getExpendedAmount();
    }

    public GoodsType getDeliverGoodsType() {
        return type.getDeliverGoodsType();
    }

    public int getDeliverAmount() {
        return type.getDeliverAmount();
    }

    /**
     * Returns the bonus (if any).
     * @param goodsType a <code>GoodsType</code> value
     * @return an <code>int</code> value
     */
    public int getBonus(GoodsType goodsType) {
        if (!isComplete()) {
            return 0;
        }
        return type.getBonus(goodsType);
    }

    /**
     * Returns the bonus Modifier (if any).
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>Modifier</code> value
     */
    public Modifier getProductionModifier(GoodsType goodsType) {
        if (!isComplete()) {
            return null;
        }
        return type.getProductionModifier(goodsType);
    }

    /**
     * Performs reduction of the movement-cost.
     * @param moveCost Original movement cost
     * @return The movement cost after any change
     */
    public int getMovementCost(int moveCost, Tile fromTile) {
        if (!isComplete()) {
            return moveCost;
        }
        String typeId = type.getId();
        if (typeId == null) {
            // No checking for matching type
            return type.getMovementCost(moveCost);
        }
        // Find matching type
        for (TileImprovement improvement : fromTile.getTileImprovements()) {
            if (improvement.getType().getId().equals(typeId)) {
                // Matched
                return type.getMovementCost(moveCost);
            }
        }
        // No match
        return moveCost;
    }

    /**
     * Returns any change of TileType
     * @return The new TileType.
     */
    public TileType getChange(TileType tileType) {
        if (!isComplete()) {
            return null;
        }
        return type.getChange(tileType);
    }

    /**
     * Returns the Style of this Improvement - used for Rivers
     * @return The style
     */
    public int getStyle() {
        return style;
    }

    /**
     * Sets the Style of this Improvement - used for Rivers
     * @param style The style
     */
    public void setStyle(int style) {
        this.style = style;
    }

    /**
     * Returns an int[NUMBER_OF_DIRECTIONS] array based on the
     * baseNumber and the 'active' directions given.
     *
     * @param directions An int[] that gives the active directions eg
     * {Map.N, Map.NE, Map.E, Map.SE, Map.S, Map.SW, Map.W, Map.NW},
     * or {Map.E, Map.SW};
     * @param baseNumber The base to be used to create the base array.
     * @return A base array that can create unique identifiers for any
     * combination
     */
    public static int[] getBase(Direction[] directions, int baseNumber) {
        Direction[] allDirections = Direction.values();
        int[] base = new int[allDirections.length];
        int n = 1;
        for (int i = 0; i < allDirections.length; i++) {
            base[i] = 0;
            for (Direction direction : directions) {
                if (direction == allDirections[i]) {
                    base[i] = n;
                    n *= baseNumber;
                    break;
                }
            }
        }
        return base;
    }

    /**
     * Breaks the Style of this Improvement into 8 directions - used for Rivers (at the moment)
     * @param directions An int[] that gives the active directions
     * eg {Map.N, Map.NE, Map.E, Map.SE, Map.S, Map.SW, Map.W, Map.NW},
     * or {Map.E, Map.SW};
     * @param baseNumber The base to be used to create the base array.
     * @return An int[] with the magnitude in each direction.
     */
    public int[] getStyleBreakdown(Direction[] directions, int baseNumber) {
        return getStyleBreakdown(getBase(directions, baseNumber));
    }

    /**
     * Breaks the Style of this Improvement into 8 directions - used for Rivers (at the moment)
     * Possible TODO: Modify this later should we modify the usage of Style.
     * @param base Use {@link #getBase}
     * @return An int[] with the magnitude in each direction.
     */
    public int[] getStyleBreakdown(int[] base) {
        int[] result = new int[8];
        int tempStyle = style;
        for (int i = base.length - 1; i >= 0; i--) {
            if (base[i] == 0) {
                continue;                       // Skip this direction
            }
            result[i] = tempStyle / base[i];    // Get an integer value 0-2 for a direction
            tempStyle -= result[i] * base[i];   // Remove the component of this direction
        }
        return result;
    }

    public void compileStyleBreakdown(int[] base, int[] breakdown) {
        if (base.length != breakdown.length) {
            logger.warning("base.length != breakdown.length");
            return;
        }
        style = 0;
        for (int i = 0; i < base.length; i++) {
            style += base[i] * breakdown[i];
        }
    }

    /**
     * Method for returning the 'most effective' TileImprovementType
     * allowed for a given <code>Tile</code>.  Useful for AI in
     * deciding the Improvements to prioritize.
     *
     * @param tile The <code>Tile</code> that will be improved
     * @param goodsType The <code>GoodsType</code> to be prioritized.
     * @return The best TileImprovementType available to be done.
     */
    public static TileImprovementType findBestTileImprovementType(Tile tile, GoodsType goodsType) {
        // Get list of TileImprovementTypes from Specification
        List<TileImprovementType> impTypeList = tile.getSpecification().getTileImprovementTypeList();
        int bestValue = 0;
        TileImprovementType bestType = null;
        for (TileImprovementType impType : impTypeList) {
            if (impType.isNatural()) {
                continue;   // Cannot be built
            }
            if (!impType.isTileTypeAllowed(tile.getType())) {
                continue;   // Not for this TileType
            }
            if (tile.findTileImprovementType(impType) != null) {
                continue;   // Already built
            }
            int value = impType.getValue(tile.getType(), goodsType);
            if (value > bestValue) {
                bestValue = value;
                bestType = impType;
            }
        }
        return bestType;
    }

    /**
     * Checks if a given worker can work at this Improvement
     */
    public boolean isWorkerAllowed(Unit unit) {
        if (unit == null) {
            return false;
        }
        if (isComplete()) {
            return false;
        }
        return type.isWorkerAllowed(unit);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTileTypeAllowed(TileType tileType) {
        return type.isTileTypeAllowed(tileType);
    }


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
        out.writeAttribute("turns", Integer.toString(turnsToComplete));
        out.writeAttribute("magnitude", Integer.toString(magnitude));
        out.writeAttribute("style", Integer.toString(style));
        if (virtual) {
            out.writeAttribute("virtual", Boolean.toString(virtual));
        }

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

        tile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "tile"));
        if (tile == null) {
            tile = new Tile(getGame(), in.getAttributeValue(null, "tile"));
        }
        type = getSpecification().getTileImprovementType(in.getAttributeValue(null, "type"));
        turnsToComplete = Integer.parseInt(in.getAttributeValue(null, "turns"));
        magnitude = Integer.parseInt(in.getAttributeValue(null, "magnitude"));
        style = Integer.parseInt(in.getAttributeValue(null, "style"));
        virtual = getAttribute(in, "virtual", false);

        in.nextTag();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "tileImprovement".
     */
    public static String getXMLElementTagName() {
        return "tileimprovement";
    }
}
