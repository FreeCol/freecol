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
    }

    /**
     * Gets the width of the map to be created.
     * @return The width of the map.
     */
    public int getWidth() {
        final int size = getInteger(MAP_SIZE);
        switch (size) {
        case MAP_SIZE_SMALL:
            return 30;
        case MAP_SIZE_MEDIUM:
            return 45;
        case MAP_SIZE_LARGE:
            return 60;
        case MAP_SIZE_VERY_LARGE:
            return 90;
        case MAP_SIZE_HUGE:
            return 120;
            //return 240;
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
            return 64;
        case MAP_SIZE_MEDIUM:
            return 96;
        case MAP_SIZE_LARGE:
            return 128;
        case MAP_SIZE_VERY_LARGE:
            return 192;
        case MAP_SIZE_HUGE:
            return 256;
            //return 512;
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
        }
    }

    /**
     * Gets the percentage of land of the map to be created.
     * @return The percentage of land.
     */
    public int getLandMass() {
        final int size = getInteger(MAP_SIZE);
        switch (size) {
        case MAP_SIZE_SMALL:
            return 25; // 25
        case MAP_SIZE_MEDIUM:
            return 25; // 45
        case MAP_SIZE_LARGE:
            return 25; // 50
        case MAP_SIZE_VERY_LARGE:
            return 25; // 65
        case MAP_SIZE_HUGE:
            return 25; // 70
        default:
            throw new IllegalStateException("Invalid map-size: " + size + ".");
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
