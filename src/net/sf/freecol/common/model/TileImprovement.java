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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.Map.Layer;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;


/**
 * Represents a tile improvement, such as a river or road.
 */
public class TileImprovement extends TileItem {

    private static final Logger logger = Logger.getLogger(TileImprovement.class.getName());

    public static final String TAG = "tileImprovement";

    public static final String EMPTY_RIVER_STYLE = "0000";
    public static final String EMPTY_ROAD_STYLE = "00000000";
    
    /** River magnitudes */
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
     * on the tile (a Colony, for example).  Virtual improvements will
     * be removed along with the structure that granted them.
     */
    private boolean virtual;


    /**
     * Creates a standard {@code TileImprovement}-instance.
     *
     * This constructor asserts that the game, tile and type are valid.
     * Does not set the style.
     *
     * @param game The enclosing {@code Game}.
     * @param tile The {@code Tile} on which this object sits.
     * @param type The {@code TileImprovementType} of this
     *     improvement.
     * @param style The {@code TileImprovementStyle} of this improvement.
     */
    public TileImprovement(Game game, Tile tile, TileImprovementType type,
                           TileImprovementStyle style) {
        super(game, tile);
        if (type == null) {
            throw new RuntimeException("Type must not be null: " + this);
        }
        this.type = type;
        if (!type.isNatural()) {
            this.turnsToComplete = tile.getType().getBasicWorkTurns()
                + type.getAddWorkTurns();
        }
        this.magnitude = type.getMagnitude();
        this.style = style;
    }

    /**
     * Create a new {@code TileImprovement} with the given identifier.
     *
     * The object should be initialized later.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
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
     * Is this tile improvement a river?
     *
     * @return True if this is a river improvement.
     */
    public boolean isRiver() {
        return "model.improvement.river".equals(type.getId());
    }

    /**
     * Is this tile improvement a road?
     *
     * @return True if this is a road improvement.
     */
    public boolean isRoad() {
        return "model.improvement.road".equals(type.getId());
    }

