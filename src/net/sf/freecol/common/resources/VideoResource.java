/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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


/**
 * A {@code Resource} wrapping a {@code Video}.
 * 
 * @see Resource
 * @see Video
 */
public class VideoResource extends Resource {

    private final Video video;

    
    /**
     * Do not use directly.
     *
     * @param resourceLocator The {@code URI} used when loading this
     *     resource.
     * @exception IOException if the URI is malformed.
     */
    public VideoResource(URI resourceLocator) throws IOException {
        super(resourceLocator);
        
        this.video = new Video(resourceLocator.toURL());
    }


    /**
     * {@inheritDoc}
     */
    public void preload() {}

    /**
     * Gets the {@code Video} represented by this resource.
     *
     * @return The {@code Video} in its original size.
     */
    public Video getVideo() {
        return this.video;
    }
}
