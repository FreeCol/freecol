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

package net.sf.freecol.server.generator;

/**
 * Keeps track of the available map generator options.
 * 
 * <br>
 * <br>
 * 
 * New options should be added to {@link #addDefaultOptions()} and each option
 * should be given an unique identifier (defined as a constant in this class).
 * 
 * @see MapGenerator
 */
public class MapGeneratorOptions {

    /**
     * Option for setting the land mass of the map.
     */
    public static final String LAND_MASS = "model.option.landMass";

    /**
     * Option for setting the number of rivers on the map.
     */
    public static final String RIVER_NUMBER = "model.option.riverNumber";

    /**
     * Option for setting the number of mountains on the map.
     */
    public static final String MOUNTAIN_NUMBER = "model.option.mountainNumber";

    /**
     * Option for setting the number of rumours on the map.
     */
    public static final String RUMOUR_NUMBER = "model.option.rumourNumber";

    /**
     * Option for setting the number of settlements on the map.
     */
    public static final String SETTLEMENT_NUMBER = "model.option.settlementNumber";

    /**
     * Option for setting the percentage of forests on the map.
     */
    public static final String FOREST_NUMBER = "model.option.forestNumber";

    /**
     * Option for setting the percentage of bonus tiles on the map.
     */
    public static final String BONUS_NUMBER = "model.option.bonusNumber";

    /**
     * Option for setting the humidity of the map.
     */
    public static final String HUMIDITY = "model.option.humidity";

    /**
     * Option for setting the temperature of the map.
     */
    public static final String TEMPERATURE = "model.option.temperature";

    /**
     * One of the settings used by {@link #TEMPERATURE}.
     */
    public static final int TEMPERATURE_COLD      = 0,
                            TEMPERATURE_CHILLY    = 1,
                            TEMPERATURE_TEMPERATE = 2,
                            TEMPERATURE_WARM      = 3,
                            TEMPERATURE_HOT       = 4;

    /**
     * Option for setting a file to be imported (map etc).
     */
    public static final String IMPORT_FILE = "model.option.importFile";

    /**
     * Option for using the terrain imported from a file.
     */
    public static final String IMPORT_TERRAIN = "model.option.importTerrain";

    /**
     * Option for using the bonuses imported from a file.
     */
    public static final String IMPORT_BONUSES = "model.option.importBonuses";

    /**
     * Option for using the lost city rumours imported from a file.
     */
    public static final String IMPORT_RUMOURS = "model.option.importRumours";

    /**
     * Option for using the settlements imported from a file.
     */
    public static final String IMPORT_SETTLEMENTS = "model.option.importSettlements";

    /**
     * Option for setting the type of land generator to be used.
     */
    public static final String LAND_GEN_TYPE = "model.option.landGeneratorType";

    public static final int LAND_GEN_CLASSIC     = 0,
                            LAND_GEN_CONTINENT   = 1,
                            LAND_GEN_ARCHIPELAGO = 2,
                            LAND_GEN_ISLANDS     = 3;

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "mapGeneratorOptions".
     */
    public static String getXMLElementTagName() {
        return "mapGeneratorOptions";
    }

}
