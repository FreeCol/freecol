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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a mapping between IDs and resources.
 * 
 * @see Resource
 */
public class ResourceMapping {

    /** Mappings between an ID and a resource. */
    protected Map<String, Resource> resources;

    
    /**
     * Creates a new empty <code>ResourceMapping</code>.
     */
    public ResourceMapping() {
        resources = new HashMap<String, Resource>();
    }
    
    
    /**
     * Adds a mapping between the given ID and a <code>Resource</code>.
     * 
     * @param id The ID identifying the given resource in the mapping. 
     * @param value The <code>Resource</code> identified by the ID
     *      in the mapping,.
     */
    public void add(String id, Resource value) {
        resources.put(id, value);
    }
    
    /**
     * Adds all mappings from the given <code>ResourceMapping</code> to
     * this object.
     * 
     * @param rc The <code>ResourceMapping</code>.
     */
    public void addAll(ResourceMapping rc) {
        if (rc != null) {
            resources.putAll(rc.getResources());
        }
    }

    public boolean containsKey(String key) {
        return resources.containsKey(key);
    }

    /**
     * Returns all the mappings between IDs and <code>Resource</code>s
     * that are kept by this object.
     * 
     * @return An unmodifiable <code>Map</code>.
     */
    public Map<String, Resource> getResources() {
        return Collections.unmodifiableMap(resources);
    }
    
    /**
     * Gets the <code>Resource</code> identified by the
     * given id.
     * 
     * @param id The ID.
     * @return The <code>Resource</code>.
     */
    public Resource get(String id) {
        return resources.get(id);
    }
}