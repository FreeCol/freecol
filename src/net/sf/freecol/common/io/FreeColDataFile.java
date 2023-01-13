/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import static net.sf.freecol.common.util.StringUtils.join;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.Resource;
import net.sf.freecol.common.resources.ResourceFactory;
import net.sf.freecol.common.resources.ResourceMapping;
import net.sf.freecol.common.util.LogBuilder;


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
        final LogBuilder lb = new LogBuilder(64);
        lb.add("Resource mappings:");
        lb.mark();
        final Properties properties = readResourcesProperties(lb);
        if (properties == null) {
            return null;
        }

        final ResourceMapping rc = new ResourceMapping();
        final List<String> virtualResources = handleResources(properties, rc);
        handleVirtualResources(virtualResources, lb, properties, rc);
        
        if (lb.grew()) lb.log(logger, Level.FINE);
        return rc;
    }

    /**
     * Handles loading of all resources defined in {@code properties}.
     * 
     * This method also handles alternate variations and sizes.
     *  
     * @param properties The resources to be loaded.
     * @param rc The output object where resolved resources should be placed.
     * @return A list of keys that are virtual resources, that is a link to
     *      another resource. Virtual resources are used by mods when the
     *      author wants to reuse graphics that are defined in other FreeCol
     *      data files.
     */
    private List<String> handleResources(final Properties properties, ResourceMapping rc) {
        final ResourceFactory resourceFactory = new ResourceFactory();
        final List<String> virtualResources = new ArrayList<>();
        final Enumeration<?> pn = properties.propertyNames();
        while (pn.hasMoreElements()) {
            final String key = (String) pn.nextElement();
            
            /*
             * We are stripping ".r0" from the end of keys in order to
             * support old keys where the variant was listed in
             * resources.properties.
             */
            final String updatedKey = stripEnding(key, ".r0");
            final String value = properties.getProperty(key);
            
            if (value.startsWith(resourceScheme)) {
                virtualResources.add(updatedKey);
            } else {
                handleNormalResource(resourceFactory, rc, key, value);
            }
        }
        return virtualResources;
    }

    private void handleNormalResource(ResourceFactory resourceFactory, ResourceMapping rc, final String key, final String value) {
        final URI uri = getURI(value);
        if (uri == null) {
            return;
        }
        
        /*
         * The caching key should be (and only be) the same when it's the
         * same actual resource data (for example, the same image file).
         * 
         * Please note that the key is not suitable for a caching key,
         * since the resource might get replaced by a mod.
         */
        final String cachingKey = uri.toString();
        final Resource resource = resourceFactory.createResource(key, cachingKey, uri);
        
        /*
         * Rivers need new keys in order to support variations.
         */
        final boolean supportsVariations = !key.contains(".improvement.river.");
        
        if (resource instanceof ImageResource && supportsVariations) {
            final ImageResource imageResource = (ImageResource) resource;
            extendWithAdditionalSizesAndVariations(resourceFactory, imageResource, value);
        }
        
        if (resource != null) {
            rc.add(key, resource);
        }
    }

    private void handleVirtualResources(List<String> virtualResources, final LogBuilder lb, final Properties properties, ResourceMapping rc) {
        boolean progress = true;
        List<String> miss = new ArrayList<>();
        while (progress && !virtualResources.isEmpty()) {
            miss.clear();
            progress = false;
            while (!virtualResources.isEmpty()) {
                final String key = virtualResources.remove(0);
                /*
                 * We are stripping ".r0" from the end of keys in order to
                 * support old keys where the variant was listed in
                 * resources.properties.
                 */
                final String updatedKey = stripEnding(key, ".r0");
                
                final String value = properties.getProperty(key)
                    .substring(resourceScheme.length());
                if (!rc.duplicateResource(value, updatedKey)) {
                    miss.add(key);
                } else {
                    progress = true;
                }
            }
            virtualResources.addAll(miss);
        }
        if (!virtualResources.isEmpty()) {
            lb.add(", could not resolve virtual resource/s: ",
                   join(" ", virtualResources));
        }
    }

    /**
     * Reads "resources.properties". We support localized versions of this
     * file, so that localized images can be provided.
     * 
     * @param lb Just used for logging.
     * @return The read properties, or {@code null} if an error occured while
     *      reading the file(s).
     */
    private Properties readResourcesProperties(final LogBuilder lb) {
        final Properties properties = new Properties();
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
        return properties;
    }
    
    private String stripEnding(String key, String ending) {
        return (key.endsWith(ending)) ? key.substring(0, key.length() - 3) : key;
    }
    
    private void extendWithAdditionalSizesAndVariations(ResourceFactory resourceFactory, ImageResource imageResource, String value) {
        Map<URI, List<URI>> variationsWithAlternateSizes = findVariationsWithAlternateSizes(value);
        imageResource.addAlternativeResourceLocators(variationsWithAlternateSizes.get(null));
        
        variationsWithAlternateSizes.entrySet()
            .stream()
            .filter(entry -> entry.getKey() != null)
            .forEach(entry -> {
                final ImageResource variationResource = (ImageResource) resourceFactory.createResource("", imageResource.getCachingKey(), entry.getKey());
                if (variationResource != null) {
                    variationResource.addAlternativeResourceLocators(entry.getValue());
                    imageResource.addVariation(variationResource);
                }
            });
    }

    private Map<URI, List<URI>> findVariationsWithAlternateSizes(String name) {
        if (name.indexOf(".") <= 0) {
            return Map.of();
        }

        FileSystem fileSystem = null;
        try {
            final Path filePath;
            if (file.isDirectory()) {
                filePath = new File(file, name).toPath();
            } else {
                /*
                 * We can use JarEntry instead, if this solution causes problems. 
                 */
                fileSystem = FileSystems.newFileSystem(new URI("jar:file", file.getAbsolutePath(), null), Map.of());
                filePath = fileSystem.getPath(jarDirectory + name);
            }
            
            /*
             * Using LinkedHashMap to ensure we keep the variations in order.
             */
            final Map<URI, List<URI>> result = new LinkedHashMap<>();
            result.put(null, findFilesWithVariationOrAlternativeSizeAsUri(filePath, false));
            
            final List<Path> variations = findFilesWithVariationOrAlternativeSize(filePath, true);
            for (Path variationPath : variations) {
                result.put(variationPath.toUri(), findFilesWithVariationOrAlternativeSizeAsUri(variationPath, false));
            }
            
            return result;
        } catch (IOException | URISyntaxException e) {
            logger.log(Level.WARNING, "Failed to read directory from jar/zip file: " + file + " jarDirectory" + jarDirectory, e);
            return Map.of();
        } finally {
            if (fileSystem != null) {
                try {
                    fileSystem.close();
                } catch (IOException e) {}
            }
        }
    }

    private List<URI> findFilesWithVariationOrAlternativeSizeAsUri(final Path filePath, boolean findVariation) throws IOException {
        return findFilesWithVariationOrAlternativeSize(filePath, findVariation)
                .stream()
                .map(p -> p.toUri())
                .collect(Collectors.toList());
    }
    
    private List<Path> findFilesWithVariationOrAlternativeSize(final Path filePath, boolean findVariation) throws IOException {
        final String variationFileRegex = "[0-9][0-9]?";
        final String sizeFileRegex = "\\.size[0-9][0-9]*";
        
        final String regex = (findVariation) ? variationFileRegex : sizeFileRegex;       
        final String resourceFilename = filePath.getFileName().toString();
        String prefix = resourceFilename.substring(0, resourceFilename.lastIndexOf("."));
        if (findVariation) {
            prefix = prefix.replaceAll("[0-9]*$", "");
        }
        final String suffix = resourceFilename.substring(resourceFilename.lastIndexOf("."));
        final String completeRegex = Pattern.quote(prefix) + regex + Pattern.quote(suffix);
        
        try (Stream<Path> pathStream = Files.list(filePath.getParent())) {
            return pathStream
                    .sorted()
                    .filter(p -> p.getFileName().toString().matches(completeRegex) && (!findVariation || !p.equals(filePath)))
                    .collect(Collectors.toList());
        }
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
        if (extension.equals("*")) {
            return new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return true;
                }
                
                @Override
                public String getDescription() {
                    return s;
                }
            };
        }
        return new FileNameExtensionFilter(s, extension);
    }
}
