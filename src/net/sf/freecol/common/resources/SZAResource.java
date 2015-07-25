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

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.io.sza.SimpleZippedAnimation;


/**
 * A <code>Resource</code> wrapping a <code>SimpleZippedAnimation</code>.
 * 
 * @see Resource
 * @see SimpleZippedAnimation
 */
public class SZAResource extends Resource implements Resource.Preloadable {
    private static final Logger logger = Logger.getLogger(SZAResource.class.getName());

    private final HashMap<Float, SimpleZippedAnimation> scaledSzAnimations
        = new HashMap<>();
    private volatile SimpleZippedAnimation szAnimation = null;
    private final Object loadingLock = new Object();


    /**
     * Do not use directly.
     *
     * @param resourceLocator The <code>URI</code> used when loading this
     *     resource.
     */
    public SZAResource(URI resourceLocator) {
        super(resourceLocator);
    }


    /**
     * Preloading the animation.
     */
    @Override
    public void preload() {
        synchronized (loadingLock) {
            if (szAnimation == null) {
                try {
                    szAnimation = new SimpleZippedAnimation(
                        getResourceLocator().toURL());
                } catch (IOException e) {
                    logger.log(Level.WARNING,
                        "Could not load SimpleZippedAnimation: "
                        + getResourceLocator(), e);
                }
            }
        }
    }

    /**
     * Gets the <code>SimpleZippedAnimation</code> represented by this
     * resource.
     *
     * @return The <code>SimpleZippedAnimation</code> in it's original size.
     */
    public SimpleZippedAnimation getSimpleZippedAnimation() {
        if (szAnimation == null) {
            logger.finest("Preload not ready for " + getResourceLocator());
            preload();
        }
        return szAnimation;
    }

    /**
     * Get the <code>SimpleZippedAnimation</code> using the specified
     * scale.
     * 
     * @param scale The size of the requested animation (with 1 being normal
     *      size, 2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The <code>SimpleZippedAnimation</code>.
     */
    public SimpleZippedAnimation getSimpleZippedAnimation(float scale) {
        final SimpleZippedAnimation sza = getSimpleZippedAnimation();
        if (scale == 1.0f) {
            return sza;
        }
        final SimpleZippedAnimation cachedScaledVersion
            = scaledSzAnimations.get(scale);
        if (cachedScaledVersion != null) {
            return cachedScaledVersion;
        }
        final SimpleZippedAnimation scaledVersion
            = sza.createScaledVersion(scale);
        scaledSzAnimations.put(scale, scaledVersion);
        return scaledVersion;
    }
}
