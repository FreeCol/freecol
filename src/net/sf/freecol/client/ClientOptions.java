/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.ModListOption;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.common.option.TextOption;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;


/**
 * Defines how available client options are displayed on the Setting
 * dialog from File/Preferences Also contains several Comparators used
 * for display purposes.
 *
 * Most available client options and their default values are defined
 * in the file {@code base/client-options.xml} in the FreeCol
 * data directory. They are overridden by the player's personal
 * settings in the file {@code options.xml} in the user
 * directory. Note that some options are generated and added dynamically.
 *
 * Each option should be given an unique identifier (defined as a
 * constant in this class). In general, the options are called
 * something like "model.option.UNIQUENAME". Since the options must
 * also be represented by the GUI, they following two keys must be
 * added to the file {@code FreeColMessages.properties}:
 *
 * <ul><li>model.option.UNIQUENAME.name</li>
 * <li>model.option.UNIQUENAME.shortDescription</li></ul>
 */
public class ClientOptions extends OptionGroup {

    private static final Logger logger = Logger.getLogger(ClientOptions.class.getName());

    public static final String TAG = "clientOptions";

    //
    // Constants for each client option.
    // Keep these in sync with data/base/client-options.xml.
    //

    // clientOptions.personal

    /** Option for the player's preferred name. */
    public static final String NAME
        = "model.option.playerName";


    // clientOptions.gui

    public static final String GUI
        = "clientOptions.gui";

    /** Option for setting the language. */
    public static final String LANGUAGE
        = "model.option.languageOption";
    /** Value for automatic language selection. */
    public static final String AUTOMATIC
        = "clientOptions.gui.languageOption.autoDetectLanguage";

    /**
     * Used by GUI, the number will be displayed when a group of goods are
     * higher than this number.
     *
     * @see net.sf.freecol.client.gui.MapViewer
     */
    public static final String MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT
        = "model.option.guiMinNumberToDisplayGoodsCount";

    /**
     * Used by GUI, this is the most repetitions drawn of a goods image for a
     * single goods grouping.
     *
     * @see net.sf.freecol.client.gui.MapViewer
     */
    public static final String MAX_NUMBER_OF_GOODS_IMAGES
        = "model.option.guiMaxNumberOfGoodsImages";

    /**
     * Used by GUI, this is the minimum number of goods a colony must
     * possess for the goods to show up at the bottom of the colony
     * panel.
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String MIN_NUMBER_FOR_DISPLAYING_GOODS
        = "model.option.guiMinNumberToDisplayGoods";

    /**
     * Selected tiles always gets centered if this option is enabled (even if
     * the tile is on screen.
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String ALWAYS_CENTER
        = "model.option.alwaysCenter";

    /**
     * If this option is enabled, the display will recenter in order
     * to display the active unit if it is not on screen.
     *
     * @see net.sf.freecol.client.gui.GUI
     */
    public static final String JUMP_TO_ACTIVE_UNIT
        = "model.option.jumpToActiveUnit";

    /** Option to scroll when dragging units on the mapboard. */
    public static final String MAP_SCROLL_ON_DRAG
        = "model.option.mapScrollOnDrag";

    /** Option to auto-scroll on mouse movement. */
    public static final String AUTO_SCROLL
        = "model.option.autoScroll";

    /** Whether to display a compass rose or not. */
    public static final String DISPLAY_COMPASS_ROSE
        = "model.option.displayCompassRose";

    /** Whether to display the map controls or not. */
    public static final String DISPLAY_MAP_CONTROLS
        = "model.option.displayMapControls";

    /** Whether to display the grid by default or not. */
    public static final String DISPLAY_GRID
        = "model.option.displayGrid";

    /** Whether to display borders by default or not. */
    public static final String DISPLAY_BORDERS
        = "model.option.displayBorders";

    /** Whether to delay on a unit's last move or not. */
    public static final String UNIT_LAST_MOVE_DELAY
        = "model.option.unitLastMoveDelay";

    /** Pixmap setting to work around Java 2D graphics bug. */
    public static final String USE_PIXMAPS
        = "model.option.usePixmaps";

    /** Enable use of openGL pipeline. */
    public static final String USE_OPENGL
        = "model.option.useOpenGL";

    /** Enable use of XRender pipeline (unix specific). */
    public static final String USE_XRENDER
        = "model.option.useXRender";

    /** Whether to remember the positions of various dialogs and panels. */
    public static final String REMEMBER_PANEL_POSITIONS
        = "model.option.rememberPanelPositions";

