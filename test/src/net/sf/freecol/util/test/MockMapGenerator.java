package net.sf.freecol.util.test;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.server.generator.IMapGenerator;
import net.sf.freecol.server.generator.MapGeneratorOptions;

public class MockMapGenerator implements IMapGenerator {
	private Map map;
	
	public MockMapGenerator(Map map){
		this.map = map;
	}
	
	public void createMap(Game game) throws FreeColException {
		// TODO Auto-generated method stub
		game.setMap(map);
	}

	public MapGeneratorOptions getMapGeneratorOptions() {
		return null;
	}
	
	public void setMap(Map map){
		this.map = map;
	}
}
