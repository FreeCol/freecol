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

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;
import net.sf.freecol.util.test.MockMapGenerator;

public class StandardAIPlayerTest extends FreeColTestCase {
    final GoodsType musketsType = spec().getGoodsType("model.goods.muskets");
    final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
    final UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
    final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    final UnitType expertSoldierType = spec().getUnitType("model.unit.veteranSoldier");
    final EquipmentType musketsEqType = spec().getEquipmentType("model.equipment.muskets");
    final EquipmentType horsesEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");
    
    FreeColServer server = null;
	
    public void tearDown() throws Exception {
        if(server != null){
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;
        }
        super.tearDown();
    }
    
    public void testSwitchIndenturedServantInsideColonyWithFreeColonistSoldier(){
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
        final GoodsType musketsType = spec().getGoodsType("model.goods.muskets");
        final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
        final UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
        final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        final UnitType expertSoldierType = spec().getUnitType("model.unit.veteranSoldier");
        final EquipmentType musketsEqType = spec().getEquipmentType("model.equipment.muskets");
        final EquipmentType horsesEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");
        
        FreeColTestUtils.ColonyBuilder builder = FreeColTestUtils.getColonyBuilder();
        builder.initialColonists(1).addColonist(indenturedServantType);
        Colony colony = builder.build();
        assertEquals("Wrong number of units in colony",1,colony.getUnitCount());
        
        Unit indenturedServant = colony.getUnitList().get(0);
        
        Unit freeColonist = new Unit(game, colony.getTile(), colony.getOwner(), colonistType, UnitState.ACTIVE, musketsEqType, horsesEqType);

        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(colony.getOwner());
        
        player.reOrganizeSoldiersOfColony(colony);
        
        assertEquals("Wrong number of units in colony",1,colony.getUnitCount());
        
        assertTrue("Indentured servant should now have horses", indenturedServant.getEquipmentCount(horsesEqType) == 1);
        assertTrue("Indentured servant should now have muskets",indenturedServant.getEquipmentCount(musketsEqType) == 1);
        
        assertFalse("Free colonist should not have horses",freeColonist.getEquipmentCount(horsesEqType) == 1);
        assertFalse("Free colonist should not have muskets",freeColonist.getEquipmentCount(musketsEqType) == 1);
    }
    
    public void testSwitchArmedFreeColonistSoldierEquipmentWithUnarmedExpertSoldierOutsideColony(){
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
        
        Colony colony = getStandardColony(1);

        final GoodsType musketsType = spec().getGoodsType("model.goods.muskets");
        final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
        final UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
        final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        final UnitType expertSoldierType = spec().getUnitType("model.unit.veteranSoldier");
        final EquipmentType musketsEqType = spec().getEquipmentType("model.equipment.muskets");
        final EquipmentType horsesEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");

        colony.addGoods(horsesType, 10);
        
        Unit expertSoldier = new Unit(game, colony.getTile(), colony.getOwner(), expertSoldierType, UnitState.ACTIVE, new EquipmentType[0]);
        
        Unit freeColonist = new Unit(game, colony.getTile(), colony.getOwner(), colonistType, UnitState.ACTIVE, musketsEqType, horsesEqType);

        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(colony.getOwner());
        
        assertTrue("Free colonist should have horses",freeColonist.getEquipmentCount(horsesEqType) == 1);
        assertTrue("Free colonist should have muskets",freeColonist.getEquipmentCount(musketsEqType) == 1);
        
        assertFalse("Expert soldier should not have muskets yet",expertSoldier.getEquipmentCount(musketsEqType) == 1);
        assertFalse("Expert soldier should not have horses yet", expertSoldier.getEquipmentCount(horsesEqType) == 1);
        
        player.reOrganizeSoldiersOfColony(colony);
        
        assertTrue("Expert soldier should now have muskets",expertSoldier.getEquipmentCount(musketsEqType) == 1);
        assertTrue("Expert soldier should now have horses", expertSoldier.getEquipmentCount(horsesEqType) == 1);
        
        assertFalse("Free colonist should not have horses",freeColonist.getEquipmentCount(horsesEqType) == 1);
        assertFalse("Free colonist should not have muskets",freeColonist.getEquipmentCount(musketsEqType) == 1);
    }
    
    public void testEquipExpertSoldiersOutsideColony(){

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

        Colony colony = getStandardColony(1);

        final GoodsType musketsType = spec().getGoodsType("model.goods.muskets");
        final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
        final UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
        final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        final UnitType expertSoldierType = spec().getUnitType("model.unit.veteranSoldier");
        final EquipmentType musketsEqType = spec().getEquipmentType("model.equipment.muskets");
        final EquipmentType horsesEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");

        colony.addGoods(musketsType, 100);
        colony.addGoods(horsesType, 100);
        assertTrue("Colony should be hable to equip units with horses",colony.canBuildEquipment(horsesEqType));
        
        Unit expertSoldier = new Unit(game, colony.getTile(), colony.getOwner(), expertSoldierType,
                                      UnitState.ACTIVE, new EquipmentType[0]);

        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(colony.getOwner());

        assertTrue("Expert soldier should not have any equipment",expertSoldier.getEquipment().isEmpty());

        player.equipSoldiersOutsideColony(colony);

        assertTrue("Expert soldier should now have muskets",expertSoldier.getEquipmentCount(musketsEqType) == 1);
        assertEquals("Expert soldier should now have horses",1,expertSoldier.getEquipmentCount(horsesEqType));
    }
}
