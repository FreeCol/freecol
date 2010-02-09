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

import java.lang.reflect.Method;

import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.util.test.FreeColTestCase;

public class DemotionTest extends FreeColTestCase {

    TileType plains = spec().getTileType("model.tile.plains");

    UnitType braveType = spec().getUnitType("model.unit.brave");
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    UnitType veteranType = spec().getUnitType("model.unit.veteranSoldier");
    UnitType artilleryType = spec().getUnitType("model.unit.artillery");
    UnitType damagedArtilleryType = spec().getUnitType("model.unit.damagedArtillery");
    UnitType colonialRegularType = spec().getUnitType("model.unit.colonialRegular");
    UnitType kingsRegularType = spec().getUnitType("model.unit.kingsRegular");
    UnitType indianConvertType = spec().getUnitType("model.unit.indianConvert");
    UnitType pettyCriminalType = spec().getUnitType("model.unit.pettyCriminal");
    UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");

    EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
    EquipmentType horses = spec().getEquipmentType("model.equipment.horses");
    EquipmentType indianMuskets = spec().getEquipmentType("model.equipment.indian.muskets");
    EquipmentType indianHorses = spec().getEquipmentType("model.equipment.indian.horses");

    public void testColonistDemotedBySoldier() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Method method = SimpleCombatModel.class.getDeclaredMethod("loseCombat", Unit.class, Unit.class);
        method.setAccessible(true);
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);
        assertTrue(colonist.hasAbility("model.ability.canBeCaptured"));
        Unit soldier = new Unit(game, tile2, french, colonistType, UnitState.ACTIVE);
        assertTrue(soldier.hasAbility("model.ability.canBeCaptured"));
        soldier.equipWith(muskets, true);
        assertFalse(soldier.hasAbility("model.ability.canBeCaptured"));

        method.invoke(combatModel, colonist, soldier);
        assertEquals(colonistType, colonist.getType());
        assertEquals(french, colonist.getOwner());
        assertEquals(tile2, colonist.getTile());

    }

    public void testSoldierDemotedBySoldier() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Method method = SimpleCombatModel.class.getDeclaredMethod("loseCombat", Unit.class, Unit.class);
        method.setAccessible(true);
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit soldier1 = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);
        soldier1.equipWith(muskets, true);
        Unit soldier2 = new Unit(game, tile2, french, colonistType, UnitState.ACTIVE);
        soldier2.equipWith(muskets, true);

        method.invoke(combatModel, soldier1, soldier2);
        assertEquals(colonistType, soldier1.getType());
        assertEquals(dutch, soldier1.getOwner());
        assertEquals(tile1, soldier1.getTile());
        assertTrue(soldier1.getEquipment().isEmpty());

        method.invoke(combatModel, soldier1, soldier2);
        assertEquals(colonistType, soldier1.getType());
        assertEquals(french, soldier1.getOwner());
        assertEquals(tile2, soldier1.getTile());
    }

    public void testDragoonDemotedBySoldier() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Method method = SimpleCombatModel.class.getDeclaredMethod("loseCombat", Unit.class, Unit.class);
        method.setAccessible(true);
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit dragoon = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);
        dragoon.equipWith(muskets, true);
        dragoon.equipWith(horses, true);
        dragoon.newTurn();
        assertEquals(12, dragoon.getInitialMovesLeft());
        assertEquals(12, dragoon.getMovesLeft());
        Unit soldier = new Unit(game, tile2, french, colonistType, UnitState.ACTIVE);
        soldier.equipWith(muskets, true);

        method.invoke(combatModel, dragoon, soldier);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(dutch, dragoon.getOwner());
        assertEquals(tile1, dragoon.getTile());
        assertEquals(1, dragoon.getEquipment().size());
        assertEquals(1, dragoon.getEquipment().getCount(muskets));
        assertEquals(3, dragoon.getInitialMovesLeft());
        assertEquals(3, dragoon.getMovesLeft());

        method.invoke(combatModel, dragoon, soldier);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(dutch, dragoon.getOwner());
        assertEquals(tile1, dragoon.getTile());
        assertTrue(dragoon.getEquipment().isEmpty());

        method.invoke(combatModel, dragoon, soldier);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(french, dragoon.getOwner());
        assertEquals(tile2, dragoon.getTile());
    }

    public void testDragoonDemotedByBrave() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Method method = SimpleCombatModel.class.getDeclaredMethod("loseCombat", Unit.class, Unit.class);
        method.setAccessible(true);
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");
        Map map = getTestMap(plains);
        game.setMap(map);

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(inca, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        // build indian settlements
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.player(inca).settlementTile(map.getTile(1, 1)).capital(true).skillToTeach(null);
        IndianSettlement settlement1 = builder.build();
        builder.reset().player(inca).settlementTile(map.getTile(8, 8)).skillToTeach(null);
        IndianSettlement settlement2 = builder.build();

        Unit dragoon = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);
        dragoon.equipWith(muskets, true);
        dragoon.equipWith(horses, true);
        Unit brave = new Unit(game, tile2, inca, braveType, UnitState.ACTIVE);
        brave.setIndianSettlement(settlement1);

        GoodsType musketType = spec().getGoodsType("model.goods.muskets");
        GoodsType horsesType = spec().getGoodsType("model.goods.horses");

        method.invoke(combatModel, dragoon, brave);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(dutch, dragoon.getOwner());
        assertEquals(tile1, dragoon.getTile());
        assertEquals(1, dragoon.getEquipment().size());
        assertEquals(1, dragoon.getEquipment().getCount(muskets));
        assertEquals(1, brave.getEquipment().size());
        assertEquals(1, brave.getEquipment().getCount(indianHorses));
        assertEquals(50, settlement1.getGoodsCount(horsesType));
        assertEquals(0, settlement2.getGoodsCount(horsesType));

        method.invoke(combatModel, dragoon, brave);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(dutch, dragoon.getOwner());
        assertEquals(tile1, dragoon.getTile());
        assertTrue(dragoon.getEquipment().isEmpty());
        assertEquals(2, brave.getEquipment().size());
        assertEquals(1, brave.getEquipment().getCount(indianHorses));
        assertEquals(1, brave.getEquipment().getCount(indianMuskets));
        assertEquals(50, settlement1.getGoodsCount(musketType));
        assertEquals(0, settlement2.getGoodsCount(musketType));

        method.invoke(combatModel, dragoon, brave);
        assertTrue(dragoon.isDisposed());

    }

    public void testScoutDemotedBySoldier() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Method method = SimpleCombatModel.class.getDeclaredMethod("loseCombat", Unit.class, Unit.class);
        method.setAccessible(true);
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit scout = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);
        scout.equipWith(horses, true);
        Unit soldier = new Unit(game, tile2, french, colonistType, UnitState.ACTIVE);
        soldier.equipWith(muskets, true);

        method.invoke(combatModel, scout, soldier);
        scout.isDisposed();
    }

    public void testVeteranSoldierDemotedBySoldier() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Method method = SimpleCombatModel.class.getDeclaredMethod("loseCombat", Unit.class, Unit.class);
        method.setAccessible(true);
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        assertEquals(colonistType, veteranType.getUnitTypeChange(ChangeType.CAPTURE, dutch));

        Unit soldier1 = new Unit(game, tile1, dutch, veteranType, UnitState.ACTIVE);
        soldier1.equipWith(muskets, true);
        Unit soldier2 = new Unit(game, tile2, french, colonistType, UnitState.ACTIVE);
        soldier2.equipWith(muskets, true);

        method.invoke(combatModel, soldier1, soldier2);
        assertEquals(veteranType, soldier1.getType());
        assertEquals(dutch, soldier1.getOwner());
        assertEquals(tile1, soldier1.getTile());
        assertTrue(soldier1.getEquipment().isEmpty());

        method.invoke(combatModel, soldier1, soldier2);
        assertEquals(colonistType, soldier1.getType());
        assertEquals(french, soldier1.getOwner());
        assertEquals(tile2, soldier1.getTile());
    }

    public void testArtilleryDemotedBySoldier() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Method method = SimpleCombatModel.class.getDeclaredMethod("loseCombat", Unit.class, Unit.class);
        method.setAccessible(true);
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        assertEquals(damagedArtilleryType, artilleryType.getUnitTypeChange(ChangeType.DEMOTION, dutch));

        Unit artillery = new Unit(game, tile1, dutch, artilleryType, UnitState.ACTIVE);
        Unit soldier = new Unit(game, tile2, french, colonistType, UnitState.ACTIVE);
        soldier.equipWith(muskets, true);

        method.invoke(combatModel, artillery, soldier);
        assertEquals(damagedArtilleryType, artillery.getType());
        assertEquals(dutch, artillery.getOwner());
        assertEquals(tile1, artillery.getTile());

        method.invoke(combatModel, artillery, soldier);
        assertTrue(artillery.isDisposed());
    }

    public void testPromotion() throws Exception {
        
        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Method promoteMethod = SimpleCombatModel.class.getDeclaredMethod("promote", Unit.class);
        promoteMethod.setAccessible(true);
        Player player = game.getPlayer("model.nation.dutch");

        // UnitType promotion
        assertEquals(indenturedServantType, pettyCriminalType.getUnitTypeChange(ChangeType.PROMOTION, player));
        assertEquals(colonistType, indenturedServantType.getUnitTypeChange(ChangeType.PROMOTION, player));
        assertEquals(veteranType, colonistType.getUnitTypeChange(ChangeType.PROMOTION, player));
        // only independent players can own colonial regulars
        assertEquals(null, veteranType.getUnitTypeChange(ChangeType.PROMOTION, player));
        assertEquals(null, colonialRegularType.getUnitTypeChange(ChangeType.PROMOTION, player));
        assertEquals(null, artilleryType.getUnitTypeChange(ChangeType.PROMOTION, player));
        assertEquals(null, kingsRegularType.getUnitTypeChange(ChangeType.PROMOTION, player));
        assertEquals(null, indianConvertType.getUnitTypeChange(ChangeType.PROMOTION, player));
        
        // Unit promotion
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(player, true);
        Unit unit = new Unit(game, tile1, player, pettyCriminalType, UnitState.ACTIVE);
        promoteMethod.invoke(combatModel, unit);
        assertEquals(unit.getType(), indenturedServantType);
        promoteMethod.invoke(combatModel, unit);
        assertEquals(unit.getType(), colonistType);
        promoteMethod.invoke(combatModel, unit);
        assertEquals(unit.getType(), veteranType);

        // further upgrading a VeteranSoldier to ColonialRegular
        // should only work once independence is declared
        assertFalse(colonialRegularType.isAvailableTo(player));
        promoteMethod.invoke(combatModel, unit);
        assertEquals(unit.getType(), veteranType);

        player.setPlayerType(PlayerType.REBEL);
        player.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));
        assertTrue(colonialRegularType.isAvailableTo(player));
        promoteMethod.invoke(combatModel, unit);
        assertEquals(unit.getType(), colonialRegularType);

        // only independent players can own colonial regulars
        player.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));
        assertEquals(colonialRegularType, veteranType.getUnitTypeChange(ChangeType.PROMOTION, player));
    }
}
