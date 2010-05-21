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
import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColModFile.ModInfo;

/**
 * Contains methods for getting a list of available mods.
 */
public class Mods {

    public static final FileFilter MOD_FILTER =
        new FileFilter() {
            public boolean accept(File f) {
                final String name = f.getName();
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
     * Gets all available mods.
     * @return A list of mods.
     */
    public static List<FreeColModFile> getMods() {
        final File[] fmods = FreeCol.getModsDirectory().listFiles(MOD_FILTER);
        final List<FreeColModFile> list = new ArrayList<FreeColModFile>(fmods.length);
        for (File f : fmods) {
            list.add(new FreeColModFile(f.getName()));
        }
        return list;
    }
    
    /**
     * Gets info about all available mods.
     * @return A list of objects describing available mods.
     */
    public static List<ModInfo> getModInfos() {
        final List<FreeColModFile> mods = getMods();
        final List<ModInfo> modInfos = new ArrayList<ModInfo>(mods.size());
        for (FreeColModFile mod : mods) {
            modInfos.add(mod.getModInfo());
        }
        return modInfos;
    }
}
