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

package net.sf.freecol.common.io.sza;

import java.awt.Image;

/**
 * An event describing that the given image should
 * be displayed.
 */
public interface ImageAnimationEvent extends AnimationEvent {

    /**
     * Returns the image this event contains.
     * @return The image to be displayed. 
     */
    public Image getImage();
    
    /**
     * Returns the duration the image should be displayed.
     * @return The amount of milliseconds to display the
     *      image.
     */
    public int getDurationInMs();
}
