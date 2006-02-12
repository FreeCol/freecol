
package net.sf.freecol.client;


import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.OptionMap;
import net.sf.freecol.common.option.SelectOption;

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
     * Used by GUI, this defines the grouping of ModelMessages.
     * Possible values include nothing, type and source.
     *
     * @see net.sf.freecol.client.gui.GUI
     * @see net.sf.freecol.common.model.ModelMessage
     */
    public static final String MESSAGES_GROUP_BY = "guiMessagesGroupBy";

    /**
     * Used by GUI, this defines whether SoL messages will be displayed.
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String SHOW_SONS_OF_LIBERTY = "guiShowSonsOfLiberty";

    public static final String SHOW_GOVERNMENT_EFFICIENCY = "guiShowGovernmentEfficiency";
    public static final String SHOW_WAREHOUSE_CAPACITY = "guiShowWarehouseCapacity";
    public static final String SHOW_UNIT_IMPROVEMENT = "guiShowUnitImprovement";
    public static final String SHOW_UNIT_PROMOTION = "guiShowUnitPromotion";
    public static final String SHOW_UNIT_DEMOTION = "guiShowUnitDemotion";
    public static final String SHOW_BUILDING_COMPLETION = "guiShowBuildingCompletion";
    public static final String SHOW_NEW_COLONIST = "guiShowNewColonist";
    public static final String SHOW_FOREIGN_DIPLOMACY = "guiShowForeignDiplomacy";


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

        guiGroup.add(new SelectOption(MESSAGES_GROUP_BY,
                                      "clientOptions.gui." + MESSAGES_GROUP_BY + ".name", 
                                      "clientOptions.gui." + MESSAGES_GROUP_BY + ".shortDescription", 
                                      new String[] {"clientOptions.gui." + MESSAGES_GROUP_BY + ".nothing",
                                                    "clientOptions.gui." + MESSAGES_GROUP_BY + ".type",
                                                    "clientOptions.gui." + MESSAGES_GROUP_BY + ".source"},
                                      0));

        /** Boolean message display options. */
        guiGroup.add(new BooleanOption(SHOW_SONS_OF_LIBERTY,
                                       "clientOptions.gui."+ SHOW_SONS_OF_LIBERTY +".name",
                                       "clientOptions.gui."+ SHOW_SONS_OF_LIBERTY +".shortDescription", 
                                       true));
        guiGroup.add(new BooleanOption(SHOW_GOVERNMENT_EFFICIENCY,
                                       "clientOptions.gui."+ SHOW_GOVERNMENT_EFFICIENCY +".name",
                                       "clientOptions.gui."+ SHOW_GOVERNMENT_EFFICIENCY +".shortDescription", 
                                       true));
        guiGroup.add(new BooleanOption(SHOW_WAREHOUSE_CAPACITY,
                                       "clientOptions.gui."+ SHOW_WAREHOUSE_CAPACITY +".name",
                                       "clientOptions.gui."+ SHOW_WAREHOUSE_CAPACITY +".shortDescription", 
                                       true));
        guiGroup.add(new BooleanOption(SHOW_UNIT_IMPROVEMENT,
                                       "clientOptions.gui."+ SHOW_UNIT_IMPROVEMENT +".name",
                                       "clientOptions.gui."+ SHOW_UNIT_IMPROVEMENT +".shortDescription", 
                                       true));
        guiGroup.add(new BooleanOption(SHOW_UNIT_PROMOTION,
                                       "clientOptions.gui."+ SHOW_UNIT_PROMOTION +".name",
                                       "clientOptions.gui."+ SHOW_UNIT_PROMOTION +".shortDescription", 
                                       true));
        guiGroup.add(new BooleanOption(SHOW_UNIT_DEMOTION,
                                       "clientOptions.gui."+ SHOW_UNIT_DEMOTION +".name",
                                       "clientOptions.gui."+ SHOW_UNIT_DEMOTION +".shortDescription", 
                                       true));
        guiGroup.add(new BooleanOption(SHOW_BUILDING_COMPLETION,
                                       "clientOptions.gui."+ SHOW_BUILDING_COMPLETION +".name",
                                       "clientOptions.gui."+ SHOW_BUILDING_COMPLETION +".shortDescription", 
                                       true));
        guiGroup.add(new BooleanOption(SHOW_NEW_COLONIST,
                                       "clientOptions.gui."+ SHOW_NEW_COLONIST +".name",
                                       "clientOptions.gui."+ SHOW_NEW_COLONIST +".shortDescription", 
                                       true));
        guiGroup.add(new BooleanOption(SHOW_FOREIGN_DIPLOMACY,
                                       "clientOptions.gui."+ SHOW_FOREIGN_DIPLOMACY +".name",
                                       "clientOptions.gui."+ SHOW_FOREIGN_DIPLOMACY +".shortDescription", 
                                       true));
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
