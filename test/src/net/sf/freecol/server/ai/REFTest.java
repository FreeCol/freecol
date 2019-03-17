/**
 *  Copyright (C) 2002-2019  The FreeCol Team
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

package net.sf.freecol.server.ai;

import java.util.List;
import java.util.Random;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Force;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;


public class REFTest extends FreeColTestCase {

    private static final UnitType artilleryType
        = spec().getUnitType("model.unit.artillery");
    private static final UnitType soldierType
        = spec().getUnitType("model.unit.kingsRegular");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    public void testCreateREFPlayer() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        final Specification spec = game.getSpecification();
        InGameController igc = ServerTestHelper.getInGameController();

        // Create player
        final ServerPlayer player1 = getServerPlayer(game, "model.nation.dutch");
        final Force exf = player1.getMonarch().getExpeditionaryForce();
        // Update to have full naval capacity
        exf.prepareToBoard();
        List<AbstractUnit> refBeforeIndependence = exf.getUnitList();
        
        ServerPlayer refPlayer = igc.createREFPlayer(player1);

        assertNotNull("REF player is null", refPlayer);
        assertNotNull("Player ref is null", player1.getREFPlayer());
        assertEquals("REF player should be player1 ref", refPlayer,
            player1.getREFPlayer());

        Force newf = new Force(spec);
        for (Unit u : refPlayer.getUnitSet()) {
            newf.add(new AbstractUnit(u.getType(), u.getRole().getId(), 1));
        }
        assertTrue("REF player force != Player monarch Expeditionary force",
                   exf.matchAll(newf));
    }


    public void testAddToREF() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        final Specification spec = game.getSpecification();
        InGameController igc = ServerTestHelper.getInGameController();
        Random random = ServerTestHelper.getServer().getServerRandom();

        // Create player
        final ServerPlayer player1 = getServerPlayer(game, "model.nation.dutch");
        final Monarch monarch = player1.getMonarch();
        game.setTurn(new Turn(200));
        assertTrue("REF addition",
                   monarch.actionIsValid(Monarch.MonarchAction.ADD_TO_REF));

        // If the REF has too many land units, it should add more naval,
        // if it has enough naval it should add more land.
        // If this does not happen, this test will fail by looping forever.
        final Force exf = monarch.getExpeditionaryForce();
        int done = 0;
        while (done != 3) {
            boolean naval = exf.getCapacity() < exf.getSpaceRequired();
            AbstractUnit au = monarch.addToREF(random);
            assertNotNull("REF add", au);
            if (naval) {
                assertTrue("Naval unit required", au.getType(spec).isNaval());
            } else {
                assertFalse("Land unit required", au.getType(spec).isNaval());
            }
            if (au.getType(spec).isNaval()) done |= 2; else done |= 1;
        }
    }
}
