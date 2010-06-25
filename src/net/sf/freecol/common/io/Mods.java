/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColModFile.ModInfo;

/**
 * Contains methods for getting a list of available mods.
 */
public class Mods {

    private static final Logger logger = Logger.getLogger(Mods.class.getName());

    public static final FileFilter MOD_FILTER =
        new FileFilter() {
            public boolean accept(File f) {
                final String name = f.getName();
                if (".".equals(f.getName().substring(0, 1))) {
                    // Ignore `hidden' files.
                    return false;
                }
                if (f.isDirectory()) {
                    return true;
                }
                for (String ending : FreeColModFile.FILE_ENDINGS) {
                    if (name.endsWith(ending)) {
                        return true;
                    }
                }
                return false;
            }
        };

    /**
     * Gets a mod file from a file (possibly a directory).
     *
     * @param file The <code>File</code> to test.
     * @return A <code>FreeColModFile</code> if the file contains a mod,
     *     or null if it did not.
     */
    public static FreeColModFile getModFile(File file) {
        try {
            // The constructor will throw on IO problems, and
            // getModDescriptor will throw if there is no valid
            // mod.xml.  That is all we require ATM to consider this a
            // valid mod.
            FreeColModFile fcmf = new FreeColModFile(file);
            return (fcmf.getModDescriptor() == null) ? null : fcmf;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Loads all valid mods from a specified directory.
     *
     * @param directory The directory to load from.
     * @return A list of valid mods.
     */
    private static List<FreeColModFile> getDirectoryMods(File directory) {
        List<FreeColModFile> mods = new ArrayList<FreeColModFile>();
        if (directory != null && directory.isDirectory()) {
            for (File f : directory.listFiles(MOD_FILTER)) {
                FreeColModFile fcmf = getModFile(f);
                if (fcmf != null) {
                    mods.add(fcmf);
                } else {
                    logger.warning("Failed to load mod from: " + f.getName());
                }
            }
        }
        return mods;
    }

    /**
     * Gets all available mods.
     * User mods before standard mods to allow user override.
     *
     * @return A list of <code>FreeColModFile</code>s contain mods.
     */
    public static List<FreeColModFile> getAllMods() {
        List<FreeColModFile> mods
            = getDirectoryMods(FreeCol.getUserModsDirectory());
        mods.addAll(getDirectoryMods(FreeCol.getStandardModsDirectory()));
        return mods;
    }
}
