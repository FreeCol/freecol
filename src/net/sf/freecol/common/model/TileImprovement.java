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

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Map.Direction;


/**
 * Represents a tile improvement, such as a river or road.
 */
public class TileImprovement extends TileItem implements Named {

    private static Logger logger = Logger.getLogger(TileImprovement.class.getName());

    /**
     * River magnitudes
     */
    public static final int NO_RIVER = 0;
    public static final int SMALL_RIVER = 1;
    public static final int LARGE_RIVER = 2;
    public static final int FJORD_RIVER = 3;

    /** The type of this improvement. */
    private TileImprovementType type;

    /** Turns remaining until the improvement is complete, if any. */
    private int turnsToComplete;

    /**
     * The improvement magnitude.  Default is type.getMagnitude(), but
     * this will override.
     */
    private int magnitude;

    /** Image and overlay style information for the improvement. */
    private TileImprovementStyle style;

    /**
     * Whether this is a virtual improvement granted by some structure
     * on the tile (a Colony, for example). Virtual improvements will
     * be removed along with the structure that granted them.
     */
    private boolean virtual;

    /** Cached bitmap of connections by direction, derived from style. */
    private long connected = 0L;


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
        this.connected = 0L;
    }

    public TileImprovement(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    public TileImprovement(Game game, Tile tile, TileImprovement template) {
        super(game, tile);
        this.type = getSpecification().getTileImprovementType(template.getId());
        this.magnitude = template.magnitude;
        this.style = template.style;
        this.turnsToComplete = template.turnsToComplete;
    }

    /**
     * Instantiates a new <code>TileImprovement</code> with the given
     * ID. The object should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belongs.
     * @param id The unique identifier for this object.
     */
    public TileImprovement(Game game, String id) {
        super(game, id);
    }

    /**
     * Gets the type of this tile improvement.
     *
     * @return The type of this improvement.
     */
    public TileImprovementType getType() {
        return type;
    }

    /**
     * Gets a key for message routines.
     *
     * @return The name key.
     */
    public String getNameKey() {
        return type.getNameKey();
    }

    /**
     * Is this <code>TileImprovement</code> a road?
     * TODO: deprecate?
     *
     * @return True if this is a road improvement.
     */
    public boolean isRoad() {
        return "model.improvement.road".equals(type.getId());
    }

    /**
     * Is this <code>TileImprovement</code> a river?
     * TODO: deprecate?
     *
     * @return True if this is a river improvement.
     */
    public boolean isRiver() {
        return "model.improvement.river".equals(type.getId());
    }

    /**
     * How many turns remain until this improvement is complete?
     *
     * @return The current turns to completion.
     */
    public int getTurnsToComplete() {
        return turnsToComplete;
    }

    /**
     * Is this improvement complete?
     *
     * @return True if complete.
     */
    public boolean isComplete() {
        return turnsToComplete <= 0;
    }

    /**
     * Sets the turns required to complete the improvement.
     *
     * @param turns The new turns to completion.
     */
    public void setTurnsToComplete(int turns) {
        turnsToComplete = turns;
    }

    /**
     * Gets the magnitude of this improvement.
     *
     * @return The magnitude of this immprovement.
     */
    public int getMagnitude() {
        return magnitude;
    }

    /**
     * Sets the magnitude of this improvement.
     *
     * @param magnitude The new magnitude.
     */
    public void setMagnitude(int magnitude) {
        this.magnitude = magnitude;
    }

    /**
     * Gets the style of this improvement.
     *
     * @return The style
     */
    public TileImprovementStyle getStyle() {
        return style;
    }

    /**
     * Sets the style of this improvement.
     *
     * @param style The new style.
     */
    public void setStyle(TileImprovementStyle style) {
        this.style = style;
    }

    /**
     * Is this a virtual improvement?
     *
     * @return True if this is a virtual improvement.
     */
    public final boolean isVirtual() {
        return virtual;
    }

    /**
     * Set the virtual status of this improvement.
     * Used for the roads in a colony center tile.
     *
     * @param virtual The new virtual value.
     */
    public final void setVirtual(final boolean virtual) {
        this.virtual = virtual;
    }

    /**
     * Is this TileImprovement connected to a similar TileImprovement
     * on a neighbouring tile?
     *
     * @param direction The <code>Direction</code> to check.
     * @return True if this improvement is connected.
     */
    public boolean isConnectedTo(Direction direction) {
        return (connected & (1 << direction.ordinal())) != 0;
    }

    /**
     * Sets the connection status in a given direction.
     *
     * @param direction The <code>Direction</code> to set.
     * @param value The new status for the connection.
     */
    public void setConnected(Direction direction, boolean value) {
        boolean now = isConnectedTo(direction);
        if (now != value) {
            if (value) {
                connected |= 1 << direction.ordinal();
            } else {
                connected &= ~(1 << direction.ordinal());
            }
        }
        style = TileImprovementStyle.getInstance(encodeConnections());
    }

    /**
     * Encode a style string suitable for TileImprovementStyle.getInstance.
     */
    private String encodeConnections() {
        String s = new String();
        for (Direction d : Direction.values()) {
            s = s.concat((isConnectedTo(d)) ? Integer.toString(magnitude) :"0");
        }
        return s;
    }

    /**
     * Gets a map of connection-direction to magnitude.
     *
     * @return A map of the connections.
     */
    public Map<Direction, Integer> getConnections() {
        Map<Direction, Integer> result
            = new EnumMap<Direction, Integer>(Direction.class);
        for (Direction d : Direction.values()) {
            if (isConnectedTo(d)) result.put(d, magnitude);
        }
        return result;
    }

    /**
     * Gets the production bonus this improvement provides for a given type
     * of goods.
     *
     * @param goodsType The <code>GoodsType</code> to test.
     * @return A production bonus, or zero if none applicable.
     */
    public int getBonus(GoodsType goodsType) {
        return (isComplete()) ? type.getBonus(goodsType) : 0;
    }

    /**
     * Gets a Modifier for the production bonus this improvement provides
     * for a given type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to test.
     * @return A production <code>Modifier</code>, or null if none applicable.
     */
    public Modifier getProductionModifier(GoodsType goodsType) {
        return (isComplete()) ? type.getProductionModifier(goodsType) : null;
    }


    /**
     * Calculates the movement cost on the basis of connected tile
     * improvements.
     *
     * @param direction The <code>Direction</code> to move.
     * @param moveCost The original movement cost.
     * @return The movement cost with this improvement.
     */
    public int getMoveCost(Direction direction, int moveCost) {
        return (isComplete() && isConnectedTo(direction))
            ? type.getMoveCost(moveCost)
            : moveCost;
    }

    /**
     * What type of tile does this improvement change a given type to?
     *
     * @param tileType The original <code>TileType</code>.
     * @return The <code>TileType</code> that results from completing this
     *     improvement, or null if nothing changes.
     */
    public TileType getChange(TileType tileType) {
        return (isComplete()) ? type.getChange(tileType) : null;
    }

    /**
     * Can a unit build this improvement?
     *
     * @param unit A <code>Unit</code> to do the building.
     * @return True if the supplied unit can build this improvement.
     */
    public boolean isWorkerAllowed(Unit unit) {
        return (unit == null || isComplete()) ? false
            : type.isWorkerAllowed(unit);
    }


    /**
     * Fixes any tile improvement style discontinuities.
     *
     * We check only if this improvement is not connected to a neighbour
     * that *is* connected to this one, and connect this one.
     *
     * TODO: drop this one day when we never have style discontinuities.
     * This alas is not the case in 0.10.x.
     *
     * @return True if the style was coherent, false if a problem was
     *     found and corrected.
     */
    public boolean fixIntegrity() {
        final Tile tile = getTile();
        boolean result = true;
        for (Tile t : tile.getSurroundingTiles(1)) {
            Direction dForward = tile.getDirection(t);
            Direction dReverse = dForward.getReverseDirection();
            for (TileImprovement ti : t.getTileImprovements()) {
                if (getType() == ti.getType()
                    && !isConnectedTo(dForward)
                    && ti.isConnectedTo(dReverse)) {
                    setConnected(dForward, true);
                    result = false;
                    logger.warning("Connecting improvement " + this
                        + " at " + tile + " to " + t);
                }
            }
        }
        return result;
    }

    /**
     * Updates the connections from the current style.
     *
     * Public for the test suite.
     */
    public void updateConnections() {
        connected = 0L;
        if (style != null) {
            Direction[] directions = (isRoad()) ? Direction.values()
                : Direction.longSides;
            String mask = style.getMask();
            for (int i = 0; i < directions.length; i++) {
                if (mask.charAt(i) != '0') {
                    connected |= 1L << directions[i].ordinal();
                }
            }
        }
    }

    /**
     * Work out what the river style at this tile should be by checking
     * its connectivity.
     *
     * @return A suitable TileImprovementStyle.
     */
    public TileImprovementStyle getRiverStyleFromMap() {
        if (!isRiver()) return null;
        final Tile tile = getTile();
        final Region region = tile.getRegion();
        String s = new String();
        for (Direction d : Direction.longSides) {
            Tile t = tile.getNeighbourOrNull(d);
            s = s.concat((t != null && t.hasRiver()
                    && t.getRegion() == region) ? "1" : "0");
        }
        return TileImprovementStyle.getInstance(s);
    }

    /**
     * Work out what the road style at this tile should be by checking
     * neighbouring tiles for roads.
     *
     * @return A suitable TileImprovementStyle.
     */
    public TileImprovementStyle getRoadStyleFromMap() {
        if (!isRoad()) return null;
        final Tile tile = getTile();
        String s = new String();
        for (Direction d : Direction.values()) {
            Tile t = tile.getNeighbourOrNull(d);
            s = s.concat((t != null && t.hasRoad()) ? "1" : "0");
        }
        return TileImprovementStyle.getInstance(s);
    }

    /**
     * Updates the connections from/to this road improvement.
     *
     * @param connect If true, add connections, otherwise remove them.
     */
    public void updateRoadConnections(boolean connect) {
        if (!isRoad() || !isComplete()) return;
        final Tile tile = getTile();
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (t.hasRoad()) {
                t.getRoad().setConnected(t.getDirection(tile), connect);
            }
        }
    }

    // Interface TileItem

    /**
     * {@inheritDoc}
     */
    public final int getZIndex() {
        return type.getZIndex();
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
        if (style != null) {
            out.writeAttribute("style", style.toString());
        }
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
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        Game game = getGame();

        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        tile = game.getFreeColGameObject(in.getAttributeValue(null, "tile"),
                                         Tile.class);
        if (tile == null) {
            tile = new Tile(game, in.getAttributeValue(null, "tile"));
        }

        String str = in.getAttributeValue(null, "type");
        type = getSpecification().getTileImprovementType(str);

        turnsToComplete = Integer.parseInt(in.getAttributeValue(null, "turns"));

        magnitude = Integer.parseInt(in.getAttributeValue(null, "magnitude"));

        str = in.getAttributeValue(null, "style");
        if (str == null) {
            style = null;
        // @compat 0.10.5
        } else if (str.length() < 4) {
            String old = TileImprovementStyle.decodeOldStyle(str, isRoad());
            if (old == null) {
                logger.warning("Ignoring bogus old TileImprovementStyle: "
                    + str);
            } else {
                style = TileImprovementStyle.getInstance(old);
            }
        // end compatibility code
        } else {
            style = TileImprovementStyle.getInstance(str);
            if (style == null) {
                logger.warning("Ignoring bogus TileImprovementStyle: " + str);
            } else {
                int slen = style.toString().length();
                if (isRiver()) {
                    if (slen != Direction.longSides.length) {
                        TileImprovementStyle old = style;
                        style = getRiverStyleFromMap();
                        logger.warning("At " + tile
                            + " bad river style (" + old.toString()
                            + ") replaced with " + style.toString());
                    }
                } else if (isRoad()) {
                    if (slen != Direction.values().length) {
                        TileImprovementStyle old = style;
                        style = getRoadStyleFromMap();
                        logger.warning("At " + tile
                            + " bad road style (" + old.toString()
                            + ") replaced with " + style.toString());
                    }
                }
            }
        }
        updateConnections();

        virtual = getAttribute(in, "virtual", false);
    }

    /**
     * Gets a textual representation of this object.
     *
     * @return The id and turns to complete if any.
     */
    public String toString() {
        return getType().getId() + ((turnsToComplete <= 0) ? ""
            : " (" + Integer.toString(turnsToComplete) + " turns left)");
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "tileimprovement".
     */
    public static String getXMLElementTagName() {
        return "tileimprovement";
    }
}
