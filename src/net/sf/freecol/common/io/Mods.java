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

package net.sf.freecol.common.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;

/**
 * Contains methods for getting a list of available mods.
 */
public class Mods {

    private static final Logger logger = Logger.getLogger(Mods.class.getName());

    private static final Map<String, FreeColModFile> allMods =
        new HashMap<String, FreeColModFile>();

    public static final FileFilter MOD_FILTER =
        new FileFilter() {
            public boolean accept(File f) {
                final String name = f.getName();
                if (name.startsWith(".")) {
                    // Ignore `hidden' files.
                    return false;
                } else if (f.isDirectory()) {
                    return true;
                } else {
                    for (String ending : FreeColModFile.FILE_ENDINGS) {
                        if (name.endsWith(ending)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        };

    static {
        getDirectoryMods(FreeCol.getUserModsDirectory());
        getDirectoryMods(FreeCol.getStandardModsDirectory());
    }


    /**
     * Gets a mod file from a file (possibly a directory).
     *
     * @param file The <code>File</code> to test.
     * @return A <code>FreeColModFile</code> if the file contains a mod,
     *     or null if it did not.
     */
    public static FreeColModFile getModFile(File file) {
        try {
            /* The constructor will throw on IO problems, and if there
             * is no valid mod.xml.  That is all we require ATM to
             * consider this a valid mod.
             */
            return new FreeColModFile(file);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the <code>FreeColModFile</code> with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>FreeColModFile</code> value
     */
    public static FreeColModFile getModFile(String id) {
        return allMods.get(id);
    }

    /**
     * Loads all valid mods from a specified directory.
     *
     * @param directory The directory to load from.
     * @return A list of valid mods.
     */
    private static void getDirectoryMods(File directory) {
        if (directory != null && directory.isDirectory()) {
            for (File f : directory.listFiles(MOD_FILTER)) {
                FreeColModFile fcmf = getModFile(f);
                if (fcmf != null) {
                    allMods.put(fcmf.getId(), fcmf);
                } else {
                    logger.warning("Failed to load mod from: " + f.getName());
                }
            }
        }
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
     * Gets all available rules.
     *
     * @return A list of <code>FreeColModFile</code>s contain mods.
     */
    public static List<FreeColTcFile> getRuleSets() {
        List<FreeColTcFile> result = new ArrayList<FreeColTcFile>();
        File directory = FreeColTcFile.getRulesDirectory();
        for (File dir : directory.listFiles()) {
            if (dir.isDirectory()) {
                File modDescription = new File(dir, FreeColModFile.MOD_DESCRIPTOR_FILE);
                if (modDescription.exists()) {
                    try {
                        result.add(new FreeColTcFile(dir));
                    } catch(IOException e) {
                        logger.warning("Failed to create rule set " + dir);
                    }
                }
            }
        }
        return result;
    }
}
