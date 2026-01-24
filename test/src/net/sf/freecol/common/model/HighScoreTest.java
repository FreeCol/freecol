/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;


public class HighScoreTest extends FreeColTestCase {

    private HighScore createHighScore(int score) {
        Game game = getStandardGame();
        Nation dutch = spec().getNation("model.nation.dutch");
        ServerPlayer player = new ServerPlayer(game, false, dutch);
        player.setScore(score);
        return new HighScore(player);
    }

    public void testAddHighScore() {
        Game game = getStandardGame();
        Nation dutchNation = spec().getNation("model.nation.dutch");
        ServerPlayer player = new ServerPlayer(game, false, dutchNation);
        HighScore hs = new HighScore(player);

        List<HighScore> scores = new ArrayList<>();

        assertEquals(0, HighScore.checkHighScore(hs, scores));
        scores.add(hs);

        assertEquals(-1, HighScore.checkHighScore(hs, scores));

        player.setScore(player.getScore() + 1);
        HighScore improvedHs = new HighScore(player);
        assertEquals(0, HighScore.checkHighScore(improvedHs, scores));
    }

    public void testHighScoreExpansion() {
        List<HighScore> scores = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            scores.add(createHighScore(i * 100)); // Scores: 100 to 1000
        }
        scores.sort(HighScore.descendingScoreComparator);
        assertEquals(10, scores.size());

        HighScore lowHs = createHighScore(50);
        assertEquals(-1, HighScore.checkHighScore(lowHs, scores));

        HighScore highHs = createHighScore(1000);
        int index = HighScore.checkHighScore(highHs, scores);
        assertEquals("Should replace the last index (index 9)", 9, index);
    }

    public void testEqualScores() {
        List<HighScore> scores = new ArrayList<>();

        Game game = getStandardGame();
        Nation dutch = spec().getNation("model.nation.dutch");

        ServerPlayer p1 = new ServerPlayer(game, false, dutch);
        p1.setScore(500);
        HighScore hs1 = new HighScore(p1);
        scores.add(hs1);

        ServerPlayer pSame = new ServerPlayer(game, false, dutch);
        pSame.setScore(500);
        HighScore hsSame = new HighScore(pSame);
        assertEquals(-1, HighScore.checkHighScore(hsSame, scores));

        HighScore hs2 = createHighScore(500);
        assertEquals(1, HighScore.checkHighScore(hs2, scores));
    }

    public void testInsertionWhenNotFull() {
        List<HighScore> scores = new ArrayList<>();

        HighScore hs1 = createHighScore(100);
        assertEquals(0, HighScore.checkHighScore(hs1, scores));
        scores.add(hs1);

        HighScore hs2 = createHighScore(200);
        assertEquals(1, HighScore.checkHighScore(hs2, scores));
        scores.add(hs2);

        HighScore hs3 = createHighScore(300);
        assertEquals(2, HighScore.checkHighScore(hs3, scores));
        scores.add(hs3);

        assertEquals(3, scores.size());
    }

    public void testListUnchangedOnReject() {
        List<HighScore> scores = new ArrayList<>();
        Game game = getStandardGame();
        Nation dutch = spec().getNation("model.nation.dutch");

        ServerPlayer p1 = new ServerPlayer(game, false, dutch);
        p1.setScore(300);
        HighScore hs1 = new HighScore(p1);
        scores.add(hs1);

        ServerPlayer pSame = new ServerPlayer(game, false, dutch);
        pSame.setScore(300);
        HighScore hs2 = new HighScore(pSame);

        int result = HighScore.checkHighScore(hs2, scores);

        assertEquals(-1, result);
        assertEquals(1, scores.size());
        assertSame(hs1, scores.get(0));
    }

    public void testNegativeScore() {
        HighScore hs = createHighScore(-100);
        List<HighScore> scores = new ArrayList<>();
        assertEquals(-1, HighScore.checkHighScore(hs, scores));
    }

    public void testSerializationRoundTrip() throws Exception {
        HighScore hs = createHighScore(5000);

        String playerName = hs.getPlayerName();
        int score = hs.getScore();
        String nationName = hs.getNationName();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            hs.toXML(xw);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();
            HighScore hs2 = new HighScore(xr);

            assertEquals(playerName, hs2.getPlayerName());
            assertEquals(score, hs2.getScore());
            assertEquals(nationName, hs2.getNationName());
            assertEquals(hs.getGameUUID(), hs2.getGameUUID());
            assertEquals(hs.getLevel(), hs2.getLevel());
        }
    }
}
