/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.filechooser.FileSystemView;

import net.sf.freecol.FreeCol;


/**
 * Simple container for the freecol file and directory structure model.
 */
public class FreeColDirectories {

    // No logger!  Many of these routines are called before logging is
    // initialized.

    private static final String AUTOSAVE_DIRECTORY = "autosave";

    private static final String BASE_DIRECTORY = "base";

    private static final String CLASSIC_DIRECTORY = "classic";

    private static final String[] CONFIG_DIRS
        = { "classic", "freecol" };

    private static final String DATA_DIRECTORY = "data";

    private static final String FREECOL_DIRECTORY = "freecol";

    private static final String HIGH_SCORE_FILE = "HighScores.xml";

    private static final String I18N_DIRECTORY = "strings";

    private static final String LOG_FILE = "FreeCol.log";

    private static final String MAPS_DIRECTORY = "maps";

    private static final String MODS_DIRECTORY = "mods";

    private static final String RULES_DIRECTORY = "rules";

    private static final String SAVE_DIRECTORY = "save";

    private static final String START_MAP_NAME = "startMap.fsg";

    private static final String SEPARATOR = System.getProperty("file.separator");

    private static final String XDG_CONFIG_HOME_ENV = "XDG_CONFIG_HOME";
    private static final String XDG_CONFIG_HOME_DEFAULT = ".config";
    private static final String XDG_DATA_HOME_ENV = "XDG_DATA_HOME";
    private static final String XDG_DATA_HOME_DEFAULT = ".local/share";
    private static final String XDG_CACHE_HOME_ENV = "XDG_CACHE_HOME";
    private static final String XDG_CACHE_HOME_DEFAULT = ".cache";

    // Public names, used by the respective dialogs

    public static final String BASE_CLIENT_OPTIONS_FILE_NAME = "client-options.xml";

    public static final String CLIENT_OPTIONS_FILE_NAME = "options.xml";

    public static final String CUSTOM_DIFFICULTY_FILE_NAME = "custom.xml";

    public static final String GAME_OPTIONS_FILE_NAME = "game_options.xml";

    public static final String MAP_FILE_NAME = "my_map.fsg";

    public static final String MAP_GENERATOR_OPTIONS_FILE_NAME
        = "map_generator_options.xml";


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
     * FIXME: defaults lamely to ./data.  Do something better in the
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
     * The directory where freecol saves transient information.
     */
    private static File userCacheDirectory = null;

    /**
     * The directory where freecol saves user configuration.
     *
     * This will be set by default but can be overridden at the
     * command line.
     */
    private static File userConfigDirectory = null;

    /**
     * The directory where freecol saves user data.
     *
     * This will be set by default but can be overridden at the
     * command line.
     */
    private static File userDataDirectory = null;

    /**
     * An optional directory containing user mods.
     */
    private static File userModsDirectory = null;


    /**
     * Does the OS look like Mac OS X?
     *
     * @return True if Mac OS X appears to be present.
     */
    public static boolean onMacOSX() {
        return "Mac OS X".equals(System.getProperty("os.name"));
    }

    /**
     * Does the OS look like some sort of unix?
     *
     * @return True we hope.
     */
    public static boolean onUnix() {
        return "/".equals(SEPARATOR);
    }

    /**
     * Does the OS look like some sort of Windows?
     *
     * @return True if Windows appears to be present.
     */
    public static boolean onWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    /**
     * Get the user home directory.
     *
     * @return The user home directory.
     */
    private static File getUserDefaultDirectory() {
        return FileSystemView.getFileSystemView().getDefaultDirectory();
    }

    /**
     * Check a directory for read and write access.
     *
     * @param dir The <code>File</code> that must be a usable directory.
     * @return Null on success, an error message key on failure.
     */
    public static String checkDir(File dir) {
        return (dir == null || !dir.exists()) ? "cli.error.home.notExists"
            : (!dir.isDirectory()) ? "cli.error.home.notDir"
            : (!dir.canRead()) ? "cli.error.home.noRead"
            : (!dir.canWrite()) ? "cli.error.home.noWrite"
            : null;
    }

