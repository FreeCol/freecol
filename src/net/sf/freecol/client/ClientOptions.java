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

package net.sf.freecol.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColModFile.ModInfo;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.ListOption;
import net.sf.freecol.common.option.ListOptionSelector;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.SelectOption;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * Defines how available client options are displayed on the Setting dialog from
 * File>Preferences Also contains several Comparators used for display purposes.
 *
 * <br>
 * <br>
 *
 * Most available client options and their default values are defined
 * in the file <code>base/client-options.xml</code> in the FreeCol
 * data directory. They are overridden by the player's personal
 * settings in the file <code>options.xml</code> in the user
 * directory. Options that can not be defined in this way because they
 * are generated dynamically (such as a list of available mods) must
 * be added to {@link #addDefaultOptions()}.
 *
 * Each option should be given an unique identifier (defined as a
 * constant in this class). In general, the options are called
 * something like "model.option.UNIQUENAME". Since the options must
 * also be represented by the GUI, they following two keys must be
 * added to the file <code>FreeColMessages.properties</code>:
 *
 * <ul><li>model.option.UNIQUENAME.name</li>
 * <li>model.option.UNIQUENAME.shortDescription</li></ul>
 */
public class ClientOptions extends OptionGroup {

    private static final Logger logger = Logger.getLogger(ClientOptions.class.getName());

    /**
     * Option for setting the language.
     */
    public static final String LANGUAGE = "model.option.languageOption";

    /**
     * If this option is enabled, the display will recenter in order to display
     * the active unit if it is not
     * {@link net.sf.freecol.client.gui.GUI#onScreen(Tile)}).
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String JUMP_TO_ACTIVE_UNIT = "model.option.jumpToActiveUnit";

    /**
     * Selected tiles always gets centered if this option is enabled (even if
     * the tile is {@link net.sf.freecol.client.gui.GUI#onScreen(Tile)}).
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String ALWAYS_CENTER = "model.option.alwaysCenter";

    /**
     * Used by GUI, this is the minimum number of goods a colony must possess for
     * the goods to show up at the bottom of the colony panel.
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String MIN_NUMBER_FOR_DISPLAYING_GOODS = "model.option.guiMinNumberToDisplayGoods";

    /**
     * Used by GUI, the number will be displayed when a group of goods are
     * higher than this number.
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT = "model.option.guiMinNumberToDisplayGoodsCount";

    /**
     * Used by GUI, this is the most repetitions drawn of a goods image for a
     * single goods grouping.
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String MAX_NUMBER_OF_GOODS_IMAGES = "model.option.guiMaxNumberOfGoodsImages";

    /**
     * Whether to display a compass rose or not.
     */
    public static final String DISPLAY_COMPASS_ROSE = "model.option.displayCompassRose";

    /**
     * Whether to display the map controls or not.
     */
    public static final String DISPLAY_MAP_CONTROLS = "model.option.displayMapControls";

    /**
     * Whether to display the grid by default or not.
     */
    public static final String DISPLAY_GRID = "model.option.displayGrid";

    /**
     * Whether to delay on a unit's last move or not.
     *
     * TODO: Add this option's name and short description to languages other than English.
     */
    public static final String UNIT_LAST_MOVE_DELAY = "model.option.unitLastMoveDelay";

    /**
     * Whether to display borders by default or not.
     */
    public static final String DISPLAY_BORDERS = "model.option.displayBorders";

    /**
     * What text to display in the tiles.
     */
    public static final String DISPLAY_TILE_TEXT = "model.option.displayTileText";

    public static final int DISPLAY_TILE_TEXT_EMPTY = 0, DISPLAY_TILE_TEXT_NAMES = 1,
        DISPLAY_TILE_TEXT_OWNERS = 2, DISPLAY_TILE_TEXT_REGIONS = 3;

    /**
     * Animation speed for friendly units.
     */
    public static final String MOVE_ANIMATION_SPEED = "model.option.moveAnimationSpeed";

    /**
     * Animation speed for enemy units.
     */
    public static final String ENEMY_MOVE_ANIMATION_SPEED = "model.option.enemyMoveAnimationSpeed";

    /**
     * Used by GUI, this defines the grouping of ModelMessages. Possible values
     * include nothing, type and source.
     *
     * @see net.sf.freecol.client.gui.GUI
     * @see net.sf.freecol.common.model.ModelMessage
     */
    public static final String MESSAGES_GROUP_BY = "model.option.guiMessagesGroupBy";

    public static final int MESSAGES_GROUP_BY_NOTHING = 0;

    public static final int MESSAGES_GROUP_BY_TYPE = 1;

    public static final int MESSAGES_GROUP_BY_SOURCE = 2;

    public static final String AUDIO_MIXER = "model.option.audioMixer";

    public static final String AUDIO_VOLUME = "model.option.audioVolume";

    /**
     * Used by GUI, this defines whether SoL messages will be displayed.
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String SHOW_COLONY_WARNINGS = "model.option.guiShowColonyWarnings";

    public static final String SHOW_PRECOMBAT = "model.option.guiShowPreCombat";

    public static final String SHOW_NOT_BEST_TILE = "model.option.guiShowNotBestTile";

    public static final String SHOW_GOODS_MOVEMENT = "model.option.guiShowGoodsMovement";

    /**
     * Use default values for savegames instead of displaying a dialog. <br>
     * <br>
     * Possible values for this option are:
     * <ol>
     * <li>{@link #SHOW_SAVEGAME_SETTINGS_NEVER}</li>
     * <li>{@link #SHOW_SAVEGAME_SETTINGS_MULTIPLAYER}</li>
     * <li>{@link #SHOW_SAVEGAME_SETTINGS_ALWAYS}</li>
     * </ol>
     */
    public static final String SHOW_SAVEGAME_SETTINGS = "model.option.showSavegameSettings";

    /**
     * A possible value for the {@link SelectOption}:
     * {@link #SHOW_SAVEGAME_SETTINGS}. Specifies that the dialog should never
     * be enabled.
     */
    public static final int SHOW_SAVEGAME_SETTINGS_NEVER = 0;

    /**
     * A possible value for the {@link SelectOption}:
     * {@link #SHOW_SAVEGAME_SETTINGS}. Specifies that the dialog should only
     * be enabled when loading savegames being marked as multiplayer..
     */
    public static final int SHOW_SAVEGAME_SETTINGS_MULTIPLAYER = 1;

    /**
     * A possible value for the {@link SelectOption}:
     * {@link #SHOW_SAVEGAME_SETTINGS}. Specifies that the dialog should always
     * be enabled.
     */
    public static final int SHOW_SAVEGAME_SETTINGS_ALWAYS = 2;

    /**
     * Option for setting the period of autosaves. The value 0 signals that
     * autosaving is disabled.
     */
    public static final String AUTOSAVE_PERIOD = "model.option.autosavePeriod";

    /**
     * Option for setting the number of autosaves to keep. If set to 0, all
     * autosaves are kept.
     */
    public static final String AUTOSAVE_GENERATIONS = "model.option.autosaveGenerations";



    /**
     * Option for deleting autosaves when a new game is started. If set to
     * true, old autosaves will be deleted if a new game is started.
     */
    public static final String AUTOSAVE_DELETE = "model.option.autosaveDelete";

    /**
     * Option for activating autoscroll when dragging units on the mapboard.
     */
    public static final String MAP_SCROLL_ON_DRAG = "model.option.mapScrollOnDrag";

	/**
     * Option for activating autoscroll when dragging units on the mapboard.
     */
    public static final String AUTO_SCROLL = "model.option.autoScroll";


    /**
     * Option for autoload emigrants on saling to america.
     */
    public static final String AUTOLOAD_EMIGRANTS = "model.option.autoloadEmigrants";

    /**
     * If selected: Enables smooth rendering of the minimap when zoomed out.
     */
    public static final String SMOOTH_MINIMAP_RENDERING = "model.option.smoothRendering";

    /**
     * Default zoom level of the minimap.
     */
    public static final String DEFAULT_MINIMAP_ZOOM = "model.option.defaultZoomLevel";

    /**
     * The color to fill in around the actual map on the minimap. Typically only
     * visible when the minimap is at full zoom-out, but at the default 'black'
     * you can't differentiate between the background and the (unexplored) map.
     * Actually: clientOptions.minimap.color.background
     */
    public static final String MINIMAP_BACKGROUND_COLOR = "model.option.color.background";

    public static final String USER_MODS ="userMods";

    /**
     * The Stock the custom house should keep when selling goods.
     */
    public static final String CUSTOM_STOCK = "model.option.customStock";

    /**
     * Generate warning of stock drops below this percentage of capacity.
     */
    public static final String LOW_LEVEL = "model.option.lowLevel";

    /**
     * Generate warning of stock exceeds this percentage of capacity.
     */
    public static final String HIGH_LEVEL = "model.option.highLevel";

    /**
     * Used by GUI to sort colonies.
     */
    public static final String COLONY_COMPARATOR = "model.option.colonyComparator";

    public static final int COLONY_COMPARATOR_NAME = 0, COLONY_COMPARATOR_AGE = 1, COLONY_COMPARATOR_POSITION = 2,
            COLONY_COMPARATOR_SIZE = 3, COLONY_COMPARATOR_SOL = 4;

    /**
     * If enabled: Automatically ends the turn when no units can be made active.
     */
    public static final String AUTO_END_TURN = "model.option.autoEndTurn";

    /**
     * The type of labour report to display.
     */
    public static final String LABOUR_REPORT = "model.option.labourReport";
    public static final int LABOUR_REPORT_CLASSIC = 0;
    public static final int LABOUR_REPORT_COMPACT = 1;

    /**
     * The Indian demand action.
     */
    public static final String INDIAN_DEMAND_RESPONSE = "model.option.indianDemandResponse";
    public static final int INDIAN_DEMAND_RESPONSE_ASK = 0;
    public static final int INDIAN_DEMAND_RESPONSE_ACCEPT = 1;
    public static final int INDIAN_DEMAND_RESPONSE_REJECT = 2;

    /**
     * The warehouse overflow on unload action.
     */
    public static final String UNLOAD_OVERFLOW_RESPONSE = "model.option.unloadOverflowResponse";
    public static final int UNLOAD_OVERFLOW_RESPONSE_ASK = 0;
    public static final int UNLOAD_OVERFLOW_RESPONSE_NEVER = 1;
    public static final int UNLOAD_OVERFLOW_RESPONSE_ALWAYS = 2;

    /**
     * Style of colony labels.
     */
    public static final String COLONY_LABELS = "model.option.displayColonyLabels";
    public static final int COLONY_LABELS_NONE = 0;
    public static final int COLONY_LABELS_CLASSIC = 1;
    public static final int COLONY_LABELS_MODERN = 2;


    /**
     * Comparators for sorting colonies.
     */
    private static Comparator<Colony> colonyAgeComparator = new Comparator<Colony>() {
        public int compare(Colony s1, Colony s2) {
            if (s1.getEstablished().getNumber() > 0
                && s2.getEstablished().getNumber() > 0) {
                return s1.getEstablished().getNumber() - s2.getEstablished().getNumber();
            } else {
                // TODO: remove 0.9.x compatibility code
                // ID should indicate age
                return s1.getIntegerID().compareTo(s2.getIntegerID());
            }
        }
    };

    private static Comparator<Colony> colonyNameComparator = new Comparator<Colony>() {
        public int compare(Colony s1, Colony s2) {
            return s1.getName().compareTo(s2.getName());
        }
    };

    private static Comparator<Colony> colonySizeComparator = new Comparator<Colony>() {
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

    private static Comparator<Colony> colonySoLComparator = new Comparator<Colony>() {
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

    private static Comparator<Colony> colonyPositionComparator = new Comparator<Colony>() {
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

    private Comparator<ModelMessage> messageSourceComparator = new Comparator<ModelMessage>() {
        // sort according to message source
        public int compare(ModelMessage message1, ModelMessage message2) {
            String sourceId1 = message1.getSourceId();
            String sourceId2 = message2.getSourceId();
            if (sourceId1 == sourceId2) {
                return messageTypeComparator.compare(message1, message2);
            }
            Game game = FreeCol.getFreeColClient().getGame();
            FreeColGameObject source1 = game.getMessageSource(message1);
            FreeColGameObject source2 = game.getMessageSource(message2);
            int base = getClassIndex(source1) - getClassIndex(source2);
            if (base == 0) {
                if (source1 instanceof Colony) {
                    return getColonyComparator().compare((Colony) source1, (Colony) source2);
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

    private Comparator<ModelMessage> messageTypeComparator = new Comparator<ModelMessage>() {
        // sort according to message type
        public int compare(ModelMessage message1, ModelMessage message2) {
            return message1.getMessageType().ordinal() - message2.getMessageType().ordinal();
        }
    };


    /**
     * Creates a new <code>ClientOptions</code>.
     * Unlike other OptionGroup classes, ClientOptions can not supply a
     * specification as it is needed before the specification is available.
     */
    public ClientOptions() {
        super(getXMLElementTagName());
        addDefaultOptions();
    }

    /**
     * Adds the options to this <code>GameOptions</code>.
     */
    protected void addDefaultOptions() {
        load(new File(new File(FreeCol.getDataDirectory(), "base"),
                      "client-options.xml"), false);

        final OptionGroup modsGroup = new OptionGroup("clientOptions.mods");
        final ListOptionSelector<ModInfo> selector = new ListOptionSelector<ModInfo>() {
            private Map<String, ModInfo> mods = null;
            private void init() {
                if (mods == null) {
                    List<FreeColModFile> fcmfs = Mods.getAllMods();
                    mods = new HashMap<String, ModInfo>();
                    for (FreeColModFile f : fcmfs) {
                        ModInfo modInfo = f.getModInfo();
                        mods.put(modInfo.getId(), modInfo);
                    }
                }
            }
            public String getId(ModInfo t) {
                return t.getId();
            }
            public ModInfo getObject(String id) {
                init();
                return mods.get(id);
            }
            public List<ModInfo> getOptions() {
                init();
                return new ArrayList<ModInfo>(mods.values());
            }
            public String toString(ModInfo t) {
                return t.getName();
            }
        };
        new ListOption<ModInfo>(selector, USER_MODS, modsGroup);
        add(modsGroup);
    }

    /**
     * Gets a list of active mods in this ClientOptions.
     *
     * @return A list of active mods.
     */
    public List<FreeColModFile> getActiveMods() {
        final List<FreeColModFile> fcmfs = Mods.getAllMods();
        List<FreeColModFile> active = new ArrayList<FreeColModFile>();
        ListOption<?> options = (ListOption<?>) getOption(ClientOptions.USER_MODS);
        for (Object o : options.getValue()) {
            ModInfo modInfo = (ModInfo) o;
            if (modInfo == null) continue;
            for (FreeColModFile f : fcmfs) {
                if (modInfo.getId().equals(f.getModInfo().getId())) {
                    active.add(f);
                    break;
                }
            }
        }
        return active;
    }

    /**
     * Reads the options from the given file.
     *
     * @param loadFile The <code>File</code> to read the
     *            options from.
     */
    public void load(File loadFile) {
        load(loadFile, true);
    }

    /**
     * Reads the options from the given file.
     *
     * @param loadFile The <code>File</code> to read the
     *            options from.
     * @param update a <code>boolean</code> value
     */
    private void load(File loadFile, boolean update) {
        if (loadFile == null || !loadFile.exists()) {
            logger.warning("Could not find the client options file.");
            return;
        }

        InputStream in = null;
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xsr = null;
        try {
            in = new BufferedInputStream(new FileInputStream(loadFile));
            xsr = xif.createXMLStreamReader(in, "UTF-8");
            xsr.nextTag();
            if (update) {
                logger.finest("Updating " + getId() + " from " + loadFile.getPath());
                updateFromXML(xsr);
            } else {
                logger.finest("Loading " + getId() + " from " + loadFile.getPath());
                readFromXML(xsr);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while loading options.", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception while closing stream.", e);
            }
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    private void updateFromXML(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(OptionGroup.getXMLElementTagName())) {
                updateFromXML(in);
            } else {
                String idStr = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                if (idStr == null) {
                    idStr = in.getLocalName();
                }
                Option o = getOption(idStr);

                if (o != null) {
                    o.readFromXML(in);
                } else {
                    // Normal only if this option is from an old save game:
                    logger.info("Option \"" + idStr + "\" (" + in.getLocalName() + ") could not be found.");

                    // Ignore the option:
                    final String ignoredTag = in.getLocalName();
                    while (in.nextTag() != XMLStreamConstants.END_ELEMENT
                           || !in.getLocalName().equals(ignoredTag));
                }
            }
        }
        // DONE BY while-loop: in.nextTag();
    }


    /**
     * Return the client's preferred tile text type.
     *
     * @return A <code>DISPLAY_TILE_TEXT_</code> value
     */
    public int getDisplayTileText() {
        return getInteger(DISPLAY_TILE_TEXT);
    }

    /**
     * Return the client's preferred comparator for colonies.
     *
     * @return a <code>Comparator</code> value
     */
    public Comparator<Colony> getColonyComparator() {
        return getColonyComparator(getInteger(COLONY_COMPARATOR));
    }

    /**
     * Return the colony comparator identified by type.
     *
     * @param type an <code>int</code> value
     * @return a <code>Comparator</code> value
     */
    public static Comparator<Colony> getColonyComparator(int type) {
        switch (type) {
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
        return (BooleanOption) getOption(message.getMessageType().getOptionName());
    }

    protected boolean isCorrectTagName(String tagName) {
        return getXMLElementTagName().equals(tagName);
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "clientOptions".
     */
    public static String getXMLElementTagName() {
        return "clientOptions";
    }

}
