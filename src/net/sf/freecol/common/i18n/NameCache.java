/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.common.i18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Region.RegionType;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.UnitType;

import static net.sf.freecol.common.util.RandomUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * A cache of proper names of various types.
 *
 * Most of these collections auto-initialize when the public accessor
 * is called, but the cities of Cibola are different because they are
 * saved in the Game, and hence requireCitiesOfCibola has to public
 * for generating a new game, and there are clear/add/get routines to
 * allow the the collection to be serialized.
 */
public class NameCache {

    private static final Logger logger = Logger.getLogger(NameCache.class.getName());

    /** Default season names to use if nameCache.season.* not found. */
    private static final String[] DEFAULT_SEASON_IDS
        = { "model.season.spring.name", "model.season.autumn.name" };
    
    private final static String CIBOLA_PREFIX
        = "nameCache.lostCityRumour.cityName.";

    /** Cities of Cibola. */
    private static List<String> cibolaKeys = null;
    private static final Object cibolaLock = new Object();

    /** Mercenary leaders. */
    private static List<String> mercenaryLeaders = null;
    private static final Object mercenaryLock = new Object();

    /** Region names and index. */
    private static final Map<String, List<String>> regionNames
        = new HashMap<>();
    private static final Object regionNameLock = new Object();
    private static final Map<String, Integer> regionIndex = new HashMap<>();

    /** Extra river names. */
    private static List<String> riverNames = null;
    private static final Object riverNameLock = new Object();

    /** Season names. */
    private static List<String> seasonNames = null;
    private static final Object seasonNamesLock = new Object();
    private static int seasonNumber = 0;
    
    /** Settlement names. */
    private static final Map<Player, String> capitalNames
        = new HashMap<>();
    private static final Map<Player, List<String>> settlementNames
        = new HashMap<>();
    private static final Object settlementNameLock = new Object();

    /** Ship names. */
    private static final Map<Player, List<String>> shipNames = new HashMap<>();
    private static final Object shipNameLock = new Object();


    /**
     * Collects all the names with a given prefix.
     *
     * Note: some collections start at 0, some at 1.
     *
     * @param prefix The prefix to check.
     * @param names A list to fill with the names found.
     */
    private static void collectNames(String prefix, List<String> names) {
        String name;
        if (Messages.containsKey(name = prefix + "0")) {
            names.add(Messages.message(name));
        }
        int i = 1;
        while (Messages.containsKey(name = prefix + Integer.toString(i))) {
            names.add(Messages.message(name));
            i++;
        }
    }

    /**
     * Initialize the cities of Cibola collection.
     *
     * Public for FreeColServer to initialize with a new game.
     *
     * @param random A pseudo-random number source.
     */
    public static void requireCitiesOfCibola(Random random) {
        synchronized (cibolaLock) {
            if (cibolaKeys == null) {
                cibolaKeys = new ArrayList<>();
                collectNames(CIBOLA_PREFIX, cibolaKeys);
                int count = cibolaKeys.size();
                // Actually, store the keys.
                cibolaKeys.clear();
                for (int i = 0; i < count; i++) {
                    cibolaKeys.add(CIBOLA_PREFIX + i);
                }
                randomShuffle(logger, "Cibola", cibolaKeys, random);
            }
        }
    }

    /**
     * Initialize the mercenary leaders collection.
     */
    private static void requireMercenaryLeaders() {
        synchronized (mercenaryLock) {
            if (mercenaryLeaders == null) {
                mercenaryLeaders = new ArrayList<>();
                collectNames("nameCache.mercenaries.", mercenaryLeaders);
            }
        }
    }

    /**
     * Make a key for a player and region type.
     *
     * @param player The <code>Player</code> to install region names for.
     * @param type The <code>RegionType</code> to get names of.
     * @return A key for the player and region type.
     */
    private static String makeRegionKey(Player player, RegionType type) {
        return player.getNationId() + ".region."
            + lastPart(type.getKey(), ".") + ".";
    }

    /**
     * Initialize the region names for a player.
     *
     * @param player The <code>Player</code> to install region names for.
     * @param type The <code>RegionType</code> to get names of.
     */
    private static void requireRegionNames(Player player, RegionType type) {
        synchronized (regionNameLock) {
            final String prefix = makeRegionKey(player, type);
            List<String> names = regionNames.get(prefix);
            if (names == null) {
                names = new ArrayList<String>();
                collectNames(prefix, names);
                regionNames.put(prefix, names);
            }
            Integer index = regionIndex.get(prefix);
            if (index == null) regionIndex.put(prefix, names.size()+1);
        }
    }

    /**
     * Initialize the riverNames collection.
     */
    private static void requireRiverNames() {
        synchronized (riverNameLock) {
            if (riverNames == null) {
                riverNames = new ArrayList<>();
                collectNames("model.other.region.river.", riverNames);
                // Does not need to use player or system PRNG
                Collections.shuffle(riverNames);
            }
        }
    }

