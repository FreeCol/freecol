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

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionMap;

import org.w3c.dom.Element;

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
public class MapGeneratorOptions extends OptionMap {

    /**
     * Option for setting the size of the map. Possible values are:
     * <ul>
     * <li>{@link #MAP_SIZE_SMALL}</li>
     * <li>{@link #MAP_SIZE_MEDIUM}</li>
     * <li>{@link #MAP_SIZE_LARGE}</li>
     * <li>{@link #MAP_SIZE_VERY_LARGE}</li>
     * <li>{@link #MAP_SIZE_HUGE}</li>
     * 
     */
    public static final String MAP_SIZE = "model.option.mapSize";

    /**
     * One of the settings used by {@link #MAP_SIZE}.
     */
    public static final int MAP_SIZE_SMALL      = 0,
                            MAP_SIZE_MEDIUM     = 1,
                            MAP_SIZE_LARGE      = 2,
                            MAP_SIZE_VERY_LARGE = 3,
                            MAP_SIZE_HUGE       = 4;

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
     * Creates a new <code>MapGeneratorOptions</code>.
     */
    public MapGeneratorOptions() {
        super(getXMLElementTagName());
    }

    /**
     * Creates a <code>MapGeneratorOptions</code> from an XML representation.
     * 
     * <br>
     * <br>
     * 
     * @param element The XML <code>Element</code> from which this object
     *            should be constructed.
     */
    public MapGeneratorOptions(Element element) {
        super(element, getXMLElementTagName());
    }

    /**
     * Creates a <code>MapGeneratorOptions</code> from an XML representation.
     * 
     * <br>
     * <br>
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
        Specification spec = Specification.getSpecification();
        add(spec.getOptionGroup("mapGeneratorOptions.import"));

        /* Add additional infos in the labels of map size and land mass options (but only once!) */
        Map<Integer, String> mapSizeValues = spec.getRangeOption(MAP_SIZE).getItemValues();
        Map<Integer, String> landMassValues = spec.getRangeOption(LAND_MASS).getItemValues();
        if(!mapSizeValues.get(0).substring(0,1).equals("<")) {
            for (int index : mapSizeValues.keySet()) {
                mapSizeValues.put(index, "<html><center>" + mapSizeValues.get(index) + "<br/>(" + getWidth(index)
                        + "\u00D7" + getHeight(index) + ")</center></html>");
            }
            for (int index : landMassValues.keySet()) {
                landMassValues.put(index, "<html><center>" + landMassValues.get(index) + "<br/>(" + index
                        + "%)</center></html>");
            }
        }
        
