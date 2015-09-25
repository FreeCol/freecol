/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.model.Specification;


/**
 * A wrapped for a file containing a FreeCol modification (mod).
 */
public class FreeColModFile extends FreeColDataFile implements ObjectWithId {

    protected static final String SPECIFICATION_FILE = "specification.xml";
    protected static final String MOD_DESCRIPTOR_FILE = "mod.xml";

    /** A file filter to select mods. */
    private static final FileFilter fileFilter
        = makeFileFilter(MOD_DESCRIPTOR_FILE, "fmd", ZIP_FILE_EXTENSION);

    /** The identifier for this mod. */
    private String id;

    /** The identifier for the parent of this mod, if any. */
    private String parent;


    /**
     * Make a FreeColModFile from a File.
     *
     * @param file The <code>File</code> containing a FreeCol mod.
     * @exception IOException if thrown while opening the file.
     */
    public FreeColModFile(final File file) throws IOException {
        super(file);

        readModDescriptor();
    }


    /**
     * Gets the input stream to the specification.
     *
     * @return An <code>InputStream</code> to the file
     *     "specification.xml" within this data file, or null if none present.
     * @exception IOException if thrown while opening the input stream.
     */
    public InputStream getSpecificationInputStream() throws IOException {
        try {
            return getInputStream(SPECIFICATION_FILE);
        } catch (FileNotFoundException fnfe) {
            ; // Normal for graphic-only mods.
        }
        return null;
    }

    /**
     * Gets the Specification.
     *
     * @return The <code>Specification</code>, or null if none present.
     * @exception IOException if an error occurs reading the specification.
     */
    public Specification getSpecification() throws IOException {
        try (InputStream si = getSpecificationInputStream()) {
            return (si == null) ? null : new Specification(si);
        }
    }

    /**
     * Gets the input stream to the mod meta file.
     *
     * @return An <code>InputStream</code> to the file "mod.xml"
     *     within this data file.
     * @exception IOException if thrown while opening the input stream.
     */
    private InputStream getModDescriptorInputStream() throws IOException {
        return getInputStream(MOD_DESCRIPTOR_FILE);
    }

    /**
     * Reads a file object representing this mod.
     *
     * @exception IOException if thrown while reading the "mod.xml" file.
     */
    protected void readModDescriptor() throws IOException {
        try (
            FreeColXMLReader xr
                = new FreeColXMLReader(getModDescriptorInputStream());
        ) {
            xr.nextTag();
            id = xr.readId();
            parent = xr.getAttribute("parent", (String)null);
        } catch (XMLStreamException xse) {
            throw new IOException(xse);
        }
    }

    /**
     * Gets the object identifier of this mod.
     *
     * @return The object identifier of the mod.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Gets the parent of the mod.
     *
     * @return The mod parent name.
     */
    public String getParent() {
        return parent;
    }

    /**
     * Get the file filter to select mod files.
     *
     * @return The mod file filter.
     */
    public static FileFilter getFileFilter() {
        return fileFilter;
    }
}