    /**
     * Gets the directions that a connection can form across for this
     * this type of improvement.
     *
     * - For rivers, it is just the longSided directions.
     * - For roads, it is all directions.
     * - In other cases, no directions are relevant.
     *
     * @return An array of relevant directions, or null if none.
     */
    public List<Direction> getConnectionDirections() {
        return (isRoad()) ? Direction.allDirections
            : (isRiver()) ? Direction.longSides
            : null;
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
     * Set the style of this improvement.
     *
     * @param style The new {@code TileImprovementStyle}.
     */
    public void setStyle(TileImprovementStyle style) {
        this.style = style;
    }

    /**
     * Is this a virtual improvement?
     *
     * @return True if this is a virtual improvement.
     */
    public final boolean getVirtual() {
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
     * Get the magnitude of the river branch in the given direction.
     * Precondition: This tile improvement is a river!
     *
     * @param direction The {@code Direction} to check.
     * @return The magnitude of the river branch or 0 if there is none.
     */
    public int getRiverConnection(Direction direction) {
        int index = Direction.longSides.indexOf(direction);
        if (index == -1 || style == null)
            return 0;
        int mag = Character.digit(style.getString().charAt(index), 10);
        return (mag == -1) ? 0 : mag;
    }

    /**
     * Is this tile improvement connected to a similar improvement on
     * a neighbouring tile?
     *
     * Public for the test suite.
     *
     * @param direction The {@code Direction} to check.
     * @return True if this improvement is connected.
     */
    public boolean isConnectedTo(Direction direction) {
        int index = isRoad() ? direction.ordinal()
            : isRiver() ? Direction.longSides.indexOf(direction) : -1;
        return (index == -1 || style == null) ? false
            : style.getString().charAt(index) != '0';
    }

    /**
     * Internal helper method to set the connection status in a given direction.
     * There is no check for backwards connections on neighbouring tiles!
     *
     * @param direction The {@code Direction} to set.
     * @param value The new status for the connection.
     */
    private void setConnected(Direction direction, boolean value) {
        if (style == null || isConnectedTo(direction) != value)
            setConnected(direction, value, Integer.toString(magnitude));
    }

    private void setConnected(Direction direction, boolean value, String magnitude) {
        if (style == null) {
            style = TileImprovementStyle.getInstance(EMPTY_ROAD_STYLE);
        }
        String old = style.toString();
        List<Direction> directions = getConnectionDirections();
        int end = directions.size();
        StringBuilder updated = new StringBuilder();
        for(int index = 0; index != end; ++index) {
            if(directions.get(index) == direction)
                updated.append(value ? magnitude : "0");
            else
                updated.append(old.charAt(index));
        }
        style = TileImprovementStyle.getInstance(updated.toString());
    }

    /**
     * Gets a map of connection-direction to magnitude.
     *
     * @return A map of the connections.
     */
    public Map<Direction, Integer> getConnections() {
        final List<Direction> dirns = getConnectionDirections();
        return (dirns == null) ? Collections.<Direction, Integer>emptyMap()
            : transform(dirns, d -> isConnectedTo(d),
                        Function.<Direction>identity(),
                        Collectors.toMap(Function.<Direction>identity(),
                                         d -> magnitude));
    }

    /**
     * Gets a Modifier for the production bonus this improvement provides
     * for a given type of goods.
     *
     * @param goodsType The {@code GoodsType} to test.
     * @return A production {@code Modifier}, or null if none applicable.
     */
    private Modifier getProductionModifier(GoodsType goodsType) {
        return (isComplete()) ? type.getProductionModifier(goodsType) : null;
    }

    /**
     * Calculates the movement cost on the basis of connected tile
     * improvements.
     *
     * @param direction The {@code Direction} to move.
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
     * @param tileType The original {@code TileType}.
     * @return The {@code TileType} that results from completing this
     *     improvement, or null if nothing changes.
     */
    public TileType getChange(TileType tileType) {
        return (isComplete()) ? type.getChange(tileType) : null;
    }

    /**
     * Can a unit build this improvement?
     *
     * @param unit A {@code Unit} to do the building.
     * @return True if the supplied unit can build this improvement.
     */
    public boolean isWorkerAllowed(Unit unit) {
        return (unit == null || isComplete()) ? false
            : type.isWorkerAllowed(unit);
    }

    /**
     * Updates the connections from the current style.
     *
     * Public for the test suite.
     *
     * @return The connections implied by the current style.
     */
    public final long getConnectionsFromStyle() {
        long conn = 0L;
        if (style != null) {
            List<Direction> directions = getConnectionDirections();
            if (directions != null) {
                String mask = style.getMask();
                for (int i = 0; i < directions.size(); i++) {
                    if (mask.charAt(i) != '0') {
                        conn |= 1L << directions.get(i).ordinal();
                    }
                }
            }
        }
        return conn;
    }

    /**
     * Sets the river style to be as close as possible to the requested
     * style, even when it has to create neighbouring rivers to prevent
     * broken connections or change the magnitude.
     *
     * @param conns The river style to set.
     */
    public void setRiverStyle(String conns) {
        if (!isRiver()) return;
        final Tile tile = getTile();
        int i = 0;
        int[] counts = {0, 0};
        for (Direction d : Direction.longSides) {
            Direction dReverse = d.getReverseDirection();
            Tile t = tile.getNeighbourOrNull(d);
            TileImprovement river = (t == null) ? null : t.getRiver();
            String c = (conns == null) ? "0" : conns.substring(i, i+1);

            if ("0".equals(c)) {
                if (river != null) {
                    river.setConnected(dReverse, false);
                }
                setConnected(d, false);
            } else {
                int mag = Integer.parseInt(c);
                if (river != null) {
                    river.setConnected(dReverse, true);
                    setConnected(d, true, c);
                    counts[mag-1]++;
                } else if (t != null) {
                    if (!t.getType().isWater()) {
                        t.addRiver(mag, "0000");
                        t.getRiver().setConnected(dReverse, true);
                    }
                    setConnected(d, true, c);
                    counts[mag-1]++;
                }
            }
            i++;
        }
        magnitude = counts[1] >= counts[0] ? 2 : 1;
    }

    /**
     * Updates the connections from/to this river improvement on the basis
     * of the expected encoded river style, as long as this would not
     * create broken connections.
     * Uses magnitude, not the connection strengths inside conns, when
     * adding new connections.
     *
     * @param conns The encoded river connections, or null to disconnect.
     * @return The actual encoded connections found.
     */
    public String updateRiverConnections(String conns) {
        // FIXME: Consider checking conns for incompatible length and content
        //        to prevent more bugs.
        if (!isRiver()) return null;
        final Tile tile = getTile();
        int i = 0;
        for (Direction d : Direction.longSides) {
            Direction dReverse = d.getReverseDirection();
            Tile t = tile.getNeighbourOrNull(d);
            TileImprovement river = (t == null) ? null : t.getRiver();
            String c = (conns == null) ? "0" : conns.substring(i, i+1);

            if ("0".equals(c)) {
                if (river != null) {
                    river.setConnected(dReverse, false);
                }
                setConnected(d, false);
            } else {
                if (river != null) {
                    river.setConnected(dReverse, true);
                    setConnected(d, true);
                } else if (t != null && t.getType().isWater()) {
                    setConnected(d, true);
                }
            }
            i++;
        }
        return (style == null) ? null : style.getString();
    }

    /**
     * Updates the connections from/to this road improvement.
     *
     * @param connect If true, add connections, otherwise remove them.
     * @return A string encoding of the correct connections for this
     *     improvement.
     */
    public String updateRoadConnections(boolean connect) {
        if (!isRoad() || !isComplete()) return null;
        final Tile tile = getTile();
        for (Direction d : Direction.values()) {
            Tile t = tile.getNeighbourOrNull(d);
            TileImprovement road = (t == null) ? null : t.getRoad();
            if (road != null && road.isComplete()) {
                road.setConnected(d.getReverseDirection(), connect);
                setConnected(d, connect);
            } else {
                setConnected(d, false);
            }
        }
        return (this.style == null) ? null : this.style.getString();
    }

    /**
     * Get the disaster choices available for this tile improvement.
     *
     * @return A stream of {@code Disaster} choices.
     */
    public Stream<RandomChoice<Disaster>> getDisasterChoices() {
        return (this.type == null)
            ? Stream.<RandomChoice<Disaster>>empty()
            : this.type.getDisasterChoices();
    }


    // Interface Named
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return (type == null) ? null : type.getNameKey();
    }


    // Interface TileItem

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getZIndex() {
        return type.getZIndex();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTileTypeAllowed(TileType tileType) {
        return type.isTileTypeAllowed(tileType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int applyBonus(GoodsType goodsType, UnitType unitType,
                          int potential) {
        // Applies the production bonuses of this tile improvement to
        // the given base potential.  Currently, the unit type
        // argument is ignored and is only provided for the sake of
        // consistency.  The bonuses of future improvements might
        // depend on the unit type, however.

        int result = potential;
        // do not apply any bonuses if the base tile does not produce
        // any goods, and don't apply bonuses for incomplete
        // improvements (such as roads)
        if (potential > 0 && isComplete()) {
            result += type.getBonus(goodsType);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canProduce(GoodsType goodsType, UnitType unitType) {
        // TileImprovements provide bonuses, but do *not* allow a tile
        // that can not produce some goods to produce due to the bonus.
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Modifier> getProductionModifiers(GoodsType goodsType,
                                                   UnitType unitType) {
        final Specification spec = getSpecification();
        Modifier m;
        return (goodsType != null && isComplete()
            && !(/* unattended */ !isNatural() && unitType == null
                && !goodsType.isFoodType()
                && spec.getBoolean(GameOptions.ONLY_NATURAL_IMPROVEMENTS))
            && (m = getProductionModifier(goodsType)) != null)
            ? Stream.of(m)
            : Stream.<Modifier>empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNatural() {
        return type.isNatural();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isComplete() {
        return turnsToComplete <= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Layer getLayer() {
        return Layer.RIVERS;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        Tile tile = getTile();
        if (isRiver()) {
            // @compat 0.11.5 Prevent NPE, TileItemContainer rechecks this.
            if (style == null) {
                lb.add("\n  Broken null style river at ", tile);
                return result.fail();
            }
            // end @compat 0.11.5

            // @compat 0.11.6 There could be broken river connections, without
            // the neighbouring tile having a corresponding connection or
            // a water tile.
            // These could at least be added using the map editor.
            String conns = style.getString();
            int i = 0;
            for (Direction d : Direction.longSides) {
                Direction dReverse = d.getReverseDirection();
                Tile t = tile.getNeighbourOrNull(d);
                TileImprovement river = (t == null) ? null : t.getRiver();
                if (conns.charAt(i) != '0') {
                    if (river != null) {
                        if (!river.isConnectedTo(dReverse)) {
                            if (fix) {
                                setConnected(d, false);
                                lb.add("\n  Removed broken river connection to ",
                                    d, " at ", tile);
                                result = result.fix();
                            } else {
                                lb.add("\n  Broken river connection to ", d,
                                    " at ", tile);
                                result = result.fail();
                            }
                        }
                    } else if (t == null || !t.getType().isWater()) {
                        if (fix) {
                            setConnected(d, false);
                            lb.add("\n  Removed broken river connection to ",
                                d, " at ", tile);
                            result = result.fix();
                        } else {
                            lb.add("\n  Broken river connection to ", d,
                                " at ", tile);
                            result = result.fail();
                        }
                    }
                }
                i++;
            }
            // end @compat 0.11.6
        } else if (isRoad() && isComplete()) {
            // @compat 0.11.6 Roads on tiles never having another adjacent
            // road tile had null styles, because updateRoadConnections
            // forgot to set one for roads without a connection.
            if (fix) {
                TileImprovementStyle oldStyle = style;
                updateRoadConnections(true);
                if (style != oldStyle) {
                    lb.add("\n  Bad road style from ", oldStyle,
                        " to ", style, " fixed at ", tile);
                    result = result.fix();
                }
            }
            if (style == null) {
                lb.add("\n  Broken road with null style at ", tile);
                result = result.fail();
            }
            // end @compat 0.11.6
        }
        return result;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        TileImprovement o = copyInCast(other, TileImprovement.class);
        if (o == null || !super.copyIn(o)) return false;
        this.type = o.getType();
        this.turnsToComplete = o.getTurnsToComplete();
        this.magnitude = o.getMagnitude();
        this.style = o.getStyle();
        this.virtual = o.getVirtual();
        return true;
    }


    // Serialization

    private static final String MAGNITUDE_TAG = "magnitude";
    private static final String STYLE_TAG = "style";
    private static final String TILE_TAG = "tile";
    private static final String TURNS_TAG = "turns";
    private static final String TYPE_TAG = "type";
    private static final String VIRTUAL_TAG = "virtual";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(TILE_TAG, getTile());

        xw.writeAttribute(TYPE_TAG, getType());

        xw.writeAttribute(TURNS_TAG, turnsToComplete);

        xw.writeAttribute(MAGNITUDE_TAG, magnitude);

        if (style != null) xw.writeAttribute(STYLE_TAG, style);

        if (virtual) xw.writeAttribute(VIRTUAL_TAG, virtual);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();
        final Game game = getGame();

        tile = xr.makeFreeColObject(game, TILE_TAG, Tile.class, true);

        type = xr.getType(spec, TYPE_TAG, TileImprovementType.class,
                          (TileImprovementType)null);

        turnsToComplete = xr.getAttribute(TURNS_TAG, 0);

        magnitude = xr.getAttribute(MAGNITUDE_TAG, 0);

        virtual = xr.getAttribute(VIRTUAL_TAG, false);

        style = null;
        String str = xr.getAttribute(STYLE_TAG, (String)null);
        List<Direction> dirns = getConnectionDirections();
        if (dirns == null) {
            if (str != null && !str.isEmpty())
                logger.warning("At " + tile + " ignored nonempty style for "
                    + type + ": " + str);
        } else if (str == null) {
            // Null style OK for incomplete roads.  Virtual roads used
            // to be null, but we are fixing that.  Some cached tiles
            // were wrongly getting null style.  Do not bother
            // complaining about these as they will get fixed and
            // logged in checkIntegrity().
        } else if (str.length() != dirns.size()) {
            logger.warning("At " + tile + " ignored bogus style for "
                + type + ": " + str);
        } else {
            style = TileImprovementStyle.getInstance(str);
        }
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
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(getType().getId());
        if (turnsToComplete > 0) {
            sb.append(" (").append(turnsToComplete).append(" turns left)");
        }
        if (style != null) sb.append(' ').append(style.getString());
        sb.append(']');
        return sb.toString();
    }
}
