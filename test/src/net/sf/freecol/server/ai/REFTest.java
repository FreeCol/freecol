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

package net.sf.freecol.server.ai;

import java.util.List;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Game;
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
        InGameController igc = ServerTestHelper.getInGameController();

        // Create player
        ServerPlayer player1 = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");
        List<AbstractUnit> refUnitsBeforeIndependence = player1.getMonarch()
            .getExpeditionaryForce().getUnits();
        int soldiersBeforeIndependence = 0;
        int dragoonsBeforeIndependence = 0;
        int artilleryBeforeIndependence = 0;
        int shipsBeforeIndependence = 0;
        for (AbstractUnit au : refUnitsBeforeIndependence) {
            UnitType unitType = spec().getUnitType(au.getId());
            if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                shipsBeforeIndependence += au.getNumber();
                continue;
            }
            if (unitType == artilleryType) {
                artilleryBeforeIndependence += au.getNumber();
                continue;
            }
            if (unitType == soldierType) {
                if ("model.role.infantry".equals(au.getRoleId())) {
                    soldiersBeforeIndependence += au.getNumber();
                } else if ("model.role.cavalry".equals(au.getRoleId())) {
                    dragoonsBeforeIndependence += au.getNumber();
                } else {
                    fail("Unknown REF role for " + unitType.getId()
                        + ": " + au.getRoleId());
                }
                continue;
            }
            fail("Unknown REF unit: " + au);
        }

        ServerPlayer refPlayer = igc.createREFPlayer(player1);

        assertNotNull("REF player is null", refPlayer);
        assertNotNull("Player ref is null", player1.getREFPlayer());
        assertEquals("REF player should be player1 ref", refPlayer,
            player1.getREFPlayer());

        // Execute
        List<Unit> refUnitsAfterIndependence = refPlayer.getUnits();

        // Get results
        int soldiersAfterIndependence = 0;
        int dragoonsAfterIndependence = 0;
        int artilleryAfterIndependence = 0;
        int shipsAfterIndependence = 0;
        for (Unit unit : refUnitsAfterIndependence) {
            UnitType unitType = unit.getType();
            if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                shipsAfterIndependence++;
                continue;
            }
            if (unitType == artilleryType) {
                artilleryAfterIndependence++;
                continue;
            }
            if (unitType == soldierType) {
                if (unit.isArmed() && !unit.isMounted()) {
                    soldiersAfterIndependence++;
                } else if (unit.isArmed() && unit.isMounted()) {
                    dragoonsAfterIndependence++;
                } else {
                    fail("Unknown REF role: " + unit.getRole());
                }
                continue;
            }
            fail("Unknown REF unit: " +  unit.toString());
        }

        // Verify results
        assertEquals("Wrong number of ships", shipsBeforeIndependence,
                     shipsAfterIndependence);
        assertEquals("Wrong number of artillery", artilleryBeforeIndependence,
                     artilleryAfterIndependence);
        assertEquals("Wrong number of soldiers", soldiersBeforeIndependence,
                     soldiersAfterIndependence);
        assertEquals("Wrong number of dragoons", dragoonsBeforeIndependence,
                     dragoonsAfterIndependence);
    }
}
