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
import java.net.URL;


/**
 * A {@code Resource} wrapping a {@code FAFile}.
 *
 * @see Resource
 * @see FAFile
 */
public class FAFileResource extends Resource {

    private final FAFile FAFile;


    private FAFileResource(String primaryKey, FAFile FAFile) {
        super(primaryKey);
        this.FAFile = FAFile;
    }


    /**
     * Do not use directly.
     *
     * @param cachingKey The caching key.
     * @param resourceLocator The {@code URI} used when loading this
     *     resource.
     * @exception IOException if FAFile can not be opened.
     */
    public FAFileResource(String cachingKey, URI resourceLocator) throws IOException {
        super(cachingKey, resourceLocator);
        URL url = resourceLocator.toURL();
        FAFile = new FAFile(url.openStream());
    }


    /**
     * {@inheritDoc}
     */
    public void preload() {}

    /**
     * Gets the {@code FAFile} represented by this resource.
     *
     * @return The {@code FAFile} for this resource, or the default
     *     Java FAFile if none found.
     */
    public FAFile getFAFile() {
        return FAFile;
    }
}
