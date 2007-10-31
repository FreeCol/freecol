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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.server.generator.River;

import org.w3c.dom.Element;


/**
* Represents a locatable goods of a specified type and quantity.
*/
public class TileImprovement extends TileItem implements Locatable, Named {


    private static Logger logger = Logger.getLogger(TileImprovement.class.getName());

    private TileImprovementType type;
    private int turnsToComplete;
    
    /**
     * Default is type.getMagnitude(), but this will override.
     */
    private int magnitude;
    
    /**
     * To store the style of multi-image TileImprovements (eg. rivers)
     * Rivers have 4 directions {NE=1, SE=3, SW=9, NW=27}, and 3 levels {0=no river, 1=minor river, 2=major river}
     * @see Map
     * @see River
     */
    private int style;

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
            throw new NullPointerException();
        }
        this.type = type;
        this.turnsToComplete = tile.getBasicWorkTurns() + type.getAddWorkTurns();
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

    public String getTypeId() {
        return type.getTypeId();
    }

    public int getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(int magnitude) {
        this.magnitude = magnitude;
    }

    /** Is this <code>TileImprovement</code> a road? */
    public boolean isRoad() {
        return getTypeId().equals("road");
    }

    /** Is this <code>TileImprovement</code> a river? */
    public boolean isRiver() {
        return getTypeId().equals("river");
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
            return getName() + " (" + Integer.toString(turnsToComplete) + " turns left)";
        } else {
            return getName();
        }
    }

    /**
     * Returns the name of this TileImprovement.
     * @return The name of this TileImprovement.
     */
    public String getName() {
        return getType().getName();
    }

    public String getOccupationString() {
        return type.getOccupationString();
    }

    /**
     * @return the current turns to completion.
     */
    public int getTurnsToComplete() {
        return turnsToComplete;
    }

    public void setTurnsToComplete(int turns) {
        turnsToComplete = turns;
    }

    public boolean isComplete() {
        return turnsToComplete <= 0;
    }

    /**
	 * Performs work towards completion of this <code>TileImprovement</code>
	 * 
	 * This function allows for a unit to perform more than 1 'turn', perhaps in
	 * the event a skilled unit is able to build improvements with a bonus. The
	 * <code>doWork</code> function without any input params assumes 1 turn of
	 * work done.
	 * 
	 * @return {@link remaining turns to completion}
	 */
	public int doWork(int turns) {
        turnsToComplete -= turns;
        if (turnsToComplete <= 0) {
            turnsToComplete = 0;
        }
        return turnsToComplete;
    }

    public int doWork() {
        return doWork(1);
    }

    public GoodsType getExpendedGoodsType() {
        return type.getExpendedGoodsType();
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
     */
    public int getBonus(GoodsType goodsType) {
        if (!isComplete()) {
            return 0;
        }
        return type.getBonus(goodsType);
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
        String typeId = type.getTypeId();
        if (typeId == null) {
            // No checking for matching type
            return type.getMovementCost(moveCost);
        }
        // Find matching type
        Iterator<TileItem> ti = fromTile.getTileItemIterator();
        while (ti.hasNext()) {
            TileItem tileItem = ti.next();
            if (tileItem instanceof TileImprovement) {
                if (((TileImprovement) tileItem).getTypeId().equals(typeId)) {
                    // Matched
                    return type.getMovementCost(moveCost);
                }
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

    public void addStyle(int addStyle) {
        this.style += addStyle;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    /**
     * Breaks the Style of this Improvement into 8 directions - used for Rivers (at the moment)
     * @param directions An int[] that gives the active directions
              eg {Map.N, Map.NE, Map.E, Map.SE, Map.S, Map.SW, Map.W, Map.NW},
              or {Map.E, Map.SW};
     * @param baseNumber The base to be used to create the base array.
     * @return An int[] with the magnitude in each direction.
     */
    public int[] getStyleBreakdown(int[] directions, int baseNumber) {
        return getStyleBreakdown(Map.getBase(directions, baseNumber));
    }

    /**
     * Breaks the Style of this Improvement into 8 directions - used for Rivers (at the moment)
     * Possible TODO: Modify this later should we modify the usage of Style.
     * @param base Use {@link Map.getBase()}
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
     * Method for returning the 'most effective' TileImprovementType allowed for a given <code>Tile</code>.
     * Useful for AI in deciding the Improvements to prioritize.
     * @param tile The <code>Tile</code> that will be improved
     * @param goodsType The <code>GoodsType</code> to be prioritized.
     * @return The best TileImprovementType available to be done.
     */
    public static TileImprovementType findBestTileImprovementType(Tile tile, GoodsType goodsType) {
        // Get list of TileImprovementTypes from Specification
        List<TileImprovementType> impTypeList = FreeCol.getSpecification().getTileImprovementTypeList();
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
     * Disposes this improvement.
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    // ------------------------------------------------------------ API methods

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
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        out.writeAttribute("ID", getId());
        out.writeAttribute("tile", getTile().getId());
        out.writeAttribute("type", getType().getId());
        out.writeAttribute("turns", Integer.toString(turnsToComplete));
        out.writeAttribute("magnitude", Integer.toString(magnitude));
        out.writeAttribute("style", Integer.toString(style));

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
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        tile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "tile"));
        if (tile == null) {
            tile = new Tile(getGame(), in.getAttributeValue(null, "tile"));
        }
        type = FreeCol.getSpecification().getTileImprovementType(in.getAttributeValue(null, "type"));
        turnsToComplete = Integer.parseInt(in.getAttributeValue(null, "turns"));
        magnitude = Integer.parseInt(in.getAttributeValue(null, "magnitude"));
        style = Integer.parseInt(in.getAttributeValue(null, "style"));
        
        in.nextTag();
    }

    /**
    * Gets the tag name of the root element representing this object.
    * @return "tileImprovement".
    */
    public static String getXMLElementTagName() {
        return "tileimprovement";
    }

}
