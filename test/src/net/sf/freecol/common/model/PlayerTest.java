/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import java.util.Iterator;

import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class PlayerTest extends FreeColTestCase {
    
    private static final UnitType freeColonist
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");


    public void testUnits() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");
        Map map = getTestMap(spec().getTileType("model.tile.plains"));
        game.setMap(map);
        map.getTile(4, 7).setExplored(dutch, true);
        map.getTile(4, 8).setExplored(dutch, true);
        map.getTile(5, 7).setExplored(dutch, true);
        map.getTile(5, 8).setExplored(dutch, true);

        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");

        Unit unit1 = new ServerUnit(game, map.getTile(4, 7), dutch,
                                    freeColonist);
        Unit unit2 = new ServerUnit(game, map.getTile(4, 8), dutch,
                                    freeColonist);
        Unit unit3 = new ServerUnit(game, map.getTile(5, 7), dutch,
                                    freeColonist);
        Unit unit4 = new ServerUnit(game, map.getTile(5, 8), dutch,
                                    freeColonist);

        int count = 0;
        Iterator<Unit> unitIterator = dutch.getUnitIterator();
        while (unitIterator.hasNext()) {
            unitIterator.next();
            count++;
        }
        assertTrue(count == 4);

        unit1.dispose();
        assertFalse(dutch.hasUnit(unit1));

        unit2.changeOwner(french);
        assertFalse(dutch.hasUnit(unit2));
        assertTrue(french.hasUnit(unit2));
    }

    public void testEuropeanPlayer(Player player) {
        assertTrue(player.canBuildColonies());
        assertTrue(player.canHaveFoundingFathers());
        assertTrue(player.canMoveToEurope());
        assertTrue(player.isColonial());
        assertFalse(player.isDead());
        assertTrue(player.isEuropean());
        assertFalse(player.isIndian());
        assertFalse(player.isREF());
        assertEquals(2, player.getMaximumFoodConsumption());
    }

    public void testIndianPlayer(Player player) {
        assertFalse(player.canBuildColonies());
        assertFalse(player.canHaveFoundingFathers());
        assertFalse(player.canMoveToEurope());
        assertFalse(player.isColonial());
        assertFalse(player.isDead());
        assertFalse(player.isEuropean());
        assertTrue(player.isIndian());
        assertFalse(player.isREF());
        assertEquals(2, player.getMaximumFoodConsumption());
    }

    public void testRoyalPlayer(Player player) {
        assertFalse(player.canBuildColonies());
        assertFalse(player.canHaveFoundingFathers());
        assertTrue(player.canMoveToEurope());
        assertFalse(player.isColonial());
        assertEquals(player.getPlayerType(), Player.PlayerType.ROYAL);
        assertFalse(player.isDead());
        assertTrue(player.isEuropean());
        assertFalse(player.isIndian());
        assertTrue(player.isREF());
        assertEquals(2, player.getMaximumFoodConsumption());
    }

    public void testClassicPlayers() {
        Game game = getStandardGame("classic");

        // europeans
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player spanish = game.getPlayerByNationId("model.nation.spanish");

        testEuropeanPlayer(dutch);
        testEuropeanPlayer(french);
        testEuropeanPlayer(english);
        testEuropeanPlayer(spanish);

        // indians
        Player inca = game.getPlayerByNationId("model.nation.inca");
        Player aztec = game.getPlayerByNationId("model.nation.aztec");
        Player arawak = game.getPlayerByNationId("model.nation.arawak");
        Player cherokee = game.getPlayerByNationId("model.nation.cherokee");
        Player iroquois = game.getPlayerByNationId("model.nation.iroquois");
        Player sioux = game.getPlayerByNationId("model.nation.sioux");
        Player apache = game.getPlayerByNationId("model.nation.apache");
        Player tupi = game.getPlayerByNationId("model.nation.tupi");
        testIndianPlayer(inca);
        testIndianPlayer(aztec);
        testIndianPlayer(arawak);
        testIndianPlayer(cherokee);
        testIndianPlayer(iroquois);
        testIndianPlayer(sioux);
        testIndianPlayer(apache);
        testIndianPlayer(tupi);

        // royal
        Player dutchREF = game.getPlayerByNationId("model.nation.dutchREF");
        Player frenchREF = game.getPlayerByNationId("model.nation.frenchREF");
        Player englishREF = game.getPlayerByNationId("model.nation.englishREF");
        Player spanishREF = game.getPlayerByNationId("model.nation.spanishREF");
        testRoyalPlayer(dutchREF);
        testRoyalPlayer(frenchREF);
        testRoyalPlayer(englishREF);
        testRoyalPlayer(spanishREF);
        assertEquals(dutchREF, dutch.getREFPlayer());
        assertEquals(frenchREF, french.getREFPlayer());
        assertEquals(englishREF, english.getREFPlayer());
        assertEquals(spanishREF, spanish.getREFPlayer());

    }

    public void testFreecolPlayers() {
        // the initialization code is basically the same as in
        // getStandardGame(), except that all European nations are
        // available
        Specification specification = spec("freecol");
        Game game = new ServerGame(specification);
        NationOptions nationOptions = new NationOptions(specification);
        for (Nation nation : specification.getEuropeanNations()) {
            nationOptions.setNationState(nation, NationOptions.NationState.AVAILABLE);
        }
        game.setNationOptions(nationOptions);

        specification.applyDifficultyLevel("model.difficulty.medium");
        for (Nation n : specification.getNations()) {
            if (n.isUnknownEnemy()) continue;
            Player p = new ServerPlayer(game, false, n, null, null);
            p.setAI(!n.getType().isEuropean() || n.getType().isREF());
            game.addPlayer(p);
        }

        // europeans
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player spanish = game.getPlayerByNationId("model.nation.spanish");
        Player portuguese = game.getPlayerByNationId("model.nation.portuguese");
        Player swedish = game.getPlayerByNationId("model.nation.swedish");
        Player danish = game.getPlayerByNationId("model.nation.danish");
        Player russian = game.getPlayerByNationId("model.nation.russian");

        testEuropeanPlayer(dutch);
        testEuropeanPlayer(french);
        testEuropeanPlayer(english);
        testEuropeanPlayer(spanish);
        testEuropeanPlayer(portuguese);
        testEuropeanPlayer(swedish);
        testEuropeanPlayer(danish);
        testEuropeanPlayer(russian);

        // indians
        Player inca = game.getPlayerByNationId("model.nation.inca");
        Player aztec = game.getPlayerByNationId("model.nation.aztec");
        Player arawak = game.getPlayerByNationId("model.nation.arawak");
        Player cherokee = game.getPlayerByNationId("model.nation.cherokee");
        Player iroquois = game.getPlayerByNationId("model.nation.iroquois");
        Player sioux = game.getPlayerByNationId("model.nation.sioux");
        Player apache = game.getPlayerByNationId("model.nation.apache");
        Player tupi = game.getPlayerByNationId("model.nation.tupi");
        testIndianPlayer(inca);
        testIndianPlayer(aztec);
        testIndianPlayer(arawak);
        testIndianPlayer(cherokee);
        testIndianPlayer(iroquois);
        testIndianPlayer(sioux);
        testIndianPlayer(apache);
        testIndianPlayer(tupi);

        // royal
        Player dutchREF = game.getPlayerByNationId("model.nation.dutchREF");
        Player frenchREF = game.getPlayerByNationId("model.nation.frenchREF");
        Player englishREF = game.getPlayerByNationId("model.nation.englishREF");
        Player spanishREF = game.getPlayerByNationId("model.nation.spanishREF");
        Player portugueseREF = game.getPlayerByNationId("model.nation.portugueseREF");
        Player swedishREF = game.getPlayerByNationId("model.nation.swedishREF");
        Player danishREF = game.getPlayerByNationId("model.nation.danishREF");
        Player russianREF = game.getPlayerByNationId("model.nation.russianREF");
        testRoyalPlayer(dutchREF);
        testRoyalPlayer(frenchREF);
        testRoyalPlayer(englishREF);
        testRoyalPlayer(spanishREF);
        testRoyalPlayer(portugueseREF);
        testRoyalPlayer(swedishREF);
        testRoyalPlayer(danishREF);
        testRoyalPlayer(russianREF);
        assertEquals(dutchREF, dutch.getREFPlayer());
        assertEquals(frenchREF, french.getREFPlayer());
        assertEquals(englishREF, english.getREFPlayer());
        assertEquals(spanishREF, spanish.getREFPlayer());
        assertEquals(portugueseREF, portuguese.getREFPlayer());
        assertEquals(swedishREF, swedish.getREFPlayer());
        assertEquals(danishREF, danish.getREFPlayer());
        assertEquals(russianREF, russian.getREFPlayer());
    }

    public void testTension(){
        String errMsg = "";
        Game game = getStandardGame();

        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        ServerPlayer french = (ServerPlayer)game.getPlayerByNationId("model.nation.french");

        int initialTension = 500;
        int change = 250;

        dutch.setTension(french, new Tension(initialTension));
        french.setTension(dutch, new Tension(initialTension));

        dutch.getTension(french).modify(change);

        int expectedDutchTension = initialTension + change;
        int expectedFrenchTension = initialTension;

        errMsg = "Dutch tension value should have changed";
        assertEquals(errMsg, expectedDutchTension, dutch.getTension(french).getValue());
        errMsg = "French tension value should have remained the same";
        assertEquals(errMsg, expectedFrenchTension ,french.getTension(dutch).getValue());
    }

    public void testAddAnotherPlayersUnit(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch =  game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        assertEquals("Wrong number of units for dutch player",0,dutch.getUnits().size());
        assertEquals("Wrong number of units for french player",0,french.getUnits().size());

        Unit colonist = new ServerUnit(game, map.getTile(6, 8), dutch,
                                       freeColonist);
        assertTrue("Colonist should be dutch", colonist.getOwner() == dutch);
        assertEquals("Wrong number of units for dutch player",1,dutch.getUnits().size());

        try{
            french.addUnit(colonist);
            fail("An IllegalStateException should have been raised");
        }
        catch (IllegalStateException e) {
            assertTrue("Colonist owner should not have been changed", colonist.getOwner() == dutch);
            assertEquals("Wrong number of units for dutch player",1,dutch.getUnits().size());
            assertEquals("Wrong number of units for french player",0,french.getUnits().size());

        }

    }
}
