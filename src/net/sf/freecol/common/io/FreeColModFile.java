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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;


/**
 * A modification.
 */
public class FreeColModFile extends FreeColDataFile {
    
    private static final String SPECIFICATION_FILE = "specification.xml";
    private static final String MOD_DESCRIPTOR_FILE = "mod.xml";
    public static final String[] FILE_ENDINGS = new String[] {".fmd", ".zip"};

    private String id;
    private final ModInfo modInfo;


    /**
     * Make a FreeColModFile from a File.
     *
     * @param file The <code>File</code> containing a FreeCol mod.
     * @throws IOException if thrown while opening the file.
     */
    public FreeColModFile(final File file) {
        super(file);

        this.id = file.getName();
        this.modInfo = new ModInfo(id);
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
     * Returns an object representing this mod.
     * 
     * @return The meta information for this mod file.
     * @throws IOException if thrown while reading the
     *      "mod.xml" file.
     */
    protected ModDescriptor getModDescriptor() throws IOException {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader in = null;
        try {
            in = xif.createXMLStreamReader(getModDescriptorInputStream());
            in.nextTag();
            final ModDescriptor mi = new ModDescriptor(in);
            return mi;
        } catch (XMLStreamException e) {
            final IOException e2 = new IOException("XMLStreamException.");
            e2.initCause(e);
            throw e2;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {}
        }
    }

    /**
     * Gets the input stream to the mod meta file.
     * 
     * @return An <code>InputStream</code> to the file
     *      "mod.xml" within this data file.
     * @throws IOException if thrown while opening the
     *      input stream.
     */
    private InputStream getModDescriptorInputStream() throws IOException {
        return getInputStream(MOD_DESCRIPTOR_FILE);
    }

    /**
     * File endings that are supported for this type of data file.
     * @return An array of: ".fmd" and ".zip".
     */
    @Override
    protected String[] getFileEndings() {
        return FILE_ENDINGS;
    }

    /**
     * Gets the ID of this mod.
     *
     * @return The ID of the mod.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the ModInfo for this mod.
     *
     * @return The ModInfo for this mod.
     */
    public ModInfo getModInfo() {
        return modInfo;
    }


    public static class ModInfo {

        private final String id;

        private ModInfo(final String id) {
            this.id = id;
        }

        /**
         * Gets the ID of this mod.
         * @return The ID of the mod.
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the name of this mod.
         */
        public String getName() {
            return Messages.message("mod." + getId() + ".name");
        }

        /**
         * Gets a short description of this mod.
         */
        public String getShortDescription() {
            return Messages.message("mod." + getId() + ".shortDescription");
        }

        /**
         * Gets the name of this mod.
         * @return The same as {@link #getName()}.
         */
        public String toString() {
            return getName();
        }
    }

    protected static class ModDescriptor {

        private final String parent;
        
        /**
         * Initiates a new <code>ModInfo</code> from XML.
         *
         * @param in The input stream containing the XML.
         * @throws XMLStreamException if a problem was encountered
         *      during parsing.
         */
        protected ModDescriptor(XMLStreamReader in) throws XMLStreamException {
            this.parent = in.getAttributeValue(null, "parent");
        }
        
        /**
         * Gets the parent of the mod.
         * @return a <code>String</code> value
         */
        public String getParent() {
            return parent;
        }
    }
}
