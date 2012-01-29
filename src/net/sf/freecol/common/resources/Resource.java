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

import java.net.URI;

/**
 * Represents a resource that either has been or can be loaded using
 * a <code>URI</code>. There can only be one instance of
 * <code>Resource</code> having the same (as in URI's equals, not the identity)
 * <code>URI</code>.
 *
 * Instances are created using {@link ResourceFactory#createResource(URI)}.
 */
public abstract class Resource {

    /**
     * The <code>URI</code> used when loading this resource.
     */
    private final URI resourceLocator;

    protected Resource() {
        // empty constructor
        resourceLocator = null;
    }

    /**
     * Do not use directly.
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URI)
     */
    Resource(URI resourceLocator) {
        this.resourceLocator = resourceLocator;
    }

    /**
     * Preload the resource.  Often implemented as a noop.
     */
    public abstract void preload();

    /**
     * Returns the <code>URI</code> used for loading the resource.
     * @return The <code>URI</code>.
     */
    public URI getResourceLocator() {
        return resourceLocator;
    }
}