        add(spec.getOptionGroup("mapGeneratorOptions.landGenerator"));
        add(spec.getOptionGroup("mapGeneratorOptions.terrainGenerator"));
    }

    /**
     * Gets the width of the map to be created.
     * 
     * @return The width of the map.
     */
    public int getWidth() {
        return getWidth(getInteger(MAP_SIZE));
    }

    public static int getWidth(final int size) {
        switch (size) {
        case MAP_SIZE_SMALL:
            return 28;
        case MAP_SIZE_MEDIUM:
            return 40;
        case MAP_SIZE_LARGE:
            return 50;
        case MAP_SIZE_VERY_LARGE:
            return 60;
        case MAP_SIZE_HUGE:
            return 75;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }

    /**
     * Gets the height of the map to be created.
     * 
     * @return The height of the map.
     */
    public int getHeight() {
        return getHeight(getInteger(MAP_SIZE));
    }

    public static int getHeight(int size) {
        switch (size) {
        case MAP_SIZE_SMALL:
            return 70;
        case MAP_SIZE_MEDIUM:
            return 100;
        case MAP_SIZE_LARGE:
            return 125;
        case MAP_SIZE_VERY_LARGE:
            return 150;
        case MAP_SIZE_HUGE:
            return 190;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }

    /**
     * Gets the percentage of land of the map to be created.
     * 
     * @return The percentage of land.
     */
    public int getLandMass() {
        return Specification.getSpecification().getRangeOption(LAND_MASS).getValue();
    }

    /**
     * Gets the approximate number of land tiles to be created.
     * 
     * @return The approximate number of land tiles.
     */
    public int getLand() {
        return getWidth() * getHeight() * getLandMass() / 100;
    }

    /**
     * Gets the type of land generator to be used.
     * 
     * @return The value of the land generator.
     */
    public int getLandGeneratorType() {
        return Specification.getSpecification().getRangeOption(LAND_GEN_TYPE).getValue();
    }

    /**
     * Gets the number of rivers on the map to be created.
     * 
     * @return The number of rivers.
     */
    public int getNumberOfRivers() {
        return getLand()/Specification.getSpecification().getRangeOption(RIVER_NUMBER).getValue();
    }

    /**
     * Gets the number of mountain tiles on the map to be created.
     * 
     * @return The number of mountain tiles.
     */
    public int getNumberOfMountainTiles() {
        return getLand()/Specification.getSpecification().getRangeOption(MOUNTAIN_NUMBER).getValue();
    }

    /**
     * Gets the number of rumours on the map to be created.
     * 
     * @return The number of rumours.
     */
    public int getNumberOfRumours() {
        return getLand()/Specification.getSpecification().getRangeOption(RUMOUR_NUMBER).getValue();
    }

    /**
     * Gets the number of settlements on the map to be created.
     * 
     * @return The number of settlements.
     */
    public int getNumberOfSettlements() {
        return getLand()/Specification.getSpecification().getRangeOption(SETTLEMENT_NUMBER).getValue();
    }

    /**
     * Gets the percentage of forests on the map to be created.
     * 
     * @return The percentage of forests.
     */
    public int getPercentageOfForests() {
        return Specification.getSpecification().getRangeOption(FOREST_NUMBER).getValue();
    }

    /**
     * Gets the percentage of bonus tiles on the map to be created.
     * 
     * @return The percentage of bonus tiles.
     */
    public int getPercentageOfBonusTiles() {
        return Specification.getSpecification().getRangeOption(BONUS_NUMBER).getValue();
    }

    /**
     * Gets the width of "short sea" of the map to be created.
     * 
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
     * Gets the maximum allowed distance between edge and possible highSea tile.
     * 
     * @return The maximum distance to edge.
     */
    public int getMaxDistToEdge() {
        final int size = getInteger(MAP_SIZE);
        switch (size) {
        case MAP_SIZE_SMALL:
            return 7;
        case MAP_SIZE_MEDIUM:
            return 10;
        case MAP_SIZE_LARGE:
            return 12;
        case MAP_SIZE_VERY_LARGE:
            return 15;
        case MAP_SIZE_HUGE:
            return 20;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }

    /**
     * Gets the preferred distance to edge of the map to be created.
     * This should always be higher than the value returned by #getDistLandHighSea(),
     * to avoid having non-highSea on the map edge.      
     *     
     * @return The preferred distance to edge.
     */
    public int getPrefDistToEdge() {
        final int size = getInteger(MAP_SIZE);
        switch (size) {
        case MAP_SIZE_SMALL:
            return 5;
        case MAP_SIZE_MEDIUM:
            return 5;
        case MAP_SIZE_LARGE:
            return 5;
        case MAP_SIZE_VERY_LARGE:
            return 5;
        case MAP_SIZE_HUGE:
            return 5;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }

    /**
     * Gets the average humidity of the map.
     * 
     * @return The humidity.
     */
    public int getHumidity() {
        return Specification.getSpecification().getRangeOption(HUMIDITY).getValue();
    }

    /**
     * Gets the average temperature of the map.
     * 
     * @return The temperature.
     */
    public int getTemperature() {
        return Specification.getSpecification().getRangeOption(TEMPERATURE).getValue();
    }

    protected boolean isCorrectTagName(String tagName) {
        return getXMLElementTagName().equals(tagName);
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "mapGeneratorOptions".
     */
    public static String getXMLElementTagName() {
        return "mapGeneratorOptions";
    }

}
