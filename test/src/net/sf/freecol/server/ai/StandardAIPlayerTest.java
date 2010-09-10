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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
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
    final EquipmentType horsesEqType = spec().getEquipmentType("model.equipment.horses");
    final EquipmentType toolsEqType = spec().getEquipmentType("model.equipment.tools");
    final UnitType artilleryType = spec().getUnitType("model.unit.artillery");
    
    FreeColServer server = null;
	
    public void tearDown() throws Exception {
        if(server != null){
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;
        }
        super.tearDown();
    }
    
    private Game start(Map map) {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        server.setMapGenerator(new MockMapGenerator(map));
        PreGameController pgc = (PreGameController) server.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);
        return game;
    }


    public void testSwitchIndenturedServantInsideColonyWithFreeColonistSoldier(){
        Game game = start(getTestMap());
        AIMain aiMain = server.getAIMain();
        final UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
        final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        final EquipmentType musketsEqType = spec().getEquipmentType("model.equipment.muskets");
        final EquipmentType horsesEqType = spec().getEquipmentType("model.equipment.horses");
        
        FreeColTestUtils.ColonyBuilder builder = FreeColTestUtils.getColonyBuilder();
        builder.initialColonists(1).addColonist(indenturedServantType);
        Colony colony = builder.build();
        assertEquals("Wrong number of units in colony",1,colony.getUnitCount());
        
        Unit indenturedServant = colony.getUnitList().get(0);
        
        Unit freeColonist = new Unit(game, colony.getTile(), colony.getOwner(), colonistType, UnitState.ACTIVE, musketsEqType, horsesEqType);

        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(colony.getOwner());
        game.setCurrentPlayer(colony.getOwner());
        
        player.reOrganizeSoldiersOfColony(colony);
        
        assertEquals("Wrong number of units in colony",1,colony.getUnitCount());
        
        assertTrue("Indentured servant should now have horses", indenturedServant.getEquipmentCount(horsesEqType) == 1);
        assertTrue("Indentured servant should now have muskets",indenturedServant.getEquipmentCount(musketsEqType) == 1);
        
        assertFalse("Free colonist should not have horses",freeColonist.getEquipmentCount(horsesEqType) == 1);
        assertFalse("Free colonist should not have muskets",freeColonist.getEquipmentCount(musketsEqType) == 1);
    }
    
    public void testSwitchArmedFreeColonistSoldierEquipmentWithUnarmedExpertSoldierOutsideColony(){
        Game game = start(getTestMap());
        AIMain aiMain = server.getAIMain();
        
        Colony colony = getStandardColony(1);

        final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
        final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        final UnitType expertSoldierType = spec().getUnitType("model.unit.veteranSoldier");
        final EquipmentType musketsEqType = spec().getEquipmentType("model.equipment.muskets");
        final EquipmentType horsesEqType = spec().getEquipmentType("model.equipment.horses");

        colony.addGoods(horsesType, 10);
        
        Unit expertSoldier = new Unit(game, colony.getTile(), colony.getOwner(), expertSoldierType, UnitState.ACTIVE, new EquipmentType[0]);
        
        Unit freeColonist = new Unit(game, colony.getTile(), colony.getOwner(), colonistType, UnitState.ACTIVE, musketsEqType, horsesEqType);

        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(colony.getOwner());
        game.setCurrentPlayer(colony.getOwner());
        
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
        Game game = start(getTestMap());
        AIMain aiMain = server.getAIMain();

        Colony colony = getStandardColony(1);

        final GoodsType musketsType = spec().getGoodsType("model.goods.muskets");
        final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
        @SuppressWarnings("unused")
        final UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
        @SuppressWarnings("unused")
        final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        final UnitType expertSoldierType = spec().getUnitType("model.unit.veteranSoldier");
        final EquipmentType musketsEqType = spec().getEquipmentType("model.equipment.muskets");
        final EquipmentType horsesEqType = spec().getEquipmentType("model.equipment.horses");

        colony.addGoods(musketsType, 100);
        colony.addGoods(horsesType, 100);
        assertTrue("Colony should be hable to equip units with horses",colony.canBuildEquipment(horsesEqType));
        
        Unit expertSoldier = new Unit(game, colony.getTile(), colony.getOwner(), expertSoldierType,
                                      UnitState.ACTIVE, new EquipmentType[0]);

        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(colony.getOwner());
        game.setCurrentPlayer(colony.getOwner());

        assertTrue("Expert soldier should not have any equipment",expertSoldier.getEquipment().isEmpty());

        player.equipSoldiersOutsideColony(colony);

        assertTrue("Expert soldier should now have muskets",expertSoldier.getEquipmentCount(musketsEqType) == 1);
        assertEquals("Expert soldier should now have horses",1,expertSoldier.getEquipmentCount(horsesEqType));
    }

    public void testSwitchEquipmentWith(){
        Game game = start(getTestMap());
        AIMain aiMain = server.getAIMain();

        Colony colony1 = getStandardColony(1);
        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(colony1.getOwner());
        game.setCurrentPlayer(colony1.getOwner());
        Tile col1Tile = colony1.getTile();
        Tile otherTile = col1Tile.getAdjacentTile(Direction.N);

        Unit insideUnit1 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE, toolsEqType);
        Unit insideUnit2 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE, musketsEqType, horsesEqType);
        Unit insideUnit3 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE);
        Unit artillery = new Unit(game, col1Tile, colony1.getOwner(), artilleryType, UnitState.ACTIVE);

        Unit outsideUnit1 = new Unit(game, otherTile, colony1.getOwner(), colonistType, UnitState.ACTIVE, toolsEqType);
        Unit outsideUnit2 = new Unit(game, otherTile, colony1.getOwner(), colonistType, UnitState.ACTIVE, musketsEqType, horsesEqType);

        boolean exceptionThrown = false;
        try{
            player.switchEquipmentWith(insideUnit1, artillery);
        }
        catch(IllegalArgumentException e){
            exceptionThrown = true;
        }
        if(!exceptionThrown){
            fail("Colonist must not change equipment with a unit not also a colonist");
        }

        exceptionThrown = false;
        try{
            player.switchEquipmentWith(outsideUnit1, outsideUnit2);
        }
        catch(IllegalStateException e){
            exceptionThrown = true;
        }
        if(!exceptionThrown){
            fail("Colonists must not change equipment outside a settlement");
        }

        exceptionThrown = false;
        try{
            player.switchEquipmentWith(insideUnit1, outsideUnit1);
        }
        catch(IllegalStateException e){
            exceptionThrown = true;
        }
        if(!exceptionThrown){
            fail("Colonists must not change equipment when in diferent locations");
        }

        player.switchEquipmentWith(insideUnit1, insideUnit2);
        assertFalse("Unit1 should not have tools",insideUnit1.getEquipmentCount(toolsEqType) == 1);
        assertTrue("Unit1 should now have horses",insideUnit1.getEquipmentCount(horsesEqType) == 1);
        assertTrue("Unit1 should now have muskets",insideUnit1.getEquipmentCount(musketsEqType) == 1);

        assertTrue("Unit2 should now have tools",insideUnit2.getEquipmentCount(toolsEqType) == 1);
        assertFalse("Unit2 should not have horses",insideUnit2.getEquipmentCount(horsesEqType) == 1);
        assertFalse("Unit2 should not have muskets",insideUnit2.getEquipmentCount(musketsEqType) == 1);

        player.switchEquipmentWith(insideUnit3, insideUnit1);
        assertTrue("Unit1 should not have equipment",insideUnit1.getEquipment().isEmpty());
        assertTrue("Unit3 should now have horses",insideUnit3.getEquipmentCount(horsesEqType) == 1);
        assertTrue("Unit3 should now have muskets",insideUnit3.getEquipmentCount(musketsEqType) == 1);
    }

    public void testSwitchEquipmentWithUnitHavingSomeAlredy(){
        Game game = start(getTestMap());
        AIMain aiMain = server.getAIMain();

        Colony colony1 = getStandardColony(1);
        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(colony1.getOwner());
        game.setCurrentPlayer(colony1.getOwner());
        Tile col1Tile = colony1.getTile();

        Unit insideUnit1 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE, musketsEqType);
        Unit insideUnit2 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE, musketsEqType, horsesEqType);

        assertEquals("Unit1 should not have horses",0,insideUnit1.getEquipmentCount(horsesEqType));
        assertEquals("Unit1 should have muskets",1,insideUnit1.getEquipmentCount(musketsEqType));
        assertEquals("Unit2 should have horses",1,insideUnit2.getEquipmentCount(horsesEqType));
        assertEquals("Unit2 should have muskets",1,insideUnit2.getEquipmentCount(musketsEqType));

        player.switchEquipmentWith(insideUnit1, insideUnit2);
        assertEquals("Unit1 should now have horses",1,insideUnit1.getEquipmentCount(horsesEqType));
        assertEquals("Unit1 should now have muskets",1,insideUnit1.getEquipmentCount(musketsEqType));
        assertEquals("Unit2 should not have horses",0,insideUnit2.getEquipmentCount(horsesEqType));
        assertEquals("Unit2 should have muskets",1,insideUnit2.getEquipmentCount(musketsEqType));
    }

    public void testEquipBraves(){
        Game game = start(getTestMap());
        AIMain aiMain = server.getAIMain();

        GoodsType horsesType = spec().getGoodsType("model.goods.horses");
        GoodsType musketsType = spec().getGoodsType("model.goods.muskets");

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.initialBravesInCamp(3).build();
        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(camp.getOwner());
        game.setCurrentPlayer(camp.getOwner());

        int bravesToEquip = camp.getUnitCount();
        int horsesReqPerUnit = spec().getEquipmentType("model.equipment.indian.horses").getAmountRequiredOf(horsesType);
        int musketsReqPerUnit = spec().getEquipmentType("model.equipment.indian.muskets").getAmountRequiredOf(musketsType);
        int totalHorsesReq = bravesToEquip * horsesReqPerUnit;
        int totalMusketsReq = bravesToEquip * musketsReqPerUnit;
        int totalHorsesAvail = totalHorsesReq*2;
        int totalMusketsAvail = totalMusketsReq*2;

        // Verify initial conditions
        assertEquals("No horses should exist in camp",0,camp.getGoodsCount(horsesType));
        assertEquals("No muskets should exist in camp",0,camp.getGoodsCount(musketsType));

        for(Unit unit : camp.getUnitList()){
            if(unit.isMounted()){
                fail("Indian should not have mounted braves");
            }
            if(unit.isArmed()){
                fail("Indian should not have armed braves");
            }
        }

        // Setup
        camp.addGoods(horsesType,totalHorsesAvail);
        camp.addGoods(musketsType,totalMusketsAvail);

        assertEquals("Wrong initial number of horses in Indian camp",totalHorsesAvail,camp.getGoodsCount(horsesType));
        assertEquals("Wrong initial number of muskets in Indian camp",totalMusketsAvail,camp.getGoodsCount(musketsType));

        // Exercise SUT
        player.equipBraves(camp);

        // Verify results
        assertEquals("Wrong final number of horses in Indian camp",totalHorsesReq,camp.getGoodsCount(horsesType));
        assertEquals("Wrong final number of muskets in Indian camp",totalMusketsReq,camp.getGoodsCount(musketsType));

        int mounted = 0;
        int armed = 0;
        for(Unit unit : camp.getUnitList()){
            if(unit.isMounted()){
                mounted++;
            }
            if(unit.isArmed()){
                armed++;
            }
        }
        assertEquals("Wrong number of units armed",camp.getUnitCount(),armed);
        assertEquals("Wrong number of units mounted",camp.getUnitCount(),mounted);
    }

    public void testEquipBravesNotEnoughReqGoods(){
        Game game = start(getTestMap());
        AIMain aiMain = server.getAIMain();

        GoodsType horsesType = spec().getGoodsType("model.goods.horses");
        GoodsType musketsType = spec().getGoodsType("model.goods.muskets");

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.initialBravesInCamp(3).build();
        StandardAIPlayer player = (StandardAIPlayer) aiMain.getAIObject(camp.getOwner());
        game.setCurrentPlayer(camp.getOwner());

        int bravesToEquip = camp.getUnitCount() - 1;
        int horsesReqPerUnit = spec().getEquipmentType("model.equipment.indian.horses").getAmountRequiredOf(horsesType);
        int musketsReqPerUnit = spec().getEquipmentType("model.equipment.indian.muskets").getAmountRequiredOf(musketsType);
        int totalHorsesAvail = bravesToEquip * horsesReqPerUnit;
        int totalMusketsAvail = bravesToEquip * musketsReqPerUnit;

        // Verify initial conditions
        assertEquals("No horses should exist in camp",0,camp.getGoodsCount(horsesType));
        assertEquals("No muskets should exist in camp",0,camp.getGoodsCount(musketsType));

        for(Unit unit : camp.getUnitList()){
            if(unit.isMounted()){
                fail("Indian should not have mounted braves");
            }
            if(unit.isArmed()){
                fail("Indian should not have armed braves");
            }
        }

        // Setup
        camp.addGoods(horsesType,totalHorsesAvail);
        camp.addGoods(musketsType,totalMusketsAvail);

        assertEquals("Wrong initial number of horses in Indian camp",totalHorsesAvail,camp.getGoodsCount(horsesType));
        assertEquals("Wrong initial number of muskets in Indian camp",totalMusketsAvail,camp.getGoodsCount(musketsType));

        // Exercise SUT
        player.equipBraves(camp);

        // Verify results
        assertEquals("Wrong final number of horses in Indian camp",0,camp.getGoodsCount(horsesType));
        assertEquals("Wrong final number of muskets in Indian camp",0,camp.getGoodsCount(musketsType));

        int mounted = 0;
        int armed = 0;
        for(Unit unit : camp.getUnitList()){
            if(unit.isMounted()){
                mounted++;
            }
            if(unit.isArmed()){
                armed++;
            }
        }
        assertEquals("Wrong number of units armed",bravesToEquip,armed);
        assertEquals("Wrong number of units mounted",bravesToEquip,mounted);
    }

}

