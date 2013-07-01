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
     * @param unit The <code>Unit</code> exploring (optional).
     * @param difficulty The difficulty level.
     * @param random A random number source.
     * @return The type of rumour.
     *
     * TODO: Make RumourType a FreeColGameObjectType and move all the
     * magic numbers in here to the specification.
     */
    public RumourType chooseType(Unit unit, int difficulty, Random random) {
        Tile tile = getTile();
        boolean allowLearn = unit != null
            && !unit.getType().getUnitTypesLearntInLostCity().isEmpty();
        boolean isExpertScout = unit != null
            && unit.hasAbility(Ability.EXPERT_SCOUT)
            && unit.hasAbility("model.ability.scoutIndianSettlement");
        boolean hasDeSoto = unit != null
            && unit.getOwner().hasAbility("model.ability.rumoursAlwaysPositive");
        // The following arrays contain percentage values for
        // "good" and "bad" events when scouting with a non-expert
        // at the various difficulty levels [0..4] exact values
        // but generally "bad" should increase, "good" decrease
        final int BAD_EVENT_PERCENTAGE[]  = { 11, 17, 23, 30, 37 };
        final int GOOD_EVENT_PERCENTAGE[] = { 75, 62, 48, 33, 17 };
        // remaining to 100, event NOTHING:   14, 21, 29, 37, 46

        // The following arrays contain the modifiers applied when
        // expert scout is at work exact values; modifiers may
        // look slightly "better" on harder levels since we're
        // starting from a "worse" percentage.
        final int BAD_EVENT_MOD[]  = { -6, -7, -7, -8, -9 };
        final int GOOD_EVENT_MOD[] = { 14, 15, 16, 18, 20 };

        // The scouting outcome is based on three factors: level,
        // expert scout or not, DeSoto or not.  Based on this, we
        // are going to calculate probabilites for neutral, bad
        // and good events.
        int percentNeutral;
        int percentBad;
        int percentGood;
        difficulty = Math.max(0, Math.min(BAD_EVENT_PERCENTAGE.length-1,
                                          difficulty));
        if (hasDeSoto) {
            percentBad  = 0;
            percentGood = 100;
            percentNeutral = 0;
        } else {
            // First, get "basic" percentages
            percentBad  = BAD_EVENT_PERCENTAGE[difficulty];
            percentGood = GOOD_EVENT_PERCENTAGE[difficulty];

            // Second, apply ExpertScout bonus if necessary
            if (isExpertScout) {
                percentBad  += BAD_EVENT_MOD[difficulty];
                percentGood += GOOD_EVENT_MOD[difficulty];
            }

            // Third, get a value for the "neutral" percentage,
            // unless the other values exceed 100 already
            if (percentBad + percentGood < 100) {
                percentNeutral = 100 - percentBad - percentGood;
            } else {
                percentNeutral = 0;
            }
        }

        Map<RumourType, Integer> events =
            new EnumMap<RumourType, Integer>(RumourType.class);

        // The NEUTRAL
        events.put(RumourType.NOTHING, 100 * percentNeutral);

        // The BAD
        // If the tile is native-owned, allow burial grounds rumour.
        if (tile.getOwner() != null && tile.getOwner().isIndian()) {
            events.put(RumourType.EXPEDITION_VANISHES, 75 * percentBad);
            events.put(RumourType.BURIAL_GROUND, 25 * percentBad);
        } else {
            events.put(RumourType.EXPEDITION_VANISHES, 100 * percentBad);
            events.put(RumourType.BURIAL_GROUND, 0);
        }

        // The GOOD
        if (allowLearn) { // if the unit can learn
            events.put(RumourType.LEARN, 30 * percentGood);
            events.put(RumourType.TRIBAL_CHIEF, 30 * percentGood);
            events.put(RumourType.COLONIST, 20 * percentGood);
        } else {
            events.put(RumourType.LEARN, 0);
            events.put(RumourType.TRIBAL_CHIEF, 50 * percentGood);
            events.put(RumourType.COLONIST, 30 * percentGood);
        }

        // The SPECIAL
        // Right now, these are considered "good" events that happen randomly.
        events.put(RumourType.MOUNDS, 8 * percentGood);
        events.put(RumourType.RUINS, 6 * percentGood);
        events.put(RumourType.CIBOLA, 4 * percentGood);
        if (unit == null
            || unit.getOwner().getPlayerType() == Player.PlayerType.COLONIAL) {
            events.put(RumourType.FOUNTAIN_OF_YOUTH, 3 * percentGood);
        }

        // Add all possible events to a RandomChoice List
        List<RandomChoice<RumourType>> choices
            = new ArrayList<RandomChoice<RumourType>>();
        for (Entry<RumourType, Integer> entry : events.entrySet()) {
            if (entry.getValue() > 0) {
                choices.add(new RandomChoice<RumourType>(entry.getKey(),
                                                         entry.getValue()));
            }
        }
        return RandomChoice.getWeightedRandom(logger,
            "Choose rumour", choices, random);
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

        if (xw.canSee(getTile())) {

            if (type != null) {
                xw.writeAttribute(TYPE_TAG, getType());
            }

            if (name != null) {
                xw.writeAttribute(NAME_TAG, name);
            }
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
