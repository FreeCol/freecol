/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class REFTest extends FreeColTestCase {

    FreeColServer server = null;
	
    public void tearDown(){
        if(server != null){
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;
        }
        setGame(null);
    }
	
    public void testCreateREFPlayer() {
        // start a server
        server = ServerTestHelper.startServer(false, true);
        
        Map map = getTestMap();
        
        server.setMapGenerator(new MockMapGenerator(map));
        
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        
        Game game = server.getGame();

        c = server.getController();
        assertTrue("Controller not instance of InGameController", c instanceof InGameController);
        InGameController igc = (InGameController) c;

        assertNotNull("InGameController is null",igc);

        // Create player
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");

        ServerPlayer refPlayer = igc.createREFPlayer(player1);

        assertNotNull("REF player is null",refPlayer);
        assertNotNull("Player ref is null",player1.getREFPlayer());
        assertEquals("REF player should be player1 ref", refPlayer, player1.getREFPlayer());        
    }
	
    public void testCreateREFUnits() {
        UnitType soldierType = spec().getUnitType("model.unit.kingsRegular");
        UnitType artilleryType = spec().getUnitType("model.unit.artillery");

        // start a server
        server = ServerTestHelper.startServer(false, true);

        Map map = getTestMap();

        server.setMapGenerator(new MockMapGenerator(map));

        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;

        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }

        Game game = server.getGame();

        c = server.getController();
        InGameController igc = (InGameController) c;

        // Create player
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer refPlayer = igc.createREFPlayer(player1);

        List <AbstractUnit> refUnitsBeforeIndependence = player1.getMonarch().getREF();
        int soldiersBeforeIndependence = 0;
        int dragoonsBeforeIndependence = 0;
        int artilleryBeforeIndependence = 0;
        int shipsBeforeIndependence = 0; 
        for(AbstractUnit unit : refUnitsBeforeIndependence){
            UnitType unitType = unit.getUnitType(spec());
            if(unitType.hasAbility("model.ability.navalUnit")){
                shipsBeforeIndependence += unit.getNumber();
                continue;
            }
            if(unitType == artilleryType){
                artilleryBeforeIndependence += unit.getNumber();
                continue;
            }
            if(unitType == soldierType){
                switch(unit.getRole()){
                case SOLDIER:
                    soldiersBeforeIndependence += unit.getNumber();
                    break;
                case DRAGOON:
                    dragoonsBeforeIndependence += unit.getNumber();
                    break;
                default:
                    fail("Unkown REF role: " + unit.getRole().toString());
                    break;
                }
                continue;
            }
            fail("Unkown REF unit: " +  unit.toString());
        }

        // Execute
        List<Unit> refUnitsAfterIndependence = igc.createREFUnits(player1, refPlayer);

        // Get results
        int soldiersAfterIndependence = 0;
        int dragoonsAfterIndependence = 0;
        int artilleryAfterIndependence = 0;
        int shipsAfterIndependence = 0; 
        for(Unit unit : refUnitsAfterIndependence){
            UnitType unitType = unit.getType();
            if(unitType.hasAbility("model.ability.navalUnit")){
                shipsAfterIndependence++;
                continue;
            }
            if(unitType == artilleryType){
                artilleryAfterIndependence++;
                continue;
            }
            if(unitType == soldierType){
                switch(unit.getRole()){
                case SOLDIER:
                    soldiersAfterIndependence++;
                    break;
                case DRAGOON:
                    dragoonsAfterIndependence++;
                    break;
                default:
                    fail("Unkown REF role: " + unit.getRole().toString());
                    break;
                }
                continue;
            }
            fail("Unkown REF unit: " +  unit.toString());
        }

        // Verify results 
        assertEquals("Wrong number of ships",shipsBeforeIndependence,shipsAfterIndependence);
        assertEquals("Wrong number of artillery",artilleryBeforeIndependence,artilleryAfterIndependence);
        assertEquals("Wrong number of soldiers",soldiersBeforeIndependence,soldiersAfterIndependence);
        assertEquals("Wrong number of dragoons",dragoonsBeforeIndependence,dragoonsAfterIndependence);
    }
}