    /**
     * Initialize the seasonNames collection.
     *
     * @return The number of seasons.
     */
    private static int requireSeasonNames() {
        synchronized (seasonNamesLock) {
            if (seasonNames == null) {
                seasonNames = new ArrayList<>();
                collectNames("nameCache.season.", seasonNames);
                seasonNumber = seasonNames.size();
                if (seasonNumber < 2) {
                    seasonNames.clear();
                    for (String s : DEFAULT_SEASON_IDS) {
                        seasonNames.add(Messages.message(s));
                    }
                    seasonNumber = seasonNames.size();
                }
            }
            return seasonNumber;
        }
    }

    /**
     * Get the nth season name.
     *
     * @param index The index to look up.
     * @return The season name, or null on failure.
     */
    public static String getSeasonName(int index) {
        requireSeasonNames();
        if (index >= 0 && index < seasonNumber) return seasonNames.get(index);
        return Messages.message(StringTemplate
            .template("nameCache.season.default")
            .addAmount("%number%", index+1));
    }
    
    /**
     * Initialize the settlement names for a player.
     *
     * @param player The <code>Player</code> to install names for.
     * @param random A pseudo-random number source.
     */
    private static void requireSettlementNames(Player player, Random random) {
        synchronized (settlementNameLock) {
            if (settlementNames.get(player) == null) {
                List<String> names = new ArrayList<>();
                // @compat 0.10.x
                // Try the base names
                collectNames(player.getNationId() + ".settlementName.", names);
                // end @compat 0.10.x

                // Try the spec-qualified version.
                if (names.isEmpty()) {
                    collectNames(player.getNationId() + ".settlementName."
                        + player.getSpecification().getId() + ".", names);
                }

                // If still empty use the "freecol" names.
                if (names.isEmpty()) {
                    collectNames(player.getNationId() + ".settlementName.freecol.",
                                 names);
                }

                // Indians have capitals and need randomization
                if (player.isIndian()) {
                    capitalNames.put(player, names.remove(0));
                    if (random != null) {
                        randomShuffle(logger, "Settlement names", names, random);
                    }
                }
                settlementNames.put(player, names);
                logger.fine("Loaded " + names.size() + " settlement names for "
                    + player.getId());
            }
        }
    }

    /**
     * Initialize the shipNames for a player.
     *
     * @param player The <code>Player</code> to install names for.
     * @param random A pseudo-random number source.
     */
    private static void requireShipNames(Player player, Random random) {
        synchronized (shipNameLock) {
            List<String> names = shipNames.get(player);
            if (names == null) {
                final String prefix = player.getNationId() + ".ship.";
                names = new ArrayList<String>();
                collectNames(prefix, names);
                if (random != null) {
                    randomShuffle(logger, "Ship names", names, random);
                }
                shipNames.put(player, names);
            }
        }
    }
    

    /**
     * Get the next name for a city of Cibola, removing it from the
     * list of available names.
     *
     * @return A name for a city of Cibola, or null if exhausted.
     */
    public static String getNextCityOfCibola() {
        synchronized (cibolaLock) {
            return (cibolaKeys == null || cibolaKeys.isEmpty()) ? null
                : Messages.message(cibolaKeys.remove(0));
        }
    }

    /**
     * Get the current list of available cities of Cibola keys.
     *
     * @return A list of city names.
     */
    public static List<String> getCitiesOfCibola() {
        synchronized (cibolaLock) {
            return (cibolaKeys == null) ? Collections.<String>emptyList()
                : cibolaKeys;
        }
    }

    /**
     * Clear the city of Cibola cache.
     */
    public static void clearCitiesOfCibola() {
        synchronized (cibolaLock) {
            if (cibolaKeys != null) cibolaKeys.clear();
        }
    }

    /**
     * Add a key for a city of Cibola.
     *
     * @param key The key to add.
     */
    public static void addCityOfCibola(String key) {
        synchronized (cibolaLock) {
            if (cibolaKeys == null) cibolaKeys = new ArrayList<>();
            cibolaKeys.add(key);
        }
    }

    /**
     * Get a random mercenary leader index.
     *
     * @param random A pseudo-random number source.
     * @return The index of a random mercenary leader.
     */
    public static int getMercenaryLeaderIndex(Random random) {
        requireMercenaryLeaders();
        int n;
        synchronized (mercenaryLock) {
            n = mercenaryLeaders.size();
        }
        return randomInt(logger, "Mercenary leader", random, n);
    }

    /**
     * Get a mercenary leader name by index.
     *
     * @param index The index to look up.
     * @return The mercenary leader name.
     */
    public static String getMercenaryLeaderName(int index) {
        requireMercenaryLeaders();
        synchronized (mercenaryLock) {
            return mercenaryLeaders.get(index);
        }
    }

    /**
     * Get the new land name for a player.
     *
     * @param player The <code>Player</code> to query.
     * @return The new land name of a player.
     */
    public static String getNewLandName(Player player) {
        return (player.getNewLandName() == null)
            ? Messages.message(player.getNationId() + ".newLandName")
            : player.getNewLandName();
    }

