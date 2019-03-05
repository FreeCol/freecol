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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.ResourceFactory;
import net.sf.freecol.common.resources.ResourceMapper;
import net.sf.freecol.common.resources.ResourceMapping;
import net.sf.freecol.common.util.LogBuilder;

import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Support for reading a FreeCol data file. The data file
 * is either a ZIP-file or a directory containing certain files.
 */
public class FreeColDataFile {

    private static final Logger logger = Logger.getLogger(FreeColDataFile.class.getName());

    protected static final String ZIP_FILE_EXTENSION = "zip";

    /** A fake URI scheme for resources delegating to other resources. */
    private static final String resourceScheme = "resource:";

    /** The file this object represents. */
    private final File file;

    /**
     * A prefix string for the jar-entries (only if {@link #file} is
     * a ZIP-file).
     */
    private final String jarDirectory;


    /**
     * Opens the given file for reading.
     *
     * @param file The file to be read.
     * @exception IOException if the file does not exist.
     */
    public FreeColDataFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File " + file.getName() + " does not exist");
        }
        this.file = file;
        this.jarDirectory = (file.isDirectory()) ? null
            : findJarDirectory(file);
    }

    /**
     * Finds the directory within the zip-file in case the data file
     * has been renamed.
     *
     * @param file The zip-file.
     * @return The name of the base directory in the zip-file or null on error.
     */
    private static String findJarDirectory(File file) {
        try (
            JarFile jf = new JarFile(file);
        ) {
            final JarEntry entry = jf.entries().nextElement();
            final String en = entry.getName();
            final int index = en.lastIndexOf('/');
            String name = "";
            if (index > 0) {
                name = en.substring(0, index + 1);
            }
            return name;
        } catch (IOException ioe) {
            logger.warning("Failed to create jar file: " + file.getName());
        }
        return null;
    }

    /**
     * Get a URI to access a resource through.
     *
     * @param name A name with special prefixes to convert to the URI.
     * @return A {@code URI}, or null if none found.
     */
    protected URI getURI(String name) {
        try {
            if (name.startsWith("urn:")) {
                try {
                    return new URI(name);
                } catch (URISyntaxException e) {
                    logger.log(Level.WARNING, "Resource creation failure with: "
                        + name, e);
                    return null;
                }
            } else if (file.isDirectory()) {
                return new File(file, name).toURI();
            } else {
                return new URI("jar:file", file + "!/" + jarDirectory + name,
                               null);
            }
        } catch (URISyntaxException e) {
            logger.log(Level.WARNING, "Failed to lookup: " + file + "/" + name,
                       e);
            return null;
        }
    }

    /**
     * Gets an input stream for the specified resource.
     *
     * @param filename The filename of a resource within this
     *     collection of data.  If this object represents a directory
     *     then the provided filename should be relative towards the
     *     path of the directory.  In case of a compressed archive it
     *     should be the path within the archive.
     * @return An {@code InputStream} to read the resource with.
     * @exception IOException if an error occurs
     */
    public BufferedInputStream getInputStream(String filename) throws IOException {
        final URLConnection connection = getURI(filename).toURL()
            .openConnection();
        connection.setDefaultUseCaches(false);
        return new BufferedInputStream(connection.getInputStream());
    }

    /**
     * Creates a {@code ResourceMapping} from the available resource files.
     *
     * @return A {@code ResourceMapping} or {@code null}
     *     there is no resource mapping file.
     */
    public ResourceMapping getResourceMapping() {
        final Properties properties = new Properties();
        LogBuilder lb = new LogBuilder(64);
        lb.add("Resource mappings:");
        lb.mark();
        for (String fileName : FreeColDirectories.getResourceFileNames()) {
            try (
                final InputStream is = getInputStream(fileName);
            ) {
                properties.load(is);
                lb.add(' ', file, '/', fileName, ":ok");
            } catch (FileNotFoundException e) { // Expected failure
                lb.add(' ', file, '/', fileName, ":not-found");
            } catch (IOException e) {
                logger.log(Level.WARNING, "ResourceMapping read exception: "
                    + file + "/" + fileName, e);
                return null;
            }
        }

        ResourceMapping rc = new ResourceMapping();
        List<String> todo = new ArrayList<>();
        List<String> alternatives = new ArrayList<>();
        Enumeration<?> pn = properties.propertyNames();
        ResourceMapper rm = new ResourceMapper(rc);
        while (pn.hasMoreElements()) {
            final String key = (String) pn.nextElement();
            int split = key.lastIndexOf('.');
            if(split != -1 && split+2 < key.length()
                && key.charAt(split+1) == 'a'
                && key.charAt(split+2) >= '0' && key.charAt(split+2) <= '9'
                && key.startsWith("image.")) {
                alternatives.add(key);
            } else {
                final String value = properties.getProperty(key);
                if (value.startsWith(resourceScheme)) {
                    todo.add(key);
                } else {
                    URI uri = getURI(value);
                    if (uri != null) {
                        rm.setKey(key);
                        ResourceFactory.createResource(uri, rm);
                    }
                }
            }
        }
        boolean progress = true;
        List<String> miss = new ArrayList<>();
        while (progress && !todo.isEmpty()) {
            miss.clear();
            progress = false;
            while (!todo.isEmpty()) {
                final String key = todo.remove(0);
                final String value = properties.getProperty(key)
                    .substring(resourceScheme.length());
                if (!rc.duplicateResource(value, key)) {
                    miss.add(key);
                } else {
                    progress = true;
                }
            }
            todo.addAll(miss);
        }
        if (!todo.isEmpty()) {
            lb.add(", could not resolve virtual resource/s: ",
                   join(" ", todo));
        }
        for (String key : alternatives) {
            final String value = properties.getProperty(key);
            URI uri = getURI(value);
            if (uri != null) {
                int split = key.lastIndexOf('.');
                ImageResource ir = rc.getImageResource(key.substring(0, split));
                if (ir != null) {
                    ir.addAlternativeResourceLocator(uri);
                } else {
                    logger.warning("Missing resource when adding alternative: "
                        + key);
                }
            }
        }
        if (lb.grew()) lb.log(logger, Level.FINE);
        return rc;
    }

    /**
     * Get the path to the underlying file.
     *
     * Useful for error messages.
     *
     * @return The path to the file.
     */
    public String getPath() {
        return file.getPath();
    }


    /**
     * Get a file filter for a given extension.
     *
     * @param extension The file extension to filter on.
     * @return File filters for the extension.
     */
    public static FileFilter getFileFilter(String extension) {
        String s = Messages.message("filter." + extension);
        return new FileNameExtensionFilter(s, extension);
    }
}
