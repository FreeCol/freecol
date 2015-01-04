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

package net.sf.freecol.server.generator;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map.Layer;


public interface MapLoader {

    /**
     * Load a map into the given game, copying all layers up to the
     * given layer. Returns the highest layer actually copied,
     * e.g. NONE if map loading failed, or the highest level available
     * if an even higher level was requested.
     *
     * @param game a <code>Game</code> value
     * @param layer a <code>Layer</code> value
     * @return a <code>Layer</code> value
     */
    public Layer loadMap(Game game, Layer layer);


    /**
     * Returns the highest layer this MapLoader is able to load.
     *
     * @return a <code>Layer</code> value
     */
    public Layer getHighestLayer();

}