    /** Whether to remember the sizes of various dialogs and panels. */
    public static final String REMEMBER_PANEL_SIZES
        = "model.option.rememberPanelSizes";

    /** Whether to enable smooth rendering of the minimap when zoomed out. */
    public static final String SMOOTH_MINIMAP_RENDERING
        = "model.option.smoothRendering";

    /** Whether to display end turn grey background or not. */
    public static final String DISABLE_GRAY_LAYER
        = "model.option.disableGrayLayer";

    /** Whether to draw the fog of war on the minimap. */
    public static final String MINIMAP_TOGGLE_FOG_OF_WAR
        = "model.option.miniMapToggleFogOfWar";

    /** Whether to draw the borders on the minimap. */
    public static final String MINIMAP_TOGGLE_BORDERS
        = "model.option.miniMapToggleBorders";

    /** Style of map controls. */
    public static final String MAP_CONTROLS
        = "model.option.mapControls";
    public static final String MAP_CONTROLS_CORNERS
        = "clientOptions.gui.mapControls.CornerMapControls";
    public static final String MAP_CONTROLS_CLASSIC
        = "clientOptions.gui.mapControls.ClassicMapControls";

    /**
     * The color to fill in around the actual map on the
     * minimap.  Typically only visible when the minimap is at full
     * zoom-out, but at the default 'black' you can't differentiate
     * between the background and the (unexplored) map.  Actually:
     * clientOptions.minimap.color.background
     */
    public static final String MINIMAP_BACKGROUND_COLOR
        = "model.option.color.background";

    /** What text to display in the tiles. */
    public static final String DISPLAY_TILE_TEXT
        = "model.option.displayTileText";
    public static final int DISPLAY_TILE_TEXT_EMPTY = 0,
        DISPLAY_TILE_TEXT_NAMES = 1,
        DISPLAY_TILE_TEXT_OWNERS = 2,
        DISPLAY_TILE_TEXT_REGIONS = 3;

    /** Style of colony labels. */
    public static final String COLONY_LABELS
        = "model.option.displayColonyLabels";
    public static final int COLONY_LABELS_NONE = 0;
    public static final int COLONY_LABELS_CLASSIC = 1;
    public static final int COLONY_LABELS_MODERN = 2;

    /** Used by GUI to sort colonies. */
    public static final String COLONY_COMPARATOR
        = "model.option.colonyComparator";
    public static final int COLONY_COMPARATOR_NAME = 0,
        COLONY_COMPARATOR_AGE = 1,
        COLONY_COMPARATOR_POSITION = 2,
        COLONY_COMPARATOR_SIZE = 3,
        COLONY_COMPARATOR_SOL = 4;

    /** Default zoom level of the minimap. */
    public static final String DEFAULT_MINIMAP_ZOOM
        = "model.option.defaultZoomLevel";

    /** Animation speed for our units. */
    public static final String MOVE_ANIMATION_SPEED
        = "model.option.moveAnimationSpeed";

    /** Animation speed for enemy units. */
    public static final String ENEMY_MOVE_ANIMATION_SPEED
        = "model.option.enemyMoveAnimationSpeed";

    /** Animation speed for friendly units. */
    public static final String FRIENDLY_MOVE_ANIMATION_SPEED
        = "model.option.friendlyMoveAnimationSpeed";


    // clientOptions.messages

    public static final String MESSAGES
        = "clientOptions.messages";
    
    /**
     * Used by GUI, this defines the grouping of ModelMessages.
     * Possible values include nothing, type and source.
     *
     * @see net.sf.freecol.client.gui.MapViewer
     * @see net.sf.freecol.common.model.ModelMessage
     */
    public static final String MESSAGES_GROUP_BY
        = "model.option.guiMessagesGroupBy";
    public static final int MESSAGES_GROUP_BY_NOTHING = 0,
        MESSAGES_GROUP_BY_TYPE = 1,
        MESSAGES_GROUP_BY_SOURCE = 2;

    /** Show goods movement messages. */
    public static final String SHOW_GOODS_MOVEMENT
        = "model.option.guiShowGoodsMovement";

    /** Show warnings about colony sites. */
    public static final String SHOW_COLONY_WARNINGS
        = "model.option.guiShowColonyWarnings";

    /** Show the pre-combat dialog? */
    public static final String SHOW_PRECOMBAT
        = "model.option.guiShowPreCombat";

    /** Show warnings about suboptimal colony tile choice. */
    public static final String SHOW_NOT_BEST_TILE
        = "model.option.guiShowNotBestTile";

