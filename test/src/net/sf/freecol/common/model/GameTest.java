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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockModelController;

public class GameTest extends FreeColTestCase {

    public void testGame() throws FreeColException {

        Game game = new ServerGame(new MockModelController(), spec());
        game.setNationOptions(new NationOptions(spec(), Advantages.SELECTABLE));
        game.setMap(getTestMap());

        game.addPlayer(new Player(game, "TestPlayer", false, spec().getNation("model.nation.dutch")));

        // map tiles are null
        //game.newTurn();

    }

    public void testAddPlayer() {
        Game game = new ServerGame(new MockModelController(), spec());
        NationOptions defaultOptions = new NationOptions(spec(), Advantages.SELECTABLE);
        game.setNationOptions(defaultOptions);

        List<Player> players = new ArrayList<Player>();

        int counter = 0;
        for (Nation n : spec().getNations()) {
            if (defaultOptions.getNationState(n) == NationOptions.NationState.NOT_AVAILABLE) {
                counter++;
            } else {
                Player p;
                if (n.getType().isEuropean() && !n.getType().isREF()) {
                    p = new Player(game, n.getType().getNameKey(), false, n);
                } else {
                    p = new Player(game, n.getType().getNameKey(), false, true, n);
                }
                game.addPlayer(p);
                players.add(p);
            }
        }

        Collections.sort(players, Player.playerComparator);
        Collections.sort(game.getPlayers(), Player.playerComparator);
        assertEquals(spec().getNations().size() - counter,
                     game.getPlayers().size());
        assertEquals(players, game.getPlayers());
    }

    public void testTurn() {
        assertEquals(1492, Turn.getYear(1));
        assertEquals(Turn.Season.YEAR, Turn.getSeason(1));
        assertEquals(1599, Turn.getYear(108));
        assertEquals(Turn.Season.YEAR, Turn.getSeason(108));
        assertEquals(1600, Turn.getYear(109));
        assertEquals(Turn.Season.SPRING, Turn.getSeason(109));
        assertEquals(1600, Turn.getYear(110));
        assertEquals(Turn.Season.AUTUMN, Turn.getSeason(110));
        assertEquals(1700, Turn.getYear(309));
        assertEquals(Turn.Season.SPRING, Turn.getSeason(309));
        assertEquals(1700, Turn.getYear(310));
        assertEquals(Turn.Season.AUTUMN, Turn.getSeason(310));
    }

}
