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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.io.sza.SimpleZippedAnimation;


/**
 * A {@code Resource} wrapping a {@code SimpleZippedAnimation}.
 * 
 * @see Resource
 * @see SimpleZippedAnimation
 */
public class SZAResource extends Resource {
    
    private static final Logger logger = Logger.getLogger(SZAResource.class.getName());

    private static final Float DEFAULT_SCALE = 1f;
    
    private final HashMap<Float, SimpleZippedAnimation> cache
        = new HashMap<>();


    /**
     * Do not use directly.
     *
     * @param cachingKey The caching key.
     * @param resourceLocator The {@code URI} used when loading this
     *     resource.
     */
    public SZAResource(String cachingKey, URI resourceLocator) {
        super(cachingKey, resourceLocator);
    }

    /**
     * Preloading the animation.
     */
    @Override
    public void preload() {
        synchronized (this) {
            if (!this.cache.isEmpty()) return;
            try {
                SimpleZippedAnimation sza
                    = new SimpleZippedAnimation(getResourceLocator().toURL());
                this.cache.put(DEFAULT_SCALE, sza);
            } catch (IOException e) {
                logger.log(Level.WARNING,
                    "Could not load SimpleZippedAnimation: "
                    + getResourceLocator(), e);
            }
        }
    }

    /**
     * Gets the {@code SimpleZippedAnimation} represented by this
     * resource.
     *
     * @return The {@code SimpleZippedAnimation} in it's original size.
     */
    public SimpleZippedAnimation getSimpleZippedAnimation() {
        return getSimpleZippedAnimation(DEFAULT_SCALE);
    }

    /**
     * Get the {@code SimpleZippedAnimation} using the specified scale.
     * 
     * @param scale The scale of the requested animation.
     * @return The {@code SimpleZippedAnimation}.
     */
    public synchronized SimpleZippedAnimation getSimpleZippedAnimation(float scale) {
        final SimpleZippedAnimation cached = this.cache.get(scale);
        if (cached != null) return cached;
        SimpleZippedAnimation sza
            = this.cache.get(DEFAULT_SCALE).createScaledVersion(scale);
        this.cache.put(scale, sza);
        return sza;
    }
}
