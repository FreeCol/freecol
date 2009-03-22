/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.common.model;

import java.util.Vector;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockModelController;

public class GameTest extends FreeColTestCase {

    public void testGame() throws FreeColException {

        Game game = new ServerGame(new MockModelController());
        
        game.setMap(getTestMap());

        game.addPlayer(new Player(game, "TestPlayer", false, FreeCol.getSpecification().getNation("model.nation.dutch")));

        // map tiles are null
        //game.newTurn();

    }

    public void testAddPlayer() {
        Game game = new ServerGame(new MockModelController());
        game.setMaximumPlayers(8);

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
