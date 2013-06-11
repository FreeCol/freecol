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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.generator.MapGenerator;


public class MockMapGenerator implements MapGenerator {

    private Map map;
	
    public MockMapGenerator(Map map){
        this.map = map;
    }
	
    public void createMap(Game game) throws FreeColException {
		
        // update references of game
        game.setMap(map);
        map.setGame(game);
        for(Tile tile : map.getAllTiles()){
            updateGameRefs(tile,game);
				
        }
    }

    public void createEmptyMap(Game game, boolean[][] landMap) {
        // do nothing yet
    }
	
    private void updateGameRefs(FreeColGameObject obj,Game game){
        if(obj == null)
            return;
        obj.setGame(game);
        if(obj instanceof Location){
            for (FreeColGameObject unit : ((Location) obj).getUnitList()){
                updateGameRefs(unit,game);
            }
        }	
    }

    public OptionGroup getMapGeneratorOptions() {
        return null;
    }
	
    public void setMap(Map map){
        this.map = map;
    }
}
