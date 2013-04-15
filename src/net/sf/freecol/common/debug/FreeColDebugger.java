/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.server.FreeColServer;


/**
 * High-level debug handling.
 */
public class FreeColDebugger {

    private static final Logger logger = Logger.getLogger(FreeColDebugger.class.getName());

    public static enum DebugMode {
        COMMS, // Trace print full c-s communications, and verbose
               // (non-i18n) server errors.
        MENUS, // Enable the Debug menu, the extra commands in
               // ColonyPanel and TilePopup, the goods-in-market
               // tooltip in MarketLabel, the extra modifiers on the
               // BuildingToolTip, the region and Mission
               // displays in MapViewer, and turn skipping.
        INIT   // An initial colony is made, and goods added to all
               // native settlements.
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
     * @param mode The <code>DebugMode</code> to test.
     * @return True if the specified mode is enabled.
     */
    public static boolean isInDebugMode(DebugMode mode) {
        return ((1 << mode.ordinal()) & FreeColDebugger.debugMode) != 0;
    }

    /**
     * Sets the debug mode
     *
     * @param mode The new debug mode.
     */
    private static void setDebugMode(int mode) {
        FreeColDebugger.debugMode = mode;
    }

    /**
     * Enables a particular debug mode.
     *
     * @param mode The <code>DebugMode</code> to enable.
     */
    public static void enableDebugMode(DebugMode mode) {
        FreeColDebugger.debugMode |= 1 << mode.ordinal();
    }

    /**
     * Gets the debug modes.
     *
     * @return A string containing the modes as csv.
     */
    public static String getDebugModes() {
        String ret = "";
        for (DebugMode mode : DebugMode.values()) {
            if (isInDebugMode(mode)) ret += "," + mode.toString();
        }
        return (ret.length() > 0) ? ret.substring(1, ret.length())
            : ret;
    }

    /**
     * Configures the debug modes.
     *
     * @param optionValue The command line option.
     */
    public static boolean setDebugModes(String optionValue) {
        if (optionValue == null || "".equals(optionValue)) {
            enableDebugMode(DebugMode.MENUS);
            return true;
        }
        // @compat 0.10.x
        try {
            int i = Integer.parseInt(optionValue);
            switch (i) {
            case 3:
                enableDebugMode(DebugMode.COMMS);
                // Fall through
            case 2:
                enableDebugMode(DebugMode.INIT);
                // Fall through
            case 1:
                enableDebugMode(DebugMode.MENUS);
                return true;
            default:
                return false;
            }
        } catch (NumberFormatException nfe) {}
        // @end compatibility code

        for (String s : optionValue.split(",")) {
            try {
                DebugMode mode = Enum.valueOf(DebugMode.class,
                                              s.toUpperCase(Locale.US));
                enableDebugMode(mode);
            } catch (Exception e) {
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
        int comma = option.indexOf(",");
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
     * @param freeColClient The <code>FreeColClient</code> of the game.
     * @param force Force early completion of a run.
     * @return True if a debug run was completed.
     */
    public static boolean finishDebugRun(FreeColClient freeColClient,
                                         boolean force) {
        if (getDebugRunTurns() < 0) return false; // Not a debug run
        if (getDebugRunTurns() > 0 && !force) return false; // Still going
        // Zero => signalEndDebugRun was called
        setDebugRunTurns(-1);

        if (getDebugRunSave() != null) {
            FreeColServer fcs = freeColClient.getFreeColServer();
            if (fcs != null) {
                try {
                    fcs.saveGame(new File(".", getDebugRunSave()),
                                 freeColClient.getClientOptions());
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
     * Handler for log records that include a crash.
     *
     * @param record The <code>LogRecord</code> with a crash.
     */
    public static void handleCrash(LogRecord record) {
        if (debugRunSave != null) signalEndDebugRun();
    }


    /**
     * Emergency run time log to use when the normal logging is failing.
     * It might as well be here.
     *
     * @param msg The message to log.
     */
    public static void debugLog(String msg) {
        try {
            new PrintStream(new FileOutputStream("/tmp/freecol.debug", true), 
                            true).println(msg);
        } catch (Exception e) {}
    }

    /**
     * Miscellaneous debug helper to get a string representation of
     * the current call stack.
     *
     * @return A stack trace as a string.
     */
    public static String stackTraceToString() {
        StringBuilder sb = new StringBuilder(512);
        for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
            sb.append(s.toString()).append("\n");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
