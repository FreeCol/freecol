/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

package net.sf.freecol.server.generator;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.option.OptionGroup;

/**
 * Creates maps and sets the starting locations for the players.
 */
public interface MapGenerator {

    /**
     * Creates the map with the current set options
     *
     * @param game a <code>Game</code> value
     * @exception FreeColException if an error occurs
     */
    public abstract void createMap(Game game) throws FreeColException;

    /**
     * Creates a <code>Map</code> for the given <code>Game</code>.
     *
     * The <code>Map</code> is added to the <code>Game</code> after
     * it is created.
     *
     * @param game The game.
     * @param landMap Determines whether there should be land
     *                or ocean on a given tile. This array also
     *                specifies the size of the map that is going
     *                to be created.
     * @see net.sf.freecol.common.model.Map
     */
    public abstract void createEmptyMap(Game game, boolean[][] landMap);

    /**
     * Gets the options used when generating the map.
     * @return The <code>MapGeneratorOptions</code>.
     */
    public abstract OptionGroup getMapGeneratorOptions();

}