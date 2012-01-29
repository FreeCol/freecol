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
import java.util.logging.Logger;

import net.sf.freecol.client.gui.video.Video;

/**
 * A <code>Resource</code> wrapping a <code>Video</code>.
 * 
 * @see Resource
 * @see Video
 */
public class VideoResource extends Resource {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(VideoResource.class.getName());

    private final Video video;
    
    /**
     * Do not use directly.
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URI)
     */
    VideoResource(URI resourceLocator) throws Exception {
        super(resourceLocator);
        
        this.video = new Video(resourceLocator.toURL());
    }
    
    /**
     * Preloading is a noop for this resource type.
     */
    public void preload() {}
    
    /**
     * Gets the <code>Video</code> represented by this resource.
     * @return The <code>Video</code> in it's original size.
     */
    public Video getVideo() {
        return video;
    }
}
