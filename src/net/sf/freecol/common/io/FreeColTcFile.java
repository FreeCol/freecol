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
import java.io.IOException;
import java.io.InputStream;

/**
 * A Total Conversion (TC).
 */
public class FreeColTcFile extends FreeColDataFile {
    
    private static final String SPECIFICATION_FILE = "specification.xml";

    /**
     * Opens the given file for reading.
     * 
     * @param file The file to be read.
     * @throws IOException if thrown while opening the file.
     */
    public FreeColTcFile(File file) throws IOException {
        super(file);
    }
    
    /**
     * Gets the input stream to the specification.
     * 
     * @return An <code>InputStream</code> to the file
     *      "specification.xml" within this data file.
     * @throws IOException if thrown while opening the
     *      input stream.
     */
    public InputStream getSpecificationInputStream() throws IOException {
        return getInputStream(SPECIFICATION_FILE);
    }
    
    /**
     * File endings that are supported for this type of data file.
     * @return An array of: ".ftc" and ".zip".
     */
    protected String[] getFileEndings() {
        return new String[] {".ftc", ".zip"};
    }
}
