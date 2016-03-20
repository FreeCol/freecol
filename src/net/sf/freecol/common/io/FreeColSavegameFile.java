/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import java.io.BufferedInputStream;

import net.sf.freecol.FreeCol;


/**
 * Represents a FreeCol savegame.
 */
public class FreeColSavegameFile extends FreeColDataFile {

    /** The tag for the version string in the saved game. */
    public static final String VERSION_TAG = "version";

    /** The name of the file that contains the actual savegame. */
    public static final String SAVEGAME_FILE = "savegame.xml";

    /**
     * The name of a properties file that contains information about
     * the saved game, such as the size of the map, the date and time
     * it was started, and so on.  The map size is used in the
     * {@link net.sf.freecol.client.gui.panel.MapGeneratorOptionsDialog},
     * for example.
     */
    public static final String SAVEGAME_PROPERTIES = "savegame.properties";

    /**
     * The name of the file that contains the
     * {@link net.sf.freecol.client.ClientOptions} saved with the game.
     */
    public static final String CLIENT_OPTIONS = "client-options.xml";

    /**
     * The name of the image file that contains the map thumbnail,
     * i.e. a view of the game map as seen by the owner of the game
     * when saving. The thumbnail image is used by the {@link
     * net.sf.freecol.client.gui.panel.MapGeneratorOptionsDialog}, in
     * particular.
     */
    public static final String THUMBNAIL_FILE = "thumbnail.png";

    /** A file filter to select the saved game files. */
    private static final FileFilter fileFilter = makeFileFilter(SAVEGAME_FILE,
        FreeCol.FREECOL_SAVE_EXTENSION, ZIP_FILE_EXTENSION);


    /**
     * Create a new save game file from a given file.
     *
     * @param file The base <code>File</code>.
     * @exception IOException if the file can not be read.
     */
    public FreeColSavegameFile(File file) throws IOException {
        super(file);
    }

    /**
     * Gets the save game version from this saved game.
     *
     * @return The saved game version, or negative on error.
     */
    public int getSavegameVersion() {
        try (
            FreeColXMLReader xr = this.getSavedGameFreeColXMLReader();
        ) {
            xr.nextTag();
            return xr.getAttribute(VERSION_TAG, -1);
        } catch (Exception e) {
            ; // Just fail
        }
        return -1;
    }

    /**
     * Gets the input stream to the saved game data.
     *
     * Only still needed by the validator.
     *
     * @return An <code>InputStream</code> to the file
     *      "savegame.xml" within this data file.
     * @exception IOException if there is a problem opening the input stream.
     */
    public BufferedInputStream getSavegameInputStream() throws IOException {
        return getInputStream(SAVEGAME_FILE);
    }

    /**
     * Get a reader for the client options data.
     *
     * @return A reader for the file "client-options.xml" within this file.
     * @exception IOException if there is a problem opening the input stream.
     */
    public FreeColXMLReader getClientOptionsFreeColXMLReader()
        throws IOException {
        return new FreeColXMLReader(getInputStream(CLIENT_OPTIONS));
    }

    /**
     * Get a reader for the saved game data.
     *
     * @return A reader for the file "savegame.xml" within this file.
     * @exception IOException if there is a problem opening the input stream.
     */
    public FreeColXMLReader getSavedGameFreeColXMLReader()
        throws IOException {
        return new FreeColXMLReader(getInputStream(SAVEGAME_FILE));
    }

    /**
     * Get the file filter to select saved game files.
     *
     * @return The saved game file filter.
     */
    public static FileFilter getFileFilter() {
        return fileFilter;
    }
}
