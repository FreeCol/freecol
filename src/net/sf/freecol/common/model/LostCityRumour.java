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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.RandomChoice;


/**
 * Represents a lost city rumour.
 */
public class LostCityRumour extends TileItem {

    private static final Logger logger = Logger.getLogger(LostCityRumour.class.getName());

    /**
     * The type of the rumour.  A RumourType, or null if the type has
     * not yet been determined.
     */
    private RumourType type = null;

    /**
     * The name of this rumour, or null, if it has none.  Rumours such
     * as the Seven Cities of Gold and Fountains of Youth may have
     * individual names.
     */
    private String name = null;

    /** Constants describing types of Lost City Rumours. */
    public static enum RumourType {
        NO_SUCH_RUMOUR,
        BURIAL_GROUND,
        EXPEDITION_VANISHES,
        NOTHING,
        LEARN,
        TRIBAL_CHIEF,
        COLONIST,
        MOUNDS,
        RUINS,
        CIBOLA,
        FOUNTAIN_OF_YOUTH
    }


    /**
     * Creates a new <code>LostCityRumour</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param tile The <code>Tile</code> where the LCR is.
     */
    public LostCityRumour(Game game, Tile tile) {
        super(game, tile);
    }

    /**
     * Creates a new <code>LostCityRumour</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param tile The <code>Tile</code> where the LCR is.
     * @param type The type of rumour.
     * @param name The name of the rumour.
     */
    public LostCityRumour(Game game, Tile tile, RumourType type, String name) {
        super(game, tile);

        this.type = type;
        this.name = name;
    }

    /**
     * Creates a new <code>LostCityRumour</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public LostCityRumour(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the type of rumour.
     *
     * @return The <code>RumourType</code>.
     */
    public final RumourType getType() {
        return type;
    }

    /**
     * Set the type of rumour.
     *
     * @param newType The new rumour type.
     */
    public final void setType(final RumourType newType) {
        this.type = newType;
    }

    /**
     * Get the name of this rumour.
     *
     * @return The name.
     */
    public final String getName() {
        return name;
    }


    /**
     * Chooses a type of Lost City Rumour.  The type of rumour depends
     * on the exploring unit, as well as player settings.
     *
     * The scouting outcome is based on three factors: good/bad percent
     * rumour difficulty option, expert scout or not, DeSoto or not.
     *
     * @param unit The <code>Unit</code> exploring (optional).
     * @param random A random number source.
     * @return The type of rumour.
     *
     * TODO: Make RumourType a FreeColGameObjectType and move all the
     * magic numbers in here to the specification.
     */
    public RumourType chooseType(Unit unit, Random random) {
        final Specification spec = getSpecification();
        final Tile tile = getTile();
        final boolean allowLearn = unit != null
            && !unit.getType().getUnitTypesLearntInLostCity().isEmpty();

        // Base bad and good chances are difficulty options.
        int percentBad = spec.getInteger(GameOptions.BAD_RUMOUR);
        int percentGood = spec.getInteger(GameOptions.GOOD_RUMOUR);

        // Expert scouts have a beneficial modifier that works on both
        // percentages
        if (unit != null) {
            float mod = unit.applyModifier(1.0f,
                Modifier.EXPLORE_LOST_CITY_RUMOUR);
            percentBad = (int)Math.round(percentBad * mod);
            percentGood = (int)Math.round(percentGood * mod);
        }

        // DeSoto forces all good results.
        if (unit != null
            && unit.getOwner().hasAbility(Ability.RUMOURS_ALWAYS_POSITIVE)) {
            percentBad = 0;
            percentGood = 100;
        }

        // Neutral is what is left.
        int percentNeutral = Math.max(0, 100 - percentBad - percentGood);

        // Add all possible events to a RandomChoice List
        List<RandomChoice<RumourType>> c
            = new ArrayList<RandomChoice<RumourType>>();

        // The GOOD
        if (allowLearn) {
            c.add(new RandomChoice<RumourType>(RumourType.LEARN,
                    30 * percentGood));
            c.add(new RandomChoice<RumourType>(RumourType.TRIBAL_CHIEF,
                    30 * percentGood));
            c.add(new RandomChoice<RumourType>(RumourType.COLONIST,
                    20 * percentGood));
        } else {
            c.add(new RandomChoice<RumourType>(RumourType.TRIBAL_CHIEF,
                    50 * percentGood));
            c.add(new RandomChoice<RumourType>(RumourType.COLONIST,
                    30 * percentGood));
        }
        if (unit == null
            || unit.getOwner().getPlayerType() == Player.PlayerType.COLONIAL) {
            c.add(new RandomChoice<RumourType>(RumourType.FOUNTAIN_OF_YOUTH,
                        2 * percentGood));
        }
        c.add(new RandomChoice<RumourType>(RumourType.MOUNDS,
                8 * percentGood));
        c.add(new RandomChoice<RumourType>(RumourType.RUINS,
                6 * percentGood));
        c.add(new RandomChoice<RumourType>(RumourType.CIBOLA, 
                4 * percentGood));

        // The BAD
        if (tile.getOwner() != null && tile.getOwner().isIndian()) {
            // If the tile is native-owned, allow burial grounds rumour.
            c.add(new RandomChoice<RumourType>(RumourType.BURIAL_GROUND,
                    25 * percentBad));
            c.add(new RandomChoice<RumourType>(RumourType.EXPEDITION_VANISHES, 
                    75 * percentBad));
        } else {
            c.add(new RandomChoice<RumourType>(RumourType.EXPEDITION_VANISHES,
                    100 * percentBad));
        }

        // The NEUTRAL
        c.add(new RandomChoice<RumourType>(RumourType.NOTHING,
                100 * percentNeutral));

        return RandomChoice.getWeightedRandom(logger, "Choose rumour", c,
                                              random);
    }


    // Interface TileItem

    /**
     * {@inheritDoc}
     */
    public final int getZIndex() {
        return RUMOUR_ZINDEX;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTileTypeAllowed(TileType tileType) {
        return !tileType.isWater();
    }

    /**
     * {@inheritDoc}
     */
    public int applyBonus(GoodsType goodsType, UnitType unitType, int potential) {
        // Just return the given potential, since lost cities do not
        // provide any production bonuses.  TODO: maybe we should
        // return zero, since lost cities actually prevent production?
        return potential;
    }

    /**
     * {@inheritDoc}
     */
    public List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                 UnitType unitType) {
        return new ArrayList<Modifier>();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNatural() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int checkIntegrity(boolean fix) {
        return (type == RumourType.NO_SUCH_RUMOUR) ? -1 : 1;
    }


    // Serialization

    private static final String NAME_TAG = "name";
    private static final String TILE_TAG = "tile";
    private static final String TYPE_TAG = "type";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(TILE_TAG, getTile());

        if (type != null) {
            xw.writeAttribute(TYPE_TAG, getType());
        }

        if (name != null) {
            xw.writeAttribute(NAME_TAG, name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        tile = xr.findFreeColGameObject(getGame(), TILE_TAG,
                                        Tile.class, (Tile)null, true);

        type = xr.getAttribute(TYPE_TAG, RumourType.class, (RumourType)null);

        name = xr.getAttribute(NAME_TAG, (String)null);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "lostCityRumour".
     */
    public static String getXMLElementTagName() {
        return "lostCityRumour";
    }
}
