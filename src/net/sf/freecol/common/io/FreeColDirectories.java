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
package net.sf.freecol.common.io;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.filechooser.FileSystemView;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.util.OSUtils;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.OSUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import static net.sf.freecol.common.util.Utils.*;


/**
 * Simple container for the freecol file and directory structure model.
 */
public class FreeColDirectories {

    // No logger!  Many of these routines are called before logging is
    // initialized.

    private static final Comparator<File> fileModificationComparator
        = Comparator.comparingLong(File::lastModified);

    private static final Comparator<File> fileNameComparator
        = Comparator.comparing(File::getName);


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

    private static final String LOG_COMMS_FILE_NAME = "FreeColComms.log";

    private static final String MAPS_DIRECTORY = "maps";

    private static final String MESSAGE_FILE_PREFIX = "FreeColMessages";

    private static final String MESSAGE_FILE_SUFFIX = ".properties";

    private static final String MODS_DIRECTORY = "mods";

    private static final String MOD_FILE_SUFFIX = ".fmd";

    private static final String MOD_MESSAGE_FILE_PREFIX = "ModMessages";

    private static final String PLURALS_FILE_NAME = "plurals.xml";

    private static final String RESOURCE_FILE_PREFIX = "resources";

    private static final String RESOURCE_FILE_SUFFIX = ".properties";

    private static final String RULES_DIRECTORY = "rules";

    private static final String SAVE_DIRECTORY = "save";

    private static final String SPECIFICATION_FILE_NAME = "specification.xml";

    private static final String START_MAP_NAME = "startMap.fsg";

    private static final String SEPARATOR
        = System.getProperty("file.separator");

    private static final String TC_FILE_SUFFIX = ".ftc";

    private static final String USER_MAPS_DIRECTORY = "maps";

    private static final String ZIP_FILE_SUFFIX = ".zip";
    
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

    public static final String MOD_DESCRIPTOR_FILE_NAME = "mod.xml";

    /** Predicate to filter suitable candidates to be made into mods. */
    private static final Predicate<File> modFileFilter = f ->
        fileAnySuffix(f, MOD_FILE_SUFFIX, ZIP_FILE_SUFFIX)
            || directoryAllPresent(f, MOD_DESCRIPTOR_FILE_NAME);

    /**
     * Predicate to select readable files that look like saved games.
     * Public for SaveGameValidator.
     */
    public static final Predicate<File> saveGameFilter = f ->
        f.isFile() && f.canRead()
            && f.getName().endsWith("." + FreeCol.FREECOL_SAVE_EXTENSION);

    /**
     * Predicate to select readable files that look like maps.
     */
    private static final Predicate<File> mapFilter = f ->
        f.isFile() && f.canRead()
            && f.getName().endsWith("." + FreeCol.FREECOL_MAP_EXTENSION);

    /** Predicate to filter suitable candidates to be made into TCs. */
    private static final Predicate<File> tcFileFilter = f ->
        fileAnySuffix(f, TC_FILE_SUFFIX, ZIP_FILE_SUFFIX)
            || directoryAllPresent(f, MOD_DESCRIPTOR_FILE_NAME,
                                   SPECIFICATION_FILE_NAME);

    /** Posix file mode 0700. */
    private static final Set<PosixFilePermission> mode0700
        = makeUnmodifiableSet(PosixFilePermission.OWNER_READ,
                              PosixFilePermission.OWNER_WRITE,
                              PosixFilePermission.OWNER_EXECUTE);
        
    
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

    /** The comms writer. */
    private static AtomicReference<Writer> commsWriter
        = new AtomicReference<Writer>(null);

    
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
     * @param dir The {@code File} that must be a usable directory.
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
     * Is the specified file a writable directory?
     *
     * @param f The {@code File} to check.
     * @return True if the file is a writable directory.
     */
    private static boolean isGoodDirectory(File f) {
        return f != null && f.exists() && f.isDirectory() && f.canWrite();
    }

    /**
     * Create the given directory if it does not exist, otherwise expect
     * it to be writable.
     *
     * @param dir The {@code File} specifying the required directory.
     * @return The required directory, or null on failure.
     */
    private static File requireDirectory(File dir) {
        if (dir.exists()) {
            if (dir.isDirectory() && dir.canWrite()) return dir;
        } else {
            if (dir.mkdir()) {
                try {
                    Files.setPosixFilePermissions(dir.toPath(), mode0700);
                } catch (IOException|UnsupportedOperationException ex) {
                    // Just log, error is not fatal
                    System.err.println("Failed to change permissions of "
                        + dir.getPath());
                }
                return dir;
            }
        }
        return null;
    }

