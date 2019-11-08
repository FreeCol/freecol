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

package net.sf.freecol.server.generator;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.util.LogBuilder;


/**
 * Creates maps and sets the starting locations for the players.
 */
public interface MapGenerator {

    /**
     * Create an empty map.
     *
     * @param game The {@code Game} to generate for.
     * @param width The map width.
     * @param height The map height.
     * @param lb A {@code LogBuilder} to log to.
     * @return A new empty {@code Map}.
     */
    public abstract Map generateEmptyMap(Game game, int width, int height,
                                         LogBuilder lb);

    /**
     * Creates the map with the current set options
     *
     * @param game The {@code Game} to generate for.
     * @param importMap An optional {@code Map} to import.
     * @param lb A {@code LogBuilder} to log to.
     * @return The new {@code Map}.
     */
    public abstract Map generateMap(Game game, Map importMap, LogBuilder lb);
}