    public static final String SHOW_REGION_NAMING
        = "model.option.guiShowRegionNaming";

    /** Option for selecting the compact colony report. */
    public static final String COLONY_REPORT
        = "model.option.colonyReport";
    public static final int COLONY_REPORT_CLASSIC = 0;
    public static final int COLONY_REPORT_COMPACT = 1;

    /** The type of labour report to display. */
    public static final String LABOUR_REPORT
        = "model.option.labourReport";
    public static final int LABOUR_REPORT_CLASSIC = 0;
    public static final int LABOUR_REPORT_COMPACT = 1;


    // clientOptions.savegames

    public static final String SAVEGAMES
        = "clientOptions.savegames";
    /** Use default values for savegames instead of displaying a dialog. */
    public static final String SHOW_SAVEGAME_SETTINGS
        = "model.option.showSavegameSettings";
    public static final int SHOW_SAVEGAME_SETTINGS_NEVER = 0,
        SHOW_SAVEGAME_SETTINGS_MULTIPLAYER = 1,
        SHOW_SAVEGAME_SETTINGS_ALWAYS = 2;

    /**
     * Option for setting the period of autosaves. The value 0 signals that
     * autosaving is disabled.
     */
    public static final String AUTOSAVE_PERIOD
        = "model.option.autosavePeriod";

    /**
     * Option for setting the number of days autosaves are keep (valid
     * time).  If set to 0, valid time is not checked.
     */
    public static final String AUTOSAVE_VALIDITY
        = "model.option.autosaveValidity";

    /**
     * Option for deleting autosaves when a new game is started.  If set to
     * true, old autosaves will be deleted if a new game is started.
     */
    public static final String AUTOSAVE_DELETE
        = "model.option.autosaveDelete";

    /**
     * Whether to display confirmation for the overwrite of existing
     * save files.
     */
    public static final String CONFIRM_SAVE_OVERWRITE
        = "model.option.confirmSaveOverwrite";

    /** Prefix for the auto-save file. */
    public static final String AUTO_SAVE_PREFIX
        = "model.option.autoSavePrefix";

    /** Stem of the last-turn save file name. */
    public static final String LAST_TURN_NAME
        = "model.option.lastTurnName";

    /** Stem of the before-last-turn save file name. */
    public static final String BEFORE_LAST_TURN_NAME
        = "model.option.beforeLastTurnName";


    // clientOptions.warehouse

    public static final String WAREHOUSE
        = "clientOptions.warehouse";

    /** The amount of stock the custom house should keep when selling goods. */
    public static final String CUSTOM_STOCK
        = "model.option.customStock";

    /** Generate warning of stock drops below this percentage of capacity. */
    public static final String LOW_LEVEL
        = "model.option.lowLevel";

    /** Generate warning of stock exceeds this percentage of capacity. */
    public static final String HIGH_LEVEL
        = "model.option.highLevel";

    /**
     * Should trade route units check production to determine goods levels at
     * stops along its route?
     */
    public static final String STOCK_ACCOUNTS_FOR_PRODUCTION
        = "model.option.stockAccountsForProduction";


    // clientOptions.audio

    /** Choose the default mixer. */
    public static final String AUDIO_MIXER
        = "model.option.audioMixer";

    /** The volume level to set. */
    public static final String AUDIO_VOLUME
        = "model.option.audioVolume";

    /** Play an alert sound on message arrival. */
    public static final String AUDIO_ALERTS
        = "model.option.audioAlerts";


    // clientOptions.other

    public static final String OTHER
        = "clientOptions.other";

    /** Option to autoload emigrants on sailing to america. */
    public static final String AUTOLOAD_EMIGRANTS
        = "model.option.autoloadEmigrants";

    /** Option to autoload sentried units. */
    public static final String AUTOLOAD_SENTRIES
        = "model.option.autoloadSentries";

    /** Automatically end the turn when no units can be * made active. */
    public static final String AUTO_END_TURN
        = "model.option.autoEndTurn";

    /** Show the end turn dialog. */
    public static final String SHOW_END_TURN_DIALOG
        = "model.option.showEndTurnDialog";

    /** Set the default native demand action. */
    public static final String INDIAN_DEMAND_RESPONSE
        = "model.option.indianDemandResponse";
    public static final int INDIAN_DEMAND_RESPONSE_ASK = 0;
    public static final int INDIAN_DEMAND_RESPONSE_ACCEPT = 1;
    public static final int INDIAN_DEMAND_RESPONSE_REJECT = 2;

