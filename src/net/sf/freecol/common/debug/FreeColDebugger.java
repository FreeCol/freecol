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

package net.sf.freecol.common.debug;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Player;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.server.FreeColServer;


/**
 * High-level debug handling.
 */
public class FreeColDebugger {

    private static final Logger logger = Logger.getLogger(FreeColDebugger.class.getName());

    /** The debug modes, any of which may be active. */
    public static enum DebugMode {
        COMMS, // Trace print full c-s communications, and verbose
               // (non-i18n) server errors.
        DESYNC,// Check frequently for desynchronization
        MENUS, // Enable the Debug menu, the extra commands in
               // ColonyPanel and TilePopup, the goods-in-market
               // tooltip in MarketLabel, the extra modifiers on the
               // BuildingToolTip, the region and Mission
               // displays in MapViewer, taking over AI players,
               // and turn skipping.
        INIT,  // An initial colony is made, and goods added to all
               // native settlements.
        PATHS  // Display more information on goto paths
    }
    private static int debugMode = 0;

    /**
     * The number of turns to run without stopping.
     */
    private static int debugRunTurns = -1;

    /**
     * The name of a file to save to at the end of a debug run.
     */
    private static String debugRunSave = null;

    /**
     * Stores the standard fog of war setting when revealing all the map
     * Allows restore to previous state when re-enabling normal vision
     */
    private static boolean normalGameFogOfWar = false;

    /** Display map coordinates? */
    private static boolean displayCoordinates = false;

    /** Display tile values as a colony site for this player? */
    private static Player displayColonyValuePlayer = null;

    /** Show the mission for an AI unit? */
    private static boolean showMission = false;

    /** Show full mission information? */
    private static boolean showMissionInfo = false;

    /** Stream for debugLog. */
    private static final AtomicReference<PrintStream> debugStream
        = new AtomicReference<PrintStream>(null);


    /**
     * Is a debug mode enabled in this game?
     *
     * @return True if any debug mode is enabled.
     */
    public static boolean isInDebugMode() {
        return FreeColDebugger.debugMode != 0;
    }

    /**
     * Is a particular debug mode enabled in this game?
     *
     * @param mode The {@code DebugMode} to test.
     * @return True if the specified mode is enabled.
     */
    public static boolean isInDebugMode(DebugMode mode) {
        return ((1 << mode.ordinal()) & FreeColDebugger.debugMode) != 0;
    }

    /**
     * Set a debug mode.
     *
     * @param mode The {@code DebugMode} to set.
     * @param val The value to set to.
     */
    public static void setDebugMode(DebugMode mode, boolean val) {
        if (val) {
            enableDebugMode(mode);
        } else {
            disableDebugMode(mode);
        }
    }

    /**
     * Enables a particular debug mode.
     *
     * @param mode The {@code DebugMode} to enable.
     */
    public static void enableDebugMode(DebugMode mode) {
        FreeColDebugger.debugMode |= 1 << mode.ordinal();
    }

    /**
     * Disable a particular debug mode.
     *
     * @param mode The {@code DebugMode} to disable.
     */
    private static void disableDebugMode(DebugMode mode) {
        if (mode != DebugMode.MENUS) { // Can not leave menus mode!
            FreeColDebugger.debugMode &= ~(1 << mode.ordinal());
        }
    }

    /**
     * Gets the debug modes.
     *
     * @return A string containing the modes as csv.
     */
    public static String getDebugModes() {
        return transform(DebugMode.values(), m -> isInDebugMode(m),
                         DebugMode::toString, Collectors.joining(","));
    }

    /**
     * Configures the debug modes.
     *
     * @param optionValue The command line option.
     * @return True if the modes were set as specified.
     */
    public static boolean setDebugModes(String optionValue) {
        if (optionValue == null) return false;
        if (optionValue.isEmpty()) return true;

        for (String s : optionValue.split(",")) {
            try {
                DebugMode mode = Enum.valueOf(DebugMode.class, upCase(s));
                enableDebugMode(mode);
            } catch (RuntimeException e) {
                logger.warning("Unrecognized debug mode: " + optionValue);
                return false;
            }
        }
        return true;
    }

    /**
     * Configures a debug run.
     *
     * @param option The command line option.
     */
    public static void configureDebugRun(String option) {
        int comma = option.indexOf(',');
        String turns = option.substring(0, (comma < 0) ? option.length()
            : comma);
        try {
            setDebugRunTurns(Integer.parseInt(turns));
        } catch (NumberFormatException e) {
            setDebugRunTurns(-1);
        }
        if (comma > 0) setDebugRunSave(option.substring(comma + 1));
    }

    /**
     * Gets the turns to run in debug mode.
     *
     * @return The turns to run in debug mode.
     */
    public static int getDebugRunTurns() {
        return FreeColDebugger.debugRunTurns;
    }

    /**
     * Sets the number of turns to run in debug mode.
     *
     * @param debugRunTurns The new number of debug turns.
     */
    public static void setDebugRunTurns(int debugRunTurns) {
        FreeColDebugger.debugRunTurns = debugRunTurns;
    }

    /**
     * Gets the debug save file name.
     *
     * @return The debug save file name.
     */
    public static String getDebugRunSave() {
        return FreeColDebugger.debugRunSave;
    }

    /**
     * Sets the debug save file name.
     *
     * @param debugRunSave The new debug save file name.
     */
    public static void setDebugRunSave(String debugRunSave) {
        FreeColDebugger.debugRunSave = debugRunSave;
    }

    /**
     * Get the normal fog of war setting.
     *
     * @return The normal fog of war setting.
     */
    public static boolean getNormalGameFogOfWar() {
        return normalGameFogOfWar;
    }

