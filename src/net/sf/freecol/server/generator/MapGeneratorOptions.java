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

package net.sf.freecol.server.generator;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.OptionMap;
import net.sf.freecol.common.option.RangeOption;

import org.w3c.dom.Element;


/**
 * Keeps track of the available map generator options.
 *
 * <br><br>
 *
 * New options should be added to
 * {@link #addDefaultOptions()} and each option should be given an unique
 * identifier (defined as a constant in this class).
 * 
 * @see MapGenerator
 */
public class MapGeneratorOptions extends OptionMap {


    /**
     * Option for setting the size of the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #MAP_SIZE_SMALL}</li>
     *     <li>{@link #MAP_SIZE_MEDIUM}</li>
     *     <li>{@link #MAP_SIZE_LARGE}</li>
     *     <li>{@link #MAP_SIZE_HUGE}</li>
     *     
     */
    public static final String MAP_SIZE = "mapSize";
    
    /**
     * One of the settings used by {@link #MAP_SIZE}.
     */
    public static final int MAP_SIZE_SMALL = 0,
                            MAP_SIZE_MEDIUM = 1,
                            MAP_SIZE_LARGE = 2,
                            MAP_SIZE_VERY_LARGE = 3,
                            MAP_SIZE_HUGE = 4;
                            
    
    /**
     * Option for setting the land mass of the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #LAND_MASS_SMALL}</li>
     *     <li>{@link #LAND_MASS_MEDIUM}</li>
     *     <li>{@link #LAND_MASS_LARGE}</li>
     *     <li>{@link #LAND_MASS_HUGE}</li>
     *     
     */
    public static final String LAND_MASS = "landMass";
    
    /**
     * One of the settings used by {@link #LAND_MASS}.
     */
    public static final int LAND_MASS_SMALL = 0,
                            LAND_MASS_MEDIUM = 1,
                            LAND_MASS_LARGE = 2,
                            LAND_MASS_VERY_LARGE = 3,
                            LAND_MASS_HUGE = 4;
                            
    
    /**
     * Option for setting the number of rivers on the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #RIVER_NUMBER_SMALL}</li>
     *     <li>{@link #RIVER_NUMBER_MEDIUM}</li>
     *     <li>{@link #RIVER_NUMBER_LARGE}</li>
     *     <li>{@link #RIVER_NUMBER_HUGE}</li>
     *     
     */
    public static final String RIVER_NUMBER = "riverNumber";
    
    /**
     * One of the settings used by {@link #RIVER_NUMBER}.
     */
    public static final int RIVER_NUMBER_SMALL = 0,
                            RIVER_NUMBER_MEDIUM = 1,
                            RIVER_NUMBER_LARGE = 2,
                            RIVER_NUMBER_VERY_LARGE = 3,
                            RIVER_NUMBER_HUGE = 4;

    /**
     * Option for setting the number of mountains on the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #MOUNTAIN_NUMBER_SMALL}</li>
     *     <li>{@link #MOUNTAIN_NUMBER_MEDIUM}</li>
     *     <li>{@link #MOUNTAIN_NUMBER_LARGE}</li>
     *     <li>{@link #MOUNTAIN_NUMBER_HUGE}</li>
     *     
     */
    public static final String MOUNTAIN_NUMBER = "mountainNumber";
    
    /**
     * One of the settings used by {@link #MOUNTAIN_NUMBER}.
     */
    public static final int MOUNTAIN_NUMBER_SMALL = 0,
                            MOUNTAIN_NUMBER_MEDIUM = 1,
                            MOUNTAIN_NUMBER_LARGE = 2,
                            MOUNTAIN_NUMBER_VERY_LARGE = 3,
                            MOUNTAIN_NUMBER_HUGE = 4;
                                                        
    
    /**
     * Option for setting the number of rumours on the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #RUMOUR_NUMBER_SMALL}</li>
     *     <li>{@link #RUMOUR_NUMBER_MEDIUM}</li>
     *     <li>{@link #RUMOUR_NUMBER_LARGE}</li>
     *     <li>{@link #RUMOUR_NUMBER_HUGE}</li>
     *     
     */
    public static final String RUMOUR_NUMBER = "rumourNumber";
    