    /** Set the default warehouse overflow on unload action. */
    public static final String UNLOAD_OVERFLOW_RESPONSE
        = "model.option.unloadOverflowResponse";
    public static final int UNLOAD_OVERFLOW_RESPONSE_ASK = 0;
    public static final int UNLOAD_OVERFLOW_RESPONSE_NEVER = 1;
    public static final int UNLOAD_OVERFLOW_RESPONSE_ALWAYS = 2;


    // clientOptions.mods

    /** The mods. */
    public static final String USER_MODS
        = "clientOptions.mods.userMods";


    // clientOptions.etc

    /** The dynamic placement options. */
    public static final String ETC
        = "clientOptions.etc";


    // The special keys that are read early.
    private static final List<String> specialKeys
        = makeUnmodifiableList(LANGUAGE, USE_OPENGL, USE_PIXMAPS, USE_XRENDER);

    // Comparators for sorting colonies.
    /** Compare by ascending age. */
    private static final Comparator<Colony> colonyAgeComparator
        = Comparator.comparingInt(c -> c.getEstablished().getNumber());
	
    /** Compare by name, initialized at run time. */
    private static AtomicReference<Comparator<Colony>> colonyNameComparator
        = new AtomicReference<Comparator<Colony>>(null);
    
    /** Compare by descending size then liberty. */
    private static final Comparator<Colony> colonySizeComparator
        = Comparator.comparingInt(Colony::getUnitCount)
            .thenComparingInt(Colony::getSoL)
            .reversed();

    /** Compare by descending liberty then size. */
    private static final Comparator<Colony> colonySoLComparator
        = Comparator.comparingInt(Colony::getSoL)
            .thenComparingInt(Colony::getUnitCount)
            .reversed();

    /** Compare by position on the map. */
    private static final Comparator<Colony> colonyPositionComparator
        = Comparator.comparingInt(c -> Location.rankOf(c));

    /** Friendly move animation speed option values. */
    private static final Map<Integer, String> friendlyMoveAnimationSpeeds
        = makeUnmodifiableMap(new Integer[] { 0, 1, 2, 3 },
            new String[] {
                "clientOptions.gui.friendlyMoveAnimationSpeed.off",
                "clientOptions.gui.friendlyMoveAnimationSpeed.slow",
                "clientOptions.gui.friendlyMoveAnimationSpeed.normal",
                "clientOptions.gui.friendlyMoveAnimationSpeed.fast"
            });
        
    
    /**
     * Creates a new {@code ClientOptions}.
     *
     * Unlike other OptionGroup classes, ClientOptions can not supply a
     * specification as it is needed before the specification is available.
     */
    public ClientOptions() {
        super(TAG);
    }


    /**
     * Loads the options from the given save game.
     *
     * @param save The {@code FreeColSaveGame} to read the options from.
     * @return True if the options were loaded without error.
     */
    private boolean load(FreeColSavegameFile save) {
        if (save == null) return false;
        boolean ret = false;
        try (
            FreeColXMLReader xr = save.getClientOptionsFreeColXMLReader();
        ) {
            ret = load(xr);
        } catch (IOException|XMLStreamException ex) {
            logger.log(Level.WARNING, "Load OptionGroup(" + getId()
                + ") from file " + save.getPath() + " crashed", ex);
            return false;
        }
        logger.info("Load OptionGroup(" + getId() + ") from " + save.getPath()
            + ((ret) ? " succeeded" : " failed"));
        return ret;
    }

    /**
     * Merge the options from the given file game.
     *
     * @param file The {@code File} to merge the options from.
     * @return True if the options were merge without error.
     */
    public boolean merge(File file) {
        ClientOptions clop = new ClientOptions();
        if (!clop.load(file)) return false;
        LogBuilder lb = new LogBuilder(64);
        boolean ret = this.merge(clop, lb);
        lb.shrink("\n"); lb.log(logger, Level.FINEST);
        return ret;
    }

    /**
     * Merge the options from the given save game.
     *
     * @param save The {@code FreeColSaveGame} to merge from.
     * @return True if the options were merged without error.
     */
    public boolean merge(FreeColSavegameFile save) {
        ClientOptions clop = new ClientOptions();
        if (!clop.load(save)) return false;
        LogBuilder lb = new LogBuilder(64);
        boolean ret = this.merge(clop, lb);
        lb.shrink("\n"); lb.log(logger, Level.FINEST);
        return ret;
    }

