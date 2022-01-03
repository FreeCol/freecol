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

package net.sf.freecol.client.gui.animation;

import java.awt.Point;
import javax.swing.JLabel;
import java.util.List;

import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * The base class for the animations.
 *
 * Note: we used to focus the map on the unit even when animation is
 * off as long as the center-active-unit option was set.  However
 * IR#115 requested that if animation is off that we display nothing
 * so as to speed up the other player moves as much as possible.
 */
public abstract class Animation {

    /** The unit to animate. */
    protected final Unit unit;

    /** The tiles where the animation occurs. */
    protected final List<Tile> tiles;

    /** Positions for the unit label during the animation. */
    protected List<Point> points;


    /**
     * Make a new animation.
     *
     * @param unit The {@code Unit} to be animated.
     * @param tiles A list of {@code Tile}s where the animation occurs.
     */
    public Animation(Unit unit, List<Tile> tiles) {
        this.unit = unit;
        this.tiles = tiles;
        this.points = null;
    }

    /**
     * Get the unit to animate.
     *
     * @return The {@code Unit}.
     */
    public Unit getUnit() {
        return this.unit;
    }

    /**
     * Get the tiles where the animation occurs.
     *
     * @return A list of {@code Tile}s.
     */
    public List<Tile> getTiles() {
        return this.tiles;
    }

    /**
     * Set the points for each tile.
     *
     * @param points A list of {@code Point}s to position the animation label.
     */
    public void setPoints(List<Point> points) {
        this.points = points;
    }
    
    /**
     * The code to be executed when a unit is out for animation.
     *
     * @param unitLabel A {@code JLabel} with an image of the unit to animate.
     * @param paintCallback A callback to request that the animation area be
     *     repainted.
     */
    public abstract void executeWithLabel(JLabel unitLabel,
                                          Animations.Procedure paintCallback);
}