    /**
     * Set the normal fog of war setting.
     *
     * @param normalGameFogOfWar The new normal fog of war setting.
     */
    public static void setNormalGameFogOfWar(boolean normalGameFogOfWar) {
        FreeColDebugger.normalGameFogOfWar = normalGameFogOfWar;
    }

    /**
     * Try to complete a debug run if one is happening.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param force Force early completion of a run.
     * @return True if a debug run was completed.
     */
    public static boolean finishDebugRun(FreeColClient freeColClient,
                                         boolean force) {
        int turns = getDebugRunTurns();
        if (turns < 0              // Not a debug run
            || turns > 0 && !force // Still going
            ) return false;

        // Zero => signalEndDebugRun was called
        setDebugRunTurns(-1);

        if (getDebugRunSave() != null) {
            FreeColServer fcs = freeColClient.getFreeColServer();
            if (fcs != null) {
                try {
                    fcs.saveGame(FreeColDirectories.getDebugRunSaveFile(),
                                 freeColClient.getClientOptions(), null);
                } catch (IOException e) {}
            }
            freeColClient.quit();
        }
        return true;
    }
    
    /**
     * Signal that a debug run should complete at the next suitable
     * opportunity.  Currently called from the server.
     */
    public static void signalEndDebugRun() {
        if (debugRunTurns > 0) setDebugRunTurns(0);
    }

    /**
     * Should the map viewer display tile coordinates?
     *
     * @return True if the map viewer should display coordinates.
     */
    public static boolean debugDisplayCoordinates() {
        return displayCoordinates;
    }

    /**
     * Set the display tile coordinates state.
     *
     * @param display Whether to display or not.
     */
    public static void setDebugDisplayCoordinates(boolean display) {
        displayCoordinates = display;
    }

    /**
     * Should the map viewer display colony values for a player?
     *
     * @return The {@code Player} to display colony values for,
     *     or null if not to display.
     */
    public static Player debugDisplayColonyValuePlayer() {
        return displayColonyValuePlayer;
    }

    /**
     * Set the player to display colony values for.
     *
     * @param display The new {@code Player} to display for.
     */
    public static void setDebugDisplayColonyValuePlayer(Player display) {
        displayColonyValuePlayer = display;
    }

    /**
     * Should the map viewer show AI missions?
     *
     * @return True if the map viewer should show AI missions?
     */
    public static boolean debugShowMission() {
        return showMission;
    }

    /**
     * Set the display of AI missions state.
     *
     * @param display Whether to display or not.
     */
    public static void setDebugShowMission(boolean display) {
        showMission = display;
    }

    /**
     * Should the map viewer show full AI mission info?
     *
     * @return True if the map viewer should show full AI mission info.
     */
    public static boolean debugShowMissionInfo() {
        return showMissionInfo;
    }

    /**
     * Set the display of full AI mission info state.
     *
     * @param display Whether to display or not.
     */
    public static void setDebugShowMissionInfo(boolean display) {
        showMissionInfo = display;
    }

    /**
     * Handler for log records that include a crash.
     */
    public static void handleCrash() {
        if (debugRunSave != null) signalEndDebugRun();
    }


    /**
     * Emergency run time log to use when the normal logging is failing.
     * It might as well be here.
     *
     * @param msg The message to log.
     */
    public static void debugLog(String msg) {
        PrintStream print = debugStream.get();
        if (print == null) {
            String tmp = System.getenv("TMPDIR");
            if (tmp == null) tmp = "/tmp";
            final Path path = Paths.get(tmp, "freecol.debug");
            try {
                OutputStream fos
                    = Files.newOutputStream(path, CREATE, APPEND);
                print = new PrintStream(fos, true, "UTF-8");
            } catch (IOException ex) {
                ; // ignored
            }
            debugStream.set(print);
        }
        if (print != null) print.println(msg);
    }

    /**
     * Miscellaneous debug helper to get a string representation of
     * the current call stack.
     *
     * @return A stack trace as a string.
     */
    public static String stackTraceToString() {
        return stackTraceToString(Thread.currentThread());
    }

    /**
     * Miscellaneous debug helper to get a string representation of
     * the stack of a given thread.
     *
     * @param thread The {@code Thread} to print.
     * @return A stack trace as a string.
     */
    private static String stackTraceToString(Thread thread) {
        LogBuilder lb = new LogBuilder(512);
        addStackTrace(lb, thread);
        return lb.toString();
    }

    /**
     * Helper that adds a stack trace for the current thread to a log builder.
     *
     * @param lb The {@code LogBuilder} to add to.
     */
    public static void addStackTrace(LogBuilder lb) {
        addStackTrace(lb, Thread.currentThread());
    }

    /**
     * Helper that adds a stack trace to a log builder.
     *
     * @param lb The {@code LogBuilder} to add to.
     * @param thread The {@code Thread} to print.
     */
    private static void addStackTrace(LogBuilder lb, Thread thread) {
        for (StackTraceElement s : thread.getStackTrace()) {
            lb.add(s.toString(), "\n");
        }
        lb.shrink("\n");
    }

    /**
     * Log a warning with a stack trace.
     *
     * @param logger The {@code Logger} to log to.
     * @param warn The warning message.
     */
    public static void trace(Logger logger, String warn) {
        LogBuilder lb = new LogBuilder(512);
        lb.add(warn, "\n");
        addStackTrace(lb);
        if (logger == null) {
            System.err.println(lb.toString());
        } else {
            logger.warning(lb.toString());
        }
    }
}