    /**
     * One of the settings used by {@link #RUMOUR_NUMBER}.
     */
    public static final int RUMOUR_NUMBER_SMALL = 0,
                            RUMOUR_NUMBER_MEDIUM = 1,
                            RUMOUR_NUMBER_LARGE = 2,
                            RUMOUR_NUMBER_VERY_LARGE = 3,
                            RUMOUR_NUMBER_HUGE = 4;
                                                        
    
    /**
     * Option for setting the number of settlements on the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #SETTLEMENT_NUMBER_SMALL}</li>
     *     <li>{@link #SETTLEMENT_NUMBER_MEDIUM}</li>
     *     <li>{@link #SETTLEMENT_NUMBER_LARGE}</li>
     *     <li>{@link #SETTLEMENT_NUMBER_HUGE}</li>
     *     
     */
    public static final String SETTLEMENT_NUMBER = "settlementNumber";
    
    /**
     * One of the settings used by {@link #SETTLEMENT_NUMBER}.
     */
    public static final int SETTLEMENT_NUMBER_SMALL = 0,
                            SETTLEMENT_NUMBER_MEDIUM = 1,
                            SETTLEMENT_NUMBER_LARGE = 2,
                            SETTLEMENT_NUMBER_VERY_LARGE = 3,
                            SETTLEMENT_NUMBER_HUGE = 4;
                                                        
    
    /**
     * Option for setting the percentage of forests on the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #FOREST_NUMBER_SMALL}</li>
     *     <li>{@link #FOREST_NUMBER_MEDIUM}</li>
     *     <li>{@link #FOREST_NUMBER_LARGE}</li>
     *     <li>{@link #FOREST_NUMBER_HUGE}</li>
     *     
     */
    public static final String FOREST_NUMBER = "forestNumber";
    
    /**
     * One of the settings used by {@link #FOREST_NUMBER}.
     */
    public static final int FOREST_NUMBER_SMALL = 0,
                            FOREST_NUMBER_MEDIUM = 1,
                            FOREST_NUMBER_LARGE = 2,
                            FOREST_NUMBER_VERY_LARGE = 3,
                            FOREST_NUMBER_HUGE = 4;
                                                        
    
    /**
     * Option for setting the percentage of bonus tiles on the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #BONUS_NUMBER_SMALL}</li>
     *     <li>{@link #BONUS_NUMBER_MEDIUM}</li>
     *     <li>{@link #BONUS_NUMBER_LARGE}</li>
     *     <li>{@link #BONUS_NUMBER_HUGE}</li>
     *     
     */
    public static final String BONUS_NUMBER = "bonusNumber";
    
    /**
     * One of the settings used by {@link #BONUS_NUMBER}.
     */
    public static final int BONUS_NUMBER_SMALL = 0,
                            BONUS_NUMBER_MEDIUM = 1,
                            BONUS_NUMBER_LARGE = 2,
                            BONUS_NUMBER_VERY_LARGE = 3,
                            BONUS_NUMBER_HUGE = 4;
    
    /**
     * Option for setting the humidity of the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #HUMIDITY_VERY_DRY}</li>
     *     <li>{@link #HUMIDITY_DRY}</li>
     *     <li>{@link #HUMIDITY_NORMAL}</li>
     *     <li>{@link #HUMIDITY_WET}</li>
     *     <li>{@link #HUMIDITY_VERY_WET}</li>
     *     
     */
    public static final String HUMIDITY = "humidity";
    
    /**
     * One of the settings used by {@link #HUMIDITY}.
     */
    public static final int HUMIDITY_VERY_DRY = 0,
                            HUMIDITY_DRY = 1,
                            HUMIDITY_NORMAL = 2,
                            HUMIDITY_WET = 3,
                            HUMIDITY_VERY_WET = 4;
    
    /**
     * Option for setting the temperature of the map.
     * Possible values are:
     *   <ul>
     *     <li>{@link #TEMPERATURE_COLD}</li>
     *     <li>{@link #TEMPERATURE_CHILLY}</li>
     *     <li>{@link #TEMPERATURE_TEMPERATE}</li>
     *     <li>{@link #TEMPERATURE_WARM}</li>
     *     <li>{@link #TEMPERATURE_HOT}</li>
     *     
     */
    public static final String TEMPERATURE = "temperature";
    
    /**
     * One of the settings used by {@link #TEMPERATURE}.
     */
    public static final int TEMPERATURE_COLD = 0,
                            TEMPERATURE_CHILLY = 1,
                            TEMPERATURE_TEMPERATE = 2,
                            TEMPERATURE_WARM = 3,
                            TEMPERATURE_HOT = 4;
    
             
    /**
     * Option for setting a file to be imported (map etc).
     */
    public static final String IMPORT_FILE = "importFile";
    
    /**
     * Option for using the terrain imported from a file.
     */
    public static final String IMPORT_TERRAIN = "importTerrain";

