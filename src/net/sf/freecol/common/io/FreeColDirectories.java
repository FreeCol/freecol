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
package net.sf.freecol.common.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.filechooser.FileSystemView;


/**
 * Simple container for the freecol file and directory structure model.
 */
public class FreeColDirectories {

    // No logger!  Many of these routines are called before logging is
    // initialized.

    private static final String AUTOSAVE_DIRECTORY = "autosave";

    private static final String BASE_DIRECTORY = "base";

    private static final String CLASSIC_DIRECTORY = "classic";

    private static final String CLIENT_OPTIONS_FILE = "options.xml";

    private static final String DATA_DIRECTORY = "data";

    private static final String HIGH_SCORE_FILE = "HighScores.xml";

    private static final String I18N_DIRECTORY = "strings";

    private static final String LOG_FILE = "FreeCol.log";

    private static final String MAPS_DIRECTORY = "maps";

    private static final String MODS_DIRECTORY = "mods";

    private static final String RULES_DIRECTORY = "rules";

    private static final String SAVE_DIRECTORY = "save";

    private static final String SEPARATOR = System.getProperty("file.separator");

    /**
     * The directory containing automatically created save games.  At
     * program start, the path of this directory is based on the path
     * where to store regular save games.  If the saved game is
     * changed by the user during the game, then the value of
     * autosaveDirectory will not change.
     */
    private static File autosaveDirectory = null;

    /**
     * A file containing the client options.
     *
     * Can be overridden at the command line.
     */
    private static File clientOptionsFile = null;

    /**
     * The directory where the standard freecol data is installed.
     *
     * Can be overridden at the command line.
     *
     * TODO: defaults lamely to ./data.  Do something better in the
     * installer.
     */
    private static File dataDirectory = new File(DATA_DIRECTORY);

    /**
     * The path to the log file.
     *
     * Can be overridden at the command line.
     */
    private static String logFilePath = null;

    /**
     * The root directory where freecol saves user information.
     *
     * This will be set by default but can be overridden at the
     * command line.
     */
    private static File mainUserDirectory = null;

    /**
     * An optional directory containing user mods.
     */
    private static File userModsDirectory = null;

    /**
     * Where games are saved.
     *
     * Can be overridden in game or from the command line by
     * specifying the save game file.
     */
    private static File saveDirectory = null;

    /**
     * The current save game file.
     *
     * Can be modified in game.
     */
    private static File savegameFile = null;

    /**
     * The TotalConversion / ruleset in play.
     *
     * Can be overridden at the command line, or specified on the NewPanel.
     */
    private static String tc = "freecol";


    /**
     * Checks/creates the freecol directory structure for the current
     * user.
     *
     * The main user directory is in the current user's home
     * directory.  It used to be called ".freecol" (UNIXes) or
     * "freecol", but now we also use Library/FreeCol under MacOSX and
     * some JFileChooser trickery with Windows.
     *
     * Note: the freecol data directory is set independently and earlier
     * in initialization than this routine.
     *
     * TODO: The default location of the main user and data
     * directories should be determined by the installer.
     *
     * @return True if the directory structure is sufficiently intact for
     *     the game to proceed.
     */
    public static boolean createAndSetDirectories() {
        if (mainUserDirectory == null) {
            if (setMainUserDirectory(null) != null) return false;
        }

        if (logFilePath == null) {
            logFilePath = getMainUserDirectory() + SEPARATOR + LOG_FILE;
        }

        if (saveDirectory == null) {
            saveDirectory = new File(getMainUserDirectory(), SAVE_DIRECTORY);
            if (!insistDirectory(saveDirectory)) return false;
        }
    
        autosaveDirectory = new File(getSaveDirectory(), AUTOSAVE_DIRECTORY);
        if (!insistDirectory(autosaveDirectory)) autosaveDirectory = null;
    
        userModsDirectory = new File(getMainUserDirectory(), MODS_DIRECTORY);
        if (!insistDirectory(userModsDirectory)) userModsDirectory = null;

        return true;
    }

