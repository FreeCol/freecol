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

package net.sf.freecol.common.resources;

import java.net.URI;


/**
 * A <code>Resource</code> wrapping an <code>String</code>.
 * @see Resource
 */
public class StringResource extends Resource {

    private String data;

    /**
     * Do not use directly.
     *
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     */
    public StringResource(URI resourceLocator) {
        super(resourceLocator);

        this.data = resourceLocator.getPath();
        if (this.data.endsWith("\"")) { // Should always be true
            this.data = this.data.substring(0, this.data.length()-1);
        }
        int idx = this.data.lastIndexOf('"');
        if (idx > 0) {
            this.data = this.data.substring(idx+1);
        }
    }


    /**
     * Gets the <code>String</code> represented by this resource.
     *
     * @return The string resource.
     */
    public String getString() {
        return this.data;
    }
}
