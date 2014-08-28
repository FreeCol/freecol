/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

import javax.swing.filechooser.FileFilter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;


/**
 * A simple file filter with a description.
 */
public class FreeColFileFilter extends FileFilter {

    private final String[] extensions;
    private final boolean allowSubdirs;
    private final String description;

    /**
     * Visible predefined constant filter for selected saved games or
     * subdirectories.
     */
    public static final FileFilter freeColSaveDirectoryFilter
        = new FreeColFileFilter(FreeCol.FREECOL_SAVE_EXTENSION, true,
                                "filter.savedGames");


    /**
     * Create a new FreeColFileFilter.
     *
     * @param extension An acceptable extension.
     * @param allowSubdirs If true accept subdirectories.
     * @param description A description for the filter.
     */
    public FreeColFileFilter(String extension, boolean allowSubdirs,
                             String description) {
        this.extensions = new String[] { extension };
        this.allowSubdirs = allowSubdirs;
        this.description = Messages.message(description);
    }


    // Implement FileFilter

    /**
     * {@inheritDoc}
     */
    public boolean accept(File f) {
        if (allowSubdirs && f.isDirectory()) return true;
        if (!f.isFile()) return false;
        for (String x : extensions) {
            if (f.getName().endsWith(x) && f.getName().length() > x.length())
                return true;
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return description;
    }
}
