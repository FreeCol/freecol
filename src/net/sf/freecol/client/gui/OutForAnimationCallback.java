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

package net.sf.freecol.client.gui;

import javax.swing.JLabel;


/**
 * A callback interface for
 * {@link MapViewer#executeWithUnitOutForAnimation(net.sf.freecol.common.model.Unit, net.sf.freecol.common.model.Tile, OutForAnimationCallback)}.
 */
public interface OutForAnimationCallback {

    /**
     * The code to be executed when a unit is out for animation.
     *
     * @param unitLabel A <code>JLabel</code> with an image of
     *      the unit to animate.
     */
    void executeWithUnitOutForAnimation(JLabel unitLabel);
}
