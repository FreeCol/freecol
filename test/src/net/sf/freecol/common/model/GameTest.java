/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;


public class GameTest extends FreeColTestCase {

    public void testGame() throws FreeColException {

        Game game = new ServerGame(spec());
        game.setNationOptions(new NationOptions(spec(), Advantages.SELECTABLE));
        game.setMap(getTestMap());

        game.addPlayer(new ServerPlayer(game, "TestPlayer", false,
                                        spec().getNation("model.nation.dutch"),
                                        null, null));

        // map tiles are null
        //game.newTurn();

    }

    public void testAddPlayer() {
        Game game = new ServerGame(spec());
        NationOptions defaultOptions = new NationOptions(spec(), Advantages.SELECTABLE);
        game.setNationOptions(defaultOptions);

        List<Player> players = new ArrayList<Player>();

        int counter = 0;
        for (Nation n : spec().getNations()) {
            if (defaultOptions.getNationState(n) == NationOptions.NationState.NOT_AVAILABLE) {
                counter++;
            } else {
                Player p = new ServerPlayer(game, n.getType().getNameKey(),
                                            false, n, null, null);
                p.setAI(!n.getType().isEuropean() || n.getType().isREF());
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
        assertEquals(1, Turn.yearToTurn(1492, Turn.Season.YEAR));
        assertEquals(Turn.Season.YEAR, Turn.getSeason(1));
        assertEquals(1, Turn.getAge(1492));

        assertEquals(1599, Turn.getYear(108));
        assertEquals(108, Turn.yearToTurn(1599, Turn.Season.YEAR));
        assertEquals(Turn.Season.YEAR, Turn.getSeason(108));
        assertEquals(1, Turn.getAge(1599));

        assertEquals(1600, Turn.getYear(109));
        assertEquals(109, Turn.yearToTurn(1600, Turn.Season.SPRING));
        assertEquals(Turn.Season.SPRING, Turn.getSeason(109));
        assertEquals(1600, Turn.getYear(110));
        assertEquals(110, Turn.yearToTurn(1600, Turn.Season.AUTUMN));
        assertEquals(Turn.Season.AUTUMN, Turn.getSeason(110));
        assertEquals(2, Turn.getAge(1600));

        assertEquals(1700, Turn.getYear(309));
        assertEquals(309, Turn.yearToTurn(1700, Turn.Season.SPRING));
        assertEquals(Turn.Season.SPRING, Turn.getSeason(309));
        assertEquals(1700, Turn.getYear(310));
        assertEquals(310, Turn.yearToTurn(1700, Turn.Season.AUTUMN));
        assertEquals(Turn.Season.AUTUMN, Turn.getSeason(310));
        assertEquals(3, Turn.getAge(1700));
    }

}
