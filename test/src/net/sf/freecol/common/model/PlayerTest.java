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

import java.util.Iterator;

import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class PlayerTest extends FreeColTestCase {
    UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
    UnitType galleonType = spec().getUnitType("model.unit.galleon");

    public void testUnits() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(spec().getTileType("model.tile.plains"));
        game.setMap(map);
        map.getTile(4, 7).setExploredBy(dutch, true);
        map.getTile(4, 8).setExploredBy(dutch, true);
        map.getTile(5, 7).setExploredBy(dutch, true);
        map.getTile(5, 8).setExploredBy(dutch, true);

        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");

        Unit unit1 = new ServerUnit(game, map.getTile(4, 7), dutch,
                                    freeColonist, UnitState.ACTIVE);
        Unit unit2 = new ServerUnit(game, map.getTile(4, 8), dutch,
                                    freeColonist, UnitState.ACTIVE);
        Unit unit3 = new ServerUnit(game, map.getTile(5, 7), dutch,
                                    freeColonist, UnitState.ACTIVE);
        Unit unit4 = new ServerUnit(game, map.getTile(5, 8), dutch,
                                    freeColonist, UnitState.ACTIVE);

        int count = 0;
        Iterator<Unit> unitIterator = dutch.getUnitIterator();
        while (unitIterator.hasNext()) {
            unitIterator.next();
            count++;
        }
        assertTrue(count == 4);

        assertTrue(dutch.getUnit(unit1.getId()) == unit1);
        assertTrue(dutch.getUnit(unit2.getId()) == unit2);
        assertTrue(dutch.getUnit(unit3.getId()) == unit3);
        assertTrue(dutch.getUnit(unit4.getId()) == unit4);

        String id = unit1.getId();
        unit1.dispose();
        assertTrue(dutch.getUnit(id) == null);

        unit2.setOwner(french);
        assertTrue(dutch.getUnit(unit2.getId()) == null);
        assertTrue(french.getUnit(unit2.getId()) == unit2);

    }

    public void testEuropeanPlayer(Player player) {
        assertTrue(player.canBuildColonies());
        assertTrue(player.canHaveFoundingFathers());
        assertTrue(player.canMoveToEurope());
        assertTrue(player.canRecruitUnits());
        assertEquals(player.getPlayerType(), Player.PlayerType.COLONIAL);
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
        assertFalse(player.canRecruitUnits());
        assertEquals(player.getPlayerType(), Player.PlayerType.NATIVE);
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
        assertFalse(player.canRecruitUnits());
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
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Player english = game.getPlayer("model.nation.english");
        Player spanish = game.getPlayer("model.nation.spanish");

        testEuropeanPlayer(dutch);
        testEuropeanPlayer(french);
        testEuropeanPlayer(english);
        testEuropeanPlayer(spanish);

        // indians
        Player inca = game.getPlayer("model.nation.inca");
        Player aztec = game.getPlayer("model.nation.aztec");
        Player arawak = game.getPlayer("model.nation.arawak");
        Player cherokee = game.getPlayer("model.nation.cherokee");
        Player iroquois = game.getPlayer("model.nation.iroquois");
        Player sioux = game.getPlayer("model.nation.sioux");
        Player apache = game.getPlayer("model.nation.apache");
        Player tupi = game.getPlayer("model.nation.tupi");
        testIndianPlayer(inca);
        testIndianPlayer(aztec);
        testIndianPlayer(arawak);
        testIndianPlayer(cherokee);
        testIndianPlayer(iroquois);
        testIndianPlayer(sioux);
        testIndianPlayer(apache);
        testIndianPlayer(tupi);

        // royal
        Player dutchREF = game.getPlayer("model.nation.dutchREF");
        Player frenchREF = game.getPlayer("model.nation.frenchREF");
        Player englishREF = game.getPlayer("model.nation.englishREF");
        Player spanishREF = game.getPlayer("model.nation.spanishREF");
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
        Specification specification = getSpecification("freecol");
        Game game = new ServerGame(specification);
        NationOptions nationOptions = new NationOptions(specification, NationOptions.Advantages.SELECTABLE);
        for (Nation nation : specification.getEuropeanNations()) {
            nationOptions.setNationState(nation, NationOptions.NationState.AVAILABLE);
        }
        game.setNationOptions(nationOptions);

        specification.applyDifficultyLevel("model.difficulty.medium");
        for (Nation n : specification.getNations()) {
            Player p = new ServerPlayer(game, n.getRulerNameKey(), false, n,
                                        null, null);
            p.setAI(!n.getType().isEuropean() || n.getType().isREF());
            game.addPlayer(p);
        }

        // europeans
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Player english = game.getPlayer("model.nation.english");
        Player spanish = game.getPlayer("model.nation.spanish");
        Player portuguese = game.getPlayer("model.nation.portuguese");
        Player swedish = game.getPlayer("model.nation.swedish");
        Player danish = game.getPlayer("model.nation.danish");
        Player russian = game.getPlayer("model.nation.russian");

        testEuropeanPlayer(dutch);
        testEuropeanPlayer(french);
        testEuropeanPlayer(english);
        testEuropeanPlayer(spanish);
        testEuropeanPlayer(portuguese);
        testEuropeanPlayer(swedish);
        testEuropeanPlayer(danish);
        testEuropeanPlayer(russian);

        // indians
        Player inca = game.getPlayer("model.nation.inca");
        Player aztec = game.getPlayer("model.nation.aztec");
        Player arawak = game.getPlayer("model.nation.arawak");
        Player cherokee = game.getPlayer("model.nation.cherokee");
        Player iroquois = game.getPlayer("model.nation.iroquois");
        Player sioux = game.getPlayer("model.nation.sioux");
        Player apache = game.getPlayer("model.nation.apache");
        Player tupi = game.getPlayer("model.nation.tupi");
        testIndianPlayer(inca);
        testIndianPlayer(aztec);
        testIndianPlayer(arawak);
        testIndianPlayer(cherokee);
        testIndianPlayer(iroquois);
        testIndianPlayer(sioux);
        testIndianPlayer(apache);
        testIndianPlayer(tupi);

        // royal
        Player dutchREF = game.getPlayer("model.nation.dutchREF");
        Player frenchREF = game.getPlayer("model.nation.frenchREF");
        Player englishREF = game.getPlayer("model.nation.englishREF");
        Player spanishREF = game.getPlayer("model.nation.spanishREF");
        Player portugueseREF = game.getPlayer("model.nation.portugueseREF");
        Player swedishREF = game.getPlayer("model.nation.swedishREF");
        Player danishREF = game.getPlayer("model.nation.danishREF");
        Player russianREF = game.getPlayer("model.nation.russianREF");
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

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        int initialTension = 500;
        int change = 250;

        dutch.setTension(french, new Tension(initialTension));
        french.setTension(dutch, new Tension(initialTension));

        dutch.modifyTension(french, change);

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

        Player dutch =  game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        assertEquals("Wrong number of units for dutch player",0,dutch.getUnits().size());
        assertEquals("Wrong number of units for french player",0,french.getUnits().size());

        Unit colonist = new ServerUnit(game, map.getTile(6, 8), dutch,
                                       freeColonist, UnitState.ACTIVE);
        assertTrue("Colonist should be dutch", colonist.getOwner() == dutch);
        assertEquals("Wrong number of units for dutch player",1,dutch.getUnits().size());

        try{
            french.setUnit(colonist);
            fail("An IllegalStateException should have been raised");
        }
        catch(IllegalStateException e){
            assertTrue("Colonist owner should not have been changed", colonist.getOwner() == dutch);
            assertEquals("Wrong number of units for dutch player",1,dutch.getUnits().size());
            assertEquals("Wrong number of units for french player",0,french.getUnits().size());

        }

    }
}
