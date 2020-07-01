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

package net.sf.freecol.client.gui.animation;

import javax.swing.JLabel;

import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * A callback interface for animations.
 *
 * Note: we used to focus the map on the unit even when animation is
 * off as long as the center-active-unit option was set.  However
 * IR#115 requested that if animation is off that we display nothing
 * so as to speed up the other player moves as much as possible.
 */
public interface Animation {

    /**
     * Get the unit to animate.
     *
     * @return The {@code Unit}.
     */
    public Unit getUnit();

    /**
     * Get the tile where the animation occurs.
     *
     * @return The {@code Tile}.
     */
    public Tile getTile();

    /**
     * The code to be executed when a unit is out for animation.
     *
     * @param unitLabel A {@code JLabel} with an image of
     *      the unit to animate.
     */
    public void executeWithUnitOutForAnimation(JLabel unitLabel);
}