    /**
     * Option for using the bonuses imported from a file.
     */
    public static final String IMPORT_BONUSES = "importBonuses";

    /**
     * Option for using the lost city rumours imported from a file.
     */
    public static final String IMPORT_RUMOURS = "importRumours";
    
    /**
     * Option for using the settlements imported from a file.
     */
    public static final String IMPORT_SETTLEMENTS = "importSettlements";
    
    /**
     * Creates a new <code>MapGeneratorOptions</code>.
     */
    public MapGeneratorOptions() {
        super(getXMLElementTagName());
    }


    /**
     * Creates a <code>MapGeneratorOptions</code> from an XML representation.
     *
     * <br><br>
     *
     * @param element The XML <code>Element</code> from which this object
     *                should be constructed.
     */
    public MapGeneratorOptions(Element element) {
        super(element, getXMLElementTagName());
    }

    /**
     * Creates a <code>MapGeneratorOptions</code> from an XML representation.
     *
     * <br><br>
     *
     * @param in The XML stream to read the data from.
     * @throws XMLStreamException if an error occurred during parsing.          
     */
    public MapGeneratorOptions(XMLStreamReader in) throws XMLStreamException {
        super(in, getXMLElementTagName());
    }



    /**
     * Adds the options to this <code>MapGeneratorOptions</code>.
     */
    protected void addDefaultOptions() {
        /* Add options here: */
        final OptionGroup importGroup = new OptionGroup("mapGeneratorOptions.import");
        new FileOption(IMPORT_FILE, importGroup);
        new BooleanOption(IMPORT_TERRAIN, importGroup, true);
        new BooleanOption(IMPORT_BONUSES, importGroup, false);
        new BooleanOption(IMPORT_RUMOURS, importGroup, false);
        new BooleanOption(IMPORT_SETTLEMENTS, importGroup, false);
        add(importGroup);
        
        String[] sizes = new String[] {"small", "medium", "large", "veryLarge", "huge"};
        for (int index = 0; index < sizes.length; index++) {
            sizes[index] = Messages.message(sizes[index]);
        }

        final OptionGroup landGeneratorGroup = new OptionGroup("mapGeneratorOptions.landGenerator");
        String[] mapSizes = new String[sizes.length];
        for (int index = 0; index < sizes.length; index++) {
            mapSizes[index] = "<html><center>" + sizes[index] + "<br/>(" + getWidth(index) + "\u00D7" +
                getHeight(index) + ")</center></html>";
        }
        new RangeOption(MAP_SIZE, landGeneratorGroup, mapSizes, 0, true);

        String[] landMasses = new String[sizes.length];
        for (int index = 0; index < sizes.length; index++) {
            landMasses[index] = "<html><center>" + sizes[index] + "<br/>(" + getLandMass(index) +
                "%)</center></html>";
        }
        new RangeOption(LAND_MASS, landGeneratorGroup, landMasses, 1, true);
        add(landGeneratorGroup);
        
        final OptionGroup terrainGeneratorGroup = new OptionGroup("mapGeneratorOptions.terrainGenerator");
        new RangeOption(RIVER_NUMBER, terrainGeneratorGroup, sizes, RIVER_NUMBER_MEDIUM, true);
        new RangeOption(MOUNTAIN_NUMBER, terrainGeneratorGroup, sizes, MOUNTAIN_NUMBER_MEDIUM, true);
        new RangeOption(RUMOUR_NUMBER, terrainGeneratorGroup, sizes, RUMOUR_NUMBER_MEDIUM, true);
        new RangeOption(SETTLEMENT_NUMBER, terrainGeneratorGroup, sizes, SETTLEMENT_NUMBER_MEDIUM, true);
        new RangeOption(FOREST_NUMBER, terrainGeneratorGroup, sizes, FOREST_NUMBER_LARGE, true);
        new RangeOption(BONUS_NUMBER, terrainGeneratorGroup, sizes, BONUS_NUMBER_MEDIUM, true);
        
        String[] humidities = new String[] {"veryDry", "dry", "normal", "wet", "veryWet"};
        for (int index = 0; index < sizes.length; index++) {
            humidities[index] = Messages.message(humidities[index]);
        }
        new RangeOption(HUMIDITY, terrainGeneratorGroup, humidities, HUMIDITY_NORMAL, true);
        
        String[] temperatures = new String[] {"cold", "chilly", "temperate", "warm", "hot"};
        for (int index = 0; index < sizes.length; index++) {
            temperatures[index] = Messages.message(temperatures[index]);
        }
        new RangeOption(TEMPERATURE, terrainGeneratorGroup, temperatures, TEMPERATURE_TEMPERATE, true);
        
        add(terrainGeneratorGroup);
    }