    /**
     * Get directories for XDG compliant systems.
     *
     * Result is:
     * - Negative if a non-XDG OS is detected or there is insufficient
     *   XDG structure to merit migrating, or what structure there is is
     *   broken in some way.
     * - Zero if there is at least one relevant XDG environment
     *   variable in use and it points to a valid writable directory,
     *   or the default exists and is writable.
     * - Positive if there are a full set of suitable XDG directories and
     *   there are freecol directories therein.
     * - Otherwise negative, including non-directories in the wrong place
     *   and unwritable directories.
     *
     * The intent is to ignore XDG on negative, migrate on zero, and use
     * on positive.
     *
     * @param dirs An array of <code>File</code> to be filled in with the
     *     XDG directory if it is present or created.
     * @return The XDG compliance state.
     */
    private static int getXDGDirs(File[] dirs) {
        if (onMacOSX() || onWindows() || !onUnix()) return -1;

        int ret = -1;
        File home = getUserDefaultDirectory();
        if (home == null) return -1; // Fail badly
        String[][] xdg = { { XDG_CONFIG_HOME_ENV, XDG_CONFIG_HOME_DEFAULT },
                           { XDG_DATA_HOME_ENV,   XDG_DATA_HOME_DEFAULT },
                           { XDG_CACHE_HOME_ENV,  XDG_CACHE_HOME_DEFAULT } };
        File[] todo = new File[xdg.length];
        for (int i = 0; i < xdg.length; i++) {
            String env = System.getenv(xdg[i][0]);
            File d = (env != null) ? new File(env) : new File(home, xdg[i][1]);
            if (d.exists()) {
                if (!d.isDirectory() || !d.canWrite()) {
                    return -1; // Fail hard if something is broken
                }
                ret = Math.max(ret, 0);
                File f = new File(d, FREECOL_DIRECTORY);
                if (f.exists()) {
                    if (!f.isDirectory() || !f.canWrite()) {
                        return -1; // Again, fail hard
                    }
                    dirs[i] = f;
                    todo[i] = null;
                    ret++;
                } else {
                    dirs[i] = d;
                    todo[i] = f;
                }
            } else {
                dirs[i] = null;
                todo[i] = d;
            }
        }
        if (ret < 0) return -1; // No evidence of interest in XDG standard
        if (ret == xdg.length) return 1; // Already fully XDG compliant

        // Create the directories for migration
        for (int i = 0; i < xdg.length; i++) {
            if (todo[i] != null) {
                if (!todo[i].getPath().endsWith(FREECOL_DIRECTORY)) {
                    if (!todo[i].mkdir()) return -1;
                    todo[i] = new File(todo[i], FREECOL_DIRECTORY);
                }
                if (!todo[i].mkdir()) return -1;
                dirs[i] = todo[i];
            }
        }
        return 0;
    }

    /**
     * Is the specified file a writable directory?
     *
     * @param f The <code>File</code> to check.
     * @return True if the file is a writable directory.
     */
    private static boolean isGoodDirectory(File f) {
        return f.exists() && f.isDirectory() && f.canWrite();
    }

    /**
     * Create the given directory if it does not exist, otherwise expect
     * it to be writable.
     *
     * @param dir The <code>File</code> specifying the required directory.
     * @return The required directory, or null on failure.
     */
    private static File requireDir(File dir) {
        if (dir.exists()) {
            if (dir.isDirectory() && dir.canWrite()) return dir;
        } else {
            if (dir.mkdir()) return dir;
        }
        return null;
    }

