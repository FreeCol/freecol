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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A wrapped for a file containing a FreeCol modification (mod).
 */
public class FreeColModFile extends FreeColDataFile implements ObjectWithId {

    private static final Logger logger = Logger.getLogger(FreeColModFile.class.getName());

    /** A cache of all the mods. */
    private static final Map<String, FreeColModFile> allMods = new HashMap<>();

    protected static final String SPECIFICATION_FILE = "specification.xml";

    /** The identifier for this mod. */
    private String id;

    /** The identifier for the parent of this mod, if any. */
    private String parent;


    /**
     * Make a FreeColModFile from a File.
     *
     * @param file The {@code File} containing a FreeCol mod.
     * @exception IOException if thrown while opening the file.
     */
    public FreeColModFile(final File file) throws IOException {
        super(file);

        readModDescriptor();
    }


    /**
     * Gets the input stream to the specification.
     *
     * @return An {@code InputStream} to the file
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
     * @return The {@code Specification}, or null if none present.
     * @exception IOException if an error occurs creating a stream to read.
     * @exception XMLStreamException if there is an error reading the stream.
     */
    public Specification getSpecification() throws IOException,
                                                   XMLStreamException {
        try (InputStream si = getSpecificationInputStream()) {
            return (si == null) ? null : new Specification(si);
        }
    }

    /**
     * Gets the input stream to the mod meta file.
     *
     * @return An {@code InputStream} to the file "mod.xml"
     *     within this data file.
     * @exception IOException if thrown while opening the input stream.
     */
    private InputStream getModDescriptorInputStream() throws IOException {
        return getInputStream(FreeColDirectories.MOD_DESCRIPTOR_FILE_NAME);
    }

    /**
     * Reads a file object representing this mod.
     *
     * @exception IOException if thrown while reading the "mod.xml" file.
     */
    protected final void readModDescriptor() throws IOException {
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
     * Gets the parent of the mod.
     *
     * @return The mod parent name.
     */
    public String getParent() {
        return parent;
    }

    /**
     * Get all the standard mods.
     *
     * @return A list of {@code FreeColModFile}s holding the mods.
     */
    public static List<FreeColModFile> getModsList() {
        List<FreeColModFile> ret = new ArrayList<>();
        for (File f : FreeColDirectories.getModFileList()) {
            try {
                ret.add(new FreeColModFile(f));
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to load mod from: " + f, ioe);
            }
        }
        return ret;
    }


    // Implement ObjectWithId

    /**
     * Gets the object identifier of this mod.
     *
     * @return The object identifier of the mod.
     */
    @Override
    public String getId() {
        return id;
    }


    // Cache manipulation

    /**
     * Require all mods to be loaded.  This must be delayed until
     * the mods directories are defined.
     *
     * User mods are loaded after standard mods to allow user override.
     */
    public static void loadMods() {
        if (allMods.isEmpty()) {
            for (FreeColModFile fcmf : FreeColModFile.getModsList()) {
                allMods.put(fcmf.getId(), fcmf);
            }
        }
    }

    /**
     * Get a mod by id.
     *
     * @param id The mod file identifier to look for.
     * @return The {@code FreeColModFile} found, or null if none present.
     */
    public static FreeColModFile getFreeColModFile(String id) {
        return allMods.get(id);
    }
}
