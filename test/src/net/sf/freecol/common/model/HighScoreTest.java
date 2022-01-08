/**
 *  Copyright (C) 2002-2022  The FreeCol Team
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
import java.util.List;

import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;


public class HighScoreTest extends FreeColTestCase {

    public void testAddHighScore() {
        Game game = getStandardGame();
        assertNotNull(game.getUUID());

        Nation dutchNation = spec().getNation("model.nation.dutch");
        ServerPlayer player = new ServerPlayer(game, false, dutchNation);
        HighScore hs = new HighScore(player);

        List<HighScore> scores = new ArrayList<>();
        assertEquals(0, HighScore.checkHighScore(hs, scores));
        scores.add(hs);
        assertEquals(-1, HighScore.checkHighScore(hs, scores));
        player.setScore(player.getScore() + 1);
        hs = new HighScore(player);
        assertEquals(0, HighScore.checkHighScore(hs, scores));
	}

}
