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

package net.sf.freecol.common.resources;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A factory class for creating <code>Resource</code> instances.
 * @see Resource
 */
public class ResourceFactory {

    private static final Logger logger = Logger.getLogger(ResourceFactory.class.getName());

    /**
     * A <code>WeakHashMap</code> to ensure that only one
     * <code>Resource</code> is created given the same
     * <code>URI</code>.
     */
    private static Map<URI, WeakReference<Resource>> resources = new WeakHashMap<URI, WeakReference<Resource>>();


    /**
     * Gets the resource with the given <code>URI</code> from
     * {@link #resources}.
     *
     * @param uri The <code>URI</code> to identify a previously created
     *      <code>Resource</code>.
     * @return The <code>Resource</code> identified by the given
     *      <code>URI</code>, or <code>null</code> if no such
     *      <code>Resource</code> exists.
     */
    private static Resource getResource(URI uri) {
        final WeakReference<Resource> wr = resources.get(uri);
        if (wr != null) {
            final Resource r = wr.get();
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns an instance of <code>Resource</code> with the
     * given <code>URI</code> as the parameter.
     *
     * @param uri The <code>URI</code> used when creating the
     *      instance.
     * @return A previously created instance of <code>Resource</code>
     *      with the given <code>URI</code> if such an object has
     *      already been created, or a new instance if not.
     */
    public static Resource createResource(URI uri) {
        Resource r = getResource(uri);
        if (r == null) {
            try {
                if ("urn".equals(uri.getScheme())) {
                    if (uri.getSchemeSpecificPart().startsWith(ColorResource.SCHEME)) {
                        r = new ColorResource(uri);
                    } else if (uri.getSchemeSpecificPart().startsWith(ChipResource.SCHEME)) {
                        r = new ChipResource(uri);
                    } else if (uri.getSchemeSpecificPart().startsWith(FontResource.SCHEME)) {
                        r = new FontResource(uri);
                    }
                } else if (uri.getPath().endsWith(".faf")) {
                    r = new FAFileResource(uri);
                } else if (uri.getPath().endsWith(".sza")) {
                    r = new SZAResource(uri);
                } else if (uri.getPath().endsWith(".ttf")) {
                    r = new FontResource(uri);
                } else if (uri.getPath().endsWith(".wav")) {
                    r = new AudioResource(uri);
                } else if (uri.getPath().endsWith(".ogg")) {
                    if (uri.getPath().endsWith(".video.ogg")) {
                        r = new VideoResource(uri);
                    } else {
                        r = new AudioResource(uri);
                    }
                } else {
                    r = new ImageResource(uri);
                }
                resources.put(uri, new WeakReference<Resource>(r));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to create resource with URI: " + uri, e);
            }
        }
        return r;
    }

}
