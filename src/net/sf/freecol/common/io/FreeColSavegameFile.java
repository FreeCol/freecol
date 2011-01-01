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
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Represents a FreeCol savegame.
 */
public class FreeColSavegameFile extends FreeColDataFile {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FreeColSavegameFile.class.getName());

    public static final String SAVEGAME_FILE = "savegame.xml";

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
    public InputStream getSavegameInputStream() throws IOException {
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