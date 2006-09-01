
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
* {@link #addDefaultOptions()} and each option should be given an unique
* identifier (defined as a constant in this class).
*/
public class ClientOptions extends OptionMap {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    
    public static final String GROUP_GUI = "gui";

    /**
     * Selected tiles always gets centered if this option is
     * enabled (even if the tile is {@link net.sf.freecol.client.gui.GUI#onScreen(Map.Position)}).
     * 
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String ALWAYS_CENTER = "alwaysCenter";
    
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
    public static final String SHOW_WARNING = "guiShowWarning";
    public static final String SHOW_GOVERNMENT_EFFICIENCY = "guiShowGovernmentEfficiency";
    public static final String SHOW_WAREHOUSE_CAPACITY = "guiShowWarehouseCapacity";
    public static final String SHOW_UNIT_IMPROVED = "guiShowUnitImproved";
    public static final String SHOW_UNIT_DEMOTED = "guiShowUnitDemoted";
    public static final String SHOW_UNIT_ADDED = "guiShowUnitAdded";
    public static final String SHOW_UNIT_LOST = "guiShowUnitLost";
    public static final String SHOW_BUILDING_COMPLETED = "guiShowBuildingCompleted";
    public static final String SHOW_FOREIGN_DIPLOMACY = "guiShowForeignDiplomacy";
    public static final String SHOW_MARKET_PRICES = "guiShowMarketPrices";
    public static final String SHOW_LOST_CITY_RUMOURS = "guiShowLostCityRumours";
    public static final String SHOW_MISSING_GOODS = "guiShowMissingGoods";
    public static final String SHOW_COLONY_WARNINGS = "guiShowColonyWarnings";

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
        guiGroup.add(new BooleanOption(ALWAYS_CENTER, "clientOptions.gui."+ ALWAYS_CENTER +".name", "clientOptions.gui."+ ALWAYS_CENTER +".shortDescription", false));        
        add(guiGroup);

        OptionGroup messagesGroup = new OptionGroup("clientOptions.messages.name", "clientOptions.messages.shortDescription");
        messagesGroup.add(new SelectOption(MESSAGES_GROUP_BY,
                                      "clientOptions.gui." + MESSAGES_GROUP_BY + ".name", 
                                      "clientOptions.gui." + MESSAGES_GROUP_BY + ".shortDescription", 
                                      new String[] {"clientOptions.gui." + MESSAGES_GROUP_BY + ".nothing",
                                                    "clientOptions.gui." + MESSAGES_GROUP_BY + ".type",
                                                    "clientOptions.gui." + MESSAGES_GROUP_BY + ".source"},
                                      0));

        /** Boolean message display options. */
        messagesGroup.add(new BooleanOption(SHOW_WARNING,
                                       "clientOptions.gui."+ SHOW_WARNING +".name",
                                       "clientOptions.gui."+ SHOW_WARNING +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_SONS_OF_LIBERTY,
                                       "clientOptions.gui."+ SHOW_SONS_OF_LIBERTY +".name",
                                       "clientOptions.gui."+ SHOW_SONS_OF_LIBERTY +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_GOVERNMENT_EFFICIENCY,
                                       "clientOptions.gui."+ SHOW_GOVERNMENT_EFFICIENCY +".name",
                                       "clientOptions.gui."+ SHOW_GOVERNMENT_EFFICIENCY +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_WAREHOUSE_CAPACITY,
                                       "clientOptions.gui."+ SHOW_WAREHOUSE_CAPACITY +".name",
                                       "clientOptions.gui."+ SHOW_WAREHOUSE_CAPACITY +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_UNIT_IMPROVED,
                                       "clientOptions.gui."+ SHOW_UNIT_IMPROVED +".name",
                                       "clientOptions.gui."+ SHOW_UNIT_IMPROVED +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_UNIT_DEMOTED,
                                       "clientOptions.gui."+ SHOW_UNIT_DEMOTED +".name",
                                       "clientOptions.gui."+ SHOW_UNIT_DEMOTED +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_UNIT_ADDED,
                                       "clientOptions.gui."+ SHOW_UNIT_ADDED +".name",
                                       "clientOptions.gui."+ SHOW_UNIT_ADDED +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_UNIT_LOST,
                                       "clientOptions.gui."+ SHOW_UNIT_LOST +".name",
                                       "clientOptions.gui."+ SHOW_UNIT_LOST +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_BUILDING_COMPLETED,
                                       "clientOptions.gui."+ SHOW_BUILDING_COMPLETED +".name",
                                       "clientOptions.gui."+ SHOW_BUILDING_COMPLETED +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_FOREIGN_DIPLOMACY,
                                       "clientOptions.gui."+ SHOW_FOREIGN_DIPLOMACY +".name",
                                       "clientOptions.gui."+ SHOW_FOREIGN_DIPLOMACY +".shortDescription", 
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_MARKET_PRICES,
                                       "clientOptions.gui."+ SHOW_MARKET_PRICES +".name",
                                       "clientOptions.gui."+ SHOW_MARKET_PRICES +".shortDescription", 
                                       false));
        messagesGroup.add(new BooleanOption(SHOW_MISSING_GOODS,
                                       "clientOptions.gui." + SHOW_MISSING_GOODS + ".name",
                                       "clientOptions.gui." + SHOW_MISSING_GOODS + ".shortDescription",
                                       true));
        messagesGroup.add(new BooleanOption(SHOW_COLONY_WARNINGS,
                                       "clientOptions.gui." + SHOW_COLONY_WARNINGS + ".name",
                                       "clientOptions.gui." + SHOW_COLONY_WARNINGS + ".shortDescription",
                                       true));
        add(messagesGroup);
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "clientOptions".
    */
    public static String getXMLElementTagName() {
        return "clientOptions";
    }

}
