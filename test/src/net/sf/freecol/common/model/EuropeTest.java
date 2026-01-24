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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

public class EuropeTest extends FreeColTestCase {

    private static final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");

    public void testMissionary() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe amsterdam = dutch.getEurope();

        new ServerUnit(game, amsterdam, dutch, colonistType);

        assertTrue("Europe should allow dressing missionaries",
            amsterdam.hasAbility(Ability.DRESS_MISSIONARY));
    }

    public void testRecruitPriceCalculation() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe amsterdam = dutch.getEurope();

        dutch.setImmigrationRequired(200);

        dutch.setImmigration(0);
        assertEquals(200, amsterdam.getCurrentRecruitPrice());

        dutch.setImmigration(100);
        assertEquals(100, amsterdam.getCurrentRecruitPrice());

        dutch.setImmigration(190);
        assertEquals(80, amsterdam.getCurrentRecruitPrice());
    }

    public void testImmigrationPenalties() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe amsterdam = dutch.getEurope();

        int baseImmigration = amsterdam.getImmigration(0);

        new ServerUnit(game, amsterdam, dutch, colonistType);
        new ServerUnit(game, amsterdam, dutch, colonistType);
        new ServerUnit(game, amsterdam, dutch, colonistType);

        assertTrue("Immigration should be penalized by units in Europe",
            amsterdam.getImmigration(0) < baseImmigration);
    }

    public void testPriceBoycottedGoods() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe amsterdam = dutch.getEurope();
        GoodsType rum = spec().getGoodsType("model.goods.rum");

        dutch.getMarket().setArrears(rum, 1000);

        List<AbstractGoods> goods = new ArrayList<>();
        goods.add(new AbstractGoods(rum, 100));

        try {
            amsterdam.priceGoods(goods);
            fail("Should have thrown FreeColException due to boycott");
        } catch (FreeColException e) {
            assertTrue(e.getMessage().contains("Can not trade"));
        }
    }

    public void testUnitStateOnArrival() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe amsterdam = dutch.getEurope();
        UnitType shipType = spec().getUnitType("model.unit.caravel");

        Unit ship = new ServerUnit(game, amsterdam, dutch, shipType);
        Unit colonist = new ServerUnit(game, amsterdam, dutch, colonistType);

        assertEquals("Ships should be ACTIVE", Unit.UnitState.ACTIVE, ship.getState());
        assertEquals("Colonists should be SENTRY", Unit.UnitState.SENTRY, colonist.getState());
    }

    public void testEuropeSerializationRoundTrip() throws Exception {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        String playerId = dutch.getId();
        Europe europe = dutch.getEurope();

        europe.baseRecruitPrice = 350;
        europe.recruitLowerCap = 120;
        europe.getRecruitables().clear();
        UnitType artillery = spec().getUnitType("model.unit.artillery");
        europe.addRecruitable(new AbstractUnit(artillery, "model.role.default", 1), true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            europe.toXML(xw);
        }

        Game game2 = new Game(spec());
        Player dutch2 = new Player(game2, playerId);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();

            Europe europe2 = new Europe(game2, europe.getId());
            europe2.readFromXML(xr);

            assertEquals("Base recruit price mismatch", 350, europe2.getBaseRecruitPrice());
            assertEquals("Lower cap mismatch", 120, europe2.getRecruitLowerCap());
            assertEquals("Artillery preserved", artillery, europe2.getRecruitables().get(0).getType(spec()));
            assertEquals("Owner should be the player we created", dutch2, europe2.getOwner());
        }
    }
}
