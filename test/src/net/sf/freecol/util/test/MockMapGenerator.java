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

package net.sf.freecol.util.test;

import java.util.Iterator;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.MarketData;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.generator.MapGenerator;


public class MockMapGenerator implements MapGenerator {

    private Map map;
	
    public MockMapGenerator(Map map) {
        this.map = map;
    }
	
    public void createMap(Game game) throws FreeColException {
        // For all map descendents in the old game, move them to the
        // new game.
        Game oldGame = map.getGame();
        Iterator<FreeColGameObject> fcgoi = oldGame.getFreeColGameObjectIterator();
        game.setMap(map);
        while (fcgoi.hasNext()) {
            FreeColGameObject fcgo = fcgoi.next();
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
                    net.sf.freecol.common.debug.FreeColDebugger.debugLog("REMOVE " + other.getId());
                    game.removeFreeColGameObject(other.getId());
                }
                game.setFreeColGameObject(fcgo.getId(), fcgo);
                net.sf.freecol.common.debug.FreeColDebugger.debugLog("ADDED " + fcgo.getId());
            }
        }
    }

    public void createEmptyMap(Game game, boolean[][] landMap) {
        // do nothing yet
    }
	
    public OptionGroup getMapGeneratorOptions() {
        return null;
    }
	
    public void setMap(Map map) {
        this.map = map;
    }
}
