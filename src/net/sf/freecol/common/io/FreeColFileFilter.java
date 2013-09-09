/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.client.gui.i18n.Messages;


/**
 * A simple file filter with a description.
 */
public class FreeColFileFilter extends FileFilter {

    private final String extension1;
    private final String extension2;
    private final String description;

    
    public FreeColFileFilter(String extension, String descriptionMessage) {
        this.extension1 = extension;
        this.extension2 = "....";
        description = Messages.message(descriptionMessage);
    }

    public FreeColFileFilter(String extension1, String extension2,
                             String descriptionMessage) {
        this.extension1 = extension1;
        this.extension2 = extension2;
        description = Messages.message(descriptionMessage);
    }
    
    public boolean accept(File f) {
        return f.isDirectory() || f.getName().endsWith(extension1)
            || f.getName().endsWith(extension2);
    }
    
    public String getDescription() {
        return description;
    }

    /**
     * Get a filter accepting "*.fgo".
     *
     * @return The filter.
     */
    public static FileFilter getFGOFileFilter() {
        return new FreeColFileFilter(".fgo", "filter.gameOptions");
    }

    /**
     * Get a filter accepting "*.fsg".
     *
     * @return The filter.
     */
    public static FileFilter getFSGFileFilter() {
        return new FreeColFileFilter(".fsg", "filter.savedGames");
    }

    /**
     * Get a filter accepting all files containing a
     * {@link net.sf.freecol.common.model.GameOptions}.
     * That is; both "*.fgo" and "*.fsg".
     *
     * @return The filter.
     */
    public static FileFilter getGameOptionsFileFilter() {
        return new FreeColFileFilter(".fgo", ".fsg",
                                     "filter.gameOptionsAndSavedGames");
    }
}
