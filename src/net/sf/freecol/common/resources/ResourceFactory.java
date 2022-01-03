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

package net.sf.freecol.common.resources;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A factory class for creating {@code Resource} instances.
 * @see Resource
 */
public class ResourceFactory {

    private static final Logger logger = Logger.getLogger(ResourceFactory.class.getName());

    /**
     * Takes a newly produced Resource.
     */
    public interface ResourceSink {

        void add(Resource r);

    }

    /**
     * Ensures that only one {@code Resource} is created given the same {@code URI}.
     */
    private static final Map<URI, Resource> resources = new HashMap<>();

    
    /**
     * Returns an instance of {@code Resource} with the
     * given {@code URI} as the parameter.
     *
     * @param uri The {@code URI} used when creating the
     *      instance.
     * @return The <code>Resource</code> if created.     
     */
    public static Resource createResource(URI uri) {
    	final Resource r = resources.get(uri);
    	if (r != null) { 
    		return r;
    	}

        final String pathPart;
        if (uri.getPath() != null) {
            pathPart = uri.getPath();
        } else if (uri.toString().indexOf("!/") >= 0) {
            pathPart = uri.toString().substring(uri.toString().indexOf("!/") + 2);
        } else {
            pathPart = null;
        }

        try {
            final Resource resource;
            if ("urn".equals(uri.getScheme())) {
                if (uri.getSchemeSpecificPart().startsWith(ColorResource.SCHEME)) {
                    resource = new ColorResource(uri);
                } else if (uri.getSchemeSpecificPart().startsWith(FontResource.SCHEME)) {
                    resource = new FontResource(uri);
                } else {
                    logger.log(Level.WARNING, "Unknown urn part: " + uri.getSchemeSpecificPart());
                    return null;
                }
            } else if (pathPart.endsWith("\"") && pathPart.lastIndexOf('"', pathPart.length()-1) >= 0) {
                resource = new StringResource(uri);
            } else if (pathPart.endsWith(".faf")) {
                resource = new FAFileResource(uri);
            } else if (pathPart.endsWith(".sza")) {
                resource = new SZAResource(uri);
            } else if (pathPart.endsWith(".ttf")) {
                resource = new FontResource(uri);
            } else if (pathPart.endsWith(".wav")) {
                resource = new AudioResource(uri);
            } else if (pathPart.endsWith(".ogg")) {
                if (pathPart.endsWith(".video.ogg")) {
                    resource = new VideoResource(uri);
                } else {
                    resource = new AudioResource(uri);
                }
            } else {
                resource = new ImageResource(uri);
            }
            resources.put(uri, resource);
            return resource;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Failed to create " + uri, ioe);
            return null;
        }
    }
}