    /**
     * Get FreeCol directories for MacOSX.
     *
     * No separate cache directory here.
     *
     * Result is:
     * - Negative on failure.
     * - Zero if a migration is needed.
     * - Positive if no migration is needed.
     *
     * @param dirs An array of <code>File</code> to be filled in with the
     *     MacOSX freecol directories if present or created.
     * @return The migration state.
     */
    private static int getMacOSXDirs(File[] dirs) {
        if (!onMacOSX()) return -1;
        int ret = 0;
        File homeDir = getUserDefaultDirectory();
        if (homeDir == null) return -1;
        File libDir = new File(homeDir, "Library");
        if (!isGoodDirectory(libDir)) return -1;

        if (dirs[0] == null) {
            File prefsDir = new File(libDir, "Preferences");
            if (isGoodDirectory(prefsDir)) {
                dirs[0] = prefsDir;
                File d = new File(prefsDir, FREECOL_DIRECTORY);
                if (d.exists()) {
                    if (d.isDirectory() && d.canWrite()) {
                        dirs[0] = d;
                        ret++;
                    } else return -1;
                }
            } else return -1;
        }

        if (dirs[1] == null) {
            File appsDir = new File(libDir, "Application Support");
            if (isGoodDirectory(appsDir)) {
                dirs[1] = appsDir;
                File d = new File(appsDir, FREECOL_DIRECTORY);
                if (d.exists()) {
                    if (d.isDirectory() && d.canWrite()) {
                        dirs[1] = d;
                        ret++;
                    } else return -1;
                }
            } else return -1;
        }

        if (dirs[2] == null) {
            dirs[2] = dirs[1];
        }

        if (ret == 2) return 1;

        File d = requireDir(new File(dirs[0], FREECOL_DIRECTORY));
        if (d == null) return -1;
        dirs[0] = d;

        d = requireDir(new File(dirs[1], FREECOL_DIRECTORY));
        if (d == null) return -1;
        dirs[1] = d;

        return 0;
    }


    /**
     * Get FreeCol directories for Windows.
     *
     * Simple case, everything is in the one directory.
     *
     * Result is:
     * - Negative on failure.
     * - Zero if a migration is needed.
     * - Positive if no migration is needed.
     *
     * @param dirs An array of <code>File</code> to be filled in with the
     *     Windows freecol directories if present or created.
     * @return The migration state.
     */
    private static int getWindowsDirs(File[] dirs) {
        if (onMacOSX() || !onWindows() || onUnix()) return -1;

        File home = getUserDefaultDirectory();
        if (home == null) return -1; // Fail badly
        File d = requireDir(new File(home, FREECOL_DIRECTORY));
        if (d == null) return -1;
        dirs[0] = dirs[1] = dirs[2] = d;
        return 1; // Do not migrate windows
    }

    /**
     * Find the old user directory.
     *
     * Does not try to be clever, just tries ~/FreeCol, ~/.freecol, and
     * ~/Library/FreeCol which should find the old directory on the three
     * known systems.
     *
     * @return The old user directory, or null if none found.
     */
    private static File getOldUserDirectory() {
        File home = getUserDefaultDirectory();
        File old = new File(home, "FreeCol");
        if (old.exists() && old.isDirectory() && old.canRead()) return old;
        old = new File(home, ".freecol");
        if (old.exists() && old.isDirectory() && old.canRead()) return old;
        old = new File(home, "Library");
        if (old.exists() && old.isDirectory() && old.canRead()) {
            old = new File(old, "FreeCol");
            if (old.exists() && old.isDirectory() && old.canRead()) return old;
        }
        return null;
    }

