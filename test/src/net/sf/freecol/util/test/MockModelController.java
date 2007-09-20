package net.sf.freecol.util.test;

import java.util.ArrayList;
import java.util.Random;

import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

public class MockModelController implements ModelController {

	public Unit createUnit(String taskID, Location location, Player owner, int type) {
		// TODO Auto-generated method stub
		return null;
	}

	public void exploreTiles(Player player, ArrayList<Tile> tiles) {
		// TODO Auto-generated method stub
		
	}

	public TradeRoute getNewTradeRoute(Player player) {
		// TODO Auto-generated method stub
		return null;
	}

	public PseudoRandom getPseudoRandom() {
		return new PseudoRandom(){

			Random r = new Random(0);
			
			public int nextInt(int n) {
				return r.nextInt(n);
			}
			
		};
	}

	public int getRandom(String taskID, int n) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setStance(Player first, Player second, int stance) {
		// TODO Auto-generated method stub
		
	}

	public Location setToVacantEntryLocation(Unit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * All objects receive a new turn. 
	 */
	public boolean shouldCallNewTurn(FreeColGameObject freeColGameObject) {
		return true;
	}

	public Unit createUnit(String taskID, Location location, Player owner,
		UnitType type) {
		// TODO Auto-generated method stub
		return null;
	}

}
