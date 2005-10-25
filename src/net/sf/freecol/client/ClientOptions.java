
package net.sf.freecol.client;

import java.util.logging.Logger;

import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.OptionMap;

import org.w3c.dom.Element;


/**
* Keeps track of the available client options.
*
* <br><br>
*
* New options should be added to
* {@link #addDefaultOptions} and each option should be given an unique
* identifier (defined as a constant in this class).
*/
public class ClientOptions extends OptionMap {
    private static Logger logger = Logger.getLogger(ClientOptions.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    
    public static final String GROUP_GUI = "gui";
    
    /**
     * Used by GUI, the number will be displayed when a group of goods are higher than this number.
     * 
     * @see GUI
     */
    public static final String MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT = "guiMinNumberToDisplayGoodsCount";
    /**
     * Used by GUI, this is the most repetitions drawn of a goods image for a single goods grouping.
     * 
     * @see GUI
     */
    public static final String MAX_NUMBER_OF_GOODS_IMAGES = "guiMaxNumberOfGoodsImages"; 
    /**
     * Used by GUI, this is how many goods icons should be drawn when the actual count is over the maximum (other option).
     * 
     * @see GUI
     */
    public static final String DISPLAY_GOODS_IMAGES_WHEN_OVER_MAX_GOODS = "guiDisplayGoodsImagesWhenOverMaxGoods"; 
    
    
    /** This is an example key: */
    //public static final String EXAMPLE = "example";



    /**
    * Creates a new <code>ClientOptions</code>.
    */
    public ClientOptions() {
        super(getXMLElementTagName(), "clientOptions.name", "clientOptions.shortDescription");
    }


    /**
    * Creates a <code>ClientOptions</code> from an XML representation.
    *
    * <br><br>
    *
    * @param element The XML <code>Element</code> from which this object
    *                should be constructed.
    */
    public ClientOptions(Element element) {
        super(element, getXMLElementTagName(), "clientOptions.name", "clientOptions.shortDescription");
    }




    /**
    * Adds the options to this <code>GameOptions</code>.
    */
    protected void addDefaultOptions() {
        /* Add options here: */

        /* Example: */
        /*
        OptionGroup exampleGroup = new OptionGroup("gameOptions.exampleGroup.name", "gameOptions.exampleGroup.shortDescription");
        exampleGroup.add(new IntegerOption(EXAMPLE, "gameOptions.example.name", "gameOptions.example.shortDescription", 0, 50000, 0));
        add(exampleGroup);
        */
        
        // TODO FIXME  Add these to the GUI interface - and to the properties file.
//        ClientOptions exampleGroup = new ClientOptions();   //"gameOptions."+ GROUP_GUI +".name", "gameOptions."+ GROUP_GUI +".shortDescription");
        this.add(new IntegerOption(MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT, "gameOptions."+ MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT +".name", "gameOptions."+ MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT +".shortDescription", 0, 20, 7));
        this.add(new IntegerOption(MAX_NUMBER_OF_GOODS_IMAGES, "gameOptions."+ MAX_NUMBER_OF_GOODS_IMAGES +".name", "gameOptions."+ MAX_NUMBER_OF_GOODS_IMAGES +".shortDescription", 1, 20, 10));
        this.add(new IntegerOption(DISPLAY_GOODS_IMAGES_WHEN_OVER_MAX_GOODS, "gameOptions."+ DISPLAY_GOODS_IMAGES_WHEN_OVER_MAX_GOODS +".name", "gameOptions."+ DISPLAY_GOODS_IMAGES_WHEN_OVER_MAX_GOODS +".shortDescription", 1, 20, 10));
//        this.add(exampleGroup);

    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "clientOptions".
    */
    public static String getXMLElementTagName() {
        return "clientOptions";
    }

}
