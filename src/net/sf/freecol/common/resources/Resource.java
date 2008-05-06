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

import java.net.URL;

/**
 * Represents a resource that either has been or can be loaded using
 * a <code>URL</code>. There can only be one instance of
 * <code>Resource</code> having the same (as in URL's equals, not the identity)
 * <code>URL</code>.
 * 
 * Instances are created using {@link ResourceManager#createResource(URL)}.
 */
public abstract class Resource {

    /**
     * The <code>URL</code> used when loading this resource.
     */
    private final URL resourceLocator;
    
    
    /**
     * Do not use directly.
     * @param resourceLocator The <code>URL</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URL)
     */
    Resource(URL resourceLocator) {
        this.resourceLocator = resourceLocator;
    }
    
    
    /**
     * Returns the <code>URL</code> used for loading the resource.
     * @return The <code>URL</code>.
     */
    public URL getResourceLocator() {
        return resourceLocator;
    }
}
