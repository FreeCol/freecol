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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final String RESOURCE_FILE_PREFIX = "resources";
    private static final String RESOURCE_FILE_SUFFIX = ".properties";

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
     * @return The name of the base directory in the zip-file.
     */
    private static String findJarDirectory(File file) {
        String expected = file.getName().substring(0, file.getName().lastIndexOf('.'));
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
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while reading data file.", e);
            return expected;
        }
    }

    /**
     * Gets a list containing the names of all message files to load.
     *
     * @param prefix The file name prefix.
     * @param suffix The file name suffix.
     * @param locale The <code>Locale</code> to generate file names for.
     * @return A list of candidate file names.
     */
    public static List<String> getFileNames(String prefix, String suffix,
                                            Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        List<String> result = new ArrayList<>(4);

        if (!language.isEmpty()) language = "_" + language;
        if (!country.isEmpty()) country = "_" + country;
        if (!variant.isEmpty()) variant = "_" + variant;

        result.add(prefix + suffix);
        String filename = prefix + language + suffix;
        if (!result.contains(filename)) result.add(filename);
        filename = prefix + language + country + suffix;
        if (!result.contains(filename)) result.add(filename);
        filename = prefix + language + country + variant + suffix;
        if (!result.contains(filename)) result.add(filename);
        return result;
    }

    /**
     * Get a list of candidate resource file names for a given locale.
     *
     * @return A list of resource file names.
     */
    public static List<String> getResourceFileNames() {
        return getFileNames(RESOURCE_FILE_PREFIX, RESOURCE_FILE_SUFFIX,
                            Locale.getDefault());
    }

    /**
     * Get a URI to access a resource through.
     *
     * @param name A name with special prefixes to convert to the URI.
     * @return A <code>URI</code>, or null if none found.
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
     * @return An <code>InputStream</code> to read the resource with.
     * @exception IOException if an error occurs
     */
    public BufferedInputStream getInputStream(String filename) throws IOException {
        final URLConnection connection = getURI(filename).toURL()
            .openConnection();
        connection.setDefaultUseCaches(false);
        return new BufferedInputStream(connection.getInputStream());
    }

    /**
     * Creates a <code>ResourceMapping</code> from the available
     * resource files.
     *
     * @return A <code>ResourceMapping</code> or <code>null</code>
     *     there is no resource mapping file.
     */
    public ResourceMapping getResourceMapping() {
        final Properties properties = new Properties();
        LogBuilder lb = new LogBuilder(64);
        lb.add("Resource mapping");
        lb.mark();
        for (String fileName : getResourceFileNames()) {
            try (
                final InputStream is = getInputStream(fileName);
            ) {
                properties.load(is);
                lb.add(", ", file, "/", fileName, " loaded");
            } catch (FileNotFoundException e) { // Expected failure
                lb.add(", ", file, "/", fileName, " not found");
            } catch (IOException e) {
                logger.log(Level.WARNING, "ResourceMapping read exception: "
                    + file + "/" + fileName, e);
                return null;
            }
        }

        ResourceMapping rc = new ResourceMapping();
        List<String> todo = new ArrayList<>();
        Enumeration<?> pn = properties.propertyNames();
        ResourceMapper rm = new ResourceMapper(rc);
        while (pn.hasMoreElements()) {
            final String key = (String) pn.nextElement();
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
        if (lb.grew()) lb.log(logger, Level.INFO);
        return rc;
    }

    /**
     * Make a <code>FileFilter</code> for a set of file endings.
     *
     * @param requiredFile If non-null, the filter will accept a directory
     *     containing this file.
     * @param endings Acceptable file suffixes.
     * @return A suitable <code>FileFilter</code>.
     */
    public static FileFilter makeFileFilter(final String requiredFile,
                                            final String... endings) {
        return f -> {
            final String name = f.getName();
            return (name.startsWith("."))
                ? false
                : (requiredFile != null && f.isDirectory())
                ? new File(f, requiredFile).exists()
                : any(endings, e -> name.endsWith("." + e)
                    && name.length() > e.length());
        };
    }
}
