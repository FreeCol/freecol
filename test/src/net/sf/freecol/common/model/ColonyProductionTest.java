package net.sf.freecol.common.model;

import java.util.Vector;

import junit.framework.TestCase;
import net.sf.freecol.util.test.MockModelController;

public class ColonyProductionTest extends TestCase {

	public static Game getStandardGame() {
		Game game = new Game(new MockModelController());

		Vector<Player> players = new Vector<Player>();

		for (int i = 0; i < Player.NUMBER_OF_NATIONS; i++) {
			Player p = new Player(game, String.valueOf(i), false, i);
			game.addPlayer(p);
			players.add(p);
		}
		return game;
	}

	public void testProductionOne() {

		Game game = getStandardGame();

		Player dutch = game.getPlayer(Player.DUTCH);

		Vector<Vector<Tile>> tiles = new Vector<Vector<Tile>>(10);

		for (int x = 0; x < 10; x++) {
			tiles.add(new Vector<Tile>(15));
		}

		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 15; y++) {
				tiles.get(x).add(new Tile(game, Tile.PLAINS, x, y));
			}
		}

		Map map = new Map(game, tiles);

		map.getTile(5, 8).setBonus(true);
		map.getTile(5, 8).setExploredBy(dutch, true);
		map.getTile(6, 8).setExploredBy(dutch, true);
		
		game.setMap(map);
		
		Unit soldier = new Unit(game, map.getTile(6, 8), dutch, Unit.VETERAN_SOLDIER, Unit.ACTIVE,
			true, false, 0, false);
		
		Colony colony = new Colony(game, dutch, "New Amsterdam", soldier.getTile());
		soldier.setWorkType(Goods.FOOD);
		soldier.buildColony(colony);

		{ // Test the colony
			assertEquals(map.getTile(6, 8), colony.getTile());

			assertEquals("New Amsterdam", colony.getLocationName());

			assertEquals(colony, colony.getTile().getSettlement());

			assertEquals(dutch.getNation(), colony.getTile().getNationOwner());

			// Should have 50 Muskets and nothing else
			for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
				if (Goods.MUSKETS == i)
					assertEquals(50, colony.getGoodsCount(i));
				else
					assertEquals(0, colony.getGoodsCount(i));
			}
		}

		{ // Test the state of the soldier
			// Soldier should be working on the field with the bonus
			assertEquals(Goods.FOOD, soldier.getWorkType());

			assertEquals(colony.getColonyTile(map.getTile(5,8)), soldier.getLocation());

			assertEquals(0, soldier.getMovesLeft());

			assertEquals(false, soldier.isArmed());
		}
	}

}