    private static StringTemplate bad() {
        return StringTemplate.key("main.userDir.fail");
    }

    private static StringTemplate badHome() {
        return StringTemplate.key("main.userDir.noHome");
    }

    private static StringTemplate badDir(File d) {
        return StringTemplate.template("main.userDir.badDir")
            .addName("%name%", d.getPath());
    }

    private static StringTemplate badConfig(File f) {
        return StringTemplate.template("main.userDir.badConfig")
            .addName("%name%", f.getPath());
    }

    private static StringTemplate badData(File f) {
        return StringTemplate.template("main.userDir.badData")
            .addName("%name%", f.getPath());
    }

    private static StringTemplate badCache(File f) {
        return StringTemplate.template("main.userDir.badCache")
            .addName("%name%", f.getPath());
    }

    /**
     * Get directories for XDG compliant systems.
     *
     * XDG say: 
     *   If, when attempting to write a file, the destination
     *   directory is non-existant an attempt should be made to create
     *   it with permission 0700.
     *
     * @param dirs An array of {@code File} to be filled in with the
     *     XDG directory if it is present or created.
     * @return Null on success, an error message on error.
     */
    private static StringTemplate getXDGDirs(File[] dirs) {
        File home = getUserDefaultDirectory();
        if (home == null) return badHome();

        String env = System.getenv(XDG_CONFIG_HOME_ENV);
        File d = (env != null) ? new File(env)
            : new File(home, XDG_CONFIG_HOME_DEFAULT);
        File xd;
        if ((xd = requireDirectory(d)) == null) return badDir(d);
        File f = new File(xd, FREECOL_DIRECTORY);
        if ((d = requireDirectory(f)) == null) return badConfig(f);
        dirs[0] = d;

        env = System.getenv(XDG_DATA_HOME_ENV);
        d = (env != null) ? new File(env)
            : new File(home, XDG_DATA_HOME_DEFAULT);
        if ((xd = requireDirectory(d)) == null) return badDir(d);
        f = new File(xd, FREECOL_DIRECTORY);
        if ((d = requireDirectory(f)) == null) return badData(f);
        dirs[1] = d;

        env = System.getenv(XDG_CACHE_HOME_ENV);
        d = (env != null) ? new File(env)
            : new File(home, XDG_CACHE_HOME_DEFAULT);
        if ((xd = requireDirectory(d)) == null) return badDir(d);
        f = new File(xd, FREECOL_DIRECTORY);
        if ((d = requireDirectory(f)) == null) return badCache(f);
        dirs[2] = d;

        return null;
    }