    /**
     * Gets the width of the map to be created.
     * @return The width of the map.
     */
    public int getWidth() {
        return getWidth(getInteger(MAP_SIZE));
    }
        
    private int getWidth(final int size) {
        switch (size) {
        case MAP_SIZE_SMALL:
            return 28;
        case MAP_SIZE_MEDIUM:
            return 28;
        case MAP_SIZE_LARGE:
            return 37;
        case MAP_SIZE_VERY_LARGE:
            return 46;
        case MAP_SIZE_HUGE:
            return 55;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }
    
    /**
     * Gets the height of the map to be created.
     * @return The height of the map.
     */
    public int getHeight() {
        return getHeight(getInteger(MAP_SIZE));
    }

    private int getHeight(int size) {
        switch (size) {
        case MAP_SIZE_SMALL:
            return 65;
        case MAP_SIZE_MEDIUM:
            return 128;
        case MAP_SIZE_LARGE:
            return 192;
        case MAP_SIZE_VERY_LARGE:
            return 257;
        case MAP_SIZE_HUGE:
            return 321;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }

    /**
     * Gets the percentage of land of the map to be created.
     * @return The percentage of land.
     */
    public int getLandMass() {
        return getLandMass(getInteger(LAND_MASS));
    }

    private int getLandMass(int landMass) {
        switch (landMass) {
        case LAND_MASS_SMALL:
            return 15;
        case LAND_MASS_MEDIUM:
            return 25;
        case LAND_MASS_LARGE:
            return 30;
        case LAND_MASS_VERY_LARGE:
            return 40;
        case LAND_MASS_HUGE:
            return 50;
        default:
            throw new IllegalStateException("Invalid land mass: " + landMass + ".");
        }
    }

    /**
     * Gets the approximate number of land tiles to be created.
     * @return The approximate number of land tiles.
     */
    public int getLand() {
        return getWidth() * getHeight() * getLandMass() / 100;
    }

    /**
     * Gets the number of rivers on the map to be created.
     * @return The number of rivers.
     */
    public int getNumberOfRivers() {
        return getNumberOfRivers(getInteger(RIVER_NUMBER));
    }

    private int getNumberOfRivers(int number) {
        switch (number) {
        case RIVER_NUMBER_SMALL:
            return getLand() / 50;
        case RIVER_NUMBER_MEDIUM:
            return getLand() / 35;
        case RIVER_NUMBER_LARGE:
            return getLand() / 30;
        case RIVER_NUMBER_VERY_LARGE:
            return getLand() / 25;
        case RIVER_NUMBER_HUGE:
            return getLand() / 20;
        default:
            throw new IllegalStateException("Invalid river number: " + number + ".");
        }
    }

    /**
     * Gets the number of mountain tiles on the map to be created.
     * @return The number of mountain tiles.
     */
    public int getNumberOfMountainTiles() {
        return getNumberOfMountainTiles(getInteger(MOUNTAIN_NUMBER));
    }

    private int getNumberOfMountainTiles(int number) {
        switch (number) {
        case MOUNTAIN_NUMBER_SMALL:
            return getLand() / 20;
        case MOUNTAIN_NUMBER_MEDIUM:
            return getLand() / 10;
        case MOUNTAIN_NUMBER_LARGE:
            return getLand() / 9;
        case MOUNTAIN_NUMBER_VERY_LARGE:
            return getLand() / 8;
        case MOUNTAIN_NUMBER_HUGE:
            return getLand() / 7;
        default:
            throw new IllegalStateException("Invalid mountain number: " + number + ".");
        }
    }

    /**
     * Gets the number of rumours on the map to be created.
     * @return The number of rumours.
     */
    public int getNumberOfRumours() {
        return getNumberOfRumours(getInteger(RUMOUR_NUMBER));
    }

    private int getNumberOfRumours(int number) {
        switch (number) {
        case RUMOUR_NUMBER_SMALL:
            return getLand() / 50;
        case RUMOUR_NUMBER_MEDIUM:
            return getLand() / 35;
        case RUMOUR_NUMBER_LARGE:
            return getLand() / 30;
        case RUMOUR_NUMBER_VERY_LARGE:
            return getLand() / 25;
        case RUMOUR_NUMBER_HUGE:
            return getLand() / 20;
        default:
            throw new IllegalStateException("Invalid rumour number: " + number + ".");
        }
    }

    /**
     * Gets the number of settlements on the map to be created.
     * @return The number of settlements.
     */
    public int getNumberOfSettlements() {
        return getNumberOfSettlements(getInteger(SETTLEMENT_NUMBER));
    }

    private int getNumberOfSettlements(int number) {
        switch (number) {
        case SETTLEMENT_NUMBER_SMALL:
            return getLand() / 20;
        case SETTLEMENT_NUMBER_MEDIUM:
            return getLand() / 14;
        case SETTLEMENT_NUMBER_LARGE:
            return getLand() / 12;
        case SETTLEMENT_NUMBER_VERY_LARGE:
            return getLand() / 10;
        case SETTLEMENT_NUMBER_HUGE:
            return getLand() / 8;
        default:
            throw new IllegalStateException("Invalid settlement number: " + number + ".");
        }
    }

    /**
     * Gets the percentage of forests on the map to be created.
     * @return The percentage of forests.
     */
    public int getPercentageOfForests() {
        return getPercentageOfForests(getInteger(FOREST_NUMBER));
    }

    private int getPercentageOfForests(int number) {
        switch (number) {
        case FOREST_NUMBER_SMALL:
            return 10;
        case FOREST_NUMBER_MEDIUM:
            return 30;
        case FOREST_NUMBER_LARGE:
            return 50;
        case FOREST_NUMBER_VERY_LARGE:
            return 70;
        case FOREST_NUMBER_HUGE:
            return 90;
        default:
            throw new IllegalStateException("Invalid forest number: " + number + ".");
        }
    }

    /**
     * Gets the percentage of bonus tiles on the map to be created.
     * @return The percentage of bonus tiles.
     */
    public int getPercentageOfBonusTiles() {
        return getPercentageOfBonusTiles(getInteger(BONUS_NUMBER));
    }

    private int getPercentageOfBonusTiles(int number) {
        switch (number) {
        case BONUS_NUMBER_SMALL:
            return 5;
        case BONUS_NUMBER_MEDIUM:
            return 10;
        case BONUS_NUMBER_LARGE:
            return 13;
        case BONUS_NUMBER_VERY_LARGE:
            return 17;
        case BONUS_NUMBER_HUGE:
            return 20;
        default:
            throw new IllegalStateException("Invalid bonus number: " + number + ".");
        }
    }

    /**
     * Gets the width of "short sea" of the map to be created.
     * @return The distance to land from high seas.
     */
    public int getDistLandHighSea() {
        final int size = getInteger(MAP_SIZE);
        switch (size) {
        case MAP_SIZE_SMALL:
            return 4;
        case MAP_SIZE_MEDIUM:
            return 4;
        case MAP_SIZE_LARGE:
            return 4;
        case MAP_SIZE_VERY_LARGE:
            return 4;
        case MAP_SIZE_HUGE:
            return 4;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }

    /**
     * Gets the maximum distance to edge of the map to be created.
     * @return The maximum distance to edge.
     */
    public int getMaxDistToEdge() {
        final int size = getInteger(MAP_SIZE);
        switch (size) {
        case MAP_SIZE_SMALL:
            return 12;
        case MAP_SIZE_MEDIUM:
            return 12;
        case MAP_SIZE_LARGE:
            return 12;
        case MAP_SIZE_VERY_LARGE:
            return 12;
        case MAP_SIZE_HUGE:
            return 12;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }

    /**
     * Gets the preferred distance to edge of the map to be created.
     * @return The preferred distance to edge.
     */
    public int getPrefDistToEdge() {
        final int size = getInteger(MAP_SIZE);
        switch (size) {
        case MAP_SIZE_SMALL:
            return 4;
        case MAP_SIZE_MEDIUM:
            return 4;
        case MAP_SIZE_LARGE:
            return 4;
        case MAP_SIZE_VERY_LARGE:
            return 4;
        case MAP_SIZE_HUGE:
            return 4;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }
    
    /**
     * Gets the average humidity of the map.
     * @return The humidity.
     */
    public int getHumidity() {
        return getInteger(HUMIDITY);
    }
    
    /**
     * Gets the average temperature of the map.
     * @return The temperature.
     */
    public int getTemperature() {
        return getInteger(TEMPERATURE);
    }

    protected boolean isCorrectTagName(String tagName) {
        return getXMLElementTagName().equals(tagName);
    }
    
    /**
     * Gets the tag name of the root element representing this object.
     * @return "mapGeneratorOptions".
     */
    public static String getXMLElementTagName() {
        return "mapGeneratorOptions";
    }

}
