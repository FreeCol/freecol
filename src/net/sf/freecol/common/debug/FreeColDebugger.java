package net.sf.freecol.common.debug;

public class FreeColDebugger {

    public static final String COPYRIGHT = "Copyright (C) 2003-2012 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision: 2763 $";

    public static final int DEBUG_OFF = 0;

    public static final int DEBUG_LIMITED = 1;

    public static final int DEBUG_FULL = 2;

    public static final int DEBUG_FULL_COMMS = 3;

    private static int debugLevel = DEBUG_OFF;

    private static int debugRunTurns = -1;

    private static String debugRunSave = null;

    /**
     * Complete debug run.
     */
    public static void completeDebugRun() {
        setDebugRunTurns(0);
    }

    public static void configureDebugLevel(String optionValue) {
        try {
            FreeColDebugger.debugLevel = Integer.parseInt(optionValue);
            FreeColDebugger.debugLevel = Math.min(Math.max(getDebugLevel(), DEBUG_OFF),
              DEBUG_FULL_COMMS);
        } catch (NumberFormatException e) {
            FreeColDebugger.debugLevel = DEBUG_FULL;
        }
    }

    /**
     * Gets the debug level.
     *
     * @return The debug level.
     */
    public static int getDebugLevel() {
        return debugLevel;
    }

    public static String getDebugRunSave() {
        return debugRunSave;
    }

    /**
     * Gets the turns to run in debug mode.
     *
     * @return The turns to run in debug mode.
     */
    public static int getDebugRunTurns() {
        return debugRunTurns;
    }

    /**
     * Checks if the program is in "Debug mode".
     * @return <code>true</code> if the program is in debug
     *       mode and <code>false</code> otherwise.
     */
    public static boolean isInDebugMode() {
        return getDebugLevel() > DEBUG_OFF;
    }

    public static void setDebugRunSave(String debugRunSave) {
        FreeColDebugger.debugRunSave = debugRunSave;
    }

    public static void setDebugRunTurns(int debugRunTurns) {
        FreeColDebugger.debugRunTurns = debugRunTurns;
    }

    /**
     * Sets the "debug mode" to be active or not.
     * @param debug Should be <code>true</code> in order
     *       to active debug mode and <code>false</code>
     *       otherwise.
     */
    public static void setInDebugMode(boolean debug) {
        FreeColDebugger.debugLevel = (debug) ? DEBUG_FULL : DEBUG_OFF;
    }

}
