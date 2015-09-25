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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.util.LogBuilder;


/**
 * Contains methods for getting a list of available mods.
 */
public class Mods {

    private static final Logger logger = Logger.getLogger(Mods.class.getName());

    /** A cache of all the mods. */
    private static final Map<String, FreeColModFile> allMods = new HashMap<>();


    /**
     * Loads all valid mods from a specified directory.
     *
     * @param directory The directory to load from.
     */
    private static void loadModDirectory(File directory) {
        if (directory != null && directory.isDirectory()) {
            LogBuilder lb = new LogBuilder(64);
            lb.add("In ", directory.getPath(), " found mod:");
            lb.mark();
            for (File f : directory.listFiles(FreeColModFile.getFileFilter())) {
                try {
                    FreeColModFile fcmf = new FreeColModFile(f);
                    allMods.put(fcmf.getId(), fcmf);
                    lb.add(" ", fcmf.getId());
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Bad mod in " + f.getPath(), e);
                }
            }
            if (lb.grew()) lb.log(logger, Level.INFO);
        }
    }

    /**
     * Require all mods to be loaded.  This must be delayed until
     * the mods directories are defined.
     *
     * User mods are loaded after standard mods to allow user override.
     */
    public static void loadMods() {
        loadModDirectory(FreeColDirectories.getStandardModsDirectory());
        loadModDirectory(FreeColDirectories.getUserModsDirectory());
    }

    /**
     * Gets the mod with the given object identifier.
     *
     * @param id The identifier of the mod to search for.
     * @return The <code>FreeColModFile</code> for the mod, or null if
     *     not found.
     */
    public static FreeColModFile getModFile(String id) {
        return allMods.get(id);
    }

    /**
     * Gets all available mods.
     * User mods before standard mods to allow user override.
     *
     * @return A list of <code>FreeColModFile</code>s contain mods.
     */
    public static Collection<FreeColModFile> getAllMods() {
        return allMods.values();
    }

    /**
     * Get a mod by id.
     *
     * @param id The mod file identifier to look for.
     * @return The <code>FreeColModFile</code> found, or null if none present.
     */
    public static FreeColModFile getFreeColModFile(String id) {
        return allMods.get(id);
    }

    /**
     * Gets all available rulesets.
     *
     * @return A list of <code>FreeColModFile</code>s containing rulesets.
     */
    public static List<FreeColTcFile> getRuleSets() {
        List<FreeColTcFile> result = new ArrayList<>();
        File directory = FreeColDirectories.getRulesDirectory();
        for (File f : directory.listFiles(FreeColTcFile.getFileFilter())) {
            try {
                result.add(new FreeColTcFile(f));
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to load rule set " + f, ioe);
            }
        }
        return result;
    }
}
