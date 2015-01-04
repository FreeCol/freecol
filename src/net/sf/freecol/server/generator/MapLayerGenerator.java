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

/**
 * Generates a map layer.
 */
public interface MapLayerGenerator {

    /**
     * Generates the layer in the map of the given game. Returns true
     * if the layer was generated, and false otherwise.
     *
     * @param game a <code>Game</code> value
     * @return a <code>boolean</code> value
     */
    public boolean generateLayer(Game game);

    /**
     * Returns the Layer this MapLayerGenerator is able to generate.
     *
     * @return a <code>Layer</code> value
     */
    public Layer getLayer();

}