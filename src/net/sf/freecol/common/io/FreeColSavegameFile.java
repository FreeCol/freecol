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
import java.io.BufferedInputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.dialog.*;

import static net.sf.freecol.common.util.CollectionUtils.*;


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
     * {@link MapGeneratorOptionsDialog},
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
     * when saving. The thumbnail image is used by
     * {@link MapGeneratorOptionsDialog}..
     */
    public static final String THUMBNAIL_FILE = "thumbnail.png";

    /** Static argument list for getVersion. */
    private static final List<String> versionList
        = makeUnmodifiableList(VERSION_TAG);


    /**
     * Create a new save game file from a given file.
     *
     * @param file The base {@code File}.
     * @exception IOException if the file can not be read.
     */
    public FreeColSavegameFile(File file) throws IOException {
        super(file);
    }


    /**
     * Peek at the attributes in a saved game.
     *
     * @param attributes A list of attribute names to peek at.
     * @return A list of corresponding attribute values, or null on error.
     * @exception IOException if there is a problem reading the attributes.
     * @exception XMLStreamException on stream error.
     */
    public List<String> peekAttributes(List<String> attributes)
        throws IOException, XMLStreamException {
        final FreeColXMLReader xr = this.getSavedGameFreeColXMLReader();
        xr.nextTag();
        List<String> ret = transform(attributes, alwaysTrue(),
                                     a -> xr.getAttribute(a, (String)null));
        xr.close();
        return ret;
    }

    /**
     * Get the properties in this save game.
     *
     * @return The {@code Properties} found.
     * @exception IOException if there is a problem reading the properties.
     */
    public Properties getProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(getInputStream(SAVEGAME_PROPERTIES));
        return properties;
    }
        
    /**
     * Gets the save game version from this saved game.
     *
     * @return The saved game version, or negative on error.
     * @exception IOException if there is a problem reading the attributes.
     * @exception XMLStreamException on stream error.
     */
    public int getSavegameVersion() throws IOException, XMLStreamException {
        List<String> v = this.peekAttributes(versionList);
        int ret = -1;
        if (v != null && v.size() == 1) {
            try {
                ret = Integer.parseInt(v.get(0));
            } catch (NumberFormatException nfe) {}
        }
        return ret;
    }

    /**
     * Gets the input stream to the saved game data.
     *
     * Only still needed by the validator.
     *
     * @return An {@code InputStream} to the save game file within
     *     this data file.
     * @exception IOException if there is a problem opening the input stream.
     */
    public BufferedInputStream getSavegameInputStream() throws IOException {
        return getInputStream(SAVEGAME_FILE);
    }

    /**
     * Gets the input stream to the thumbnail file.
     *
     * @return An {@code InputStream} to the thumbnail file within
     *      this data file.
     * @exception IOException if there is a problem opening the input stream.
     */
    public BufferedInputStream getThumbnailInputStream() throws IOException {
        return getInputStream(THUMBNAIL_FILE);
    }

    /**
     * Get a reader for the client options data.
     *
     * @return A reader for the file "client-options.xml" within this file.
     * @exception IOException if there is a problem opening the input stream.
     * @exception XMLStreamException if there is a problem creating the reader.
     */
    public FreeColXMLReader getClientOptionsFreeColXMLReader()
        throws IOException, XMLStreamException {
        return new FreeColXMLReader(getInputStream(CLIENT_OPTIONS));
    }

    /**
     * Get a reader for the saved game data.
     *
     * @return A reader for the file "savegame.xml" within this file.
     * @exception IOException if there is a problem opening the input stream.
     * @exception XMLStreamException if there is a problem creating the reader.
     */
    public FreeColXMLReader getSavedGameFreeColXMLReader()
        throws IOException, XMLStreamException {
        return new FreeColXMLReader(getInputStream(SAVEGAME_FILE));
    }
}
