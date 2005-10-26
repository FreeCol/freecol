
package net.sf.freecol.client;

import java.util.logging.Logger;

import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.OptionGroup;
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
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT = "guiMinNumberToDisplayGoodsCount";

    /**
     * Used by GUI, this is the most repetitions drawn of a goods image for a single goods grouping.
     * 
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String MAX_NUMBER_OF_GOODS_IMAGES = "guiMaxNumberOfGoodsImages";



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

        OptionGroup guiGroup = new OptionGroup("clientOptions.gui.name", "clientOptions.gui.shortDescription");
        guiGroup.add(new IntegerOption(MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT, "clientOptions.gui."+ MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT +".name", "clientOptions.gui."+ MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT +".shortDescription", 0, 10, 7));
        guiGroup.add(new IntegerOption(MAX_NUMBER_OF_GOODS_IMAGES, "clientOptions.gui."+ MAX_NUMBER_OF_GOODS_IMAGES +".name", "clientOptions.gui."+ MAX_NUMBER_OF_GOODS_IMAGES +".shortDescription", 1, 10, 7));
        add(guiGroup);

    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "clientOptions".
    */
    public static String getXMLElementTagName() {
        return "clientOptions";
    }

}
