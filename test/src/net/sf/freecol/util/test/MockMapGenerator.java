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
