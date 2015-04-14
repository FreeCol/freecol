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
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * A cache of proper names of various types.
 */
public class NameCache {

    private static final Logger logger = Logger.getLogger(NameCache.class.getName());

    private final static String CIBOLA_PREFIX = "lostCityRumour.cityName.";
    
    /** Cities of Cibola. */
    private static List<String> cibolaKeys = null;
    private static Object cibolaLock = new Object();

    /** Mercenary leaders. */
    private static List<String> mercenaryLeaders = null;

    /** Extra river names. */
    private static List<String> otherRivers = null;

    /** Settlement names. */
    private static Map<Player, String> capitalNames = null;
    private static Map<Player, List<String>> settlementNames = null;
    private static final Object settlementNameLock = new Object();

    /** Ship names. */
    private static final Map<Player, List<String>> shipNames = new HashMap<>();


    /**
     * Collects all the names with a given prefix.
     *
     * @param prefix The prefix to check.
     * @param names A list to fill with the names found.
     */
    private static void collectNames(String prefix, List<String> names) {
        String name;
        int i = 0;
        while (Messages.containsKey(name = prefix + Integer.toString(i))) {
            names.add(Messages.message(name));
            i++;
        }
    }

    /**
     * Initialize the cities of Cibola collection.
     *
     * Public for FreeColServer.
     *
     * @param random A pseudo-random number source.
     */
    public static void requireCitiesOfCibola(Random random) {
        synchronized (cibolaLock) {
            if (cibolaKeys == null) {
                cibolaKeys = new ArrayList<>();
                collectNames("lostCityRumour.cityName.", cibolaKeys);
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
    private static synchronized void requireMercenaryLeaders() {
        if (mercenaryLeaders == null) {
            mercenaryLeaders = new ArrayList<>();
            collectNames("model.mercenaries.", mercenaryLeaders);
        }
    }

    /**
     * Initialize the otherRivers collection.
     */
    private static synchronized void requireOtherRivers() {
        if (otherRivers == null) {
            otherRivers = new ArrayList<>();
            collectNames("model.other.region.river.", otherRivers);
            // Does not need to use player or system PRNG
            Collections.shuffle(otherRivers);
        }
    }

    /**
     * Initialize the settlement names for a player.
     *
     * @param player The <code>Player</code> to install names for.
     * @param random A pseudo-random number source.
     */
    private static synchronized void requireSettlementNames(Player player,
                                                            Random random) {
        if (settlementNames == null) {
            capitalNames = new HashMap<>();
            settlementNames = new HashMap<>();
        }
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

    /**
     * Initialize the shipNames for a player.
     *
     * @param player The <code>Player</code> to install names for.
     * @param random A pseudo-random number source.
     * @return A list of ship names for the player.
     */
    public static synchronized List<String> requireShipNames(Player player,
                                                             Random random) {
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
        return names;
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
    public static synchronized void clearCitiesOfCibola() {
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
        return randomInt(logger, "Mercenary leader", random,
                         mercenaryLeaders.size());
    }

    /**
     * Get a mercenary leader name by index.
     *
     * @param index The index to look up.
     * @return The mercenary leader name.
     */
    public static String getMercenaryLeaderName(int index) {
        requireMercenaryLeaders();
        return mercenaryLeaders.get(index);
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
        if (region.isPacific()) {
            return Messages.message(Region.PACIFIC_NAME_KEY);
        }
        // Try national names first.
        net.sf.freecol.common.model.Map map = player.getGame().getMap();
        int index = player.getNameIndex(region.getType().getNameIndexKey());
        if (index < 1) index = 1;
        String prefix = player.getNationId() + ".region."
            + region.getType().getKey() + ".";
        String name;
        do {
            name = null;
            if (Messages.containsKey(prefix + Integer.toString(index))) {
                name = Messages.message(prefix + Integer.toString(index));
                index++;
            }
        } while (name != null && map.getRegionByName(name) != null);
        player.setNameIndex(region.getType().getNameIndexKey(), index);

        // There are a bunch of extra rivers not attached to a specific
        // nation at model.other.region.river.*.
        if (name == null && region.getType() == Region.RegionType.RIVER) {
            requireOtherRivers();
            while (!otherRivers.isEmpty()) {
                name = otherRivers.remove(0);
                if (map.getRegionByName(name) == null) return name;
            }
            name = null;
        }

        // Fall back to generic names.
        if (name == null) {
            StringTemplate nn = player.getNationName();
            do {
                name = Messages.message(StringTemplate
                    .label("").addStringTemplate(nn)
                    .addName(" ").addNamed(region.getType())
                    .addName(" " + String.valueOf(index)));
                index++;
            } while (map.getRegionByName(name) != null);
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
        return Messages.message((player.isEuropean()) ? "Colony"
            : "Settlement") + "-";
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
                if (game.getSettlement(name) == null) return name;
            }
        }

        // Use the fallback name
        final String base = getFallbackSettlementName(player);
        int i = player.getSettlements().size() + 1;
        String name = null;
        for (;;) {
            name = base + Integer.toString(i);
            if (game.getSettlement(name) == null) break;
            i++;
        }
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
        String base = Messages.message("tradeRoute.newRoute");
        if (player.getTradeRoute(base) == null) return base;
        int i = 1;
        String name;
        for (;;) {
            name = base + Integer.toString(i);
            if (player.getTradeRoute(name) == null) break;
            i++;
        }
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
        List<String> names = requireShipNames(player, random);

        // Collect all the names of existing naval units.
        List<String> navalNames = new ArrayList<>();
        for (Unit u : player.getUnits()) {
            if (u.isNaval() && u.getName() != null) {
                navalNames.add(u.getName());
            }
        }

        // Find a new name in the installed ship names if possible.
        String name;
        while (!names.isEmpty()) {
            name = names.remove(0);
            if (!navalNames.contains(name)) return name;
        }

        // Get a fallback ship name
        final String base = Messages.message("Ship") + "-";
        int i = 0;
        for (;;) {
            name = base + Integer.toString(i);
            if (player.getUnit(name) == null) break;
            i++;
        }
        return name;
    }
}