    /**
     * Gets a list of active mods in this ClientOptions.
     *
     * @return A list of active mods.
     */
    public List<FreeColModFile> getActiveMods() {
        final Predicate<FreeColModFile> validModPred = m ->
            m != null && m.getId() != null;
        final Function<FreeColModFile, FreeColModFile> modFileMapper = m ->
            FreeColModFile.getFreeColModFile(m.getId());
        return transform(getModList(ClientOptions.USER_MODS), validModPred,
                         modFileMapper, toListNoNulls());
    }

    /**
     * Get the client's preferred tile text type.
     *
     * @return A {@code DISPLAY_TILE_TEXT_} value
     */
    public int getDisplayTileText() {
        return getInteger(DISPLAY_TILE_TEXT);
    }

    /**
     * Get the client's preferred comparator for colonies.
     *
     * @return The current colony {@code Comparator}.
     */
    public Comparator<Colony> getColonyComparator() {
        return getColonyComparatorInternal(getInteger(COLONY_COMPARATOR));
    }

    /**
     * Get the colony comparator identified by type.
     *
     * @param type The colony comparator option integer value.
     * @return The corresponding colony {@code Comparator}.
     */
    private static Comparator<Colony> getColonyComparatorInternal(int type) {
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
            Comparator<Colony> cnc = colonyNameComparator.get();
            if (cnc == null) {
                // Can not be done statically, must wait for CLI parsing
                cnc = Comparator.comparing(Colony::getName,
                    Collator.getInstance(FreeCol.getLocale()));
                colonyNameComparator.set(cnc);
            }
            return cnc;
        default:
            throw new RuntimeException("Unknown comparator: " + type);
        }
    }

    /**
     * Return the client's preferred comparator for ModelMessages.
     *
     * @param game The {@code Game} to extract a comparator for.
     * @return The preferred {@code Comparator}.
     */
    public Comparator<ModelMessage> getModelMessageComparator(Game game) {
        switch (getInteger(MESSAGES_GROUP_BY)) {
        case MESSAGES_GROUP_BY_SOURCE:
            Map<String, Comparator<?>> specialized
                = new HashMap<>();
            specialized.put("Colony", getColonyComparator());
            return ModelMessage.getSourceComparator(game, specialized);
        case MESSAGES_GROUP_BY_TYPE:
            return ModelMessage.messageTypeComparator;
        default:
            return null;
        }
    }

    /**
     * Perform backward compatibility fixups on new client options as
     * they are introduced.  Annotate with introduction version so we
     * can clean these up as they become standard.
     */
    public void fixClientOptions() {
        // @compact 0.11.0
        addBooleanOption(MINIMAP_TOGGLE_BORDERS, ClientOptions.GUI, true);
        addBooleanOption(MINIMAP_TOGGLE_FOG_OF_WAR, ClientOptions.GUI, true);
        addTextOption(AUTO_SAVE_PREFIX, ClientOptions.SAVEGAMES, "Autosave");
        addTextOption(LAST_TURN_NAME, ClientOptions.SAVEGAMES, "last-turn");
        addTextOption(BEFORE_LAST_TURN_NAME,
                      ClientOptions.SAVEGAMES, "before-last-turn");
        // end @compact 0.11.0

        // @compat 0.11.1
        addBooleanOption(STOCK_ACCOUNTS_FOR_PRODUCTION,
                         ClientOptions.WAREHOUSE, false);
        // end @compat 0.11.1

        // @compat 0.11.3
        addBooleanOption(AUTOLOAD_SENTRIES, ClientOptions.OTHER, false);
        try { // Zoom range was increased
            RangeOption ro = getOption(DEFAULT_MINIMAP_ZOOM,
                                       RangeOption.class);
            if (ro.getItemValues().size() != 6) {
                Integer value = ro.getValue();
                ro.clearItemValues();
                ro.addItemValue(1, "1");
                ro.addItemValue(2, "2");
                ro.addItemValue(3, "3");
                ro.addItemValue(4, "4");
                ro.addItemValue(5, "5");
                ro.addItemValue(6, "6");
                ro.setValue(value); // Make sure the value is valid
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to fix " + DEFAULT_MINIMAP_ZOOM
                + " option", e);
        }
        // end @compat 0.11.3

        // @compat 0.11.6
        addBooleanOption(SHOW_REGION_NAMING, ClientOptions.MESSAGES, true);

        // These have computed keys in ModelMessage.MessageType
        addBooleanOption("model.option.guiShowCombatResult",
                         ClientOptions.MESSAGES, true);
        addBooleanOption("model.option.guiShowUnitRepaired",
                         ClientOptions.MESSAGES, true);
        addBooleanOption("model.option.guiShowArrived",
                         ClientOptions.MESSAGES, true);
        addBooleanOption("model.option.guiShowDisasters",
                         ClientOptions.MESSAGES, true);
        addBooleanOption(USE_OPENGL, ClientOptions.GUI, true);
        addBooleanOption(USE_XRENDER, ClientOptions.GUI, true);
        addRangeOption(FRIENDLY_MOVE_ANIMATION_SPEED, ClientOptions.GUI, 3,
                       friendlyMoveAnimationSpeeds);
        // end @compat 0.11.6
    }

    private void addBooleanOption(String id, String gr, boolean val) {
        if (!hasOption(id, BooleanOption.class)) {
            BooleanOption op = new BooleanOption(id, null);
            op.setGroup(gr);
            op.setValue(val);
            add(op);
        }
    }

    /** Currently unused.
    private void addIntegerOption(String id, String gr, int val) {
        if (!hasOption(id, IntegerOption.class)) {
            IntegerOption op = new IntegerOption(id, null);
            op.setGroup(gr);
            op.setValue(val);
            add(op);
        }
    }*/

    /** Currently unused.
    private void addOptionGroup(String id, String gr) {
        if (!hasOption(id, OptionGroup.class)) {
            OptionGroup og = new OptionGroup(id);
            og.setGroup(gr);
            add(og);
        }
    }*/

    /**
     * Add a new range option.
     *
     * @param id The option identifier.
     * @param gr The group identifier.
     * @param rank The option rank.
     * @param entries The options in the range.
     */
    private void addRangeOption(String id, String gr, int rank,
                                Map<Integer, String> entries) {
        if (!hasOption(id, RangeOption.class)) {
            RangeOption op = new RangeOption(id, (Specification)null);
            op.setGroup(gr);
            op.clearItemValues();
            for (Entry<Integer, String> e : entries.entrySet()) {
                op.addItemValue(e.getKey(), e.getValue());
            }
            op.setValueRank(rank);
            add(op);
        }
    }

    /**
     * Add a new text option.
     *
     * @param id The option identifier.
     * @param gr The group identifier.
     * @param val The the text value.
     */
    private void addTextOption(String id, String gr, String val) {
        if (!hasOption(id, TextOption.class)) {
            TextOption op = new TextOption(id, null);
            op.setGroup(gr);
            op.setValue(val);
            add(op);
        }
    }

    /**
     * Extract the panel position options for a given panel class name
     * as a point.
     *
     * @param className The panel class name.
     * @return A {@code Point} for the position if found, else null.
     */
    public Point getPanelPosition(String className) {
        OptionGroup etc = getOptionGroup(ETC);
        try {
            return (etc == null) ? null
                : new Point(etc.getInteger(className + ".x"),
                    etc.getInteger(className + ".y"));
        } catch (Exception ex) {
            logger.log(Level.FINEST, "Missing position option", ex);
            return null;
        }
    }

    /**
     * Extract the panel size options for a given panel class name as
     * a dimension.
     *
     * @param className The panel class name.
     * @return A {@code Dimension} for the size if found, else null.
     */
    public Dimension getPanelSize(String className) {
        OptionGroup etc = getOptionGroup(ETC);
        try {
            return (etc == null) ? null
                : new Dimension(etc.getInteger(className + ".w"),
                                etc.getInteger(className + ".h"));
        } catch (Exception ex) {
            logger.log(Level.FINEST, "Missing size option", ex);
            return null;
        }
    }

    /**
     * Extracts the special options from the client options file.
     *
     * Note: called early, the logger is not yet reliably available.
     *
     * @return A map of the special options and the values found for them.
     * @exception FreeColException if there is a problem reading the stream.
     */
    public static Map<String,String> getSpecialOptions()
        throws FreeColException {
        // Initialize the map
        Map<String, String> ret = new HashMap<>(specialKeys.size());
        for (String key : specialKeys) ret.put(key, null);

        // Extract the values and return
        final File optionsFile = FreeColDirectories.getClientOptionsFile();
        try (FreeColXMLReader xr = new FreeColXMLReader(optionsFile)) {
            xr.readAttributeValues(ret, "value");
        } catch (IOException|XMLStreamException xe) {
            throw new FreeColException(xe);
        }
        return ret;
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