    /**
     * Copy directory with given name under an old directory to a new
     * directory.
     *
     * @param oldDir The old directory.
     * @param name The name of the directory to copy.
     * @param newDir The new directory.
     */
    private static void copyIfFound(File oldDir, String name, File newDir) {
        File src = new File(oldDir, name);
        File dst = new File(newDir, name);
        if (src.exists() && src.isDirectory() && !dst.exists()) {
            try {
                Files.copy(src.toPath(), dst.toPath(),
                    StandardCopyOption.COPY_ATTRIBUTES);
            } catch (IOException ioe) {
                System.err.println("Could not copy " + src.toString() + " to "
                    + dst.toString() + ": " + ioe.getMessage());
            }
        }
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
     * Derive the directory for the autosave files from the save directory.
     */
    private static void deriveAutosaveDirectory() {
        if (autosaveDirectory == null && saveDirectory != null) {
            autosaveDirectory = new File(saveDirectory, AUTOSAVE_DIRECTORY);
            if (!insistDirectory(autosaveDirectory)) autosaveDirectory = null;
        }
    }
        

    // Main initialization/bootstrap routines.
    // These need to be called early before the subsidiary directory
    // accessors are used.

    /**
     * Sets the data directory.
     *
     * Insist that the base resources and i18n subdirectories are present.
     *
     * @param path The path to the new data directory, or null to
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
     * Checks/creates the freecol directory structure for the current
     * user.
     *
     * The main user directory used to be in the current user's home
     * directory, and called ".freecol" (UNIXes including Mac in
     * 0.9.x) or "freecol" or even FreeCol.  Now we use:
     *
     * - on XDG standard compliant Unixes:
     *   - config:  ~/.config/freecol
     *   - data:    ~/.local/share/freecol
     *   - logging: ~/.cache/freecol
     * - on Mac:
     *   - config:  ~/Library/Preferences/freecol
     *   - else:    ~/Library/Application Support/freecol
     * - on Windows:
     *   - everything in \<default directory\>/freecol
     * - otherwise use what was there
     *
     * Note: the freecol data directory is set independently and earlier
     * in initialization than this routine.
     *
     * FIXME: Should the default location of the main user and data
     * directories be determined by the installer?
     *
     * @return A message key to use to create a message to the user
     *     possibly describing any directory migration, or null if
     *     nothing to say.
     */
    public static synchronized String setUserDirectories() {
        if (userConfigDirectory != null
            && !isGoodDirectory(userConfigDirectory))
            userConfigDirectory = null;
        if (userDataDirectory != null
            && !isGoodDirectory(userDataDirectory))
            userDataDirectory = null;
        if (userCacheDirectory != null
            && !isGoodDirectory(userCacheDirectory))
            userCacheDirectory = null;
        File dirs[] = { userConfigDirectory, userDataDirectory,
                        userCacheDirectory };

        // If the CL-specified directories are valid, all is well.
        // Check for OSX next because it is a Unix.
        int migrate = (dirs[0] != null && isGoodDirectory(dirs[0])
            && dirs[1] != null && isGoodDirectory(dirs[1])
            && dirs[2] != null && isGoodDirectory(dirs[2])) ? 1
            : (onMacOSX()) ? getMacOSXDirs(dirs)
            : (onUnix()) ? getXDGDirs(dirs)
            : (onWindows()) ? getWindowsDirs(dirs)
            : -1;
        File oldDir = getOldUserDirectory();
        if (migrate < 0) {
            if (oldDir == null) return "main.userDir.fail";
            dirs[0] = dirs[1] = dirs[2] = oldDir; // Do not migrate.
            migrate = 1;
        }

        // Only set user directories if not already overridden at the
        // command line, and do not migrate in such cases.
        if (userConfigDirectory == null) {
            userConfigDirectory = dirs[0];
        } else migrate = 1;
        if (userDataDirectory == null) {
            userDataDirectory = dirs[1];
        } else migrate = 1;
        if (userCacheDirectory == null) {
            userCacheDirectory = dirs[2];
        } else migrate = 1;
        if (migrate == 0 && oldDir != null) {
            copyIfFound(oldDir, "classic", userConfigDirectory);
            copyIfFound(oldDir, "freecol", userConfigDirectory);
            copyIfFound(oldDir, "save",    userDataDirectory);
            copyIfFound(oldDir, "mods",    userDataDirectory);
        }

        if (logFilePath == null) {
            logFilePath = getUserCacheDirectory() + SEPARATOR + LOG_FILE;
        }

        if (saveDirectory == null) {
            saveDirectory = new File(getUserDataDirectory(), SAVE_DIRECTORY);
            if (!insistDirectory(saveDirectory)) return "main.userDir.fail";
        }
        deriveAutosaveDirectory();

        userModsDirectory = new File(getUserDataDirectory(), MODS_DIRECTORY);
        if (!insistDirectory(userModsDirectory)) userModsDirectory = null;

        return (migrate > 0) ? null
            : (onMacOSX())  ? "main.userDir.macosx"
            : (onUnix())    ? "main.userDir.unix"
            : (onWindows()) ? "main.userDir.windows"
            : null;
    }

    // Directory accessors.
    // Where there are supported command line arguments there will also
    // be a mutator.

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
     * Gets the base client options file.
     *
     * @return The base client options file.
     */
    public static File getBaseClientOptionsFile() {
        return new File(getBaseDirectory(), BASE_CLIENT_OPTIONS_FILE_NAME);
    }

    /**
     * Gets the file containing the client options.
     *
     * @return The client options file, if any.
     */
    public static File getClientOptionsFile() {
        return (clientOptionsFile != null) ? clientOptionsFile
            : getOptionsFile(CLIENT_OPTIONS_FILE_NAME);
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
     * Gets the high score file.
     *
     * @return The high score file, if it exists.
     */
    public static File getHighScoreFile() {
        return new File(getUserDataDirectory(), HIGH_SCORE_FILE);
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
     * Gets the directory containing the predefined maps.
     *
     * @return The predefined maps.
     */
    public static File getMapsDirectory() {
        return new File(getDataDirectory(), MAPS_DIRECTORY);
    }

    /**
     * Gets the directory where the user options are saved.
     *
     * @return The directory to save user options in.
     */
    public static File getOptionsDirectory() {
        File dir = new File(getUserConfigDirectory(), FreeCol.getTC());
        return (insistDirectory(dir)) ? dir : null;
    }

    /**
     * Get an options file from the options directory.
     *
     * @param name The name of the file within the options directory.
     * @return The options file.
     */
    public static File getOptionsFile(String name) {
        File dir = getOptionsDirectory();
        return (dir == null) ? null : new File(dir, name);
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
     * Gets the save game file.
     *
     * @return The save game file.
     */
    public static File getSavegameFile() {
        return savegameFile;
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
        savegameFile = file;
        File parent = file.getParentFile();
        if (parent == null) parent = new File(".");
        saveDirectory = parent;
        deriveAutosaveDirectory();
        return true;
    }

    /**
     * Gets the most recently saved game file, or <b>null</b>.  (This
     * may be either from a recent arbitrary user operation or an
     * autosave function.)
     *
     *  @return The recent save game file
     */
    public static File getLastSaveGameFile() {
        File lastSave = null;
        for (File directory : new File[] {
                FreeColDirectories.getSaveDirectory(),
                FreeColDirectories.getAutosaveDirectory() }) {
            for (File savegame : directory.listFiles(FreeColSavegameFile.getFileFilter())) {
                if (lastSave == null
                    || savegame.lastModified() > lastSave.lastModified()) {
                    lastSave = savegame;
                }
            }
        }
        return lastSave;
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
     * Get the map file to start from, if any.
     *
     * @return The start map file if any.
     */
    public static File getStartMapFile() {
        return new File(getAutosaveDirectory(), START_MAP_NAME);
    }

    /**
     * Gets the user cache directory, that is the directory under which
     * the transient user files live.
     *
     * @return The user cache directory.
     */
    public static File getUserCacheDirectory() {
        return userCacheDirectory;
    }

    /**
     * Sets the user cache directory, that is the directory under which
     * the user-specific cache files live.
     *
     * @param path The path to the new user cache directory.
     * @return Null on success, an error message key on failure.
     */
    public static String setUserCacheDirectory(String path) {
        File dir = new File(path);
        String ret = checkDir(dir);
        if (ret == null) userCacheDirectory = dir;
        return ret;
    }

    /**
     * Gets the user config directory, that is the directory under which
     * the user-specific config files live.
     *
     * @return The user config directory.
     */
    public static File getUserConfigDirectory() {
        return userConfigDirectory;
    }

    /**
     * Sets the user config directory, that is the directory under which
     * the user-specific config files live.
     *
     * @param path The path to the new user config directory.
     * @return Null on success, an error message key on failure.
     */
    public static String setUserConfigDirectory(String path) {
        File dir = new File(path);
        String ret = checkDir(dir);
        if (ret == null) userConfigDirectory = dir;
        return ret;
    }

    /**
     * Gets the user data directory, that is the directory under which
     * the user-specific data lives.
     *
     * @return The user data directory.
     */
    public static File getUserDataDirectory() {
        return userDataDirectory;
    }

    /**
     * Sets the main user data directory, creating it if necessary.
     * If pre-existing, it must be a directory, readable and writable.
     *
     * @param path The path to the new main data user directory, or
     *     null to apply the default.
     * @return Null on success, an error message key on failure.
     */
    public static String setUserDataDirectory(String path) {
        File dir = new File(path);
        String ret = checkDir(dir);
        if (ret == null) userDataDirectory = dir;
        return ret;
    }

    /**
     * Gets the user mods directory.
     *
     * @return The directory where user mods are located, or null if none.
     */
    public static File getUserModsDirectory() {
        return userModsDirectory;
    }
}
