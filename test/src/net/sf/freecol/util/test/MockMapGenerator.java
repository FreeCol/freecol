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

package net.sf.freecol.util.test;

import java.util.Iterator;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.MarketData;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.generator.MapGenerator;


public class MockMapGenerator implements MapGenerator {

    private Map map;

	
    public MockMapGenerator(Map map) {
        this.map = map;
    }


    public void setMap(Map map) {
        this.map = map;
    }
    

    // Implement MapGenerator

    /**
     * {@inheritDoc}
     */
    public Map generateMap(Game game, Map importMap, LogBuilder lb) {
        // For all map descendents in the old game, move them to the
        // new game.
        Game oldGame = map.getGame();
        game.setMap(map);
        for (FreeColGameObject fcgo : oldGame.getFreeColGameObjectList()) {
            if (fcgo instanceof Europe
                || fcgo instanceof HighSeas
                || fcgo instanceof Market
                || fcgo instanceof MarketData
                || fcgo instanceof Monarch
                || fcgo instanceof Player) continue; // Not map descendents

            fcgo.setGame(game);
            FreeColGameObject other = game.getFreeColGameObject(fcgo.getId());
            if (other != fcgo) {
                if (other != null) {
                    game.removeFreeColGameObject(other.getId(), "mock");
                }
                game.addFreeColGameObject(fcgo.getId(), fcgo);
            }
        }
        return map;
    }

    /**
     * {@inheritDoc}
     */
    public Map generateEmptyMap(Game game, int width, int height,
                                LogBuilder lb) {
        return this.map; // Do nothing yet
    }
}
