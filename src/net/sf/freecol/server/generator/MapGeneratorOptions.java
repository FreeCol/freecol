package net.sf.freecol.server.generator;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.option.OptionMap;
import net.sf.freecol.common.option.SelectOption;

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

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

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
     * Creates a new <code>MapGeneratorOptions</code>.
     */
    public MapGeneratorOptions() {
        super(getXMLElementTagName(), "mapGeneratorOptions.name", "mapGeneratorOptions.shortDescription");
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
        super(element, getXMLElementTagName(), "mapGeneratorOptions.name", "mapGeneratorOptions.shortDescription");
    }

    /**
     * Creates a <code>MapGeneratorOptions</code> from an XML representation.
     *
     * <br><br>
     *
     * @param in The XML stream to read the data from.
     * @throws XMLStreamException if an error occured during parsing.          
     */
    public MapGeneratorOptions(XMLStreamReader in) throws XMLStreamException {
         super(in, getXMLElementTagName(), "mapGeneratorOptions.name", "mapGeneratorOptions.shortDescription");
    }



    /**
     * Adds the options to this <code>MapGeneratorOptions</code>.
     */
    protected void addDefaultOptions() {
        /* Add options here: */
        add(new SelectOption(MAP_SIZE,
                "mapGeneratorOptions." + MAP_SIZE + ".name", 
                "mapGeneratorOptions." + MAP_SIZE + ".shortDescription", 
                new String[] {"small", "medium", "large", "veryLarge", "huge"},
                0)
        );
        add(new SelectOption(LAND_MASS,
                "mapGeneratorOptions." + LAND_MASS + ".name", 
                "mapGeneratorOptions." + LAND_MASS + ".shortDescription", 
                new String[] {"small", "medium", "large", "veryLarge", "huge"},
                1)
        );
        add(new SelectOption(RIVER_NUMBER,
                "mapGeneratorOptions." + RIVER_NUMBER + ".name", 
                "mapGeneratorOptions." + RIVER_NUMBER + ".shortDescription", 
                new String[] {"small", "medium", "large", "veryLarge", "huge"},
                1)
        );
        add(new SelectOption(MOUNTAIN_NUMBER,
                "mapGeneratorOptions." + MOUNTAIN_NUMBER + ".name", 
                "mapGeneratorOptions." + MOUNTAIN_NUMBER + ".shortDescription", 
                new String[] {"small", "medium", "large", "veryLarge", "huge"},
                1)
        );
        add(new SelectOption(RUMOUR_NUMBER,
                "mapGeneratorOptions." + RUMOUR_NUMBER + ".name", 
                "mapGeneratorOptions." + RUMOUR_NUMBER + ".shortDescription", 
                new String[] {"small", "medium", "large", "veryLarge", "huge"},
                1)
        );
        add(new SelectOption(FOREST_NUMBER,
                "mapGeneratorOptions." + FOREST_NUMBER + ".name", 
                "mapGeneratorOptions." + FOREST_NUMBER + ".shortDescription", 
                new String[] {"small", "medium", "large", "veryLarge", "huge"},
                2)
        );
        add(new SelectOption(BONUS_NUMBER,
                "mapGeneratorOptions." + BONUS_NUMBER + ".name", 
                "mapGeneratorOptions." + BONUS_NUMBER + ".shortDescription", 
                new String[] {"small", "medium", "large", "veryLarge", "huge"},
                1)
        );
    }

    /**
     * Gets the width of the map to be created.
     * @return The width of the map.
     */
    public int getWidth() {
        final int size = getInteger(MAP_SIZE);
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
        final int size = getInteger(MAP_SIZE);
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
        final int landMass = getInteger(LAND_MASS);
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
        final int number = getInteger(RIVER_NUMBER);
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
        final int number = getInteger(MOUNTAIN_NUMBER);
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
     * @return The number of rumours..
     */
    public int getNumberOfRumours() {
        final int number = getInteger(RUMOUR_NUMBER);
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
     * Gets the percentage of forests on the map to be created.
     * @return The percentage of forests.
     */
    public int getPercentageOfForests() {
        final int number = getInteger(FOREST_NUMBER);
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
        final int number = getInteger(BONUS_NUMBER);
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
     * Gets the prefered distance to edge of the map to be created.
     * @return The prefered distance to edge.
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
     * Gets the tag name of the root element representing this object.
     * @return "mapGeneratorOptions".
     */
    public static String getXMLElementTagName() {
        return "mapGeneratorOptions";
    }

}
