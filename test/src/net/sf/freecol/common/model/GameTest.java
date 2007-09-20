package net.sf.freecol.common.model;

import java.util.Vector;

import junit.framework.TestCase;
import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.util.test.MockModelController;

public class GameTest extends TestCase {

	public void testGame() throws FreeColException {

		Game game = new Game(new MockModelController());
        
        game.setMap(new Map(game, Map.SMALL));

		game.addPlayer(new Player(game, "TestPlayer", false, FreeCol.getSpecification().getNation("model.nation.dutch")));

		game.newTurn();

	}

	public void testAddPlayer() {
		Game game = new Game(new MockModelController());

		Vector<Player> players = new Vector<Player>();

		for (Nation n : FreeCol.getSpecification().getNations()) {
			Player p;
			if (n.getType().isEuropean() && !n.getType().isREF()) {
				p = new Player(game, n.getType().getName(), false, n);
			} else {
				p = new Player(game, n.getType().getName(), false, true, n);
			}
			game.addPlayer(p);
			players.add(p);
		}

		assertEquals(FreeCol.getSpecification().getNations().size(), game
			.getPlayers().size());
		assertEquals(players, game.getPlayers());
	}

}
