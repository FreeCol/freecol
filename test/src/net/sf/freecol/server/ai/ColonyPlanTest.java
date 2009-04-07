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

import java.util.Iterator;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class ColonyPlanTest extends FreeColTestCase {	
	UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
		
	FreeColServer server = null;
	
	public void tearDown() throws Exception {
		if(server != null){
			// must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;
		}
		super.tearDown();
	}
	
	public void testPlanFoodProductionBeforeWorkerAllocation() {
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
        
        FreeColTestCase.setGame(game);
        
        AIMain aiMain = server.getAIMain();
        	
        Colony colony = getStandardColony();
        
        ColonyPlan plan = new ColonyPlan(aiMain,colony);
        
        // get food production of central colony tile
        int expAmount = 0;
        for (GoodsType foodType : FreeCol.getSpecification().getGoodsFood()) {
            expAmount += colony.getTile().getMaximumPotential(foodType, null);
        }
        
        int amount = plan.getFoodProduction();
        
        assertEquals("Wrong initial food ammount",expAmount,amount);
	}
}
