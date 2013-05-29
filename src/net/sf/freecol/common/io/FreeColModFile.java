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
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.util.XMLStream;


/**
 * A modification.
 */
public class FreeColModFile extends FreeColDataFile {

    public static final String SPECIFICATION_FILE = "specification.xml";
    public static final String MOD_DESCRIPTOR_FILE = "mod.xml";
    public static final String[] FILE_ENDINGS = new String[] {".fmd", ".zip"};

    private String id;
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
     *      "specification.xml" within this data file.
     * @exception IOException if thrown while opening the input stream.
     */
    public InputStream getSpecificationInputStream() throws IOException {
        return getInputStream(SPECIFICATION_FILE);
    }

    /**
     * Gets the Specification of this Mod.
     *
     * @return The <code>Specification</code> in this mod.
     * @exception IOException if an error occurs
     */
    public Specification getSpecification() throws IOException {
        InputStream si = getSpecificationInputStream();
        Specification specification = new Specification(si);
        si.close();
        return specification;
    }

    /**
     * Reads a file object representing this mod.
     *
     * @exception IOException if thrown while reading the "mod.xml" file.
     */
    protected void readModDescriptor() throws IOException {
        XMLStream xr = null;
        try {
            xr = new XMLStream(getModDescriptorInputStream());
            xr.nextTag();
            id = xr.readId();
            parent = xr.getAttribute("parent", (String)null);
        } catch (XMLStreamException e) {
            final IOException e2 = new IOException("XMLStreamException.");
            e2.initCause(e);
            throw e2;
        } finally {
            if (xr != null) xr.close();
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
     * File endings that are supported for this type of data file.
     *
     * @return An array of: ".fmd" and ".zip".
     */
    @Override
    protected String[] getFileEndings() {
        return FILE_ENDINGS;
    }

    /**
     * Gets the object identifier of this mod.
     *
     * @return The object identifier of the mod.
     */
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
}