    /**
     * Creates a unique region name for a player by fetching a new
     * name from the list of default names if possible.
     *
     * @param player The <code>Player</code> to find a region name for.
     * @param region The <code>Region</code> to name.
     * @return A suitable name.
     */
    public static String getRegionName(Player player, Region region) {
        if (region.hasName()) {
            return Messages.message(region.getLabel());
        }
        final net.sf.freecol.common.model.Map map = player.getGame().getMap();
        String name;
        int index;

        // Try national names first.
        final String prefix = makeRegionKey(player, region.getType());
        requireRegionNames(player, region.getType());
        synchronized (regionNameLock) {
            List<String> names = regionNames.get(prefix);
            while (!names.isEmpty()) {
                name = names.remove(0);
                if (map.getRegionByName(name) == null) return name;
            }
        }
            
        // There are a bunch of extra rivers not attached to a specific
        // nation at model.other.region.river.*.
        if (region.getType() == Region.RegionType.RIVER) {
            requireRiverNames();
            synchronized (riverNameLock) {
                while (!riverNames.isEmpty()) {
                    name = riverNames.remove(0);
                    if (map.getRegionByName(name) == null) return name;
                }
            }
        }

        // Fall back to generic names.
        synchronized (regionNameLock) {
            index = regionIndex.get(prefix);
        }
        StringTemplate nn = player.getNationLabel();
        do {
            name = Messages.message(StringTemplate.label(" ")
                .addStringTemplate(nn)
                .addNamed(region.getType())
                .addName(String.valueOf(index)));
            index++;
        } while (map.getRegionByName(name) != null);
        synchronized (regionNameLock) {
            regionIndex.put(prefix, index);
        }
        return name;
    }

    /**
     * Get a fallback settlement name for a player.
     *
     * @param player The <code>Player</code> to get a fallback
     *     settlement name for.
     * @return A unique fallback settlement name for the player.
     */
    private static String getFallbackSettlementName(Player player) {
        return Messages.message((player.isEuropean())
            ? "nameCache.base.colony"
            : "nameCache.base.settlement") + "-";
    }

    /**
     * Get the name of this players capital.  Only meaningful to natives.
     *
     * @param player The <code>Player</code> to get a capital name for.
     * @param random An optional pseudo-random number source.
     * @return The name of this players capital.
     */
    public static String getCapitalName(Player player, Random random) {
        requireSettlementNames(player, random);
        synchronized (settlementNameLock) {
            return capitalNames.get(player);
        }
    }

    /**
     * Get a settlement name suitable for a player.
     *
     * @param player The <code>Player</code> to get a settlement name for.
     * @param random An optional pseudo-random number source.
     * @return A new settlement name.
     */
    public static String getSettlementName(Player player, Random random) {
        requireSettlementNames(player, random);
        final Game game = player.getGame();
        synchronized (settlementNameLock) {
            List<String> names = settlementNames.get(player);
            while (!names.isEmpty()) {
                String name = names.remove(0);
                if (game.getSettlementByName(name) == null) return name;
            }
        }

        // Use the fallback name
        final String base = getFallbackSettlementName(player);
        int i = player.getSettlements().size() + 1;
        String name = null;
        while (game.getSettlementByName(name = base + i++) != null);
        return name;
    }           

    /**
     * Puts a suggested settlement name back into the pool.
     *
     * @param player The <code>Player</code> returning the settlement name.
     * @param name A formerly suggested settlement name.
     */
    public static void putSettlementName(Player player, String name) {
        if (!name.startsWith(getFallbackSettlementName(player))) {
            requireSettlementNames(player, null);
            synchronized (settlementNameLock) {
                List<String> names = settlementNames.get(player);
                names.add(name);
            }
        }
    }

    /**
     * Get a new default trade route name for a player.
     *
     * @param player The <code>Player</code> to get the name for.
     * @return A new trade route name.
     */
    public static String getTradeRouteName(Player player) {
        String base = Messages.message("nameCache.base.tradeRoute");
        if (player.getTradeRouteByName(base) == null) return base;
        String name;
        int i = 1;
        while (player.getTradeRouteByName(name = base + i++) != null);
        return name;
    }

    /**
     * Gets a new name for a unit.
     *
     * Currently only names naval units, not specific to type.
     * FIXME: specific names for types.
     *
     * @param player The <code>Player</code> who will own the unit.
     * @param type The <code>UnitType</code> to choose a name for.
     * @param random A pseudo-random number source.
     * @return A name for the unit, or null if not available.
     */
    public static String getUnitName(Player player, UnitType type,
                                     Random random) {
        if (!type.isNaval()) return null;
        String name;

        // Find a new name in the installed ship names if possible.
        requireShipNames(player, random);
        synchronized (shipNameLock) {
            List<String> names = shipNames.get(player);
            while (!names.isEmpty()) {
                name = names.remove(0);
                if (player.getUnitByName(name) == null) return name;
            }
        }

        // Get a fallback ship name
        final String base = Messages.message("nameCache.base.ship") + "-";
        int i = 1;
        while (player.getUnitByName(name = base + i++) != null);
        return name;
    }
}