    /**
     * Insist that a directory either already exists, or is created.
     *
     * @param file A <code>File</code> specifying where to make the directory.
     * @return True if the directory is now there.
     */
    private static boolean insistDirectory(File file) {
        if (file.exists()) {
            if (file.isDirectory()) return true;
            System.err.println("Could not create directory " + file.getPath()
                + " because a non-directory with that name is already there.");
            return false;
        }
        try {
            return file.mkdir();
        } catch (Exception e) {
            System.err.println("Could not make directory " + file.getPath()
                + ": " + e.getMessage());
            return false;
        }
    }


    /**
     * Gets the directory where the automatically saved games should be put.
     *
     * @return The autosave directory.
     */
    public static File getAutosaveDirectory() {
        return autosaveDirectory;
    }

    /**
     * Gets the base resources directory.
     *
     * @return The base resources directory.
     */
    public static File getBaseDirectory() {
        return new File(getDataDirectory(), BASE_DIRECTORY);
    }

    /**
     * Gets the file containing the client options.
     *
     * @return The client options file, if any.
     */
    public static File getClientOptionsFile() {
        return (clientOptionsFile != null) ? clientOptionsFile
            : new File(getOptionsDirectory(), CLIENT_OPTIONS_FILE);
    }

    /**
     * Sets the client options file.
     *
     * @param path The new client options file.
     * @return True if the file was set successfully.
     */
    public static boolean setClientOptionsFile(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile() && file.canRead()) {
            clientOptionsFile = file;
            return true;
        }
        return false;
    }

    /**
     * Gets the data directory.
     *
     * @return The directory where the data files are located.
     */
    public static File getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Sets the data directory.
     *
     * Insist that the base resources and i18n subdirectories are present.
     *
     * @param dir The new value for the data directory, or null to
     *     apply the default.
     * @return A (non-i18n) error message on failure, null on success.
     */
    public static String setDataDirectory(String path) {
        if (path == null) path = DATA_DIRECTORY;
        File dir = new File(path);
        if (!dir.isDirectory()) return "Not a directory: " + path;
        if (!dir.canRead()) return "Can not read directory: " + path;
        dataDirectory = dir;
        if (getBaseDirectory() == null) {
            return "Can not find base resources directory: " + path
                + SEPARATOR + BASE_DIRECTORY;
        }
        if (getI18nDirectory() == null) {
            return "Can not find I18n resources directory: " + path
                + SEPARATOR + I18N_DIRECTORY;
        }
        return null;
    }

    /**
     * Gets the high score file.
     *
     * @return The high score file, if it exists.
     */
    public static File getHighScoreFile() {
        return new File(getDataDirectory(), HIGH_SCORE_FILE);
    }

    /**
     * Gets the directory containing language property files.
     *
     * @return The FreeCol i18n directory.
     */
    public static File getI18nDirectory() {
        return new File(getDataDirectory(), I18N_DIRECTORY);
    }

    /**
     * Gets the log file path.
     *
     * @return The log file path.
     */
    public static String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Sets the log file path.
     *
     * @param path The new log file path.
     */
    public static void setLogFilePath(String path) {
        logFilePath = path;
    }

    /**
     * Gets the main user directory, that is the directory under which
     * the user-specific data lives.
     *
     * @return The main user directory.
     */
    public static File getMainUserDirectory() {
        return mainUserDirectory;
    }

    /**
     * Gets the default main user directory under their home.
     *
     * @return The default main user directory.
     */
    public static File getDefaultMainUserDirectory() {
        String freeColDirectoryName = "/".equals(SEPARATOR) ? ".freecol"
            : "freecol";
        File userHome = FileSystemView.getFileSystemView()
            .getDefaultDirectory();
        if (userHome == null) return null;

        // Checks for OS specific paths, however if the old
        // {home}/.freecol exists that overrides OS-specifics for
        // backwards compatibility.
        // TODO: remove compatibility code and fix BR#3526832
        if (System.getProperty("os.name").equals("Mac OS X")) {
            // We are running on a Mac and should use {home}/Library/FreeCol
            if (!new File(userHome, freeColDirectoryName).isDirectory()) {
                return new File(new File(userHome, "Library"), "FreeCol");
            }
        } else if (System.getProperty("os.name").startsWith("Windows")) {
            // We are running on Windows and should use "My Documents"
            // (or localized equivalent)
            if (!new File(userHome, freeColDirectoryName).isDirectory()) {
                freeColDirectoryName = "FreeCol";
            }
        }
        
        return new File(userHome, freeColDirectoryName);
    }

    /**
     * Sets the main user directory, creating it if necessary.
     * If pre-existing, it must be a directory, readable and writable.
     *
     * @param path The path to the new main user directory, or null to apply
     *     the default.
     * @return Null on success, an error message key on failure.
     */
    public static String setMainUserDirectory(String path) {
        String ret = null;
        File dir = (path == null) ? getDefaultMainUserDirectory()
            : new File(path);
        if (!dir.exists()) {
            ret = "cli.error.home.notExists";
            try {
                if (dir.mkdir()) {
                    mainUserDirectory = dir;
                    ret = null;
                }
            } catch (Exception e) {}
        } else if (!dir.isDirectory()) {
            ret = "cli.error.home.notExists";
        } else if (!dir.canRead()) {
            ret = "cli.error.home.noRead";
        } else if (!dir.canWrite()) {
            ret = "cli.error.home.noWrite";
        } else {
            mainUserDirectory = dir;
        }
        return ret;
    }

    /**
     * Gets the directory containing the predefined maps.
     *
     * @return The predefined maps.
     */
    public static File getMapsDirectory() {
        return new File(getDataDirectory(), MAPS_DIRECTORY);
    }

    /**
     * Gets the standard mods directory.
     *
     * @return The directory where the standard mods are located.
     */
    public static File getStandardModsDirectory() {
        return new File(getDataDirectory(), MODS_DIRECTORY);
    }

    /**
     * Gets the user mods directory.
     *
     * @return The directory where user mods are located, or null if none.
     */
    public static File getUserModsDirectory() {
        return userModsDirectory;
    }

    /**
     * Gets the directory where the user options are saved.
     *
     * @return The directory to save user options in.
     */
    public static File getOptionsDirectory() {
        return new File(getMainUserDirectory(), getTC());
    }

    /**
     * Gets the directory containing the classic rules.
     *
     * @return The classic rules directory.
     */
    public static File getRulesClassicDirectory() {
        return new File(getRulesDirectory(), CLASSIC_DIRECTORY);
    }

    /**
     * Gets the directory containing the various rulesets.
     *
     * @return The ruleset directory.
     */
    public static File getRulesDirectory() {
        return new File(getDataDirectory(), RULES_DIRECTORY);
    }

    /**
     * Gets the directory where the savegames should be put.
     *
     * @return The save directory.
     */
    public static File getSaveDirectory() {
        return saveDirectory;
    }
    
    /**
     * Set the directory where the saved games should be put.
     *
     * @param dir The new saved games directory.
     */
    public static void setSaveDirectory(File dir) {
        saveDirectory = dir;
    }

    /**
     * Gets the save game file.
     *
     * @param The save game file.
     */
    public static File getSavegameFile() {
        return savegameFile;
    }

    /**
     * Sets the save game file.
     *
     * @param file The new save game file.
     */
    public static void setSavegameFile(File file) {
        savegameFile = file;
    }

    /**
     * Sets the save game file.
     *
     * @param path The path to the new save game file.
     * @return True if the setting succeeds.
     */
    public static boolean setSavegameFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            file = new File(getSaveDirectory(), path);
            if (!file.exists() || !file.isFile() || !file.canRead()) return false;
        }
        setSavegameFile(file);
        setSaveDirectory(file.getParentFile());
        return true;
    }

    /**
     * Gets the current Total-Conversion.
     */
    public static String getTC() {
        return tc;
    }

    /**
     * Sets the Total-Conversion.
     *
     * @param tc The name of the new total conversion.
     */
    public static void setTC(String tc) {
        FreeColDirectories.tc = tc;
    }
}
