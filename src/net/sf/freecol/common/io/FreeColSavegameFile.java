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
import java.io.BufferedInputStream;
import java.util.logging.Logger;

/**
 * Represents a FreeCol savegame.
 */
public class FreeColSavegameFile extends FreeColDataFile {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FreeColSavegameFile.class.getName());

    /**
     * The name of the file that contains the actual savegame.
     */
    public static final String SAVEGAME_FILE = "savegame.xml";

    /**
     *  The name of a properties file that contains information about
     *  the saved game, such as the size of the map, the date and time
     *  it was started, and so on.  The map size is used in the
     *  {@link net.sf.freecol.client.gui.panel.MapGeneratorOptionsDialog},
     *  for example.
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



    public FreeColSavegameFile(File file) throws IOException {
        super(file);
    }

    /**
     * Gets the input stream to the savegame data.
     *
     * @return An <code>InputStream</code> to the file
     *      "savegame.xml" within this data file.
     * @throws IOException if thrown while opening the
     *      input stream.
     */
    public BufferedInputStream getSavegameInputStream() throws IOException {
        return getInputStream(SAVEGAME_FILE);
    }

    /**
     * File endings that are supported for this type of data file.
     * @return An array of: ".fsg" and ".zip".
     */
    @Override
    protected String[] getFileEndings() {
        return new String[] {".fsg", ".zip"};
    }
}