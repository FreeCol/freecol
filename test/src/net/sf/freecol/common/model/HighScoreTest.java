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

    public void testHighScoreWithSameUuidAndHigherScoreIsUpdated() {
        Game game = getStandardGame();
        Nation dutch = spec().getNation("model.nation.dutch");

        ServerPlayer p1 = new ServerPlayer(game, false, dutch);
        p1.setScore(500);
        HighScore originalHs = new HighScore(p1);
        
        List<HighScore> scores = new ArrayList<>();
        scores.add(originalHs);

        ServerPlayer pUpdated = new ServerPlayer(game, false, dutch);
        pUpdated.setScore(800);
        
        HighScore updatedHs = new HighScore(pUpdated);
        
        int index = HighScore.checkHighScore(updatedHs, scores);
        
        assertEquals("Should target index 0 for replacement", 0, index);
    }

    public void testHighScoreWithSameUuidAndLowerScoreIsRejected() {
        Game game = getStandardGame();
        Nation dutch = spec().getNation("model.nation.dutch");

        ServerPlayer p1 = new ServerPlayer(game, false, dutch);
        p1.setScore(800);
        HighScore originalHs = new HighScore(p1);
        
        List<HighScore> scores = new ArrayList<>();
        scores.add(originalHs);

        ServerPlayer pLower = new ServerPlayer(game, false, dutch);
        pLower.setScore(500);
        HighScore lowerHs = new HighScore(pLower);

        int index = HighScore.checkHighScore(lowerHs, scores);

        assertEquals("Should reject lower score from the same game UUID", -1, index);
    }

    public void testHighScoreWithSameUuidAndEqualScoreIsRejected() {
        Game game = getStandardGame();
        Nation dutch = spec().getNation("model.nation.dutch");

        ServerPlayer p1 = new ServerPlayer(game, false, dutch);
        p1.setScore(500);
        HighScore originalHs = new HighScore(p1);
        
        List<HighScore> scores = new ArrayList<>();
        scores.add(originalHs);

        ServerPlayer pEqual = new ServerPlayer(game, false, dutch);
        pEqual.setScore(500);
        HighScore equalHs = new HighScore(pEqual);

        int index = HighScore.checkHighScore(equalHs, scores);

        assertEquals("Should reject equal score from the same game UUID", -1, index);
    }

    // --- tidyScores Tests ---

    public void testTidyScoresTrimsToTen() {
        List<HighScore> scores = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            scores.add(createHighScore(i * 100));
        }

        HighScore.tidyScores(scores);

        assertEquals("List should be trimmed to NUMBER_OF_HIGH_SCORES",
                     HighScore.NUMBER_OF_HIGH_SCORES, scores.size());

        assertEquals(2000, scores.get(0).getScore());
        assertEquals(1100, scores.get(HighScore.NUMBER_OF_HIGH_SCORES - 1).getScore());
    }

    public void testTidyScoresSortsCorrectly() {
        List<HighScore> scores = new ArrayList<>();

        scores.add(createHighScore(300));
        scores.add(createHighScore(1000));
        scores.add(createHighScore(700));
        scores.add(createHighScore(500));

        HighScore.tidyScores(scores);

        assertEquals(4, scores.size());
        assertEquals(1000, scores.get(0).getScore());
        assertEquals(700, scores.get(1).getScore());
        assertEquals(500, scores.get(2).getScore());
        assertEquals(300, scores.get(3).getScore());
    }

    public void testTidyScoresDoesNotTrimWhenExactlyFull() {
        List<HighScore> scores = new ArrayList<>();

        for (int i = 1; i <= HighScore.NUMBER_OF_HIGH_SCORES; i++) {
            scores.add(createHighScore(i * 100));
        }

        HighScore.tidyScores(scores);

        assertEquals(HighScore.NUMBER_OF_HIGH_SCORES, scores.size());
    }

    public void testTidyScoresHandlesEmptyList() {
        List<HighScore> scores = new ArrayList<>();

        HighScore.tidyScores(scores);

        assertTrue(scores.isEmpty());
    }

    public void testTidyScoresHandlesSingleElement() {
        List<HighScore> scores = new ArrayList<>();
        scores.add(createHighScore(500));

        HighScore.tidyScores(scores);

        assertEquals(1, scores.size());
        assertEquals(500, scores.get(0).getScore());
    }

    // --- XML Round-Trip & Integration Tests ---

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

    public void testHighScoreXMLRoundTripMultiple() throws Exception {
        List<HighScore> scores = new ArrayList<>();

        scores.add(createHighScore(1000));
        scores.add(createHighScore(800));
        scores.add(createHighScore(600));
        scores.add(createHighScore(400));
        scores.add(createHighScore(200));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement("highScores");
            for (HighScore hs : scores) hs.toXML(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        List<HighScore> loaded = new ArrayList<>();

        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();
            while (xr.moreTags()) {
                if (HighScore.TAG.equals(xr.getLocalName())) {
                    loaded.add(new HighScore(xr));
                }
            }
        }

        assertEquals("Should load the same number of scores", scores.size(), loaded.size());

        for (int i = 0; i < scores.size(); i++) {
            assertEquals(scores.get(i).getScore(), loaded.get(i).getScore());
            assertEquals(scores.get(i).getPlayerName(), loaded.get(i).getPlayerName());
            assertEquals(scores.get(i).getGameUUID(), loaded.get(i).getGameUUID());
            assertEquals(scores.get(i).getLevel(), loaded.get(i).getLevel());
        }
    }

    public void testSaveAndLoadHighScoresIntegration() throws Exception {
        List<HighScore> scores = new ArrayList<>();

        scores.add(createHighScore(1200));
        scores.add(createHighScore(900));
        scores.add(createHighScore(300));

        assertTrue("Saving high scores should succeed", HighScore.saveHighScores(scores));

        List<HighScore> loaded = HighScore.loadHighScores();

        assertEquals("Loaded list should match saved list size",
                     scores.size(), loaded.size());

        assertEquals(1200, loaded.get(0).getScore());
        assertEquals(900, loaded.get(1).getScore());
        assertEquals(300, loaded.get(2).getScore());
    }

    public void testHighScoreXMLRoundTripPreservesDate() throws Exception {
        HighScore hs = createHighScore(5000);
        long originalDate = hs.getDate().getTime();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            hs.toXML(xw);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        HighScore hs2;

        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();
            hs2 = new HighScore(xr);
        }

        assertEquals("Date should roundtrip correctly",
                     originalDate, hs2.getDate().getTime());
    }

    public void testHighScoreXMLRoundTripNationMetadata() throws Exception {
        HighScore hs = createHighScore(3000);

        String nationId = hs.getNationId();
        String nationTypeId = hs.getNationTypeId();
        String nationName = hs.getNationName();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            hs.toXML(xw);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        HighScore hs2;

        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();
            hs2 = new HighScore(xr);
        }

        assertEquals(nationId, hs2.getNationId());
        assertEquals(nationTypeId, hs2.getNationTypeId());
        assertEquals(nationName, hs2.getNationName());
    }

    public void testHighScoreXMLRoundTripGameMetadata() throws Exception {
        HighScore hs = createHighScore(4000);

        String difficulty = hs.getDifficulty();
        int colonies = hs.getColonyCount();
        int units = hs.getUnitCount();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            hs.toXML(xw);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        HighScore hs2;

        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();
            hs2 = new HighScore(xr);
        }

        assertEquals(difficulty, hs2.getDifficulty());
        assertEquals(colonies, hs2.getColonyCount());
        assertEquals(units, hs2.getUnitCount());
    }
}