    /**
     * Get FreeCol directories for MacOSX.
     *
     * No separate cache directory here.
     *
     * @param dirs An array of {@code File} to be filled in with the
     *     MacOSX freecol directories.
     * @return Null on success, an error message on failure.
     */
    private static StringTemplate getMacOSXDirs(File[] dirs) {
        File home = getUserDefaultDirectory();
        if (home == null) return badHome();
        File libDir = new File(home, "Library");
        if (!isGoodDirectory(libDir)) return badDir(libDir);

        File prefsDir = new File(libDir, "Preferences");
        if (!isGoodDirectory(prefsDir)) return badDir(prefsDir);
        File d = new File(prefsDir, FREECOL_DIRECTORY);
        if (!isGoodDirectory(d)) return badConfig(d);
        dirs[0] = d;

        File appsDir = new File(libDir, "Application Support");
        if (!isGoodDirectory(appsDir)) return badDir(appsDir);
        d = new File(appsDir, FREECOL_DIRECTORY);
        File d2 = requireDirectory(d);
        if (d2 == null) return badData(d);
        dirs[1] = dirs[2] = d2;

        return null;
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
     * @param dirs An array of {@code File} to be filled in with the
     *     Windows freecol directories.
     * @return Null on success, an error message on failure.
     */
    private static StringTemplate getWindowsDirs(File[] dirs) {
        File home = getUserDefaultDirectory();
        if (home == null) return badHome();

        File d = new File(home, FREECOL_DIRECTORY);
        File d2 = requireDirectory(d);
        if (d2 == null) return badDir(d);
        dirs[0] = dirs[1] = dirs[2] = d;
        return null;
    }

    /**
     * Insist that a directory either already exists, or is created.
     *
     * @param file A {@code File} specifying where to make the directory.
     * @return True if the directory is now there.
     */
    private static boolean insistDirectory(File file) {
        boolean ret;
        if (file.exists()) {
            if (!(ret = file.isDirectory())) {
                System.err.println("Could not create directory "
                    + file.getPath() + " because a non-directory with that name is already there.");
            }
        } else {
            try {
                ret = file.mkdir();
            } catch (Exception e) {
                ret = false;
                System.err.println("Could not make directory " + file.getPath()
                    + ": " + e.getMessage());
            }
        }
        return ret;
    }

    /**
     * Safely derive a subdirectory of a root.
     *
     * @param root The root directory.
     * @param subdir The name of the subdirectory to find or create.
     * @return The directory found, or null on error.
     */
    private static File deriveDirectory(File root, String subdir) {
        File dir;
        return (isGoodDirectory(root)
            && insistDirectory(dir = new File(root, subdir))) ? dir : null;
    }

    /**
     * Derive the directory for the autosave files from the save directory.
     *
     * @return The new autosave directory, or null if not possible.
     */
    private static File deriveAutosaveDirectory() {
        return deriveDirectory(saveDirectory, AUTOSAVE_DIRECTORY);
    }

    /**
     * Collect files from a directory that match a predicate.
     *
     * @param dir The directory to load from.
     * @param pred A {@code Predicate} to match files with.
     * @return A list of {@code File}s.
     */
    private static List<File> collectFiles(File dir, Predicate<File> pred) {
        return transform(fileStream(dir), pred, Function.<File>identity(),
                         fileNameComparator);
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
        if (!getBaseDirectory().exists()) {
            return "Base data directory missing from: " + path;
        }
        return null;
    }

    /**
     * Set the user directories for the current user.
     *
     * - on XDG standard compliant Unixes:
     *   - config:  ~/.config/freecol
     *   - data:    ~/.local/share/freecol
     *   - logging: ~/.cache/freecol
     * - on Mac:
     *   - config:  ~/Library/Preferences/freecol
     *   - else:    ~/Library/Application Support/freecol
     * - on Windows:
     *   - everything in <em>default directory</em>/freecol
     *
     * Note: the freecol data directory is set independently and earlier
     * in initialization than this routine.
     *
     * FIXME: Should the default location of the main user and data
     * directories be determined by the installer?
     *
     * @return Null on success, otherwise an error message template.
     */
    public static synchronized StringTemplate setUserDirectories() {
        // Find the OS-specific directories.
        // Check OSX before XDG because OSX is still unix-like.
        File[] dirs = { null, null, null };
        StringTemplate err = (onMacOSX()) ? getMacOSXDirs(dirs)
            : (onUnix()) ? getXDGDirs(dirs)
            : (onWindows()) ? getWindowsDirs(dirs)
            : bad();
        if (err != null) return err;

        // Override directories if not set or not valid.
        if (userConfigDirectory == null
            || !isGoodDirectory(userConfigDirectory)) {
            userConfigDirectory = dirs[0];
        }
        if (userDataDirectory == null
            || !isGoodDirectory(userDataDirectory)) {
            userDataDirectory = dirs[1];
        }
        if (userCacheDirectory == null
            || !isGoodDirectory(userCacheDirectory)) {
            userCacheDirectory = dirs[2];
        }

        // Derive the other directories and files
        if (logFilePath == null) {
            logFilePath = getUserCacheDirectory() + SEPARATOR + LOG_FILE;
        }
        if (saveDirectory == null) {
            saveDirectory = new File(getUserDataDirectory(), SAVE_DIRECTORY);
            if (!insistDirectory(saveDirectory)) return badDir(saveDirectory);
        }
        // TODO: Drop trace when BR#3097b is settled
        File dir = deriveAutosaveDirectory();
        System.err.println("Autosave directory initialized to " + dir);
        setAutosaveDirectory(dir);
        userModsDirectory = new File(getUserDataDirectory(), MODS_DIRECTORY);
        if (!insistDirectory(userModsDirectory)) userModsDirectory = null;

        return null;
    }

    /**
     * Remove disallowed parts of a user supplied file name.
     *
     * @param fileName The input file name.
     * @return A sanitized file name.
     */
    private static String sanitize(String fileName) {
        List<String> strings = new ArrayList<String>();
        for (int i = 0; i < fileName.length(); i++) {
            String s = fileName.substring(i, i+1);
            if (SEPARATOR.equals(s)) continue;
            strings.add(s);
        }
        return join("", strings);
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
        // Re-establish the autosave directory if it has gone away
        if (!isGoodDirectory(autosaveDirectory)) {
            File dir = deriveAutosaveDirectory();
            // TODO: Drop trace when BR#3097b is settled
            System.err.println("Autosave directory " + autosaveDirectory
                + " is broken, replacing with " + dir);
            setAutosaveDirectory(dir);
        }
        return autosaveDirectory;
    }

    /**
     * Set the autosave directory.
     *
     * @param dir The new autosave directory.
     */
    private static void setAutosaveDirectory(File dir) {
        autosaveDirectory = dir;
    }

    /**
     * Get a specific autosave file.
     *
     * @param fileName The name of the file.
     * @return The {@code File} found, or null on error.
     */
    public static File getAutosaveFile(String fileName) {
        File dir = getAutosaveDirectory();
        return (dir == null) ? null : new File(dir, sanitize(fileName));
    }

    /**
     * Get the autosave files.
     *
     * @param prefix The autosave file prefix.
     * @param pred A {@code Predicate} to select files with.
     * @return A list of of autosaved {@code File}s.
     */
    private static List<File> getAutosaveFiles(String prefix,
                                               Predicate<File> pred) {
        final String suffix = "." + FreeCol.FREECOL_SAVE_EXTENSION;
        final File asd = getAutosaveDirectory();
        final Predicate<File> fullPred = pred.and(f ->
            f.getName().startsWith(prefix) && f.getName().endsWith(suffix));
        return (asd == null) ? Collections.emptyList()
            : collectFiles(asd, fullPred);
    }

    /**
     * Remove out of date autosaves. This only removed generic autosaves, not
     * the last-turn autosave, which can be useful for continuing the game on
     * the next play-session.
     *
     * @param prefix The autosave file prefix.
     * @param excludeSuffixes Only files not ending with any of these prefixes
     *     will be removed.
     * @param validDays Only files older than this amount of days will
     *     be removed.
     * @return A suitable log message, or null if nothing was deleted.
     */
    public static String removeOutdatedAutosaves(String prefix,
        List<String> excludeSuffixes, long validDays) {
        if (validDays <= 0L) return null;
        final long validMS = 1000L * 24L * 60L * 60L * validDays; // days to ms
        final long timeNow = now();
        final Predicate<File> outdatedPred = f -> 
            f.lastModified() + validMS < timeNow;
        final String extension = "." + FreeCol.FREECOL_SAVE_EXTENSION;
        final List<String> ex = transform(excludeSuffixes, alwaysTrue(),
                                          s -> sanitize(s));
        final Predicate<File> suffixPred = f ->
            ex.stream().noneMatch(suf -> f.getName().endsWith(suf + extension));

        List<File> files = getAutosaveFiles(sanitize(prefix),
                                            outdatedPred.and(suffixPred)); 
        if (files.isEmpty()) return null;
        deleteFiles(files);
        StringBuilder sb = new StringBuilder();
        sb.append("Deleted outdated (> ").append(validDays)
            .append(" old) autosave/s: ");
        for (File f : files) sb.append(' ').append(f.getPath());
        return sb.toString();        
    }

    /**
     * Remove all autosave files.
     *
     * @param prefix The autosave file prefix.
     * @return A suitable log message, or null if nothing was deleted.
     */
    public static String removeAutosaves(String prefix) {
        List<File> files = getAutosaveFiles(sanitize(prefix), alwaysTrue());
        if (files.isEmpty()) return null;
        deleteFiles(files);
        StringBuilder sb = new StringBuilder();
        sb.append("Deleted autosave/s: ");
        for (File f : files) sb.append(' ').append(f.getPath());
        return sb.toString();        
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
     * Get a compatibility file.
     *
     * Not sanitizing the file name as it is fixed in all current uses.
     *
     * @param fileName The name of the compatibility file.
     * @return The {@code File} found.
     */
    public static File getCompatibilityFile(String fileName) {
        return new File(getBaseDirectory(), fileName);
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
     * Get the debug-run save file.
     *
     * @return The save {@code File}, if any.
     */
    public static File getDebugRunSaveFile() {
        return new File(System.getProperty("user.dir"),
                        FreeColDebugger.getDebugRunSave());
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
     * Get the contents of the log file.
     *
     * @return A string of the log file contents, or null on error.
     */
    public static String getLogFileContents() {
        return getUTF8Contents(new File(getLogFilePath()));
    }

    /**
     * Get a list of candidate message file names for a given locale.
     *
     * @param locale The {@code Locale} to generate file names for.
     * @return A list of message {@code File}s.
     */
    public static List<File> getI18nMessageFileList(Locale locale) {
        List<File> result = new ArrayList<>();
        File i18nDirectory = getI18nDirectory();
        for (String name : getMessageFileNameList(locale)) {
            File f = new File(i18nDirectory, name);
            if (f.canRead()) result.add(f);
        }
        return result;
    }

    /**
     * Get the i18n plurals file.
     *
     * @return The plurals {@code File}.
     */
    public static File getI18nPluralsFile() {
        return new File(getI18nDirectory(), PLURALS_FILE_NAME);
    }

    /**
     * Get a list of all the supported language identifiers.
     *
     * @return A list of language identifiers for which there is an
     *     i18n-message file.
     */
    public static List<String> getLanguageIdList() {
        File[] files = getI18nDirectory().listFiles();
        return (files == null) ? Collections.<String>emptyList()
            : transform(files, File::canRead, f -> getLanguageId(f));
    }
    
    /**
     * If this a messages file, work out which language identifier it
     * belongs to.
     *
     * @param file The {@code File} to test.
     * @return The language identifier found, or null on failure.
     */
    public static String getLanguageId(File file) {
        if (file == null) return null;
        final String name = file.getName();
        // Make sure it is at least a messages file.
        if (name == null
            || !name.startsWith(MESSAGE_FILE_PREFIX)
            || !name.endsWith(MESSAGE_FILE_SUFFIX)) return null;
        String languageId = name.substring(MESSAGE_FILE_PREFIX.length(),
            name.length() - MESSAGE_FILE_SUFFIX.length());
        return ("".equals(languageId)) ? "en" // FreeColMessages.properties
            : ("_qqq".equals(languageId)) ? null // qqq is explanations only
            : (languageId.startsWith("_")) ? languageId.substring(1)
            : languageId;
    }

    /**
     * Gets a list containing the names of all possible message files
     * for a locale.
     *
     * @param prefix The file name prefix.
     * @param suffix The file name suffix.
     * @param locale The {@code Locale} to generate file names for.
     * @return A list of candidate file names.
     */
    public static List<String> getLocaleFileNames(String prefix,
                                                  String suffix,
                                                  Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        List<String> result = new ArrayList<>(4);

        if (!language.isEmpty()) language = "_" + language;
        if (!country.isEmpty()) country = "_" + country;
        if (!variant.isEmpty()) variant = "_" + variant;

        result.add(prefix + suffix);
        String filename = prefix + language + suffix;
        if (!result.contains(filename)) result.add(filename);
        filename = prefix + language + country + suffix;
        if (!result.contains(filename)) result.add(filename);
        filename = prefix + language + country + variant + suffix;
        if (!result.contains(filename)) result.add(filename);
        return result;
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
     * Gets a new log writer.
     *
     * @return The log {@code Writer}.
     * @exception FreeColException if there was a problem creating the writer.
     */
    public static Writer getLogWriter() throws FreeColException {
        String path = getLogFilePath();
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new FreeColException("Log file \"" + path
                    + "\" could not be created.");
            } else if (file.isFile()) {
                deleteFile(file);
            }
        }
        try {
            if (!file.createNewFile()) {
                throw new FreeColException("Log file \"" + path
                    + "\" creation failed.");
            }
        } catch (IOException|SecurityException e) {
            throw new FreeColException("Log file \"" + path
                + "\" could not be created.", e);
        }
        if (!file.canWrite()) {
            throw new FreeColException("Can not write in log file \""
                + path + "\".");
        }
        Writer writer = getFileUTF8Writer(file);
        if (writer == null) {
            throw new FreeColException("Can not create writer for log file \""
                + path + "\".");
        }
        return writer;
    }

    /**
     * Get a writer for the comms log file.
     *
     * @return A suitable {@code Writer}.
     * @exception FreeColException on error.
     */
    public static Writer getLogCommsWriter() throws FreeColException {
        Writer writer = commsWriter.get();
        if (writer == null) {
            File file = new File(getUserCacheDirectory(), LOG_COMMS_FILE_NAME);
            if (file.exists()) deleteFile(file);
            writer = getFileUTF8AppendWriter(file);
            if (writer == null) {
                throw new FreeColException("Can not create writer for comms log file: " + file.getPath());
            }
            commsWriter.set(writer);
        }
        return writer;
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
     * Get the map files.
     *
     * @return A list of map files which may be empty.
     */
    public static List<File> getMapFileList() {
        List<File> ret = new ArrayList<>();
        for (File f : new File[] { getMapsDirectory(), getUserMapsDirectory() }) {
            if (f != null && f.isDirectory()) {
                ret.addAll(collectFiles(f, mapFilter));
            }
        }
        return ret;
    }

    /**
     * Get the message file names for a given locale.
     *
     * @param locale The {@code Locale} to generate names for.
     * @return A list of potential message file names.
     */
    public static List<String> getMessageFileNameList(Locale locale) {
        return getLocaleFileNames(MESSAGE_FILE_PREFIX, MESSAGE_FILE_SUFFIX,
                                  locale);
    }

    /**
     * Get the mod message file names for a given locale.
     *
     * @param locale The {@code Locale} to generate names for.
     * @return A list of potential mod message file names.
     */
    public static List<String> getModMessageFileNames(Locale locale) {
        return getLocaleFileNames(MOD_MESSAGE_FILE_PREFIX, MESSAGE_FILE_SUFFIX,
                                  locale);
    }

    /**
     * Get a list of the standard and current user mod files.
     *
     * @return A list of mod {@code File}s.
     */
    public static List<File> getModFileList() {
        List<File> ret = new ArrayList<>();
        ret.addAll(collectFiles(getStandardModsDirectory(), modFileFilter));
        ret.addAll(collectFiles(getUserModsDirectory(), modFileFilter));
        return ret;
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
     * @param fileName The name of the file within the options directory.
     * @return The options file.
     */
    public static File getOptionsFile(String fileName) {
        File dir = getOptionsDirectory();
        return (dir == null) ? null : new File(dir, sanitize(fileName));
    }

    /**
     * Get a list of candidate resource file names for a given locale.
     *
     * @return A list of resource file names.
     */
    public static List<String> getResourceFileNames() {
        return getLocaleFileNames(RESOURCE_FILE_PREFIX, RESOURCE_FILE_SUFFIX,
                                  Locale.getDefault());
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
     * Gets the save game files in a given directory.
     *
     * @param directory The base directory, or the default locations if null.
     * @return A stream of save game {@code File}s.
     */
    public static Stream<File> getSavegameFiles(File directory) {
        return (directory == null)
            ? flatten(Stream.of(FreeColDirectories.getSaveDirectory(),
                                FreeColDirectories.getAutosaveDirectory()),
                      d -> fileStream(d, saveGameFilter))
            : fileStream(directory, saveGameFilter);
    }

    /**
     * Gets the save game files in a given directory.
     *
     * @param directory The base directory, or the default locations if null.
     * @return A list of save game {@code File}s.
     */
    public static List<File> getSavegameFileList(File directory) {
        return toList(getSavegameFiles(directory));
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
        // TODO: Drop trace when BR#3097b is settled
        File dir = deriveAutosaveDirectory();
        System.err.println("Autosave directory " + autosaveDirectory
                + " follows saveDirectory change to " + dir);
        setAutosaveDirectory(dir);
        return true;
    }

    /**
     * Gets the most recently saved game file, or <b>null</b>.  (This
     * may be either from a recent arbitrary user operation or an
     * autosave function.)
     *
     * @return The recent save game {@code File}, or null if not found.
     */
    public static File getLastSaveGameFile() {
        return maximize(getSavegameFiles(null),
                        fileModificationComparator);
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
     * @return The start map file, or null on error.
     */
    public static File getStartMapFile() {
        File dir = getAutosaveDirectory();
        return (dir == null) ? null : new File(dir, START_MAP_NAME);
    }

    /**
     * Get all available rules files.
     *
     * @return A list of {@code File}s containing rulesets.
     */
    public static List<File> getTcFileList() {
        return collectFiles(getRulesDirectory(), tcFileFilter);
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
     * Get the user maps directory.
     *
     * @return The directory to save user maps to, or null if none.
     */
    public static File getUserMapsDirectory() {
        return deriveDirectory(getUserDataDirectory(), USER_MAPS_DIRECTORY);
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
