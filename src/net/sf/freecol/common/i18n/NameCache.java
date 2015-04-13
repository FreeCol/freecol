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

    /** Mercenary leaders. */
    private static List<String> mercenaryLeaders = null;

    /** Extra river names. */
    private static List<String> otherRivers = null;

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
    public static synchronized void requireOtherRivers() {
        if (otherRivers == null) {
            otherRivers = new ArrayList<>();
            collectNames("model.other.region.river.", otherRivers);
            // Does not need to use player or system PRNG
            Collections.shuffle(otherRivers);
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
     * Get the stem of the fallback settlement name for a player.
     *
     * @param player The <code>Player</code> to get the base settlement name
     *     for.
     * @return The base settlement name for a player.
     */
    private static String getBaseSettlementName(Player player) {
        return Messages.message((player.isEuropean()) ? "Colony"
            : "Settlement") + "-";
    }

    /**
     * Is a name a fallback settlement name for a player?
     *
     * @param name The settlement name to check.
     * @param player The <code>Player</code> to check.
     * @return True if the name is a fallback settlement name for the player.
     */
    public static boolean isFallbackSettlementName(String name, Player player) {
        return name.startsWith(getBaseSettlementName(player));
    }

    /**
     * Get a fallback settlement name for a player.
     *
     * @param player The <code>Player</code> to get a fallback
     *     settlement name for.
     * @return A unique fallback settlement name for the player.
     */
    public static String getFallbackSettlementName(Player player) {
        final String base = getBaseSettlementName(player);
        int i = player.getSettlements().size() + 1;
        String name = null;
        for (;;) {
            name = base + Integer.toString(i);
            if (player.getGame().getSettlement(name) == null) break;
            i++;
        }
        return name;
    }

    /**
     * Gets a list of settlement names and a fallback prefix for a player.
     *
     * @param player The <code>Player</code> to get names for.
     * @return A list of settlement names, with the first being the
     *     fallback prefix.
     */
    public static List<String> getSettlementNames(Player player) {
        List<String> names = new ArrayList<>();

        collectNames(player.getNationId() + ".settlementName.", names);

        // Try the spec-qualified version.
        if (names.isEmpty()) {
            collectNames(player.getNationId() + ".settlementName."
                + player.getSpecification().getId() + ".", names);
        }

        // If still empty and not using the "freecol" ruleset, try
        // those names.
        if (names.isEmpty()) {
            collectNames(player.getNationId() + ".settlementName."
                + "freecol.", names);
        }

        return names;
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
