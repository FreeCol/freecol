
package net.sf.freecol.client;

import java.util.Comparator;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
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



    private static final String GROUP_GUI = "gui";

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
    public static final int MESSAGES_GROUP_BY_NOTHING = 0;
    public static final int MESSAGES_GROUP_BY_TYPE = 1;
    public static final int MESSAGES_GROUP_BY_SOURCE = 2;

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
    public static final String SHOW_PRECOMBAT = "guiShowPreCombat";
    
    /**
     * Use default values for savegames instead of displaying a dialog.
     * <br><br>
     * Possible values for this option are:
     * <ol>
     *   <li>{@link #SHOW_SAVEGAME_SETTINGS_NEVER}</li>
     *   <li>{@link #SHOW_SAVEGAME_SETTINGS_MULTIPLAYER}</li>
     *   <li>{@link #SHOW_SAVEGAME_SETTINGS_ALWAYS}</li>
     * </ol>
     */
    public static final String SHOW_SAVEGAME_SETTINGS = "showSavegameSettings";
    
    /**
     * A possible value for the {@link SelectOption}: {@link #SHOW_SAVEGAME_SETTINGS}.
     * Specifies that the dialog should never be enabled.
     */
    public static final int SHOW_SAVEGAME_SETTINGS_NEVER = 0;
    
    /**
     * A possible value for the {@link SelectOption}: {@link #SHOW_SAVEGAME_SETTINGS}.
     * Specifies that the dialog should only be enabled when loading savegames being
     * marked as multiplayer..
     */
    public static final int SHOW_SAVEGAME_SETTINGS_MULTIPLAYER = 1;
    
    /**
     * A possible value for the {@link SelectOption}: {@link #SHOW_SAVEGAME_SETTINGS}.
     * Specifies that the dialog should always be enabled.
     */
    public static final int SHOW_SAVEGAME_SETTINGS_ALWAYS = 2;

    /**
     * Option for setting the period of autosaves. The value 0 signals that autosaving
     * is disabled. 
     */
    public static final String AUTOSAVE_PERIOD = "autosavePeriod"; 

    /**
     * Option for setting wether or not the fog of war should be displayed.
     */
    public static final String DISPLAY_FOG_OF_WAR = "displayFogOfWar";
    
    /**
     * Option for activating autoscroll when dragging units on the mapboard.
     */
    public static final String MAP_SCROLL_ON_DRAG = "mapScrollOnDrag";
    
    /** 
     * The Stock the custom house should keep when selling goods.
     */
    public static final String CUSTOM_STOCK = "customStock";

    /**
     * Generate warning of stock drops below this percentage of capacity.
     */
    public static final String LOW_LEVEL = "lowLevel";

    /**
     * Generate warning of stock exceeds this percentage of capacity.
     */
    public static final String HIGH_LEVEL = "highLevel";

    /**
     * Used by GUI to sort colonies.
     */
    public static final String COLONY_COMPARATOR = "colonyComparator";
    public static final int COLONY_COMPARATOR_NAME = 0,
                            COLONY_COMPARATOR_AGE = 1,
                            COLONY_COMPARATOR_POSITION = 2,
                            COLONY_COMPARATOR_SIZE = 3,
                            COLONY_COMPARATOR_SOL = 4;

    /**
     * Comparators for sorting colonies.
     */
    private static Comparator colonyAgeComparator = new Comparator<Colony>() {
        // ID should indicate age
        public int compare(Colony s1, Colony s2) {
            return s1.getIntegerID().compareTo(s2.getIntegerID());
        }
    };

    private static Comparator colonyNameComparator = new Comparator<Colony>() {
        public int compare(Colony s1, Colony s2) {
            return s1.getName().compareTo(s2.getName());
        }
    };

    private static Comparator colonySizeComparator = new Comparator<Colony>() {
        // sort size descending, then SoL descending
        public int compare(Colony s1, Colony s2) {
            int dsize = s2.getUnitCount() - s1.getUnitCount();
            if (dsize == 0) {
                return s2.getSoL() - s1.getSoL();
            } else {
                return dsize;
            }
        }
    };

    private static Comparator colonySoLComparator = new Comparator<Colony>() {
        // sort SoL descending, then size descending
        public int compare(Colony s1, Colony s2) {
            int dsol = s2.getSoL() - s1.getSoL();
            if (dsol == 0) {
                return s2.getUnitCount() - s1.getUnitCount();
            } else {
                return dsol;
            }
        }
    };

    private static Comparator colonyPositionComparator = new Comparator<Colony>() {
        // sort north to south, then west to east
        public int compare(Colony s1, Colony s2) {
            int dy = s1.getTile().getY() - s2.getTile().getY();
            if (dy == 0) {
                return s1.getTile().getX() - s2.getTile().getX();
            } else {
                return dy;
            }
        }
    };


    private Comparator messageSourceComparator = new Comparator<ModelMessage>() {
        // sort according to message source
        public int compare(ModelMessage message1, ModelMessage message2) {
            Object source1 = message1.getSource();
            Object source2 = message2.getSource();
            if (source1 == source2) {
                return messageTypeComparator.compare(message1, message2);
            }
            int base = getClassIndex(source1) - getClassIndex(source2);
            if (base == 0) {
                if (source1 instanceof Colony) {
                    return getColonyComparator().compare(source1, source2);
                } else if (source1 instanceof FreeColGameObject) {
                    return ((FreeColGameObject) source1).getIntegerID() -
                           ((FreeColGameObject) source2).getIntegerID();
                } 
            }
            return base;
        }

        private int getClassIndex(Object object) {
            if (object instanceof Player) {
                return 10;
            } else if (object instanceof Colony) {
                return 20;
            } else if (object instanceof Europe) {
                return 30;
            } else if (object instanceof Unit) {
                return 40;
            } else if (object instanceof FreeColGameObject) {
                return 50;
            } else {
                return 1000;
            }
        }

    }; 


    private Comparator messageTypeComparator = new Comparator<ModelMessage>() {
        // sort according to message type
        public int compare(ModelMessage message1, ModelMessage message2) {
            int dtype = message1.getType() - message2.getType();
            if (dtype == 0 && message1.getSource() != message2.getSource()) {
                return messageSourceComparator.compare(message1, message2);
            } else {
                return dtype;
            }
        }
    };

    
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
        guiGroup.add(new BooleanOption(DISPLAY_FOG_OF_WAR, "clientOptions.gui."+ DISPLAY_FOG_OF_WAR +".name", "clientOptions.gui."+ DISPLAY_FOG_OF_WAR +".shortDescription", false));        
        guiGroup.add(new BooleanOption(MAP_SCROLL_ON_DRAG, "clientOptions.gui."+ MAP_SCROLL_ON_DRAG +".name", "clientOptions.gui."+ MAP_SCROLL_ON_DRAG +".shortDescription", true));
        add(guiGroup);

        guiGroup.add(new SelectOption(COLONY_COMPARATOR,
                                      "clientOptions.gui." + COLONY_COMPARATOR + ".name",
                                      "clientOptions.gui." + COLONY_COMPARATOR + ".shortDescription",
                                      new String[] {"clientOptions.gui." + COLONY_COMPARATOR + ".byName",
                                                    "clientOptions.gui." + COLONY_COMPARATOR + ".byAge",
                                                    "clientOptions.gui." + COLONY_COMPARATOR + ".byPosition",
                                                    "clientOptions.gui." + COLONY_COMPARATOR + ".bySize",
                                                    "clientOptions.gui." + COLONY_COMPARATOR + ".bySoL"},
                                      0));


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
        messagesGroup.add(new BooleanOption(SHOW_PRECOMBAT,
                                       "clientOptions.gui." + SHOW_PRECOMBAT + ".name",
                                       "clientOptions.gui." + SHOW_PRECOMBAT + ".shortDescription",
                                       true));
        add(messagesGroup);
        
        OptionGroup savegamesGroup = new OptionGroup("clientOptions.savegames.name", "clientOptions.savegames.shortDescription");
        savegamesGroup.add(new SelectOption(SHOW_SAVEGAME_SETTINGS,
                "clientOptions.savegames." + SHOW_SAVEGAME_SETTINGS + ".name", 
                "clientOptions.savegames." + SHOW_SAVEGAME_SETTINGS + ".shortDescription", 
                new String[] {"clientOptions.savegames." + SHOW_SAVEGAME_SETTINGS + ".never",
                              "clientOptions.savegames." + SHOW_SAVEGAME_SETTINGS + ".multiplayer",
                              "clientOptions.savegames." + SHOW_SAVEGAME_SETTINGS + ".always"},
                1)
        );
        savegamesGroup.add(new IntegerOption(AUTOSAVE_PERIOD, "clientOptions.savegames."+ AUTOSAVE_PERIOD +".name", "clientOptions.savegames."+ AUTOSAVE_PERIOD +".shortDescription", 0, 100, 0));
        add(savegamesGroup);          

        OptionGroup warehouseGroup = new OptionGroup("clientOptions.warehouse.name",
                                                     "clientOptions.warehouse.shortDescription");
        warehouseGroup.add(new IntegerOption(CUSTOM_STOCK, 
                                             "clientOptions.warehouse." + CUSTOM_STOCK + ".name", 
                                             "clientOptions.warehouse." + CUSTOM_STOCK + ".shortDescription",
                                             0, 300, 50));
        warehouseGroup.add(new IntegerOption(LOW_LEVEL, 
                                             "clientOptions.warehouse." + LOW_LEVEL + ".name", 
                                             "clientOptions.warehouse." + LOW_LEVEL + ".shortDescription",
                                             0, 100, 10));
        warehouseGroup.add(new IntegerOption(HIGH_LEVEL, 
                                             "clientOptions.warehouse." + HIGH_LEVEL + ".name", 
                                             "clientOptions.warehouse." + HIGH_LEVEL + ".shortDescription",
                                             0, 100, 90));
        add(warehouseGroup);

    }


    /**
     * Return the client's preferred comparator for colonies.
     *
     * @return a <code>Comparator</code> value
     */
    public Comparator getColonyComparator() {
        switch(getInteger(COLONY_COMPARATOR)) {
        case COLONY_COMPARATOR_AGE:
            return colonyAgeComparator;
        case COLONY_COMPARATOR_POSITION:
            return colonyPositionComparator;
        case COLONY_COMPARATOR_SIZE:
            return colonySizeComparator;
        case COLONY_COMPARATOR_SOL:
            return colonySoLComparator;
        case COLONY_COMPARATOR_NAME:
            return colonyNameComparator;
        default:
            throw new IllegalStateException("Unknown comparator");
        }
    }


    /**
     * Return the client's preferred comparator for ModelMessages.
     *
     * @return a <code>Comparator</code> value
     */
    public Comparator<ModelMessage> getModelMessageComparator() {
        switch (getInteger(MESSAGES_GROUP_BY)) {
        case MESSAGES_GROUP_BY_SOURCE:
            return messageSourceComparator;
        case MESSAGES_GROUP_BY_TYPE:
            return messageTypeComparator;
        default:
            return null;
        }
    }

    /**
     * Returns the boolean option associated with a ModelMessage.
     *
     * @param message a <code>ModelMessage</code> value
     * @return a <code>BooleanOption</code> value
     */
    public BooleanOption getBooleanOption(ModelMessage message) {
        switch (message.getType()) {
        case ModelMessage.WARNING:
            return (BooleanOption) getObject(ClientOptions.SHOW_WARNING);
        case ModelMessage.SONS_OF_LIBERTY:
            return (BooleanOption) getObject(ClientOptions.SHOW_SONS_OF_LIBERTY);
        case ModelMessage.GOVERNMENT_EFFICIENCY:
            return (BooleanOption) getObject(ClientOptions.SHOW_GOVERNMENT_EFFICIENCY);
        case ModelMessage.WAREHOUSE_CAPACITY:
            return (BooleanOption) getObject(ClientOptions.SHOW_WAREHOUSE_CAPACITY);
        case ModelMessage.UNIT_IMPROVED:
            return (BooleanOption) getObject(ClientOptions.SHOW_UNIT_IMPROVED);
        case ModelMessage.UNIT_DEMOTED:
            return (BooleanOption) getObject(ClientOptions.SHOW_UNIT_DEMOTED);
        case ModelMessage.UNIT_LOST:
            return (BooleanOption) getObject(ClientOptions.SHOW_UNIT_LOST);
        case ModelMessage.UNIT_ADDED:
            return (BooleanOption) getObject(ClientOptions.SHOW_UNIT_ADDED);
        case ModelMessage.BUILDING_COMPLETED:
            return (BooleanOption) getObject(ClientOptions.SHOW_BUILDING_COMPLETED);
        case ModelMessage.FOREIGN_DIPLOMACY:
            return (BooleanOption) getObject(ClientOptions.SHOW_FOREIGN_DIPLOMACY);
        case ModelMessage.MARKET_PRICES:
            return (BooleanOption) getObject(ClientOptions.SHOW_MARKET_PRICES);
        case ModelMessage.MISSING_GOODS:
            return (BooleanOption) getObject(ClientOptions.SHOW_MISSING_GOODS);
        case ModelMessage.DEFAULT:
        default:
            return null;
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "clientOptions".
    */
    public static String getXMLElementTagName() {
        return "clientOptions";
    }

}

