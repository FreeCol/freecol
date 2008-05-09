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

package net.sf.freecol.common.resources;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A factory class for creating <code>Resource</code> instances.
 * @see Resource
 */
public class ResourceFactory {

    /**
     * A <code>WeakHashMap</code> to ensure that only one
     * <code>Resource</code> is created given the same
     * <code>URL</code>.
     */
    private static Map<URL, WeakReference<Resource>> resources = new WeakHashMap<URL, WeakReference<Resource>>();
    

    /**
     * Gets the resource with the given <code>URL</code> from
     * {@link #resources}.
     * 
     * @param url The <code>URL</code> to identify a previously created
     *      <code>Resource</code>.
     * @return The <code>Resource</code> identified by the given
     *      <code>URL</code>, or <code>null</code> if no such
     *      <code>Resource</code> exists.
     */
    private static Resource getResource(URL url) {
        final WeakReference<Resource> wr = resources.get(url);
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
     * given <code>URL</code> as the parameter.
     * 
     * @param url The <code>URL</code> used when creating the
     *      instance.
     * @return A previously created instance of <code>Resource</code>
     *      with the given <code>URL</code> if such an object has
     *      already been created, or a new instance if not.
     */
    public static Resource createResource(URL url) {
        Resource r = getResource(url);
        if (r == null) {
            r = new ImageResource(url);
            resources.put(url, new WeakReference<Resource>(r));
        }
        return r;
    }
}
